package com.example.attendanceservice.controller;

import com.example.attendanceservice.dto.AttendanceRequest;
import com.example.attendanceservice.dto.AttendanceResponse;
import com.example.attendanceservice.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendances")
@RequiredArgsConstructor
public class AttendanceController {
  private final AttendanceService attendanceService;

  @PostMapping()
  public ResponseEntity<Void> checkIn(@RequestBody AttendanceRequest attendanceRequest) {
    attendanceService.checkIn(attendanceRequest);
    return ResponseEntity.accepted().build();
  }
  @GetMapping("/status")
  public ResponseEntity<AttendanceResponse> checkAttendanceStatus(
          @RequestParam String studentCIC,
          @RequestParam int scheduleId,
          @RequestParam String date) {

    AttendanceResponse response = attendanceService.checkAttendanceStatus(studentCIC, scheduleId, date);
    return ResponseEntity.ok(response);
  }
}
