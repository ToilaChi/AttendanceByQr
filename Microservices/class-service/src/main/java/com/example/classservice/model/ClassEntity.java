package com.example.classservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "classes")
public class ClassEntity {
  @Id
  @Column(name = "class_code", nullable = false, unique = true)
  private String classCode;

  @Column(nullable = false)
  private String className;

  @Column(nullable = false)
  private String subjectName;

  @Column
  private String teacherCIC;

  @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL)
  private List<Schedule> schedules;

  @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL)
  private List<Enrollment> enrollments;
}
