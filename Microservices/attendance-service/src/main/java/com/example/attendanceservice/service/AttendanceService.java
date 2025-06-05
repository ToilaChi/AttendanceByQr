package com.example.attendanceservice.service;

import com.example.attendanceservice.client.ClassServiceClient;
import com.example.attendanceservice.dto.AttendanceEvent;
import com.example.attendanceservice.dto.AttendanceRequest;
import com.example.attendanceservice.dto.AttendanceResponse;
import com.example.attendanceservice.model.Attendance;
import com.example.attendanceservice.redis.RedisQrSession;
import com.example.attendanceservice.redis.RedisService;
import com.example.attendanceservice.repository.AttendanceRepository;
import com.example.classservice.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AttendanceService {
  private final RedisService qrRedisService;
  private final AttendanceRepository attendanceRepository;
  private final HttpServletRequest httpServletRequest;
  private final ClassServiceClient classServiceClient;
  private final NatsPublisherService natsPublisherService;

  @Transactional
  public AttendanceResponse checkIn(AttendanceRequest attendanceRequest) {
    String studentCIC =  attendanceRequest.getStudentCIC();
    LocalDateTime now = LocalDateTime.now();
    // Lấy qr từ redis
    RedisQrSession qrSession = qrRedisService.getQrSession(attendanceRequest.getQrSignature());
    if (qrSession == null) {
      String message = "QR session không tồn tại hoặc đã hết hạn";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, null, null, now, message
      );
      natsPublisherService.publishAttendanceFailed(failedEvent);
      return new AttendanceResponse(message, false);
    }

    if(now.isBefore(qrSession.getStartTime()) || now.isAfter(qrSession.getEndTime())) {
      String message = "QR session không còn hiệu lực";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishAttendanceFailed(failedEvent);
      return new AttendanceResponse(message, false);
    }

    //Gọi class-service để kiểm tra sinh viên
    try {
      System.out.println("Class Code 0: "  + qrSession.getClassCode());
      ResponseEntity<ApiResponse<Boolean>> response = classServiceClient.checkStudentEnrollment(studentCIC, qrSession.getClassCode());
      ApiResponse<Boolean> body = response.getBody();
      System.out.println("Response: " + response);
      System.out.println("Body: " + body);
      if (body == null || Boolean.FALSE.equals(body.getData())) {
        System.out.println("Class Code 1: "  + qrSession.getClassCode());
        String message = "Bạn không thuộc lớp này!!!";
        //Publish failed event
        AttendanceEvent failedEvent = new AttendanceEvent(
                studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
        );
        natsPublisherService.publishAttendanceFailed(failedEvent);
        return new AttendanceResponse(message, false);
      }
    } catch (Exception e) {
      System.out.println("Class Code 2: "  + qrSession.getClassCode());
      String message = "Không thể kiểm tra lớp học. Vui lòng thử lại sau.";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishAttendanceFailed(failedEvent);
      return new AttendanceResponse(message, false);
    }

    //Kiểm tra xem có điểm danh 2 lần không
    boolean alreadyCheckIn = attendanceRepository.existsByStudentCICAndScheduleId(studentCIC, qrSession.getScheduleId());
    if(alreadyCheckIn) {
      String message = "Bạn đã điểm danh trước đó rồi";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishAttendanceFailed(failedEvent);
      return new AttendanceResponse(message, false);
    }

    //Lấy ip client
    String ipAddress = extractClientIp();

    Attendance attendance = new Attendance();
    attendance.setStudentCIC(studentCIC);
    attendance.setScheduleId(qrSession.getScheduleId());
    attendance.setTimestamp(now);
    attendance.setIpAddress(ipAddress);
    attendance.setDeviceInfo(attendanceRequest.getDeviceInfo());
    attendance.setLatitude(attendanceRequest.getLatitude());
    attendance.setLongtitude(attendanceRequest.getLongtitude());

    attendanceRepository.save(attendance);

    // Publish success event
    String successMessage = "Bạn đã điểm danh thành công!!!";
    AttendanceEvent successEvent = new AttendanceEvent(
            studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now,
            successMessage, ipAddress, attendanceRequest.getDeviceInfo(),
            attendanceRequest.getLatitude(), attendanceRequest.getLongtitude()
    );
    natsPublisherService.publishAttendanceSuccess(successEvent);

    return new AttendanceResponse(successMessage, true);
  }

  private String extractClientIp() {
    String xHeader =  httpServletRequest.getHeader("X-Forwarded-For");
    if (xHeader == null) {
      xHeader = httpServletRequest.getRemoteAddr();
    }
    return xHeader.split(",")[0];
  }
}
