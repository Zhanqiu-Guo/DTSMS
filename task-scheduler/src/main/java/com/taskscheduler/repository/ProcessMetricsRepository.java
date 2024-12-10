package com.taskscheduler.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskscheduler.model.ProcessMetrics;

public interface ProcessMetricsRepository extends JpaRepository<ProcessMetrics, Long> {

    // Query by ID, return list
    List<ProcessMetrics> findByParentTaskId(Long parentTaskId);

    // Remove from metrics
    void deleteByParentTaskId(Long parentTaskId);
    // DELETE FROM process_metrics WHERE parent_task_id = ?;
}