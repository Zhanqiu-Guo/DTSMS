package com.scheduler.common.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String handlerClass;
    private String parameters;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    
    private Integer retryCount = 0;
    private Integer maxRetries = 3;
    private String errorMessage;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    
    public enum TaskStatus {
        CREATED, RUNNING, COMPLETED, FAILED
    }
}