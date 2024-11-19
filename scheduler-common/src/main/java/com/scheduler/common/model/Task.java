
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
    private String cronExpression;
    private String handlerClass;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    
    @Column(columnDefinition = "text")
    private String parameters;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    private Integer retryCount;
    private Integer maxRetries;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum TaskStatus {
        CREATED, SCHEDULED, RUNNING, COMPLETED, FAILED, PAUSED
    }
}