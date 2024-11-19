package com.scheduler.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan({"com.scheduler.common.model", "com.scheduler.executor.model"})
public class SchedulerExecutorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerExecutorApplication.class, args);
    }
}
