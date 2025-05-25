package com.example.attendanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "attendance")
public class Attendance {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String studentCIC;

  @Column(nullable = false)
  private int scheduleId;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @Column
  private String ipAddress;

  @Column
  private String deviceInfo;

  @Column
  private String locationInfo;
}
