package com.example.classservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
  private LocalDateTime dateTime;

  @Column(nullable = false)
  private int durationMinutes;

  @Column(nullable = false)
  private String room;

  @ManyToOne
  @JoinColumn(name = "class_code", referencedColumnName = "class_code")
  private ClassEntity classEntity;
}
