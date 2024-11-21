package com.taskscheduler.websocket;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.taskscheduler.model.*;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    
    public void notifyTaskUpdate(Task task) {
        messagingTemplate.convertAndSend("/topic/tasks", task);
    }
    
    public void notifyTaskProgress(Long taskId, int progress) {
        messagingTemplate.convertAndSend(
            "/topic/tasks/" + taskId + "/progress",
            Map.of("taskId", taskId, "progress", progress)
        );
    }
    
    public void notifyTaskError(Long taskId, String error) {
        messagingTemplate.convertAndSend(
            "/topic/tasks/" + taskId + "/error",
            Map.of("taskId", taskId, "error", error)
        );
    }
}