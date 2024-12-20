package com.taskscheduler.service;

import com.taskscheduler.model.Task;
import com.taskscheduler.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final MetricsService metricsService;
    
    private final PriorityBlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>(
        11,
        Comparator
            .<Task>comparingInt(task -> task.getPriority().ordinal())
            .thenComparing(Task::getScheduledTime)
    );

    @Scheduled(fixedRate = 1000) // Check every second
    @Transactional
    public void scheduleTasksExecution() {
        List<Task> executableTasks = taskRepository.findExecutableTasks();
        
        for (Task task : executableTasks) {
            if (shouldScheduleTask(task)) {
                taskQueue.offer(task);
            }
        }

        processPendingTasks();
    }

    @Transactional
    public void scheduleTask(Task task) {
        if (task.getScheduledTime() == null) {
            task.setScheduledTime(LocalDateTime.now());
        }
        taskQueue.offer(task);
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void cleanupStuckTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        
        List<Task> stuckTasks = taskRepository.findByStatusAndScheduledTimeBefore(
            Task.TaskStatus.RUNNING,
            threshold
        );
        
        for (Task task : stuckTasks) {
            task.setStatus(Task.TaskStatus.FAILED);
            taskRepository.save(task);
            metricsService.recordTaskFailure(task);
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    @Transactional
    public void archiveOldTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<Task> oldCompletedTasks = taskRepository.findByStatusAndCompletedTimeBefore(
            Task.TaskStatus.COMPLETED,
            threshold
        );
        
        // Archive tasks (could move to archive table or external storage)
        for (Task task : oldCompletedTasks) {
            task.setStatus(Task.TaskStatus.ARCHIVED);
            taskRepository.save(task);
        }
    }

    private void processPendingTasks() {
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            if (task != null && shouldExecuteTask(task)) {
                taskService.executeTask(task);
            }
        }
    }

    private boolean shouldScheduleTask(Task task) {
        return task.getStatus() == Task.TaskStatus.PENDING &&
               (task.getScheduledTime() == null || 
                !task.getScheduledTime().isAfter(LocalDateTime.now()));
    }

    private boolean shouldExecuteTask(Task task) {
        // Check if task is still in PENDING state (might have changed since being queued)
        Task freshTask = taskRepository.findById(task.getId()).orElse(null);
        if (freshTask == null || freshTask.getStatus() != Task.TaskStatus.PENDING) {
            return false;
        }

        // Check dependencies
        if (freshTask.getDependentTask() != null && 
            freshTask.getDependentTask().getStatus() != Task.TaskStatus.COMPLETED) {
            return false;
        }

        // Check scheduling time
        return freshTask.getScheduledTime() == null || 
               !freshTask.getScheduledTime().isAfter(LocalDateTime.now());
    }

    public List<Task> getScheduledTasks() {
        return taskRepository.findByStatus(Task.TaskStatus.PENDING);
    }

    public void cancelScheduledTask(Long taskId) {
        taskQueue.removeIf(task -> task.getId().equals(taskId));
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
        if (task.getStatus() == Task.TaskStatus.PENDING) {
            task.setStatus(Task.TaskStatus.CANCELLED);
            taskRepository.save(task);
        }
    }

    public void rescheduleTask(Long taskId, LocalDateTime newScheduledTime) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
        if (task.getStatus() != Task.TaskStatus.COMPLETED && 
            task.getStatus() != Task.TaskStatus.CANCELLED) {
            task.setScheduledTime(newScheduledTime);
            task.setStatus(Task.TaskStatus.PENDING);
            taskRepository.save(task);
            taskQueue.offer(task);
        }
    }
}
