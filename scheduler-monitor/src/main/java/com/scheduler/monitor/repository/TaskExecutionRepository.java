package com.scheduler.monitor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.scheduler.common.dto.TaskExecutionRequest;

import java.util.List;

public interface TaskExecutionRepository extends JpaRepository<TaskExecutionRequest, Long> {
    List<TaskExecutionRequest> findByTaskId(Long taskId);
    
    @Query("SELECT DISTINCT te.taskId FROM TaskExecution te")
    List<Long> findDistinctTaskIds();
}