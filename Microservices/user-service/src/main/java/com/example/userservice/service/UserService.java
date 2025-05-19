package com.example.userservice.service;

import com.example.userservice.dto.ClassResponse;
import com.example.userservice.dto.StudentResponse;
import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

  private final RestTemplate restTemplate;
  private final UserRepository userRepository;

  @Value("${class.service.url}")
  private String classServiceUrl;

  public ApiResponse<List<StudentResponse>> getStudentByClassForTeacher(String classCode, String cic) {
    // Gọi đến class-service để lấy thông tin lớp học
    ResponseEntity<ApiResponse<ClassResponse>> response = restTemplate.exchange(
            classServiceUrl + "/classes/" + classCode,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<ClassResponse>>() {}
    );

    ApiResponse<ClassResponse> apiResponse = response.getBody();
    ClassResponse classResponse = apiResponse != null ? apiResponse.getData() : null;

    if(classResponse == null || !cic.equals(classResponse.getTeacherCIC())) {
      throw new SecurityException("Access denied: Bạn không dạy lớp này!!!");
    }

    List<User> students = userRepository.findByClassCodeAndRole(classCode, Role.STUDENT);

    List<StudentResponse> studentResponseList = students.stream()
            .map(this::convertToResponse)
            .toList();

    return new ApiResponse<>("Lấy danh sách thành công!!!", studentResponseList);
  }

  private StudentResponse convertToResponse(User user) {
    StudentResponse studentResponse = new StudentResponse();
    studentResponse.setFullName(user.getFullName());
    studentResponse.setStudentCode(user.getStudentCode());
    studentResponse.setCIC(user.getCIC());
    studentResponse.setEmail(user.getEmail());
    studentResponse.setPhone(user.getPhone());
    studentResponse.setDateOfBirth(user.getDateOfBirth());
    return studentResponse;
  }
}
