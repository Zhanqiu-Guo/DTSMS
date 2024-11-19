package com.scheduler.common.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task_executions")
public class TaskExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long taskId;
    private String executorId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    
    private String errorMessage;
    private String result;
    
    public enum ExecutionStatus {
        STARTED, COMPLETED, FAILED
    }
}