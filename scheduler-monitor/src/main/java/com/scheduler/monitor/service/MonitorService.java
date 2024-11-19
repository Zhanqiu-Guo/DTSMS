package com.scheduler.monitor.service;

import com.scheduler.common.event.TaskEvent;
import com.scheduler.common.model.TaskExecution;
import com.scheduler.monitor.repository.TaskExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MonitorService {
    @Autowired
    private TaskExecutionRepository executionRepository;

    @KafkaListener(topics = "task-results")
    public void handleTaskResult(TaskEvent event) {
        log.info("Received task result: {}", event);

        TaskExecution execution = new TaskExecution();
        execution.setTaskId(event.getTaskId());
        execution.setStartTime(event.getTimestamp());
        execution.setEndTime(LocalDateTime.now());
        execution.setStatus("TASK_COMPLETED".equals(event.getEventType()) 
            ? TaskExecution.ExecutionStatus.COMPLETED 
            : TaskExecution.ExecutionStatus.FAILED);
        execution.setErrorMessage(event.getDetails());

        executionRepository.save(execution);
    }

    public List<TaskExecution> getTaskExecutions(Long taskId) {
        return executionRepository.findByTaskId(taskId);
    }

    public List<Long> getAllTaskIds() {
        return executionRepository.findDistinctTaskIds();
    }

    public Map<String, Object> getTaskStats(Long taskId) {
        List<TaskExecution> executions = executionRepository.findByTaskId(taskId);
        
        long totalExecutions = executions.size();
        long successfulExecutions = executions.stream()
            .filter(e -> e.getStatus() == TaskExecution.ExecutionStatus.COMPLETED)
            .count();
        long failedExecutions = totalExecutions - successfulExecutions;

        return Map.of(
            "taskId", taskId,
            "totalExecutions", totalExecutions,
            "successfulExecutions", successfulExecutions,
            "failedExecutions", failedExecutions,
            "successRate", totalExecutions > 0 ? (double) successfulExecutions / totalExecutions * 100 : 0
        );
    }
}
