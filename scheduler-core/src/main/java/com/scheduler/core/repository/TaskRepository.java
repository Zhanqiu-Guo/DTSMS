package com.scheduler.core.repository;

import com.scheduler.common.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(Task.TaskStatus status);
    
    List<Task> findByStatusInAndNextExecutionTimeBefore(
        List<Task.TaskStatus> statuses, 
        LocalDateTime dateTime
    );
    
    int deleteByStatusAndLastExecutionTimeBefore(
        Task.TaskStatus status, 
        LocalDateTime dateTime
    );
}