package com.example.notificationservice.service;

import com.example.notificationservice.dto.AttendanceEvent;
import com.example.notificationservice.dto.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class NatsSubscriberService {
  private final Connection natsConnection;
  private final ObjectMapper objectMapper;
  private final WebSocketNotificationService webSocketNotificationService;

  private static final String ATTENDANCE_SUCCESS_SUBJECT = "attendance.checkin.success";
  private static final String ATTENDANCE_FAILED_SUBJECT = "attendance.checkin.failed";

  @EventListener(ApplicationReadyEvent.class)
  public void startSubscribing() {
    log.info("Starting NATS subscriptions...");

    try {
      Dispatcher dispatcher = natsConnection.createDispatcher();

      //Sub to success event
      dispatcher.subscribe(ATTENDANCE_SUCCESS_SUBJECT, this::handleAttendanceSuccess);
      log.info("Subscribed to: {}", ATTENDANCE_SUCCESS_SUBJECT);

      //Sub to failed event
      dispatcher.subscribe(ATTENDANCE_FAILED_SUBJECT, this::handleAttendanceFailed);
      log.info("Subscribed to: {}", ATTENDANCE_FAILED_SUBJECT);
    } catch (Exception e) {
      log.error("Failed to start NATS subscriptions.", e);
    }
  }

  private void handleAttendanceSuccess(Message message) {
    try {
      String jsonData = new String(message.getData(), StandardCharsets.UTF_8);
      log.info("Received notification from: {}", jsonData);

      AttendanceEvent event = objectMapper.readValue(jsonData, AttendanceEvent.class);

      //Tao notification message
      NotificationMessage notificationMessage = new NotificationMessage().fromAttendanceEvent(
              event, "ATTENDANCE_SUCCESS"
      );

      //Gui notifi thong qua socket
      sendNotification(notificationMessage);
    }catch (Exception e) {
      log.error("Failed to process attendance success event", e);
    }
  }

  private void handleAttendanceFailed(Message message) {
    try {
      String jsonData = new String(message.getData(), StandardCharsets.UTF_8);
      log.info("Received notification from: {}", jsonData);

      AttendanceEvent event = objectMapper.readValue(jsonData, AttendanceEvent.class);

      //Tao notification message
      NotificationMessage notificationMessage = new NotificationMessage().fromAttendanceEvent(
              event, "ATTENDANCE_FAILED"
      );

      //Gui notification thong qua websocket
      sendNotification(notificationMessage);
    } catch (Exception e) {
      log.error("Failed to process attendance failed event", e);
    }
  }

  private void sendNotification(NotificationMessage notificationMessage) {
    //Gửi đến topic cụ thể của class
    if(notificationMessage.getClassCode() != null) {
      webSocketNotificationService.sendClassNotification(
              notificationMessage.getClassCode(), notificationMessage
      );
    }

    //Gửi đến topic cụ thể của schedule
    if(notificationMessage.getScheduleId() != null) {
      webSocketNotificationService.sendScheduleNotification(
              notificationMessage.getScheduleId(), notificationMessage
      );
    }

    //Gửi đến general attendance topic
    webSocketNotificationService.sendGeneralNotification(notificationMessage);
  }
}
