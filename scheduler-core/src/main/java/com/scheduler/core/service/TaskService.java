package com.scheduler.core.service;

import com.scheduler.common.model.Task;
import com.scheduler.common.event.TaskEvent;
import com.scheduler.core.repository.TaskRepository;
import com.scheduler.core.metrics.TaskMetrics;
import com.scheduler.core.exception.TaskNotFoundException;
import com.scheduler.core.exception.TaskValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.support.CronExpression;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Slf4j
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private KafkaTemplate<String, TaskEvent> kafkaTemplate;

    @Autowired
    private TaskMetrics taskMetrics;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Creates a new task with validation
     */
    @Transactional
    public Task createTask(Task task) {
        validateTask(task);
        
        task.setStatus(Task.TaskStatus.CREATED);
        task.setRetryCount(0);
        // createdAt will be set by @PrePersist
        
        if (task.getMaxRetries() == null) {
            task.setMaxRetries(3); // Default retry count
        }

        // Calculate next execution time based on cron
        try {
            CronExpression cronExpression = CronExpression.parse(task.getCronExpression());
            task.setNextExecutionTime(cronExpression.next(LocalDateTime.now()));
        } catch (Exception e) {
            throw new TaskValidationException("Invalid cron expression: " + task.getCronExpression());
        }

        Task savedTask = taskRepository.save(task);
        
        // Send task created event
        TaskEvent event = new TaskEvent();
        event.setTaskId(savedTask.getId());
        event.setEventType("TASK_CREATED");
        event.setTimestamp(LocalDateTime.now());
        kafkaTemplate.send("task-events", event);

        taskMetrics.incrementTasksCreated();
        log.info("Created task: {}", savedTask.getId());
        
        return savedTask;
    }

    /**
     * Schedules tasks for execution every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void scheduleTasks() {
        log.debug("Checking for tasks to schedule...");
        
        LocalDateTime now = LocalDateTime.now();
        
        List<Task> tasksToSchedule = taskRepository.findByStatusInAndNextExecutionTimeBefore(
            List.of(Task.TaskStatus.CREATED, Task.TaskStatus.SCHEDULED),
            now
        );

        for (Task task : tasksToSchedule) {
            try {
                scheduleTask(task);
            } catch (Exception e) {
                log.error("Error scheduling task {}: {}", task.getId(), e.getMessage());
                handleTaskError(task, e);
            }
        }
    }

    /**
     * Schedules a single task for execution
     */
    @Transactional
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void scheduleTask(Task task) {
        log.debug("Scheduling task: {}", task.getId());

        // Update task status
        task.setStatus(Task.TaskStatus.SCHEDULED);
        
        // Calculate next execution time
        CronExpression cronExpression = CronExpression.parse(task.getCronExpression());
        task.setNextExecutionTime(cronExpression.next(LocalDateTime.now()));
        
        taskRepository.save(task);

        // Send execution event
        TaskEvent event = new TaskEvent();
        event.setTaskId(task.getId());
        event.setEventType("TASK_SCHEDULED");
        event.setTimestamp(LocalDateTime.now());
        event.setDetails(task.getParameters());
        
        kafkaTemplate.send("task-execution", event);
        
        log.info("Task scheduled: {}", task.getId());
    }

    /**
     * Handles task execution results
     */
    @Transactional
    public void handleTaskCompletion(Long taskId, boolean success, String result) {
        Task task = getTaskById(taskId);
        
        if (success) {
            task.setStatus(Task.TaskStatus.COMPLETED);
            task.setLastExecutionTime(LocalDateTime.now());
            task.setRetryCount(0);
            taskMetrics.incrementTasksCompleted();
            
            // Calculate next execution if it's a recurring task
            try {
                CronExpression cronExpression = CronExpression.parse(task.getCronExpression());
                task.setNextExecutionTime(cronExpression.next(LocalDateTime.now()));
                task.setStatus(Task.TaskStatus.CREATED); // Reset for next execution
            } catch (Exception e) {
                log.error("Error calculating next execution time for task {}", taskId, e);
            }
        } else {
            handleTaskError(task, new RuntimeException(result));
        }

        taskRepository.save(task);
    }

    /**
     * Handles task execution errors
     */
    private void handleTaskError(Task task, Exception error) {
        task.setRetryCount(task.getRetryCount() + 1);
        
        if (task.getRetryCount() >= task.getMaxRetries()) {
            task.setStatus(Task.TaskStatus.FAILED);
            taskMetrics.incrementTasksFailed();
            log.error("Task {} failed after {} retries", task.getId(), task.getRetryCount());
        } else {
            // Exponential backoff for retries
            long delayMinutes = (long) Math.pow(2, task.getRetryCount());
            task.setNextExecutionTime(LocalDateTime.now().plusMinutes(delayMinutes));
            task.setStatus(Task.TaskStatus.CREATED);
            log.info("Scheduling retry {} for task {} in {} minutes", 
                    task.getRetryCount(), task.getId(), delayMinutes);
        }

        taskRepository.save(task);
    }

    /**
     * Gets a task by ID
     */
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));
    }

    /**
     * Updates task status
     */
    @Transactional
    public Task updateTaskStatus(Long id, Task.TaskStatus status) {
        Task task = getTaskById(id);
        task.setStatus(status);
        
        if (status == Task.TaskStatus.PAUSED) {
            task.setNextExecutionTime(null);
        } else if (status == Task.TaskStatus.CREATED) {
            CronExpression cronExpression = CronExpression.parse(task.getCronExpression());
            task.setNextExecutionTime(cronExpression.next(LocalDateTime.now()));
        }

        return taskRepository.save(task);
    }

    /**
     * Gets all tasks with pagination
     */
    public Page<Task> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }

    /**
     * Gets tasks by status
     */
    public List<Task> getTasksByStatus(Task.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    /**
     * Validates task parameters
     */
    private void validateTask(Task task) {
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new TaskValidationException("Task name is required");
        }

        if (task.getCronExpression() == null || task.getCronExpression().trim().isEmpty()) {
            throw new TaskValidationException("Cron expression is required");
        }

        try {
            CronExpression.parse(task.getCronExpression());
        } catch (Exception e) {
            throw new TaskValidationException("Invalid cron expression: " + task.getCronExpression());
        }

        if (task.getHandlerClass() == null || task.getHandlerClass().trim().isEmpty()) {
            throw new TaskValidationException("Handler class is required");
        }

        // Validate parameters JSON if present
        if (task.getParameters() != null && !task.getParameters().trim().isEmpty()) {
            try {
                objectMapper.readTree(task.getParameters());
            } catch (Exception e) {
                throw new TaskValidationException("Invalid parameters JSON format");
            }
        }
    }

    /**
     * Cleans up completed tasks older than retention period
     */
    @Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
    @Transactional
    public void cleanupOldTasks() {
        LocalDateTime retentionDate = LocalDateTime.now().minusDays(30); // 30 days retention
        int deletedCount = taskRepository.deleteByStatusAndLastExecutionTimeBefore(
            Task.TaskStatus.COMPLETED, retentionDate);
        log.info("Cleaned up {} completed tasks", deletedCount);
    }
}