package com.example.attendanceservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceVerificationEvent {
  private String studentCIC;
//  private Long scheduleId;
//  private String classCode;
  private LocalDateTime timestamp;
  private boolean success;
  private String message;
  private Double confidence; // Độ tin cậy của face recognition (0.0 - 1.0)
  @JsonProperty("correlation_id")
  private String correlationId; // Thêm correlation ID để trace
}