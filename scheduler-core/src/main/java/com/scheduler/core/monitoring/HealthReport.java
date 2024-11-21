package com.scheduler.core.monitoring;

import com.scheduler.common.model.Task;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class HealthReport {
    private LocalDateTime timestamp;
    private Map<Task.TaskStatus, Long> taskCounts;
    private double successRate;
    private double averageExecutionTime;
    private int activeExecutors;
    private int queuedTasks;
}