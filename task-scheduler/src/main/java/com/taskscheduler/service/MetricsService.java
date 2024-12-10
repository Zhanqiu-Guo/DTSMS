package com.taskscheduler.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.taskscheduler.model.ProcessMetrics;
import com.taskscheduler.repository.ProcessMetricsRepository;

import lombok.RequiredArgsConstructor;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class); // debug logger
    private final ProcessMetricsRepository processMetricsRepository;

    private final SystemInfo systemInfo = new SystemInfo();
    private final OperatingSystem os = systemInfo.getOperatingSystem();

    // Collect metrics for parent and child
    public void collectMetricsForTask(Long taskId, Long pid, String taskName) {
        if (pid == null || pid <= 0) {
            logger.debug("Invalid PID: " + pid);
            return;
        }

        // Get the parent process
        OSProcess parentProcess = os.getProcess(pid.intValue());
        if (parentProcess == null) {
            logger.debug("Null parentProcess: " + pid);
            return;
        }

        // Save parent process metrics
        saveProcessMetrics(taskId, null, pid, null, parentProcess, taskName);

        // Get child processes
        List<OSProcess> childProcesses = os.getProcesses().stream()
                .filter(proc -> proc.getParentProcessID() == pid.intValue())
                .toList();

        // Save metrics for child processes
        for (OSProcess childProcess : childProcesses) {
            saveProcessMetrics(taskId, taskId, (long) childProcess.getProcessID(), pid, childProcess, childProcess.getName());
        }
    }
    
    // Update current exist Task
    public void updateMetricsForTask(Long parentTaskId) {
        if (parentTaskId == null || parentTaskId <= 0) {
            return;
        }

        // Check if exist in DB
        List<ProcessMetrics> existingMetrics = processMetricsRepository.findByParentTaskId(parentTaskId);

        if (existingMetrics != null) {
            for (ProcessMetrics metrics: existingMetrics){
                OSProcess process = os.getProcess(parentTaskId.intValue());
                if (process != null) { 
                    double cpuUsage = process.getProcessCpuLoadCumulative() * 100; // CPU %
                    double memoryUsage = process.getResidentSetSize() / (1024.0 * 1024.0); // MEM (MB)

                    metrics.setCpuUsage(cpuUsage);
                    metrics.setMemoryUsage(memoryUsage);
                }
            }
        }
        processMetricsRepository.saveAll(existingMetrics);
    }

    public List<ProcessMetrics> getMetricsByParentTaskId(Long parentTaskId) {
        return processMetricsRepository.findByParentTaskId(parentTaskId);
    }

    public List<ProcessMetrics> getAllMetrics() {
        return processMetricsRepository.findAll();
    }

    // Save process to DB
    private void saveProcessMetrics(Long taskId, Long parentTaskId, Long pid, Long parentPid, OSProcess process, String processName) {
        
        if (process == null) return;

        double cpuUsage = process.getProcessCpuLoadCumulative() * 100; // CPU %
        double memoryUsage = process.getResidentSetSize() / (1024.0 * 1024.0); // MB

        ProcessMetrics metrics = new ProcessMetrics();
        metrics.setTaskId(taskId);
        metrics.setParentTaskId(parentTaskId == null ? taskId : parentTaskId); // Set PTID to itself
        metrics.setPid(pid);
        metrics.setParentPid(parentPid == null ? pid : parentPid); // Set PPID to itself
        metrics.setProcessName(processName);
        metrics.setCpuUsage(cpuUsage);
        metrics.setMemoryUsage(memoryUsage);
        metrics.setStartTime(LocalDateTime.now());
        logger.debug("DEBUG metrics: " + metrics);
        processMetricsRepository.save(metrics);
    }

    // Delete all metrics associated with a taskId
    public void deleteTaskMetrics(Long taskId) {
        List<ProcessMetrics> metrics = processMetricsRepository.findByParentTaskId(taskId);
        if (!metrics.isEmpty()) {
            processMetricsRepository.deleteAll(metrics);
        }
    }
}