package com.scheduler.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskExecutionRequest {
    private Long taskId;
    private String handlerClass;
    private String parameters;
}