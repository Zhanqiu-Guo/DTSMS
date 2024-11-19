package com.scheduler.core.config;

import com.scheduler.core.metrics.TaskMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public TaskMetrics taskMetrics(MeterRegistry registry) {
        return new TaskMetrics(registry);
    }
}