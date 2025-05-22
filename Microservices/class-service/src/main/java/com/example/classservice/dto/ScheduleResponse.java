package com.example.classservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleResponse {
  private String classCode;
  private String className;
  private String subjectName;
  private String day_of_week;
  private String startTime;
  private String endTime;
  private String startDate;
  private String endDate;
  private String room;
}
