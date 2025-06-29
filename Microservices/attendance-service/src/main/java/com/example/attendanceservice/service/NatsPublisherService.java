package com.example.attendanceservice.service;

import com.example.attendanceservice.dto.AttendanceEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nats.client.Connection;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
class NatsPublisherService {
  private final Connection natsConnection;
  private ObjectMapper objectMapper;

  private static final String QR_SCAN_SUCCESS_SUBJECT = "qr.attendance.scan.success";
  private static final String QR_SCAN_FAILED_SUBJECT = "qr.attendance.scan.failed";
  private static final String ATTENDANCE_COMPLETED_SUBJECT = "attendance.checkin.success";
  private static final String ATTENDANCE_FAILED_SUBJECT = "attendance.checkin.failed";

  @PostConstruct
  public void init() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Để LocalDateTime được serialize thành chuỗi ISO 8601
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Cấu hình thêm nếu cần
  }

  void publishQRScanFailed(AttendanceEvent attendanceEvent) {
    publisherEvent(QR_SCAN_FAILED_SUBJECT, attendanceEvent);
  }

  void publishQRScanSuccess(AttendanceEvent attendanceEvent) {
    publisherEvent(QR_SCAN_SUCCESS_SUBJECT, attendanceEvent);
  }

  void publishAttendanceCompleted(AttendanceEvent attendanceEvent) {
    publisherEvent(ATTENDANCE_COMPLETED_SUBJECT, attendanceEvent);
  }

  void publishAttendanceFailed(AttendanceEvent attendanceEvent) {
    publisherEvent(ATTENDANCE_FAILED_SUBJECT, attendanceEvent);
  }

  private void publisherEvent(String subject, AttendanceEvent event) {
    try {
      String eventJson = objectMapper.writeValueAsString(event);
      natsConnection.publish(subject, eventJson.getBytes(StandardCharsets.UTF_8));
      log.info("Publisher event to subject '{}': {}", subject, eventJson);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize attendance event: {}", event, e);
    } catch (Exception e) {
      log.error("Failed to publish event to subject '{}': {}", subject, event, e);
    }
  }
}
