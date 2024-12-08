package com.taskscheduler.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@Table(name = "tasks")
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "command")
    private String command;

    @Column(name = "threads_needed")
    private int threadsNeeded;

    @Column(name = "pid")
    private Long  pid;

    private String arguments;
    
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_task_id")
    private Task dependentTask;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }    
    
    public Long getId() {return id;}
    public String getName() {return name;}
    public Long setPid() {return pid;}
    public String getCommand() {return command;}
    public int getThreadsNeeded() {return threadsNeeded;}
    public String getArguments() {return arguments;}
    public String getDescription() {return description;}
    public TaskPriority getPriority() {return priority;}
    public TaskStatus getStatus() {return status;}
    public User getAssignedUser() {return assignedUser;}
    public Task getDependentTask() {return dependentTask;}
    public LocalDateTime getScheduledTime() {return scheduledTime;}
    public LocalDateTime getCompletedTime() {return completedTime;}
    public LocalDateTime getCreatedAt() {return createdAt;}
    public LocalDateTime getUpdatedAt() {return updatedAt;}
    
    public void setId(Long id) {this.id = id;}
    public void setName(String name) {this.name = name;}
    public void setPid(Long pid) {this.pid = pid;}
    public void setCommand(String command) {this.command = command;}
    public void setThreadsNeeded(int threadsNeeded) {this.threadsNeeded = threadsNeeded;}
    public void setArguments(String arguments) {this.arguments = arguments;}
    public void setDescription(String description) {this.description = description;}
    public void setPriority(TaskPriority priority) {this.priority = priority;}
    public void setStatus(TaskStatus status) {this.status = status;}
    public void setAssignedUser(User assignedUser) {this.assignedUser = assignedUser;}
    public void setDependentTask(Task dependentTask) {this.dependentTask = dependentTask;}
    public void setScheduledTime(LocalDateTime scheduledTime) {this.scheduledTime = scheduledTime;}
    public void setCompletedTime(LocalDateTime completedTime) {this.completedTime = completedTime;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
    public void setUpdatedAt(LocalDateTime updatedAt) {this.updatedAt = updatedAt;}
    
    public enum TaskPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
}