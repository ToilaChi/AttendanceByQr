package com.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentResponse {
  private String fullName;
  private String studentCode;
  private String CIC;
  private String email;
  private String phone;
  private String gender;
  private Date dateOfBirth;
  private String regularClassCode;
}
