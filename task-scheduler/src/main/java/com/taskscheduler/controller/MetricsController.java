package com.taskscheduler.controller;

import com.taskscheduler.model.ProcessMetrics;
import com.taskscheduler.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/process-metrics")
@RequiredArgsConstructor
public class MetricsController {
    private final MetricsService metricsService;

    @GetMapping
    public ResponseEntity<List<ProcessMetrics>> getAllMetrics() {
        List<ProcessMetrics> allMetrics = metricsService.getAllMetrics();
        return ResponseEntity.ok(allMetrics);
    }

    @GetMapping("/{parentTaskId}")
    public ResponseEntity<List<ProcessMetrics>> getMetricsByParentTaskId(@PathVariable Long parentTaskId) {
        List<ProcessMetrics> metricsList = metricsService.getMetricsByParentTaskId(parentTaskId);
        return ResponseEntity.ok(metricsList);
    }

    @PutMapping("/{parentTaskId}/update")
    public ResponseEntity<String> updateMetricsForTask(@PathVariable Long parentTaskId) {
        metricsService.updateMetricsForTask(parentTaskId);
        return ResponseEntity.ok("Metrics updated successfully for ParentTaskId: " + parentTaskId);
    }

    @DeleteMapping("/{parentTaskId}")
    public ResponseEntity<String> deleteMetricsByParentTaskId(@PathVariable Long parentTaskId) {
        metricsService.deleteTaskMetrics(parentTaskId);
        return ResponseEntity.ok("Metrics deleted successfully for ParentTaskId: " + parentTaskId);
    }
}