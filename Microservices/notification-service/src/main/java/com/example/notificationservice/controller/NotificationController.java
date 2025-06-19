package com.example.notificationservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
  @GetMapping("/healthy")
  public ResponseEntity<Map<String, Object>> healthy() {
    return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "notification-service",
            "timestamp", LocalDateTime.now()
    ));
  }

  @GetMapping("/info")
  public ResponseEntity<Map<String, Object>> info() {
    return ResponseEntity.ok(Map.of(
            "service",  "notification-service",
            "description", "Real-time notification service for attendance system",
            "websocket_endpoint", "/ws-notifications",
            "topic", Map.of(
                    "class_notifications", "/topic/class/{classCode}",
                    "schedule_notifications", "/topic/schedule/{scheduleId}",
                    "general_notifications", "/topic/attendance"
            )
    ));
  }
}
