package com.example.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage {
  private String type;
  private String studentCIC;
  private Long scheduleId;
  private String classCode;
  private String message;

  private LocalDateTime timestamp;

  private String ipAddress;
  private String deviceInfo;
  private String latitude;
  private String longitude;

  public static NotificationMessage fromAttendanceEvent(AttendanceEvent event, String type) {
    return new NotificationMessage(
            type,
            event.getStudentCIC(),
            event.getScheduleId(),
            event.getClassCode(),
            event.getMessage(),
            event.getTimestamp(),
            event.getIpAddress(),
            event.getDeviceInfo(),
            event.getLatitude(),
            event.getLongitude()
    );
  }
}

