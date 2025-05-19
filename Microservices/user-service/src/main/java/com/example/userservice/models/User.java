package com.example.userservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_profiles")
public class User {
  @Id
  @Column(nullable = false, unique = true)
  private String CIC;

  @Column
  private String fullName;

  @Column
  private String email;

  @Column
  private String phone;

  @Column
  private String gender;

  @Column
  private Date dateOfBirth;

  @Enumerated(EnumType.STRING)
  private Role role;

  @Column
  private String studentCode;

  @Column
  private String teacherCode;

  @Column
  private String classCode;

  @Column
  private LocalDate createdAt;
}
