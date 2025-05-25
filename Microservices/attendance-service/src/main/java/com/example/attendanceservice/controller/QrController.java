package com.example.attendanceservice.controller;

import com.example.attendanceservice.dto.QrGenerateResponse;
import com.example.attendanceservice.service.QrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qr")
public class QrController {
  private final QrService qrService;

  @Autowired
  public QrController(QrService qrService) {
    this.qrService = qrService;
  }

  @PostMapping("/generate")
  public ResponseEntity<QrGenerateResponse>  generateQr(
          @RequestHeader("X-User-CIC") String teacherCIC) {
    return ResponseEntity.ok(qrService.generateQrCode(teacherCIC));
  }
}
