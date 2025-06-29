package com.example.attendanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
  private String correlationId; // Thêm correlation ID để trace

  // Constructor cho success event (backward compatibility)
  public AttendanceEvent(String studentCIC, Long scheduleId, String classCode,
                         LocalDateTime timestamp, String message, String ipAddress,
                         String deviceInfo, Double latitude, Double longitude, String correlationId) {
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
    this.correlationId = correlationId;
  }

  // Constructor cho failed event (backward compatibility)
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