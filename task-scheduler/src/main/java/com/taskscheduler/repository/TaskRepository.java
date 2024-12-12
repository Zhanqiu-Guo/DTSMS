package com.taskscheduler.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.taskscheduler.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(Task.TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.status = 'PENDING' AND " +
           "(t.dependentTask IS NULL OR t.dependentTask.status = 'COMPLETED')")
    List<Task> findExecutableTasks();
}
