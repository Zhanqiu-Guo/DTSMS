package com.taskscheduler.controller;

import com.taskscheduler.model.*;
import com.taskscheduler.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {
    private final MetricsService metricsService;
    
    @GetMapping("/recent")
    public ResponseEntity<List<SystemMetrics>> getRecentMetrics() {
        return ResponseEntity.ok(metricsService.getRecentMetrics());
    }
}