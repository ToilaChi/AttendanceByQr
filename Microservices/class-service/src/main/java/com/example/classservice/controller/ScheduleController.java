package com.example.classservice.controller;

import com.example.classservice.dto.ScheduleResponse;
import com.example.classservice.service.ScheduleService;
import com.example.classservice.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {
  @Autowired
  private ScheduleService scheduleService;

  @GetMapping("/student")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getStudentSchedule(
           @RequestHeader("X-User-CIC") String studentCIC,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate currentDate
           ){
    ApiResponse<List<ScheduleResponse>> listApiResponse = scheduleService.getStudentSchedule(studentCIC, currentDate);
    return new ResponseEntity<>(listApiResponse, HttpStatus.OK);
  }

  @GetMapping("/teacher")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getTeacherSchedule(
           @RequestHeader("X-User-CIC") String teacherCIC,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate currentDate
          ){
    ApiResponse<List<ScheduleResponse>> listApiResponse = scheduleService.getTeacherSchedule(teacherCIC, currentDate);
    return new ResponseEntity<>(listApiResponse, HttpStatus.OK);
  }
}
