//package com.example.notificationservice.controller;
//
//import com.example.notificationservice.dto.NotificationMessage;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.messaging.handler.annotation.DestinationVariable;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.SendTo;
//import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Controller;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
//@Controller
//@Slf4j
//@RequiredArgsConstructor
//public class WebSocketController {
//
//  private final SimpMessagingTemplate messagingTemplate;
//
//  /**
//   * Handle client connection and subscription
//   */
//  @MessageMapping("/connect")
//  @SendTo("/topic/attendance")
//  public Map<String, Object> handleConnection(SimpMessageHeaderAccessor headerAccessor) {
//    String sessionId = headerAccessor.getSessionId();
//    log.info("Client connected with session: {}", sessionId);
//
//    assert sessionId != null;
//    return Map.of(
//            "type", "CONNECTION_ACK",
//            "message", "Connected successfully",
//            "timestamp", LocalDateTime.now(),
//            "sessionId", sessionId
//    );
//  }
//
//  /**
//   * Handle student subscription to their personal topic
//   */
//  @MessageMapping("/subscribe/student/{studentCIC}")
//  public void subscribeToStudentTopic(@DestinationVariable String studentCIC,
//                                      SimpMessageHeaderAccessor headerAccessor) {
//    String sessionId = headerAccessor.getSessionId();
//    log.info("Student {} subscribed to personal topic, session: {}", studentCIC, sessionId);
//
//    // Send confirmation message to the specific student
//    messagingTemplate.convertAndSend(
//            "/topic/student/" + studentCIC,
//            Map.of(
//                    "type", "SUBSCRIPTION_ACK",
//                    "message", "Subscribed to personal notifications",
//                    "studentCIC", studentCIC,
//                    "timestamp", LocalDateTime.now()
//            )
//    );
//  }
//
//  /**
//   * Handle class subscription
//   */
//  @MessageMapping("/subscribe/class/{classCode}")
//  public void subscribeToClassTopic(@DestinationVariable String classCode,
//                                    SimpMessageHeaderAccessor headerAccessor) {
//    String sessionId = headerAccessor.getSessionId();
//    log.info("Client subscribed to class {}, session: {}", classCode, sessionId);
//
//    messagingTemplate.convertAndSend(
//            "/topic/class/" + classCode,
//            Map.of(
//                    "type", "SUBSCRIPTION_ACK",
//                    "message", "Subscribed to class notifications",
//                    "classCode", classCode,
//                    "timestamp", LocalDateTime.now()
//            )
//    );
//  }
//
//  /**
//   * Handle schedule subscription
//   */
//  @MessageMapping("/subscribe/schedule/{scheduleId}")
//  public void subscribeToScheduleTopic(@DestinationVariable Long scheduleId,
//                                       SimpMessageHeaderAccessor headerAccessor) {
//    String sessionId = headerAccessor.getSessionId();
//    log.info("Client subscribed to schedule {}, session: {}", scheduleId, sessionId);
//
//    messagingTemplate.convertAndSend(
//            "/topic/schedule/" + scheduleId,
//            Map.of(
//                    "type", "SUBSCRIPTION_ACK",
//                    "message", "Subscribed to schedule notifications",
//                    "scheduleId", scheduleId,
//                    "timestamp", LocalDateTime.now()
//            )
//    );
//  }
//
//  /**
//   * Handle client disconnection
//   */
//  @MessageMapping("/disconnect")
//  public void handleDisconnection(SimpMessageHeaderAccessor headerAccessor) {
//    String sessionId = headerAccessor.getSessionId();
//    log.info("Client disconnected, session: {}", sessionId);
//  }
//
//  /**
//   * Test endpoint to send notification manually
//   */
//  @MessageMapping("/test/notification")
//  @SendTo("/topic/attendance")
//  public NotificationMessage testNotification(Map<String, Object> payload) {
//    log.info("Test notification triggered: {}", payload);
//
//    NotificationMessage testMessage = new NotificationMessage();
//    testMessage.setType("TEST_NOTIFICATION");
//    testMessage.setMessage("This is a test notification");
//    testMessage.setTimestamp(LocalDateTime.now());
//
//    return testMessage;
//  }
//}