package com.example.classservice.service;

import com.example.classservice.dto.EnrollmentResponse;
import com.example.classservice.model.Enrollment;
import com.example.classservice.repository.EnrollmentRepository;
import com.example.classservice.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EnrollmentService {
  @Autowired
  private EnrollmentRepository enrollmentRepository;

  public ApiResponse<List<String>> getStudentByClassCode(String classCode){
    List<String> studentCICs = enrollmentRepository.findStudentCICByClassCode(classCode);

    return new ApiResponse<>("Lấy danh sách căn cước của sinh viên thành công!!!", studentCICs);
  }
}
