package com.example.classservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "schedule")
public class Schedule {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String teacherCIC;

  @Column(nullable = false)
  private Integer day_of_week;

  @Column(nullable = false)
  private LocalTime start_time;

  @Column(nullable = false)
  private LocalTime end_time;

  @Column(nullable = false)
  private LocalDate start_date;

  @Column(nullable = false)
  private LocalDate end_date;

  @Column(nullable = false)
  private String room;

  @ManyToOne
  @JoinColumn(name = "class_code", referencedColumnName = "class_code")
  private ClassEntity classEntity;
}
