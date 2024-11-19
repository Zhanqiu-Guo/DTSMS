package com.scheduler.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan({"com.scheduler.common.model", "com.scheduler.monitor.model"})
public class SchedulerMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerMonitorApplication.class, args);
    }
}