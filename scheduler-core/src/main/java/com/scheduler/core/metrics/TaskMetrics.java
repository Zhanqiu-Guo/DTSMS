package com.scheduler.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TaskMetrics {
    private final Counter taskCreatedCounter;
    private final Counter taskCompletedCounter;
    private final Counter taskFailedCounter;
    private final Timer taskExecutionTimer;
    private final AtomicInteger activeTasksGauge;

    public TaskMetrics(MeterRegistry registry) {
        this.taskCreatedCounter = registry.counter("scheduler.tasks.created");
        this.taskCompletedCounter = registry.counter("scheduler.tasks.completed");
        this.taskFailedCounter = registry.counter("scheduler.tasks.failed");
        this.taskExecutionTimer = registry.timer("scheduler.tasks.execution.time");
        this.activeTasksGauge = registry.gauge("scheduler.tasks.active", 
            new AtomicInteger(0));
    }

    public void incrementTasksCreated() {
        taskCreatedCounter.increment();
    }

    public void incrementTasksCompleted() {
        taskCompletedCounter.increment();
        activeTasksGauge.decrementAndGet();
    }

    public void incrementTasksFailed() {
        taskFailedCounter.increment();
        activeTasksGauge.decrementAndGet();
    }

    public void incrementActiveTasks() {
        activeTasksGauge.incrementAndGet();
    }

    public Timer getTaskExecutionTimer() {
        return taskExecutionTimer;
    }
}
