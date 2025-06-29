package com.example.attendanceservice.service;

import com.example.attendanceservice.dto.AttendanceEvent;
import com.example.attendanceservice.dto.FaceVerificationEvent;
import com.example.attendanceservice.model.Attendance;
import com.example.attendanceservice.redis.RedisService;
import com.example.attendanceservice.repository.AttendanceRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class NatsSubscriberService {

  private final Connection natsConnection;
  private final AttendanceRepository attendanceRepository;
  private final RedisService redisService;
  private final NatsPublisherService natsPublisherService;

  private ObjectMapper objectMapper;
  private Dispatcher dispatcher;

  // Standardized subject names
  private static final String FACE_VERIFICATION_SUCCESS_SUBJECT = "face.verification.success";
  private static final String FACE_VERIFICATION_FAILED_SUBJECT = "face.verification.failed";

  // Redis keys for pending attendances
  private static final String PENDING_ATTENDANCE_KEY = "pending:attendance:";
  private static final String PENDING_KEYS_SET = "pending:attendance:keys";
  private static final int PENDING_TIMEOUT_MINUTES = 5; // 5 phút timeout

  @PostConstruct
  public void init() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Create dispatcher for NATS subscriptions
    this.dispatcher = natsConnection.createDispatcher(this::handleMessage);

    // Subscribe to face verification events
    dispatcher.subscribe(FACE_VERIFICATION_SUCCESS_SUBJECT);
    dispatcher.subscribe(FACE_VERIFICATION_FAILED_SUBJECT);

    log.info("NATS Subscriber initialized and subscribed to: {}, {}",
            FACE_VERIFICATION_SUCCESS_SUBJECT, FACE_VERIFICATION_FAILED_SUBJECT);
  }

  @PreDestroy
  public void cleanup() {
    if (dispatcher != null) {
      dispatcher.unsubscribe(FACE_VERIFICATION_SUCCESS_SUBJECT);
      dispatcher.unsubscribe(FACE_VERIFICATION_FAILED_SUBJECT);
    }
    log.info("NATS Subscriber cleaned up");
  }

  @Transactional
  protected void handleMessage(Message message) {
    String subject = message.getSubject();
    String messageBody = new String(message.getData(), StandardCharsets.UTF_8);

    log.info("Received message on subject '{}': {}", subject, messageBody);

    try {
      if (FACE_VERIFICATION_SUCCESS_SUBJECT.equals(subject)) {
        handleFaceVerificationSuccess(messageBody);
      } else if (FACE_VERIFICATION_FAILED_SUBJECT.equals(subject)) {
        handleFaceVerificationFailed(messageBody);
      }
    } catch (Exception e) {
      log.error("Error handling message on subject '{}': {}", subject, messageBody, e);
    }
  }

  @Transactional
  public void handleFaceVerificationSuccess(String messageBody) {
    try {
      FaceVerificationEvent faceEvent = objectMapper.readValue(messageBody, FaceVerificationEvent.class);
      String pendingKey = PENDING_ATTENDANCE_KEY + faceEvent.getCorrelationId();

      // Lấy thông tin attendance đang pending từ Redis
      String pendingAttendanceJson = redisService.get(pendingKey);
      if (pendingAttendanceJson == null) {
        log.warn("No pending attendance found for key: {} with correlationId: {}",
                pendingKey, faceEvent.getCorrelationId());
        return;
      }

      // Parse pending attendance
      AttendanceEvent pendingAttendance = objectMapper.readValue(pendingAttendanceJson, AttendanceEvent.class);

      // Tạo Attendance entity và save vào DB
      Attendance attendance = new Attendance();
      attendance.setStudentCIC(pendingAttendance.getStudentCIC());
      attendance.setScheduleId(pendingAttendance.getScheduleId().intValue());
      attendance.setTimestamp(LocalDateTime.now());
      attendance.setIpAddress(pendingAttendance.getIpAddress());
      attendance.setDeviceInfo(pendingAttendance.getDeviceInfo());
      attendance.setLatitude(pendingAttendance.getLatitude());
      attendance.setLongtitude(pendingAttendance.getLongitude());

      attendanceRepository.save(attendance);

      // Cleanup pending records
      cleanupPendingRecord(pendingKey);

      // Publish completed event
      AttendanceEvent completedEvent = AttendanceEvent.builder()
              .studentCIC(pendingAttendance.getStudentCIC())
              .scheduleId(pendingAttendance.getScheduleId())
              .classCode(pendingAttendance.getClassCode())
              .timestamp(LocalDateTime.now())
              .success(true)
              .message("Điểm danh thành công!!!")
              .ipAddress(pendingAttendance.getIpAddress())
              .deviceInfo(pendingAttendance.getDeviceInfo())
              .latitude(pendingAttendance.getLatitude())
              .longitude(pendingAttendance.getLongitude())
              .correlationId(pendingAttendance.getCorrelationId())
              .build();

      natsPublisherService.publishAttendanceCompleted(completedEvent);

      log.info("Attendance completed successfully with correlationId: {}",
              faceEvent.getCorrelationId());

    } catch (Exception e) {
      log.error("Error processing face verification success: {}", messageBody, e);
    }
  }

  @Transactional
  public void handleFaceVerificationFailed(String messageBody) {
    try {
      FaceVerificationEvent faceEvent = objectMapper.readValue(messageBody, FaceVerificationEvent.class);
      String pendingKey = PENDING_ATTENDANCE_KEY + faceEvent.getCorrelationId();

      // Lấy thông tin attendance đang pending từ Redis
      String pendingAttendanceJson = redisService.get(pendingKey);
      if (pendingAttendanceJson == null) {
        log.warn("No pending attendance found for key: {} with correlationId: {}",
                pendingKey, faceEvent.getCorrelationId());
        return;
      }

      // Parse pending attendance
      AttendanceEvent pendingAttendance = objectMapper.readValue(pendingAttendanceJson, AttendanceEvent.class);

      // Cleanup pending records
      cleanupPendingRecord(pendingKey);

      // Publish failed event với thông tin từ face verification
      AttendanceEvent failedEvent = AttendanceEvent.builder()
              .studentCIC(pendingAttendance.getStudentCIC())
              .scheduleId(pendingAttendance.getScheduleId())
              .classCode(pendingAttendance.getClassCode())
              .timestamp(LocalDateTime.now())
              .success(false)
              .message("Điểm danh thất bại: " + faceEvent.getMessage())
              .correlationId(pendingAttendance.getCorrelationId())
              .build();

      natsPublisherService.publishAttendanceFailed(failedEvent);

      log.info("Attendance failed for student: {} in schedule: {} - Reason: {} with correlationId: {}",
              faceEvent.getStudentCIC(),
              faceEvent.getMessage(), faceEvent.getCorrelationId());

    } catch (Exception e) {
      log.error("Error processing face verification failed: {}", messageBody, e);
    }
  }

  /**
   * Save pending attendance to Redis with timeout
   */
  void savePendingAttendance(AttendanceEvent attendanceEvent) {
    try {
      String pendingKey = PENDING_ATTENDANCE_KEY + attendanceEvent.getCorrelationId();

      String attendanceJson = objectMapper.writeValueAsString(attendanceEvent);

      // Save with timeout (5 minutes)
      redisService.setWithExpiry(pendingKey, attendanceJson, PENDING_TIMEOUT_MINUTES * 60);

      // Track pending key for cleanup job
      redisService.addToSet(PENDING_KEYS_SET, pendingKey);

      log.info("Saved pending attendance for key: {} with correlationId: {}",
              pendingKey, attendanceEvent.getCorrelationId());

    } catch (Exception e) {
      log.error("Error saving pending attendance with correlationId '{}': {}",
              attendanceEvent.getCorrelationId(), attendanceEvent, e);
    }
  }

  /**
   * Cleanup expired pending attendances every 2 minutes
   */
  @Scheduled(fixedRate = 120000) // 2 phút
  public void cleanupExpiredPendingAttendances() {
    try {
      Set<String> pendingKeys = redisService.getSetMembers(PENDING_KEYS_SET);

      if (pendingKeys == null || pendingKeys.isEmpty()) {
        return;
      }

      int expiredCount = 0;
      for (String pendingKey : pendingKeys) {
        String pendingData = redisService.get(pendingKey);

        if (pendingData == null) {
          // Key đã expired, extract data từ pending key
          try {
            // Lấy correlationId từ key
            String correlationId = pendingKey.replace(PENDING_ATTENDANCE_KEY, "");

            AttendanceEvent timeoutEvent = AttendanceEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .success(false)
                    .message("Điểm danh thất bại: Hết thời gian chờ xác thực khuôn mặt (5 phút)")
                    .correlationId(correlationId)
                    .build();

            natsPublisherService.publishAttendanceFailed(timeoutEvent);
            expiredCount++;
          } catch (Exception e) {
            log.error("Error processing expired pending attendance for key: {}", pendingKey, e);
          }

          // Remove from tracking set
          redisService.removeFromSet(PENDING_KEYS_SET, pendingKey);
        }
      }

      if (expiredCount > 0) {
        log.info("Cleaned up {} expired pending attendances", expiredCount);
      }

    } catch (Exception e) {
      log.error("Error during cleanup of expired pending attendances", e);
    }
  }

  private void cleanupPendingRecord(String pendingKey) {
    redisService.delete(pendingKey);
    redisService.removeFromSet(PENDING_KEYS_SET, pendingKey);
  }
}