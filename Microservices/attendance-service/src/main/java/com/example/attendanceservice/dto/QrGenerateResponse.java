package com.example.attendanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QrGenerateResponse {
  private String qrSignature;
  private Long expiredTime;
  private String qrImageBase64;
  private String qrContent;
}
