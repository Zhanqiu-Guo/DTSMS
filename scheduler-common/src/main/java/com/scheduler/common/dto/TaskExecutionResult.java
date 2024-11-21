package com.scheduler.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskExecutionResult {
    private Long taskId;
    private boolean success;
    private String result;
    private String errorMessage;
    private LocalDateTime executionTime;
    private long executionDurationMs;
}