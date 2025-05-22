package com.example.userservice.service;

import com.example.userservice.dto.ClassResponse;
import com.example.userservice.dto.StudentResponse;
import com.example.userservice.dto.UserResponse;
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

import java.util.Collections;
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
    System.out.println("ClassCode: " + classCode);

    ResponseEntity<ApiResponse<ClassResponse>> classResponseEntity = restTemplate.exchange(
            classServiceUrl + "/classes/" + classCode,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<ClassResponse>>() {}
    );

    ClassResponse classResponse = classResponseEntity.getBody() != null
            ? classResponseEntity.getBody().getData()
            : null;

    if (classResponse == null || !cic.equals(classResponse.getTeacherCIC())) {
      throw new SecurityException("Access denied: Bạn không dạy lớp này!!!");
    }

    ResponseEntity<ApiResponse<List<String>>> studentCICResponseEntity = restTemplate.exchange(
            classServiceUrl + "/enrollments/class/" + classCode,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<List<String>>>() {}
    );

    List<String> studentCICs = studentCICResponseEntity.getBody() != null
            ? studentCICResponseEntity.getBody().getData()
            : Collections.emptyList();

    if (studentCICs.isEmpty()) {
      return new ApiResponse<>("Lớp không có sinh viên nào!");
    }

    List<User> students = userRepository.findByCICInAndRole(studentCICs, Role.STUDENT);

    List<StudentResponse> studentResponseList = students.stream()
            .map(this::convertToStudentResponse)
            .toList();

    return new ApiResponse<>("Lấy danh sách thành công!!!", studentResponseList);
  }

  public ApiResponse<UserResponse> getUserByCIC(String cic) {
    User user = userRepository.findByCIC(cic);
    if(user == null) {
      throw new SecurityException("Không tồn tại!!!");
    }

    UserResponse userResponse = convertToUserResponse(user);

    return new ApiResponse<>("Lấy người dùng thành công!!!", userResponse);
  }

  private StudentResponse convertToStudentResponse(User user) {
    StudentResponse studentResponse = new StudentResponse();
    studentResponse.setFullName(user.getFullName());
    studentResponse.setStudentCode(user.getStudentCode());
    studentResponse.setCIC(user.getCIC());
    studentResponse.setEmail(user.getEmail());
    studentResponse.setPhone(user.getPhone());
    studentResponse.setGender(user.getGender());
    studentResponse.setDateOfBirth(user.getDateOfBirth());
    studentResponse.setRegularClassCode(user.getRegularClassCode());
    return studentResponse;
  }

  private UserResponse convertToUserResponse(User user) {
    UserResponse userResponse = new UserResponse();
    userResponse.setFullName(user.getFullName());
    userResponse.setCIC(user.getCIC());
    userResponse.setEmail(user.getEmail());
    userResponse.setPhone(user.getPhone());
    userResponse.setGender(user.getGender());
    userResponse.setDateOfBirth(user.getDateOfBirth());
    userResponse.setRole(String.valueOf(user.getRole()));
    userResponse.setStudentCode(user.getStudentCode());
    userResponse.setTeacherCode(user.getTeacherCode());
    userResponse.setRegularClassCode(user.getRegularClassCode());
    return userResponse;
  }
}
