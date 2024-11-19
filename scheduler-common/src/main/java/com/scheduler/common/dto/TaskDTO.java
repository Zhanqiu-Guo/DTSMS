package com.scheduler.common.dto;

import lombok.Data;
import com.scheduler.common.model.Task.TaskStatus;

@Data
public class TaskDTO {
    private Long id;
    private String name;
    private String cronExpression;
    private String handlerClass;
    private TaskStatus status;
    private String parameters;
    private Integer maxRetries;
}
