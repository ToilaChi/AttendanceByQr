package com.example.attendanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QRRequest {
  private String qrSignature;
  private String studentCIC;
  private String deviceInfo;
  private Double latitude;
  private Double longitude;
}
