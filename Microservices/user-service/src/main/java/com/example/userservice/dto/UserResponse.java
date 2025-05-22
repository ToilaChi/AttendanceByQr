package com.example.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
  private String CIC;
  private String fullName;
  private String email;
  private String phone;
  private String gender;
  private Date dateOfBirth;
  private String role;
  private String studentCode;
  private String teacherCode;
  private String regularClassCode;
}
