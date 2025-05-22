package com.example.classservice.controller;

import com.example.classservice.dto.ScheduleResponse;
import com.example.classservice.service.ScheduleService;
import com.example.classservice.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {
  @Autowired
  private ScheduleService scheduleService;

  @GetMapping("/student/{cic}")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getStudentSchedule
          (@PathVariable("cic") String studentCIC,
           @RequestParam(required = false) Integer day,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date
           ){
    ApiResponse<List<ScheduleResponse>> listApiResponse = scheduleService.getStudentSchedule(studentCIC, day, date);
    return new ResponseEntity<>(listApiResponse, HttpStatus.OK);
  }

  @GetMapping("/teacher/{cic}")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getTeacherSchedule
          (@PathVariable("cic") String teacherCIC,
           @RequestParam(required = false) Integer day,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date
          ){
    ApiResponse<List<ScheduleResponse>> listApiResponse = scheduleService.getTeacherSchedule(teacherCIC, day, date);
    return new ResponseEntity<>(listApiResponse, HttpStatus.OK);
  }
}
