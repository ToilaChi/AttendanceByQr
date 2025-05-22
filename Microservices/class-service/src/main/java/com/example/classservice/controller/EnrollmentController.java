package com.example.classservice.controller;

import com.example.classservice.service.EnrollmentService;
import com.example.classservice.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {
  @Autowired
  private EnrollmentService enrollmentService;

  @GetMapping("/class/{classCode}")
  public ResponseEntity<ApiResponse<List<String>>> getStudentCICByClassCode(@PathVariable String classCode){
    ApiResponse<List<String>> studentCICs = enrollmentService.getStudentByClassCode(classCode);

    return new ResponseEntity<>(studentCICs, HttpStatus.OK);
  }
}
