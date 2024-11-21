package com.taskscheduler.service;

import com.taskscheduler.model.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.taskscheduler.repository.*;
import lombok.RequiredArgsConstructor;
import com.taskscheduler.model.SystemMetrics;
import com.taskscheduler.model.Task;
import com.taskscheduler.repository.MetricsRepository;
import com.taskscheduler.repository.TaskRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private final MetricsRepository metricsRepository;
    private final TaskRepository taskRepository;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void collectMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        metrics.setTimestamp(LocalDateTime.now());
        metrics.setActiveTasks(countActiveTasks());
        metrics.setCpuUsage(getCpuUsage());
        metrics.setMemoryUsage(getMemoryUsage());
        metrics.setThreadPoolSize(taskExecutor.getPoolSize());
        metrics.setQueueSize(taskExecutor.getQueueSize());
        
        metricsRepository.save(metrics);
    }

     public List<SystemMetrics> getRecentMetrics() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(24); // Last 24 hours
        return metricsRepository.findRecentMetrics(startTime);
    }

    public void recordTaskCompletion(Task task) {
        // Record task completion metrics
        SystemMetrics metrics = new SystemMetrics();
        metrics.setTimestamp(LocalDateTime.now());
        metrics.setActiveTasks(countActiveTasks());
        metricsRepository.save(metrics);
    }

    public void recordTaskFailure(Task task) {
        // Record task failure metrics
        SystemMetrics metrics = new SystemMetrics();
        metrics.setTimestamp(LocalDateTime.now());
        metrics.setActiveTasks(countActiveTasks());
        metricsRepository.save(metrics);
    }

    private int countActiveTasks() {
        return taskRepository.findByStatus(Task.TaskStatus.RUNNING).size();
    }

    private double getCpuUsage() {
        com.sun.management.OperatingSystemMXBean osBean = 
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return osBean.getProcessCpuLoad() * 100.0;
    }

    private double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return ((double) (totalMemory - freeMemory) / totalMemory) * 100.0;
    }
}
