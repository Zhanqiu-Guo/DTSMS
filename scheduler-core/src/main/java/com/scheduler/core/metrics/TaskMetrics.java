package com.scheduler.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class TaskMetrics {
    private final Counter taskCreatedCounter;
    private final Counter taskCompletedCounter;
    private final Counter taskFailedCounter;
    private final Timer taskExecutionTimer;

    public TaskMetrics(MeterRegistry registry) {
        this.taskCreatedCounter = Counter.builder("scheduler.tasks.created")
            .description("Number of tasks created")
            .register(registry);
            
        this.taskCompletedCounter = Counter.builder("scheduler.tasks.completed")
            .description("Number of tasks completed")
            .register(registry);
            
        this.taskFailedCounter = Counter.builder("scheduler.tasks.failed")
            .description("Number of tasks failed")
            .register(registry);
            
        this.taskExecutionTimer = Timer.builder("scheduler.tasks.execution.time")
            .description("Task execution time")
            .register(registry);
    }

    public void incrementTasksCreated() {
        taskCreatedCounter.increment();
    }

    public void incrementTasksCompleted() {
        taskCompletedCounter.increment();
    }

    public void incrementTasksFailed() {
        taskFailedCounter.increment();
    }

    public Timer getTaskExecutionTimer() {
        return taskExecutionTimer;
    }
}