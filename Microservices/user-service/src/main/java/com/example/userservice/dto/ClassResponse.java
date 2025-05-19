package com.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassResponse {
  private String classCode;
  private String className;
  private String subjectName;
  private String teacherCIC;
}
