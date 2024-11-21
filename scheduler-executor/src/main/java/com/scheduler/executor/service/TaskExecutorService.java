package com.scheduler.executor.service;

import com.scheduler.common.dto.TaskExecutionRequest;
import com.scheduler.common.event.TaskEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class TaskExecutorService {
    private final String executorId = UUID.randomUUID().toString();
    
    @Autowired
    private KafkaTemplate<String, TaskEvent> kafkaTemplate;
    
    @KafkaListener(topics = "task-execution")
    public void executeTask(TaskEvent event) {
        log.info("Executing task: {}", event.getTaskId());
        
        TaskExecutionRequest execution = new TaskExecutionRequest();
        execution.setTaskId(event.getTaskId());
        execution.setExecutorId(executorId);
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus(TaskExecutionRequest.ExecutionStatus.STARTED);
        
        try {
            // Simulate task execution
            Thread.sleep(1000);
            
            execution.setStatus(TaskExecutionRequest.ExecutionStatus.COMPLETED);
            execution.setEndTime(LocalDateTime.now());
            
            TaskEvent completionEvent = new TaskEvent();
            completionEvent.setTaskId(event.getTaskId());
            completionEvent.setEventType("TASK_COMPLETED");
            completionEvent.setTimestamp(LocalDateTime.now());
            
            kafkaTemplate.send("task-results", completionEvent);
        } catch (Exception e) {
            log.error("Task execution failed", e);
            
            execution.setStatus(TaskExecutionRequest.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setEndTime(LocalDateTime.now());
            
            TaskEvent failureEvent = new TaskEvent();
            failureEvent.setTaskId(event.getTaskId());
            failureEvent.setEventType("TASK_FAILED");
            failureEvent.setTimestamp(LocalDateTime.now());
            failureEvent.setDetails(e.getMessage());
            
            kafkaTemplate.send("task-results", failureEvent);
        }
    }
}