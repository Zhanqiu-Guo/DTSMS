package com.taskscheduler.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // Running task and check it's result
    @Transactional
    public void executeTask(Task task, Runnable onComplete) {
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
                
                webSocketService.notifyTaskUpdate(task);

            // Auto close buffer
            } catch (InterruptedException | RuntimeException | ExecutionException e) {
                handleTaskFailure(task, e);
                // Release threads
                if (onComplete != null) {
                    onComplete.run();
                }
            } finally {
                programExecutor.shutdown();
                runningTasks.remove(task.getId());

                // Release threads
                if (onComplete != null) {
                    onComplete.run();
                }

                // Remove task from metrics
                if (task.getStatus() == Task.TaskStatus.COMPLETED || task.getStatus() == Task.TaskStatus.FAILED) {
                    metricsService.deleteTaskMetrics(task.getId());
                }
            }
        });
    }

    @Transactional
    public Pair<Boolean, String> executeCode(Task task) {
        Process process = null;

        // Construct command
        CommandLine cmdLine = CommandLine.parse(task.getCommand());
        String[] args = cmdLine.toStrings();

        // Create work folder: ../Tasks/timestamp
        LocalDateTime createdAt = task.getCreatedAt();
        String folderName = createdAt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path tasksRoot = Paths.get("../Tasks");
        Path folderPath = tasksRoot.resolve(folderName);

        try {
            if (!Files.exists(tasksRoot)) {
                Files.createDirectory(tasksRoot);
            }
            if (!Files.exists(folderPath)) {
                Files.createDirectory(folderPath);
            }
        } catch (IOException e) {
            return Pair.of(false, "Error creating task folder: " + e.getMessage());
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        try {
            Path logFilePath = folderPath.resolve("output.log");
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.directory(folderPath.toFile());
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            task.setPid(process.pid());
             // Initial
             metricsService.collectMetricsForTask(task.getId(), task.getPid(), task.getName());

            // Start metrics collection every 1 second
            scheduler.scheduleAtFixedRate(() -> {
                metricsService.updateMetricsForTask(task.getId());
            }, 0, 1, TimeUnit.SECONDS);

            // Save stdout&stderr to a new file
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFilePath.toFile()))) {

                StringBuilder output = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logWriter.write(line);
                    logWriter.newLine();
                }

                int exitValue = process.waitFor();
                boolean success = exitValue == 0;

                task.setStatus(success ? Task.TaskStatus.COMPLETED : Task.TaskStatus.FAILED);
                taskRepository.save(task);

                return Pair.of(success, output.toString());

            } catch (IOException | InterruptedException e) {
                task.setStatus(Task.TaskStatus.FAILED);
                taskRepository.save(task);
                return Pair.of(false, "Error: " + e.getMessage());
            }

        } catch (IOException e) {
            return Pair.of(false, "Error starting process: " + e.getMessage());
        } finally {
            // Shut down scheduler and process cleanup
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            if (process != null && process.isAlive()) {
                process.destroy();
            }
            //  delete task metrics
            metricsService.deleteTaskMetrics(task.getId());
        }
    }

    
    public Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, Task.TaskStatus newStatus) {
        Task task = findTaskById(taskId);
            
        if (newStatus == Task.TaskStatus.CANCELLED) {
            cancelTask(task);
        }
        
        return task;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public void cancelTask(Task task) {
        Thread taskThread = runningTasks.get(task.getId());
        if (taskThread != null) {
            taskThread.interrupt();
        }
        task.setStatus(Task.TaskStatus.CANCELLED);
        taskRepository.save(task);
        // Delete task from DB
        metricsService.deleteTaskMetrics(task.getId());
        webSocketService.notifyTaskUpdate(task);
    }

    // Delete from Queue and remove folder
    public void deleteTask(Task task) {
        Thread taskThread = runningTasks.get(task.getId());
        if (taskThread != null) {
            taskThread.interrupt();
            runningTasks.remove(task.getId());
        }

        deleteTaskFolder(task);

        taskRepository.deleteById(task.getId());
    }

    private void deleteTaskFolder(Task task) {
        if (task.getCreatedAt() == null) {
            return;
        }
        try {
            // Retrieve task folder
            LocalDateTime createdAt = task.getCreatedAt();
            String folderName = createdAt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path tasksRoot = Paths.get("../Tasks");
            Path folderPath = tasksRoot.resolve(folderName);
    
            // rm -rf task folder
            if (Files.exists(folderPath)) {
                Files.walk(folderPath)
                     .sorted((a, b) -> b.compareTo(a))
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             System.err.println("Failed to delete file: " + path + ", error: " + e.getMessage());
                         }
                     });
                System.out.println("Deleted folder: " + folderPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to delete task folder: " + e.getMessage());
        }
    }

    private void handleTaskFailure(Task task, Exception e) {
        task.setStatus(Task.TaskStatus.FAILED);
        taskRepository.save(task);
        metricsService.deleteTaskMetrics(task.getId());
        webSocketService.notifyTaskUpdate(task);
        webSocketService.notifyTaskError(task.getId(), e.getMessage());
    }
}
