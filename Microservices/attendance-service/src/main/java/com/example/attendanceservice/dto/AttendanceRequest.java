package com.example.attendanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceRequest {
  private String qrSignature;
  private String studentCIC;
  private String deviceInfo;
  private String locationInfo;
}
