package com.example.userservice.controller;

import com.example.userservice.dto.StudentResponse;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.models.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.UserService;
import com.example.classservice.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
  private final UserService userService;
  private final UserRepository userRepository;

  public UserController(UserService userService, UserRepository userRepository) {
    this.userService = userService;
    this.userRepository = userRepository;
  }

  private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentCIC = authentication.getName();
    return userRepository.findByCIC(currentCIC);
  }

  @GetMapping("/class/{classCode}")
  public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudentsByClass(
          @PathVariable String classCode
  ) {
    User user = getCurrentUser();
    String cic = user.getCIC();

    System.out.println("📥 [UserService] Nhận request GET /users/class/" + classCode + " từ giáo viên " + cic);

    ApiResponse<List<StudentResponse>> studentResponseList = userService.getStudentByClassForTeacher(classCode, cic);
    return ResponseEntity.ok(studentResponseList);
  }

  @GetMapping("/cic")
  public ResponseEntity<ApiResponse<UserResponse>> getUserByCIC() {
    User user = getCurrentUser();
    String cic = user.getCIC();

    ApiResponse<UserResponse> userResponse = userService.getUserByCIC(cic);

    return ResponseEntity.ok(userResponse);
  }
}
