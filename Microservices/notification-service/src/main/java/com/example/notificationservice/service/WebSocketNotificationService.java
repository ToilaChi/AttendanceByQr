package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketNotificationService {
  private final SimpMessagingTemplate messagingTemplate;

  //Gửi thông báo đến sinh viên cụ thể
  //Topic pattern: /topic/student/{studentCIC}
  public void sendStudentNotification(String studentCIC, NotificationMessage message) {
    String destination = "/topic/student/" + studentCIC;
    try {
      messagingTemplate.convertAndSend(destination, message);
      log.info("Sent notification to student {}: {}", studentCIC, message.getMessage());
    } catch (Exception e) {
      log.error("Failed to send notification to student {}: {}", studentCIC, message, e);
    }
  }

  //Gửi thông báo đến tất cả những sub của class cụ thể
  //Topic pattern: /topic/class/{classCode}
  public void sendClassNotification(String classCode, NotificationMessage message) {
    String destination = "/topic/class/" + classCode;
    try {
      messagingTemplate.convertAndSend(destination, message);
      log.info("Sent notification to {}: {}", destination, message.getMessage());
    } catch (Exception e) {
      log.error("Failed to send notification to {}: {}", destination, message, e);
    }
  }

  //Gửi thông báo đến tất cả những sub của schdule cụ thể
  //Topic pattern: /topic/schedule/{scheduleId}
  public void sendScheduleNotification(Long scheduleId, NotificationMessage message) {
    String destination = "/topic/schedule/" + scheduleId;
    try {
      messagingTemplate.convertAndSend(destination, message);
      log.info("Sent notification to {}: {}", destination, message.getMessage());
    } catch (Exception e) {
      log.error("Failed to send notification to {}: {}", destination, message, e);
    }
  }

  //Gửi thông báo đến tất cả những sub chung
  //Topic pattern: /topic/attendance
  public void sendGeneralNotification(NotificationMessage message) {
    String destination = "/topic/attendance";
    try {
      messagingTemplate.convertAndSend(destination, message);
      log.info("Sent notification to: {}", message.getMessage());
    } catch (Exception e) {
      log.error("Failed to send notification to: {}", message, e);
    }
  }

  //Gửi thông báo đến cả sinh viên cụ thể và general topic
  public void sendAttendanceNotification(NotificationMessage message) {
    //Gửi đến sinh viên cụ thể
    if(message.getStudentCIC() != null) {
      sendStudentNotification(message.getStudentCIC(), message);
    }

    //Gửi đến class 
    if(message.getClassCode() != null) {
      sendClassNotification(message.getClassCode(), message);
    }

    //Gửi đến schedule
    if(message.getScheduleId() != null) {
      sendScheduleNotification(message.getScheduleId(), message);
    }

    //Gửi đến general topic
    sendGeneralNotification(message);
  }
}
