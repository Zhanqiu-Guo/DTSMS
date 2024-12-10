package com.taskscheduler.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskscheduler.model.Task;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.websocket.WebSocketService;

@Service
public class SchedulerService {
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final MetricsService metricsService;
    private final WebSocketService webSocketService;
    
    private final PriorityBlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>(
        11,
        Comparator
            .<Task>comparingInt(task -> task.getPriority().ordinal())
            .thenComparing(Task::getScheduledTime)
    );

    // Assume total thread available is num of cpu cores, so that to achieve the best perf
    private final int totalThreads = Runtime.getRuntime().availableProcessors();
    private int availableThreads = totalThreads;
    // thread lock
    private final Object threadLock = new Object();

    // Constructor
    public SchedulerService(TaskService taskService, TaskRepository taskRepository, MetricsService metricsService, WebSocketService webSocketService) {
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.metricsService = metricsService;
        this.webSocketService = webSocketService;
    }

    // Create and save task
    @Transactional
    public Task createTask(Task task) {
        
        // Check thread and task
        int MAX_THREADS = Runtime.getRuntime().availableProcessors();
        if (task.getThreadsNeeded() > MAX_THREADS) {
            throw new IllegalArgumentException("Task requires too many threads. Maximum allowed: " + MAX_THREADS);
        }
        else if (task.getThreadsNeeded() < 1) {
            throw new IllegalArgumentException("Task should use more than 1 thread!");
        }
        validateTask(task);

        // Task is pending
        task.setStatus(Task.TaskStatus.PENDING);
        task.setScheduledTime(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);
        scheduleTask(savedTask);
        webSocketService.notifyTaskUpdate(savedTask);
        return savedTask;
    }

    private void validateTask(Task task) {
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }
        if (task.getPriority() == null) {
            throw new IllegalArgumentException("Task priority is required");
        }
    }

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

    private void processPendingTasks() {
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.peek();
            if (task == null) break;
    
            synchronized (threadLock) {
                if (task.getThreadsNeeded() <= availableThreads) {
                    taskQueue.poll();
                    availableThreads -= task.getThreadsNeeded();
                } else {
                    // Not enough threads for this task, wait
                    break;
                }
            }
            if (shouldExecuteTask(task)) {
                taskService.executeTask(task, () -> onTaskCompletion(task));
            }
        }
    }

    // Add back threads
    private void onTaskCompletion(Task task) {
        synchronized (threadLock) {
            availableThreads += task.getThreadsNeeded();
        }
    
        processPendingTasks();
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
}
