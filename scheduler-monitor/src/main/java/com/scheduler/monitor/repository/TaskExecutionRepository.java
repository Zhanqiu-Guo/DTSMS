package com.scheduler.monitor.repository;

import com.scheduler.common.model.TaskExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {
    List<TaskExecution> findByTaskId(Long taskId);
    
    @Query("SELECT DISTINCT te.taskId FROM TaskExecution te")
    List<Long> findDistinctTaskIds();
}