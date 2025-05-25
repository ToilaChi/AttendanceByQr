package com.example.attendanceservice.service;

import com.example.attendanceservice.dto.AttendanceRequest;
import com.example.attendanceservice.dto.AttendanceResponse;
import com.example.attendanceservice.model.Attendance;
import com.example.attendanceservice.redis.RedisQrSession;
import com.example.attendanceservice.redis.RedisService;
import com.example.attendanceservice.repository.AttendanceRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AttendanceService {
  private final RedisService qrRedisService;
  private final AttendanceRepository attendanceRepository;
  private final RestTemplate restTemplate;
  private final HttpServletRequest httpServletRequest;

  @Value("${class.service.url}")
  private String classServiceUrl;

  @Transactional
  public AttendanceResponse checkIn(AttendanceRequest attendanceRequest) {
    // Lấy qr từ redis
    RedisQrSession qrSession = qrRedisService.getQrSession(attendanceRequest.getQrSignature());
    if (qrSession == null) {
      return new AttendanceResponse("QR session không tồn tại hoặc đã hết hạn", false);
    }
    LocalDateTime now = LocalDateTime.now();

    if(now.isBefore(qrSession.getStartTime()) || now.isAfter(qrSession.getEndTime())) {
      return new AttendanceResponse("QR session không còn hiệu lực", false);
    }

    String studentCIC =  attendanceRequest.getStudentCIC();

    //Gọi class-service để kiểm tra sinh viên
    String url = classServiceUrl + "?studentCIC=" + studentCIC +  "&classCode=" + qrSession.getClassCode();
    try {
      ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
      if (Boolean.FALSE.equals(response.getBody())) {
        return new AttendanceResponse("Bạn không thuộc lớp này!!!", false);
      }
    } catch (Exception e) {
      return new AttendanceResponse("Không thể kiểm tra lớp học. Vui lòng thử lại sau.", false);
    }

    //Kiểm tra xem có điểm danh 2 lần không
    boolean alreadyCheckIn = attendanceRepository.existsByStudentCICAndScheduleId(studentCIC, qrSession.getScheduleId());
    if(alreadyCheckIn) {
      return new AttendanceResponse("Bạn đã điểm danh trước đó rồi", false);
    }

    //Lấy ip client
    String ipAddress = extractClientIp();

    Attendance attendance = new Attendance();
    attendance.setStudentCIC(studentCIC);
    attendance.setScheduleId(qrSession.getScheduleId());
    attendance.setTimestamp(now);
    attendance.setIpAddress(ipAddress);
    attendance.setDeviceInfo(attendanceRequest.getDeviceInfo());
    attendance.setLocationInfo(attendanceRequest.getLocationInfo());

    attendanceRepository.save(attendance);

    //Sau này ứng dụng NATS vào đây để thông báo

    return new AttendanceResponse("Bạn đã điểm danh thành công!!!", true);
  }

  private String extractClientIp() {
    String xHeader =  httpServletRequest.getHeader("X-Forwarded-For");
    if (xHeader == null) {
      xHeader = httpServletRequest.getRemoteAddr();
    }
    return xHeader.split(",")[0];
  }
}
