package com.taskscheduler.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskscheduler.model.ProcessMetrics;
import com.taskscheduler.service.MetricsService;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/process-metrics")
@RequiredArgsConstructor
public class MetricsController {
    private final MetricsService metricsService;

    // Get all metrics from DB
    @GetMapping
    public ResponseEntity<List<ProcessMetrics>> getAllMetrics() {
        List<ProcessMetrics> allMetrics = metricsService.getAllMetrics();
        return ResponseEntity.ok(allMetrics);
    }

    // Find all metrics through a ParentTaskId
    @GetMapping("/{parentTaskId}")
    public ResponseEntity<List<ProcessMetrics>> getMetricsByParentTaskId(@PathVariable Long parentTaskId) {
        List<ProcessMetrics> metricsList = metricsService.getMetricsByParentTaskId(parentTaskId);
        return ResponseEntity.ok(metricsList);
    }

    // Update metrics
    @PutMapping("/{parentTaskId}/update")
    public ResponseEntity<String> updateMetricsForTask(@PathVariable Long parentTaskId) {
        metricsService.updateMetricsForTask(parentTaskId);
        return ResponseEntity.ok("Metrics updated successfully for ParentTaskId: " + parentTaskId);
    }

    // Delete
    @DeleteMapping("/{parentTaskId}")
    public ResponseEntity<String> deleteMetricsByParentTaskId(@PathVariable Long parentTaskId) {
        metricsService.deleteTaskMetrics(parentTaskId);
        return ResponseEntity.ok("Metrics deleted successfully for ParentTaskId: " + parentTaskId);
    }
}