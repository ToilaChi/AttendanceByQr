package com.example.userservice.client;

import com.example.classservice.dto.ClassResponse;
import com.example.classservice.util.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "class-service")
public interface ClassServiceClient {
  @GetMapping("classes/{classCode}")
  ResponseEntity<ApiResponse<ClassResponse>> getClassByCode(@PathVariable String classCode);

  @GetMapping("enrollments/class/{classCode}")
  ResponseEntity<ApiResponse<List<String>>> getStudentCICByClassCode(@PathVariable String classCode);
}
