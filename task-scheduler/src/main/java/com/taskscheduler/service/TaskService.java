package com.taskscheduler.service;

import com.taskscheduler.model.*;
import com.taskscheduler.websocket.WebSocketService;
import com.taskscheduler.repository.*;
import lombok.RequiredArgsConstructor;
import com.taskscheduler.model.Task;
import com.taskscheduler.repository.TaskRepository;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final MetricsService metricsService;
    private final WebSocketService webSocketService;
    
    private final ConcurrentHashMap<Long, Thread> runningTasks = new ConcurrentHashMap<>();

    @Transactional
    public Task createTask(Task task) {
        validateTask(task);
        task.setStatus(Task.TaskStatus.PENDING);
        task.setScheduledTime(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);
        webSocketService.notifyTaskUpdate(savedTask);
        return savedTask;
    }

    @Transactional
    public void executeTask(Task task) {
        if (!canExecuteTask(task)) {
            throw new IllegalStateException("Task cannot be executed at this time");
        }

        taskExecutor.execute(() -> {
            Thread currentThread = Thread.currentThread();
            runningTasks.put(task.getId(), currentThread);
            
            try {
                task.setStatus(Task.TaskStatus.RUNNING);
                taskRepository.save(task);
                webSocketService.notifyTaskUpdate(task);
                
                // Simulate task execution
                processTask(task);
                
                task.setStatus(Task.TaskStatus.COMPLETED);
                task.setCompletedTime(LocalDateTime.now());
                taskRepository.save(task);
                
                metricsService.recordTaskCompletion(task);
                webSocketService.notifyTaskUpdate(task);
            } catch (InterruptedException e) {
                handleTaskInterruption(task);
            } catch (Exception e) {
                handleTaskFailure(task, e);
            } finally {
                runningTasks.remove(task.getId());
            }
        });
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, Task.TaskStatus newStatus) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
        if (newStatus == Task.TaskStatus.PAUSED) {
            pauseTask(task);
        } else if (newStatus == Task.TaskStatus.RUNNING) {
            resumeTask(task);
        } else if (newStatus == Task.TaskStatus.CANCELLED) {
            cancelTask(task);
        }
        
        return task;
    }

    private void processTask(Task task) throws InterruptedException {
        // Simulate work with periodic interruption checks
        int totalSteps = 10;
        for (int i = 0; i < totalSteps; i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            // Simulated work
            Thread.sleep(1000);
            
            // Update progress
            webSocketService.notifyTaskProgress(task.getId(), (i + 1) * 100 / totalSteps);
        }
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    private void pauseTask(Task task) {
        Thread taskThread = runningTasks.get(task.getId());
        if (taskThread != null) {
            taskThread.interrupt();
        }
        task.setStatus(Task.TaskStatus.PAUSED);
        taskRepository.save(task);
        webSocketService.notifyTaskUpdate(task);
    }

    private void resumeTask(Task task) {
        if (task.getStatus() == Task.TaskStatus.PAUSED) {
            task.setStatus(Task.TaskStatus.PENDING);
            taskRepository.save(task);
            executeTask(task);
        }
    }

    private void cancelTask(Task task) {
        Thread taskThread = runningTasks.get(task.getId());
        if (taskThread != null) {
            taskThread.interrupt();
        }
        task.setStatus(Task.TaskStatus.CANCELLED);
        taskRepository.save(task);
        webSocketService.notifyTaskUpdate(task);
    }

    private void handleTaskInterruption(Task task) {
        task.setStatus(Task.TaskStatus.PAUSED);
        taskRepository.save(task);
        webSocketService.notifyTaskUpdate(task);
    }

    private void handleTaskFailure(Task task, Exception e) {
        task.setStatus(Task.TaskStatus.FAILED);
        taskRepository.save(task);
        metricsService.recordTaskFailure(task);
        webSocketService.notifyTaskUpdate(task);
        webSocketService.notifyTaskError(task.getId(), e.getMessage());
    }

    private boolean canExecuteTask(Task task) {
        if (task.getDependentTask() != null) {
            return task.getDependentTask().getStatus() == Task.TaskStatus.COMPLETED;
        }
        return true;
    }

    private void validateTask(Task task) {
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }
        if (task.getPriority() == null) {
            throw new IllegalArgumentException("Task priority is required");
        }
    }
}
