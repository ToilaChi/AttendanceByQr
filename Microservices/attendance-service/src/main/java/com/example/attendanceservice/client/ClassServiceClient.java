package com.example.attendanceservice.client;

import com.example.classservice.dto.ScheduleResponse;
import com.example.classservice.util.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "class-service")
public interface ClassServiceClient {
  @GetMapping("/schedules/teacher")
  ApiResponse<List<ScheduleResponse>> getTeacherSchedule(
          @RequestHeader("X-User-CIC") String teacherCIC,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentDate
  );

  @GetMapping("/check")
  ResponseEntity<ApiResponse<Boolean>> checkStudentEnrollment(
          @RequestHeader("X-User-CIC") String studentCIC,
          @RequestParam String classCode
  );
}
