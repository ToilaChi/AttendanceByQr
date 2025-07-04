package com.example.attendanceservice.service;

import com.example.attendanceservice.client.ClassServiceClient;
import com.example.attendanceservice.dto.QRResponse;
import com.example.attendanceservice.redis.RedisService;
import com.example.attendanceservice.repository.AttendanceRepository;
import com.example.classservice.dto.ScheduleResponse;
import com.example.classservice.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {
  private final RedisService qrRedisService;
  private final AttendanceRepository attendanceRepository;
  private final ClassServiceClient classServiceClient;
  private final NatsPublisherService natsPublisherService;

//  @Transactional
//  public void checkIn(AttendanceRequest attendanceRequest, HttpServletRequest request) {
//    String studentCIC =  attendanceRequest.getStudentCIC();
//    LocalDateTime now = LocalDateTime.now();
//
//
//    // Lấy qr từ redis
//    RedisQrSession qrSession = qrRedisService.getQrSession(attendanceRequest.getQrSignature());
//    if (qrSession == null) {
//      String message = "QR session không tồn tại hoặc đã hết hạn";
//      //Publish failed event
//      AttendanceEvent failedEvent = new AttendanceEvent(
//              studentCIC, null, null, now, message
//      );
//      natsPublisherService.publishAttendanceFailed(failedEvent);
//      return;
//    }
//
//    if(now.isBefore(qrSession.getStartTime()) || now.isAfter(qrSession.getEndTime())) {
//      String message = "QR session không còn hiệu lực";
//      //Publish failed event
//      AttendanceEvent failedEvent = new AttendanceEvent(
//              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
//      );
//      natsPublisherService.publishAttendanceFailed(failedEvent);
//      return;
//    }
//
//    //Gọi class-service để kiểm tra sinh viên
//    try {
//      ResponseEntity<ApiResponse<Boolean>> response = classServiceClient.checkStudentEnrollment(studentCIC, qrSession.getClassCode());
//      ApiResponse<Boolean> body = response.getBody();
//      if (body == null || Boolean.FALSE.equals(body.getData())) {
//        String message = "Bạn không thuộc lớp này!!!";
//        //Publish failed event
//        AttendanceEvent failedEvent = new AttendanceEvent(
//                studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
//        );
//        natsPublisherService.publishAttendanceFailed(failedEvent);
//        return;
//      }
//    } catch (Exception e) {
//      String message = "Không thể kiểm tra lớp học. Vui lòng thử lại sau.";
//      //Publish failed event
//      AttendanceEvent failedEvent = new AttendanceEvent(
//              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
//      );
//      natsPublisherService.publishAttendanceFailed(failedEvent);
//      return;
//    }
//
//    //Kiểm tra thông tin thiết bị
//    String device = attendanceRequest.getDeviceInfo();
//    if(device.isEmpty() || device.equals("Unknown Device")) {
//      String message = "Bạn phải cung cấp thông tin thiết bị";
//      //Publish failed event
//      AttendanceEvent failedEvent = new AttendanceEvent(
//              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
//      );
//      natsPublisherService.publishAttendanceFailed(failedEvent);
//      return;
//    }
//
//    //Kiểm tra vị trí
//    Double latitude = attendanceRequest.getLatitude();
//    Double longitude = attendanceRequest.getLongitude();
//    if(latitude == null || longitude == null) {
//      String message = "Bạn phải cung cấp vị trí của mình";
//      //Publish failed event
//      AttendanceEvent failedEvent = new AttendanceEvent(
//              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
//      );
//      natsPublisherService.publishAttendanceFailed(failedEvent);
//      return;
//    }
//
//    LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
//    LocalDateTime startOfNextDay = startOfDay.plusDays(1);
//    //Kiểm tra xem có điểm danh 2 lần không
//    boolean alreadyCheckIn = attendanceRepository.existsByStudentCicAndScheduleIdAndDateRange(
//            studentCIC, qrSession.getScheduleId(), startOfDay, startOfNextDay);
//    if(alreadyCheckIn) {
//      String message = "Bạn đã điểm danh trước đó rồi";
//      //Publish failed event
//      AttendanceEvent failedEvent = new AttendanceEvent(
//              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
//      );
//      natsPublisherService.publishAttendanceFailed(failedEvent);
//      return;
//    }
//
//    String clientIP = IPUtils.getClientIP(request);
//    //Kiểm tra điểm danh hộ
//    List<Attendance> sameDeviceAttendances = attendanceRepository.findByIpAndDeviceAndDateRange(
//            clientIP, attendanceRequest.getDeviceInfo(), startOfDay, startOfNextDay);
//
//    List<Attendance> suspiciousAttendances = sameDeviceAttendances.stream()
//            .filter(a -> !a.getStudentCIC().equals(studentCIC))
//            .toList();
//
//    if(!suspiciousAttendances.isEmpty()) {
//      //Phân loại theo scheduleId
//      Map<Integer, List<Attendance>> groupBySchedule =suspiciousAttendances.stream()
//              .collect(Collectors.groupingBy(Attendance::getScheduleId));
//
//      StringBuilder messageBuilder = new StringBuilder("Cảnh báo: thiết bị này được sử dụng bởi sinh viên khác hôm nay: ");
//
//      for (Map.Entry<Integer, List<Attendance>> entry : groupBySchedule.entrySet()) {
//        int scheduleId = entry.getKey();
//        List<String> students = entry.getValue().stream()
//                .map(Attendance::getStudentCIC)
//                .distinct()
//                .toList();
//
//        if(scheduleId == qrSession.getScheduleId()) {
//          messageBuilder.append("[CÙNG CA HỌC]");
//        }
//        else {
//          messageBuilder.append("[KHÁC CA HỌC]");
//        }
//        messageBuilder.append("Schedule ").append(scheduleId)
//                .append(": ").append(String.join(", ", students))
//                .append("; ");
//      }
//      String message = messageBuilder + "Nghi ngờ điểm danh hộ, nếu có nhầm lẫn hãy thông báo với giảng viên";
//
//      //Publish failed event
//      AttendanceEvent failedEvent = new AttendanceEvent(
//              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
//      );
//      natsPublisherService.publishAttendanceFailed(failedEvent);
//      return;
//    }
//
//    Attendance attendance = new Attendance();
//    attendance.setStudentCIC(studentCIC);
//    attendance.setScheduleId(qrSession.getScheduleId());
//    attendance.setTimestamp(now);
//    attendance.setIpAddress(clientIP);
//    attendance.setDeviceInfo(attendanceRequest.getDeviceInfo());
//    attendance.setLatitude(attendanceRequest.getLatitude());
//    attendance.setLongtitude(attendanceRequest.getLongitude());
//
//    attendanceRepository.save(attendance);
//
//    // Publish success event
//    String successMessage = "Bạn đã điểm danh thành công!!!";
//    AttendanceEvent successEvent = new AttendanceEvent(
//            studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now,
//            successMessage, clientIP, attendanceRequest.getDeviceInfo(),
//            attendanceRequest.getLatitude(), attendanceRequest.getLongitude()
//    );
//    natsPublisherService.publishAttendanceSuccess(successEvent);
//  }

  @Transactional(readOnly = true)
  public QRResponse checkAttendanceStatus(String studentCIC, int scheduleId, String date) {
    try {
      // Gọi class-service để lấy schedule của ngày đó
      LocalDate targetDate = LocalDate.parse(date);
      ApiResponse<List<ScheduleResponse>> response =
              classServiceClient.getStudentSchedule(studentCIC, targetDate);

      if (response == null || response.getData().isEmpty()) {
        return new QRResponse("Không tìm thấy lịch học", false);
      }

      // Kiểm tra xem sinh viên đã điểm danh chưa
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
      LocalDateTime startOfNextDay = startOfDay.plusDays(1);
      boolean hasAttended = attendanceRepository.existsByStudentCicAndScheduleIdAndDateRange(
              studentCIC, scheduleId, startOfDay, startOfNextDay);

      String message = hasAttended ? "Đã điểm danh" : "Chưa điểm danh";

      return new QRResponse(message, true);

    } catch (Exception e) {
      return new QRResponse("Không thể kiểm tra trạng thái điểm danh", false);
    }
  }
}
