package com.example.attendanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEvent {
  private String studentCIC;
  private Long scheduleId;
  private String classCode;
  private LocalDateTime timestamp;
  private boolean success;
  private String message;
  private String ipAddress;
  private String deviceInfo;
  private Double latitude;
  private Double longitude;

  // Constructor cho success event
  public AttendanceEvent(String studentCIC, Long scheduleId, String classCode,
                            LocalDateTime timestamp, String message, String ipAddress,
                            String deviceInfo, Double latitude, Double longitude) {
    this.studentCIC = studentCIC;
    this.scheduleId = scheduleId;
    this.classCode = classCode;
    this.timestamp = timestamp;
    this.success = true;
    this.message = message;
    this.ipAddress = ipAddress;
    this.deviceInfo = deviceInfo;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  // Constructor cho failed event
  public AttendanceEvent(String studentCIC, Long scheduleId, String classCode,
                            LocalDateTime timestamp, String message) {
    this.studentCIC = studentCIC;
    this.scheduleId = scheduleId;
    this.classCode = classCode;
    this.timestamp = timestamp;
    this.success = false;
    this.message = message;
  }
}
