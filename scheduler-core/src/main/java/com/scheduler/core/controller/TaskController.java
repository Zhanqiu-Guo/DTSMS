package com.scheduler.core.controller;

import com.scheduler.common.model.Task;
import com.scheduler.common.dto.TaskDTO;
import com.scheduler.core.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/tasks")
@Slf4j
public class TaskController {
    @Autowired
    private TaskService taskService;
    
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskDTO taskDTO) {
        Task task = new Task();
        task.setName(taskDTO.getName());
        task.setCronExpression(taskDTO.getCronExpression());
        task.setHandlerClass(taskDTO.getHandlerClass());
        task.setParameters(taskDTO.getParameters());
        task.setMaxRetries(taskDTO.getMaxRetries());
        
        return ResponseEntity.ok(taskService.createTask(task));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(
            @PathVariable Long id,
            @RequestParam Task.TaskStatus status) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, status));
    }
}