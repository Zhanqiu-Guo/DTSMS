package com.scheduler.common.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskEvent {
    private Long taskId;
    private String eventType;
    private LocalDateTime timestamp;
    private String details;
}