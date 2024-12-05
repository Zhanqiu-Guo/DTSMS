package com.scheduler.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scheduler.common.dto.TaskExecutionRequest;
import com.scheduler.common.dto.TaskExecutionResult;
import com.scheduler.common.model.Task;
import com.scheduler.core.client.ExecutorClient;
import com.scheduler.core.metrics.TaskMetrics;
import com.scheduler.core.repository.TaskRepository;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ExecutorClient executorClient;
    
    @Autowired
    private TaskMetrics taskMetrics;

    @Transactional
    public Task createTask(Task task) {
        task.setStatus(Task.TaskStatus.CREATED);
        task.setCreatedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);
        taskMetrics.incrementTasksCreated();
        
        // Async execution
        scheduleTask(savedTask);
        
        return savedTask;
    }

    @Async("taskExecutor")
    public CompletableFuture<TaskExecutionResult> scheduleTask(Task task) {
        log.info("Scheduling task: {}", task.getId());
        taskMetrics.incrementActiveTasks();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                task.setStatus(Task.TaskStatus.RUNNING);
                taskRepository.save(task);

                Timer.Sample sample = Timer.start();
                
                TaskExecutionResult result = executorClient.executeTask(
                    TaskExecutionRequest.builder()
                        .taskId(task.getId())
                        .handlerClass(task.getHandlerClass())
                        .parameters(task.getParameters())
                        .build()
                );

                sample.stop(taskMetrics.getTaskExecutionTimer());
                
                handleTaskCompletion(task, result);
                return result;
            } catch (Exception e) {
                log.error("Task execution failed", e);
                handleTaskError(task, e);
                throw e;
            }
        });
    }

    @Scheduled(fixedRate = 5000)
    public void checkPendingTasks() {
        List<Task> pendingTasks = taskRepository.findByStatus(Task.TaskStatus.CREATED);
        pendingTasks.forEach(this::scheduleTask);
    }

    private void handleTaskCompletion(Task task, TaskExecutionResult result) {
        if (result.isSuccess()) {
            task.setStatus(Task.TaskStatus.COMPLETED);
            task.setLastExecutionTime(LocalDateTime.now());
            taskMetrics.incrementTasksCompleted();
        } else {
            handleTaskError(task, new RuntimeException(result.getErrorMessage()));
        }
        taskRepository.save(task);
    }

    private void handleTaskError(Task task, Exception error) {
        task.setRetryCount(task.getRetryCount() + 1);
        if (task.getRetryCount() >= task.getMaxRetries()) {
            task.setStatus(Task.TaskStatus.FAILED);
            taskMetrics.incrementTasksFailed();
        } else {
            long delayMinutes = (long) Math.pow(2, task.getRetryCount());
            task.setNextExecutionTime(LocalDateTime.now().plusMinutes(delayMinutes));
            task.setStatus(Task.TaskStatus.CREATED);
        }
        taskRepository.save(task);
    }
}