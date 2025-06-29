package com.example.attendanceservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QRResponse {
  private String message;
  private boolean success;
  @JsonProperty("correlation_id")
  private String correlationId;

  public QRResponse(String message, boolean success) {
    this.message = message;
    this.success = success;
  }
}
