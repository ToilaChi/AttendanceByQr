package com.example.attendanceservice.controller;

import com.example.attendanceservice.dto.QRRequest;
import com.example.attendanceservice.dto.QRResponse;
import com.example.attendanceservice.service.AttendanceService;
import com.example.attendanceservice.service.QRScanService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendances")
@RequiredArgsConstructor
public class AttendanceController {
  private final AttendanceService attendanceService;
  private final QRScanService  qrScanService;

  @PostMapping("/qr-scan")
  public ResponseEntity<QRResponse> qrScan(@RequestBody QRRequest qrRequest,
                                      HttpServletRequest request) {
    QRResponse qrResponse = qrScanService.qrScan(qrRequest, request);

    if (!qrResponse.isSuccess()) {
      return ResponseEntity.badRequest().body(qrResponse);
    }

    return ResponseEntity.ok(qrResponse);
  }

  @GetMapping("/status")
  public ResponseEntity<QRResponse> checkAttendanceStatus(
          @RequestParam String studentCIC,
          @RequestParam int scheduleId,
          @RequestParam String date) {

    QRResponse response = attendanceService.checkAttendanceStatus(studentCIC, scheduleId, date);
    return ResponseEntity.ok(response);
  }
}
