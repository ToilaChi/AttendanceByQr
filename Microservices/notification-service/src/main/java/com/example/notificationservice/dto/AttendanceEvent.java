package com.example.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceEvent {
  private String studentCIC;
  private Long scheduleId;
  private String classCode;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;

  private boolean success;
  private String message;
  private String ipAddress;
  private String deviceInfo;
  private String latitude;
  private String longitude;
}
