package com.scheduler.core.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.scheduler.common.dto.TaskExecutionRequest;
import com.scheduler.common.dto.TaskExecutionResult;

@FeignClient(name = "executor-service", url = "${executor.service.url}")
public interface ExecutorClient {
    @PostMapping("/api/execute")
    TaskExecutionResult executeTask(@RequestBody TaskExecutionRequest request);
}
