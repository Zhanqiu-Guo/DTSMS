package com.scheduler.monitor.service;

import com.scheduler.core.model.Task;
import com.scheduler.core.repository.TaskRepository;
import com.scheduler.core.metrics.TaskMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MonitorService {
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private TaskMetrics taskMetrics;

    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void monitorTasks() {
        checkStuckTasks();
        updateMetrics();
    }

    private void checkStuckTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<Task> stuckTasks = taskRepository.findStuckTasks(threshold);
        
        for (Task task : stuckTasks) {
            log.warn("Found stuck task: {}", task.getId());
            task.setStatus(Task.TaskStatus.FAILED);
            task.setErrorMessage("Task stuck in RUNNING state");
            taskRepository.save(task);
            taskMetrics.incrementTasksFailed();
        }
    }

    public Map<String, Object> getTaskStats() {
        return Map.of(
            "totalTasks", taskRepository.count(),
            "activeTasks", taskRepository.countByStatus(Task.TaskStatus.RUNNING),
            "completedTasks", taskRepository.countByStatus(Task.TaskStatus.COMPLETED),
            "failedTasks", taskRepository.countByStatus(Task.TaskStatus.FAILED)
        );
    }
}