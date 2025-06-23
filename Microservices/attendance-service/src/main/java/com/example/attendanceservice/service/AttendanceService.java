package com.example.attendanceservice.service;

import com.example.attendanceservice.client.ClassServiceClient;
import com.example.attendanceservice.dto.AttendanceEvent;
import com.example.attendanceservice.dto.AttendanceRequest;
import com.example.attendanceservice.dto.AttendanceResponse;
import com.example.attendanceservice.model.Attendance;
import com.example.attendanceservice.redis.RedisQrSession;
import com.example.attendanceservice.redis.RedisService;
import com.example.attendanceservice.repository.AttendanceRepository;
import com.example.classservice.dto.ScheduleResponse;
import com.example.classservice.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {
  private final RedisService qrRedisService;
  private final AttendanceRepository attendanceRepository;
  private final HttpServletRequest httpServletRequest;
  private final ClassServiceClient classServiceClient;
  private final NatsPublisherService natsPublisherService;

  @Transactional
  public void checkIn(AttendanceRequest attendanceRequest) {
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
      return;
    }

    if(now.isBefore(qrSession.getStartTime()) || now.isAfter(qrSession.getEndTime())) {
      String message = "QR session không còn hiệu lực";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishAttendanceFailed(failedEvent);
      return;
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
        return;
      }
    } catch (Exception e) {
      System.out.println("Class Code 2: "  + qrSession.getClassCode());
      String message = "Không thể kiểm tra lớp học. Vui lòng thử lại sau.";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishAttendanceFailed(failedEvent);
      return;
    }

    LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
    LocalDateTime startOfNextDay = startOfDay.plusDays(1);
    //Kiểm tra xem có điểm danh 2 lần không
    boolean alreadyCheckIn = attendanceRepository.existsByStudentCicAndScheduleIdAndDateRange(
            studentCIC, qrSession.getScheduleId(), startOfDay, startOfNextDay);
    System.out.println("Already check in? " + alreadyCheckIn);
    if(alreadyCheckIn) {
      String message = "Bạn đã điểm danh trước đó rồi";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishAttendanceFailed(failedEvent);
      return;
    }

    //Lấy ip client
    String ipAddress = extractClientIp();

    //Kiểm tra điểm danh hộ
    List<Attendance> sameDeviceAttendances = attendanceRepository.findByIpAndDeviceAndDateRange(
            ipAddress, attendanceRequest.getDeviceInfo(), startOfDay, startOfNextDay);

    List<Attendance> suspiciousAttendances = sameDeviceAttendances.stream()
            .filter(a -> !a.getStudentCIC().equals(studentCIC))
            .toList();

    if(!suspiciousAttendances.isEmpty()) {
      //Phân loại theo scheduleId
      Map<Integer, List<Attendance>> groupBySchedule =suspiciousAttendances.stream()
              .collect(Collectors.groupingBy(Attendance::getScheduleId));

      StringBuilder messageBuilder = new StringBuilder("Cảnh báo: thiết bị này được sử dụng bởi sinh viên khác hôm nay: ");

      for (Map.Entry<Integer, List<Attendance>> entry : groupBySchedule.entrySet()) {
        int scheduleId = entry.getKey();
        List<String> students = entry.getValue().stream()
                .map(Attendance::getStudentCIC)
                .distinct()
                .toList();

        if(scheduleId == qrSession.getScheduleId()) {
          messageBuilder.append("[CÙNG CA HỌC]");
        }
        else {
          messageBuilder.append("[KHÁC CA HỌC]");
        }
        messageBuilder.append("Schedule ").append(scheduleId)
                .append(": ").append(String.join(", ", students))
                .append("; ");
      }
      String message = messageBuilder + "Nghi ngờ điểm danh hộ, nếu có nhầm lẫn hãy thông báo với giảng viên";

      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishAttendanceFailed(failedEvent);
      return;
    }

    Attendance attendance = new Attendance();
    attendance.setStudentCIC(studentCIC);
    attendance.setScheduleId(qrSession.getScheduleId());
    attendance.setTimestamp(now);
    attendance.setIpAddress(ipAddress);
    attendance.setDeviceInfo(attendanceRequest.getDeviceInfo());
    attendance.setLatitude(attendanceRequest.getLatitude());
    attendance.setLongtitude(attendanceRequest.getLongitude());

    attendanceRepository.save(attendance);

    // Publish success event
    String successMessage = "Bạn đã điểm danh thành công!!!";
    AttendanceEvent successEvent = new AttendanceEvent(
            studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now,
            successMessage, ipAddress, attendanceRequest.getDeviceInfo(),
            attendanceRequest.getLatitude(), attendanceRequest.getLongitude()
    );
    natsPublisherService.publishAttendanceSuccess(successEvent);
  }

  @Transactional(readOnly = true)
  public AttendanceResponse checkAttendanceStatus(String studentCIC, int scheduleId, String date) {
    try {
      // Gọi class-service để lấy schedule của ngày đó
      LocalDate targetDate = LocalDate.parse(date);
      ApiResponse<List<ScheduleResponse>> response =
              classServiceClient.getStudentSchedule(studentCIC, targetDate);

      if (response == null || response.getData().isEmpty()) {
        return new AttendanceResponse("Không tìm thấy lịch học", false);
      }

      ScheduleResponse schedule = response.getData().get(0);

      // Kiểm tra xem sinh viên đã điểm danh chưa
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
      LocalDateTime startOfNextDay = startOfDay.plusDays(1);
      boolean hasAttended = attendanceRepository.existsByStudentCicAndScheduleIdAndDateRange(
              studentCIC, scheduleId, startOfDay, startOfNextDay);

      System.out.println("Check " + hasAttended);
      String message = hasAttended ? "Đã điểm danh" : "Chưa điểm danh";

      return new AttendanceResponse(message, true);

    } catch (Exception e) {
      return new AttendanceResponse("Không thể kiểm tra trạng thái điểm danh", false);
    }
  }

  private String extractClientIp() {
    String xHeader =  httpServletRequest.getHeader("X-Forwarded-For");
    if (xHeader == null) {
      xHeader = httpServletRequest.getRemoteAddr();
    }
    return xHeader.split(",")[0];
  }
}
