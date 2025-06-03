package com.example.attendanceservice.service;

import com.example.attendanceservice.client.ClassServiceClient;
import com.example.attendanceservice.dto.QrGenerateResponse;
import com.example.attendanceservice.redis.RedisQrSession;
import com.example.attendanceservice.redis.RedisService;
import com.example.classservice.dto.ScheduleResponse;
import com.example.classservice.util.ApiResponse;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.ws.rs.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;

@Service
public class QrService {
  private final RedisService redisService;
  private final ClassServiceClient classServiceClient;

  @Value("${app.qr.base-url}")
  private String baseUrl;

  public QrService(RedisService redisService, ClassServiceClient classServiceClient) {
    this.redisService = redisService;
    this.classServiceClient = classServiceClient;
  }

  public QrGenerateResponse generateQrCode(String teacherCIC) {
    //Gọi đến class-service để lấy thông tin lịch học
    LocalDate today =  LocalDate.now();
    ApiResponse<List<ScheduleResponse>> response = classServiceClient.getTeacherSchedule(teacherCIC,  today);

    System.out.println("Finding schedule for teacherCIC: " + teacherCIC + " on date: " + today);
    List<ScheduleResponse> scheduleResponse = response.getData();
    if(scheduleResponse.isEmpty()){
      throw new BadRequestException("Không tìm thấy lịch dạy hôm nay");
    }

    //Tìm lịch dạy
    ScheduleResponse targetSchedule = findCurrentSchedule(scheduleResponse);
    if(targetSchedule == null){
      throw new BadRequestException("Không tìm thấy lịch dạy phù hợp");
    }

    System.out.println("Target schedule found: " + targetSchedule.getSubjectName() + " - " + targetSchedule.getClassCode());
    validateScheduleTime(targetSchedule);

    String qrSignature = UUID.randomUUID().toString();
    long ttlSeconds = calculateTTL(targetSchedule);
    System.out.println("Calculated TTL (seconds): " + ttlSeconds);
    RedisQrSession redisQrSession = RedisQrSession.builder()
            .qrSignature(qrSignature)
            .scheduleId(targetSchedule.getScheduleId())
            .teacherCIC(teacherCIC)
            .classCode(targetSchedule.getClassCode())
            .startTime(parseDateTime(targetSchedule.getDate(), targetSchedule.getStartTime()))
            .endTime(parseDateTime(targetSchedule.getDate(), targetSchedule.getEndTime()))
            .build();

    redisService.saveQrSession(qrSignature, redisQrSession, ttlSeconds);

    String qrContent = String.format("%s/scan?signature=%s", baseUrl, qrSignature);

    String qrImageBase64 = generateQrImage(qrContent);

    long expirationTimestamp = System.currentTimeMillis() + ttlSeconds * 1000;
    System.out.println("QR Expiration Timestamp: " + expirationTimestamp + " (Date: " + new Date(expirationTimestamp) + ")"); // Log expiration time

    return new QrGenerateResponse(qrSignature, expirationTimestamp, qrImageBase64, qrContent);
  }

  private ScheduleResponse findCurrentSchedule(List<ScheduleResponse> schedules) {
    LocalDateTime now = LocalDateTime.now();
    return schedules.stream()
            .filter(schedule -> {
              LocalDateTime startTime = parseDateTime(schedule.getDate(), schedule.getStartTime());
              LocalDateTime endTime = parseDateTime(schedule.getDate(), schedule.getEndTime());
              System.out.println("Schedule: " + schedule);
              System.out.println("Start Time: " + startTime);
              System.out.println("End Time: " + endTime);
              System.out.println("Current Time: " + now);
              return now.isAfter(startTime.minusMinutes(15)) && now.isBefore(endTime);
            })
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Không có lịch học nào đang diễn ra"));
  }

  private String generateQrImage(String content) {
    try {
      // Tạo QR Code Writer
      QRCodeWriter qrCodeWriter = new QRCodeWriter();

      // Cấu hình hints cho QR code
      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      hints.put(EncodeHintType.MARGIN, 1);

      // Kích thước QR code
      int width = 400;
      int height = 400;

      // Tạo BitMatrix
      BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

      // Tạo BufferedImage
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

      // Vẽ QR code
      for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
          // Nếu bit là true thì vẽ màu đen, ngược lại vẽ màu trắng
          image.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
        }
      }

      // Chuyển đổi BufferedImage thành byte array
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "PNG", baos);
      byte[] imageBytes = baos.toByteArray();

      // Encode thành Base64
      return Base64.getEncoder().encodeToString(imageBytes);

    } catch (WriterException | IOException e) {
      throw new RuntimeException("Không thể tạo QR code image", e);
    }
  }

  private void validateScheduleTime(ScheduleResponse schedule) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = parseDateTime(schedule.getDate(), schedule.getStartTime());
    LocalDateTime endTime = parseDateTime(schedule.getDate(), schedule.getEndTime());

    if (now.isBefore(startTime.minusMinutes(15))) {
      throw new BadRequestException("Chưa đến giờ điểm danh (15 phút trước giờ học)");
    }
    if (now.isAfter(endTime)) {
      throw new BadRequestException("Đã hết giờ điểm danh");
    }
  }

  private long calculateTTL(ScheduleResponse schedule) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endTime = parseDateTime(schedule.getDate(), schedule.getEndTime());

    long remainingSeconds = Duration.between(now, endTime).getSeconds();
    return Math.min(900, Math.max(300, remainingSeconds)); // Min 5 phút, max 15 phút
  }

  private LocalDateTime parseDateTime(String date, String time) {
    LocalDate localDate = LocalDate.parse(date);
    LocalTime localTime = LocalTime.parse(time);
    return LocalDateTime.of(localDate, localTime);
  }
}
