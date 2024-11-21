package com.taskscheduler.repository;

import com.taskscheduler.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface MetricsRepository extends JpaRepository<SystemMetrics, Long> {
    List<SystemMetrics> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT m FROM SystemMetrics m WHERE m.timestamp >= :startTime " +
           "ORDER BY m.timestamp DESC")
    List<SystemMetrics> findRecentMetrics(LocalDateTime startTime);
}
