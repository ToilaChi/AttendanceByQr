package com.example.attendanceservice.redis;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RedisQrSession implements Serializable {
  private String qrSignature;
  private int scheduleId;
  private String teacherCIC;
  private String classCode;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}

