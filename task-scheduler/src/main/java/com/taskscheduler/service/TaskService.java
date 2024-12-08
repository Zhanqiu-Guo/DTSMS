package com.taskscheduler.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.exec.CommandLine;

import com.taskscheduler.model.Task;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.websocket.WebSocketService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final MetricsService metricsService;
    private final WebSocketService webSocketService;
    
    private final ConcurrentHashMap<Long, Thread> runningTasks = new ConcurrentHashMap<>();

    
    @Transactional
    public void executeTask(Task task) {

        taskExecutor.execute(() -> {
            Thread currentThread = Thread.currentThread();
            runningTasks.put(task.getId(), currentThread);
            ExecutorService programExecutor = Executors.newFixedThreadPool(task.getThreadsNeeded());
            
            try {
                task.setStatus(Task.TaskStatus.RUNNING);
                taskRepository.save(task);
                webSocketService.notifyTaskUpdate(task);
                
                Pair<Boolean, String> result = programExecutor.submit(() -> 
                    executeCode(task)
                ).get();
                
                if (!result.getFirst()) {
                    throw new RuntimeException("Execution failed: " + result.getSecond());
                }
                
                task.setStatus(Task.TaskStatus.COMPLETED);
                task.setCompletedTime(LocalDateTime.now());
                taskRepository.save(task);
                
                metricsService.recordTaskCompletion(task);
                webSocketService.notifyTaskUpdate(task);
            } catch (Exception e) {
                handleTaskFailure(task, e);
            } finally {
                programExecutor.shutdown();
                runningTasks.remove(task.getId());
            }
        });
    }

    @Transactional
    public Pair<Boolean, String> executeCode(Task task) {
        Process process = null;

        CommandLine cmdLine = CommandLine.parse(task.getCommand());
        String[] args = cmdLine.toStrings();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            Long pid = process.pid();
            task.setPid(pid);
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitValue = process.waitFor();
            boolean success = exitValue == 0;

            return Pair.of(success, output.toString());
            
        } catch (Exception e) {
            return Pair.of(false, "Error: " + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, Task.TaskStatus newStatus) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
        if (newStatus == Task.TaskStatus.PAUSED) {
            pauseTask(task);
        } else if (newStatus == Task.TaskStatus.RUNNING) {
            resumeTask(task);
        } else if (newStatus == Task.TaskStatus.CANCELLED) {
            cancelTask(task);
        }
        
        return task;
    }

    // private void validatePythonFile(String pythonFilePath) {
    //     if (pythonFilePath == null || pythonFilePath.trim().isEmpty()) {
    //         throw new IllegalArgumentException("Python file path is required");
    //     }

    //     File pythonFile = new File(pythonFilePath);
    //     if (!pythonFile.exists() || !pythonFile.isFile()) {
    //         throw new IllegalArgumentException("Python file does not exist: " + pythonFilePath);
    //     }
        
    //     if (!pythonFilePath.endsWith(".py")) {
    //         throw new IllegalArgumentException("File must be a Python file (.py)");
    //     }
    // }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    private void pauseTask(Task task) {
        Thread taskThread = runningTasks.get(task.getId());
        if (taskThread != null) {
            taskThread.interrupt();
        }
        task.setStatus(Task.TaskStatus.PAUSED);
        taskRepository.save(task);
        webSocketService.notifyTaskUpdate(task);
    }

    private void resumeTask(Task task) {
        if (task.getStatus() == Task.TaskStatus.PAUSED) {
            task.setStatus(Task.TaskStatus.PENDING);
            taskRepository.save(task);
            executeTask(task);
        }
    }

    private void cancelTask(Task task) {
        Thread taskThread = runningTasks.get(task.getId());
        if (taskThread != null) {
            taskThread.interrupt();
        }
        task.setStatus(Task.TaskStatus.CANCELLED);
        taskRepository.save(task);
        webSocketService.notifyTaskUpdate(task);
    }

    private void handleTaskFailure(Task task, Exception e) {
        task.setStatus(Task.TaskStatus.FAILED);
        taskRepository.save(task);
        metricsService.recordTaskFailure(task);
        webSocketService.notifyTaskUpdate(task);
        System.out.println("Error Message: " + e.getMessage());
        webSocketService.notifyTaskError(task.getId(), e.getMessage());
    }
}
