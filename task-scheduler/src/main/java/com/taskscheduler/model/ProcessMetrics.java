package com.taskscheduler.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "process_metrics")
public class ProcessMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique Key

    @Column(nullable = false)
    private Long taskId; // Task ID

    @Column(nullable = false)
    private Long pid; // Process ID

    @Column(nullable = false)
    private Long parentTaskId; // Parent Task ID, for delete

    @Column(nullable = false)
    private Long parentPid; // parent PID

    @Column(nullable = false)
    private String processName; // Task Name

    @Column(nullable = false)
    private Double cpuUsage; // CPU ratio%

    @Column(nullable = false)
    private Double memoryUsage; // mem (MB)

    @Column(nullable = false)
    private LocalDateTime startTime; // Task start time

    // Default Constructor
    public ProcessMetrics() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getPid() {
        return pid;
    }

    public void setPid(Long pid) {
        this.pid = pid;
    }

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public Long getParentPid() {
        return parentPid;
    }

    public void setParentPid(Long parentPid) {
        this.parentPid = parentPid;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    // To string
    @Override
    public String toString() {
        return "ProcessMetrics{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", pid=" + pid +
                ", parentTaskId=" + parentTaskId +
                ", parentPid=" + parentPid +
                ", processName='" + processName + '\'' +
                ", cpuUsage=" + cpuUsage +
                ", memoryUsage=" + memoryUsage +
                ", startTime=" + startTime +
                '}';
    }
}