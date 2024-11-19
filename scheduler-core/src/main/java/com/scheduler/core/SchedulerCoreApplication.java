package com.scheduler.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan({"com.scheduler.common.model", "com.scheduler.core.model"})
@EnableScheduling
public class SchedulerCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerCoreApplication.class, args);
    }
}