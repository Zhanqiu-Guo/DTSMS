package com.scheduler.core.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic taskEventsTopic() {
        return TopicBuilder.name("task-events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic taskExecutionTopic() {
        return TopicBuilder.name("task-execution")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic taskResultsTopic() {
        return TopicBuilder.name("task-results")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
