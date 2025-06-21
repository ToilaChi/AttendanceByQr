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
      // Log chi tiết message để debug
      log.info("Sending to destination: {}", destination);
      log.info("Message content: type={}, message={}, studentCIC={}",
              message.getType(), message.getMessage(), message.getStudentCIC());

      messagingTemplate.convertAndSend(destination, message);
      log.info("✅ Sent notification to student {}: {}", studentCIC, message.getMessage());
    } catch (Exception e) {
      log.error("❌ Failed to send notification to student {}: {}", studentCIC, message, e);
    }
  }

  //Gửi thông báo đến tất cả những sub của class cụ thể
  //Topic pattern: /topic/class/{classCode}
  public void sendClassNotification(String classCode, NotificationMessage message) {
    String destination = "/topic/class/" + classCode;
    try {
      log.info("Sending to destination: {}", destination);
      log.info("Message content: type={}, message={}, classCode={}",
              message.getType(), message.getMessage(), message.getClassCode());

      messagingTemplate.convertAndSend(destination, message);
      log.info("✅ Sent notification to {}: {}", destination, message.getMessage());
    } catch (Exception e) {
      log.error("❌ Failed to send notification to {}: {}", destination, message, e);
    }
  }

  //Gửi thông báo đến tất cả những sub của schedule cụ thể
  //Topic pattern: /topic/schedule/{scheduleId}
  public void sendScheduleNotification(Long scheduleId, NotificationMessage message) {
    String destination = "/topic/schedule/" + scheduleId;
    try {
      log.info("Sending to destination: {}", destination);
      log.info("Message content: type={}, message={}, scheduleId={}",
              message.getType(), message.getMessage(), message.getScheduleId());

      messagingTemplate.convertAndSend(destination, message);
      log.info("✅ Sent notification to {}: {}", destination, message.getMessage());
    } catch (Exception e) {
      log.error("❌ Failed to send notification to {}: {}", destination, message, e);
    }
  }

  //Gửi thông báo đến tất cả những sub chung
  //Topic pattern: /topic/attendance
  public void sendGeneralNotification(NotificationMessage message) {
    String destination = "/topic/attendance";
    try {
      log.info("Sending to destination: {}", destination);
      log.info("Message content: type={}, message={}, studentCIC={}",
              message.getType(), message.getMessage(), message.getStudentCIC());

      messagingTemplate.convertAndSend(destination, message);
      log.info("✅ Sent general notification: {}", message.getMessage());
    } catch (Exception e) {
      log.error("❌ Failed to send general notification: {}", message, e);
    }
  }

  //Gửi thông báo đến cả sinh viên cụ thể và general topic
  public void sendAttendanceNotification(NotificationMessage message) {
    log.info("🚀 Starting to send attendance notification for student: {}", message.getStudentCIC());

    // Đảm bảo message có đầy đủ thông tin
    if (message.getStudentCIC() == null) {
      log.error("❌ Cannot send attendance notification: studentCIC is null");
      return;
    }

    boolean sentAtLeastOne = false;

    //Gửi đến sinh viên cụ thể (PRIORITY)
    try {
      sendStudentNotification(message.getStudentCIC(), message);
      sentAtLeastOne = true;
    } catch (Exception e) {
      log.error("❌ Failed to send to student topic", e);
    }

    //Gửi đến class nếu có
    if(message.getClassCode() != null) {
      try {
        sendClassNotification(message.getClassCode(), message);
        sentAtLeastOne = true;
      } catch (Exception e) {
        log.error("❌ Failed to send to class topic", e);
      }
    }

    //Gửi đến schedule nếu có
    if(message.getScheduleId() != null) {
      try {
        sendScheduleNotification(message.getScheduleId(), message);
        sentAtLeastOne = true;
      } catch (Exception e) {
        log.error("❌ Failed to send to schedule topic", e);
      }
    }

    //Gửi đến general topic (FALLBACK)
    try {
      sendGeneralNotification(message);
      sentAtLeastOne = true;
    } catch (Exception e) {
      log.error("❌ Failed to send to general topic", e);
    }

    if (sentAtLeastOne) {
      log.info("✅ Attendance notification sent successfully for student: {}", message.getStudentCIC());
    } else {
      log.error("❌ Failed to send attendance notification to any topic for student: {}", message.getStudentCIC());
    }
  }
}