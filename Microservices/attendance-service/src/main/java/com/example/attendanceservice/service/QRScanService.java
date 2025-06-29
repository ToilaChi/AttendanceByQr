package com.example.attendanceservice.service;

import com.example.attendanceservice.client.ClassServiceClient;
import com.example.attendanceservice.dto.AttendanceEvent;
import com.example.attendanceservice.dto.QRRequest;
import com.example.attendanceservice.dto.QRResponse;
import com.example.attendanceservice.model.Attendance;
import com.example.attendanceservice.redis.RedisQrSession;
import com.example.attendanceservice.redis.RedisService;
import com.example.attendanceservice.repository.AttendanceRepository;
import com.example.attendanceservice.util.IPUtils;
import com.example.classservice.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QRScanService {
  private final RedisService qrRedisService;
  private final AttendanceRepository attendanceRepository;
  private final ClassServiceClient classServiceClient;
  private final NatsPublisherService natsPublisherService;

  private final NatsSubscriberService natsSubscriberService;

  @Transactional
  public QRResponse qrScan(QRRequest qrRequest, HttpServletRequest request) {
    String studentCIC =  qrRequest.getStudentCIC();
    LocalDateTime now = LocalDateTime.now();
    String correlationId = UUID.randomUUID().toString();

    // Lấy qr từ redis
    RedisQrSession qrSession = qrRedisService.getQrSession(qrRequest.getQrSignature());
    if (qrSession == null) {
      String message = "QR session không tồn tại hoặc đã hết hạn";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, null, null, now, message
      );
      natsPublisherService.publishQRScanFailed(failedEvent);
      return new QRResponse(message, false);
    }

    if(now.isBefore(qrSession.getStartTime()) || now.isAfter(qrSession.getEndTime())) {
      String message = "QR session không còn hiệu lực";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishQRScanFailed(failedEvent);
      return new  QRResponse(message, false);
    }

    //Gọi class-service để kiểm tra sinh viên
    try {
      ResponseEntity<ApiResponse<Boolean>> response = classServiceClient.checkStudentEnrollment(studentCIC, qrSession.getClassCode());
      ApiResponse<Boolean> body = response.getBody();
      if (body == null || Boolean.FALSE.equals(body.getData())) {
        String message = "Bạn không thuộc lớp này!!!";
        //Publish failed event
        AttendanceEvent failedEvent = new AttendanceEvent(
                studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
        );
        natsPublisherService.publishQRScanFailed(failedEvent);
        return new  QRResponse(message, false);
      }
    } catch (Exception e) {
      String message = "Không thể kiểm tra lớp học. Vui lòng thử lại sau.";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishQRScanFailed(failedEvent);
      return new  QRResponse(message, false);
    }

    //Kiểm tra thông tin thiết bị
    String device = qrRequest.getDeviceInfo();
    if(device.isEmpty() || device.equals("Unknown Device")) {
      String message = "Bạn phải cung cấp thông tin thiết bị";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishQRScanFailed(failedEvent);
      return new  QRResponse(message, false);
    }

    //Kiểm tra vị trí
    Double latitude = qrRequest.getLatitude();
    Double longitude = qrRequest.getLongitude();
    if(latitude == null || longitude == null) {
      String message = "Bạn phải cung cấp vị trí của mình";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishQRScanFailed(failedEvent);
      return new  QRResponse(message, false);
    }

    LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
    LocalDateTime startOfNextDay = startOfDay.plusDays(1);
    //Kiểm tra xem có điểm danh 2 lần không
    boolean alreadyCheckIn = attendanceRepository.existsByStudentCicAndScheduleIdAndDateRange(
            studentCIC, qrSession.getScheduleId(), startOfDay, startOfNextDay);
    if(alreadyCheckIn) {
      String message = "Bạn đã điểm danh trước đó rồi";
      //Publish failed event
      AttendanceEvent failedEvent = new AttendanceEvent(
              studentCIC, (long) qrSession.getScheduleId(), qrSession.getClassCode(), now, message
      );
      natsPublisherService.publishQRScanFailed(failedEvent);
      return new  QRResponse(message, false);
    }

    String clientIP = IPUtils.getClientIP(request);
    //Kiểm tra điểm danh hộ
    List<Attendance> sameDeviceAttendances = attendanceRepository.findByIpAndDeviceAndDateRange(
            clientIP, qrRequest.getDeviceInfo(), startOfDay, startOfNextDay);

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
      natsPublisherService.publishQRScanFailed(failedEvent);
      return new  QRResponse(message, false);
    }

    AttendanceEvent pendingEvent = AttendanceEvent.builder()
            .studentCIC(studentCIC)
            .scheduleId((long) qrSession.getScheduleId())
            .classCode(qrSession.getClassCode())
            .timestamp(now)
            .message("QR scan thành công, chờ face verification")
            .ipAddress(clientIP)
            .deviceInfo(qrRequest.getDeviceInfo())
            .latitude(qrRequest.getLatitude())
            .longitude(qrRequest.getLongitude())
            .correlationId(correlationId)
            .build();

    // Save to pending state
    natsSubscriberService.savePendingAttendance(pendingEvent);

    // Publish success event
    String successMessage = "QR scan thành công, vui lòng tiếp tục với xác thực khuôn mặt!!!";
    AttendanceEvent successEvent = AttendanceEvent.builder()
            .studentCIC(studentCIC)
            .scheduleId((long) qrSession.getScheduleId())
            .classCode(qrSession.getClassCode())
            .timestamp(now)
            .success(true)
            .message(successMessage)
            .ipAddress(clientIP)
            .deviceInfo(qrRequest.getDeviceInfo())
            .latitude(qrRequest.getLatitude())
            .longitude(qrRequest.getLongitude())
            .correlationId(correlationId)
            .build();
    natsPublisherService.publishQRScanSuccess(successEvent);

    return new QRResponse("Xác thực khuôn mặt thành công, vui lòng tiếp tục với bước xác thực khuôn mặt!!!", true, correlationId);
  }
}
