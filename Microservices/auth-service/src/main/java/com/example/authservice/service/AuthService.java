package com.example.authservice.service;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.LogoutRequest;
import com.example.authservice.dto.LogoutResponse;
import com.example.authservice.models.RefreshToken;
import com.example.authservice.models.User;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private TokenBlackListService tokenBlackListService;
  @Autowired
  private RefreshTokenService refreshTokenService;

  public LoginResponse login(LoginRequest loginRequest) {
    try {
      User CIC = userRepository.findByCIC(loginRequest.getCIC());
      if(CIC == null) {
        return new LoginResponse(null,
                "Căn cước công dân sai. Vui lòng nhập lại!!!");
      }

      if(!isPasswordValid(CIC, loginRequest.getPassword())) {
        return new LoginResponse(null, "Mật khẩu không chính xác. Vui lòng nhập lại!!!");
      }

      if(isPasswordValid(CIC, loginRequest.getPassword())) {
        if(!CIC.getPassword().startsWith("$2a$")
        && !CIC.getPassword().startsWith("$2b$")
        && !CIC.getPassword().startsWith("$2y$")) {
          System.out.println("Updating password with encryption");
          updatePasswordWithEncryption(CIC, loginRequest.getPassword());
        }

        //Create accessToken
        String accessToken = jwtUtil.generateAccessToken(CIC.getUsername(), CIC.getRole());

        //Create refreshToken
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(CIC);
        String refreshTokenString = refreshToken.getToken();

        if(accessToken == null) {
          System.err.println("accessToken is null");
          throw new Exception("Failed to generate access token");
        }

        //Get info cus
        LoginResponse.AccountInfo accountInfo = new LoginResponse.AccountInfo(
                CIC.getId(),
                CIC.getUsername(),
                CIC.getRole()
        );

        LoginResponse.DataInfo dataInfo = new LoginResponse.DataInfo(
                accessToken,
                refreshTokenString,
                accountInfo
        );

        return new LoginResponse(dataInfo, "Đăng nhập thành công!!!");
      }
      else {
        System.out.println("Lỗi mật khẩu!!!");
        throw new RuntimeException("Lỗi mật khẩu!!!");
      }
    }
    catch (Exception e) {
      System.err.println("Đăng nhập lỗi: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Đăng nhập thất bại: " + e.getMessage(), e);
    }
  }

  @Transactional
  public LogoutResponse logout(LogoutRequest logoutRequest) {
    try {
      if(logoutRequest.getRefreshToken() != null) {
        RefreshToken refreshToken = refreshTokenService.findByToken(logoutRequest.getRefreshToken());

        String accessToken = logoutRequest.getAccessToken();
        if (accessToken != null && !accessToken.isEmpty()) {
          tokenBlackListService.addBlacklistToken(accessToken);
        }
        refreshTokenService.deleteByToken(refreshToken.getToken());
        return new LogoutResponse("Logout thành công!!!");
      }
      return new LogoutResponse("Refresh token không tìm thấy");
    }
    catch (Exception e) {
      return new LogoutResponse("Logout thất bại: " + e.getMessage());
    }
  }

  private boolean isPasswordValid(User user, String rawPassword) {
    if (user.getPassword().startsWith("$2a$") ||
            user.getPassword().startsWith("$2b$") ||
            user.getPassword().startsWith("$2y$")) {
      // Nếu password đã được mã hóa, so sánh bằng passwordEncoder
      return passwordEncoder.matches(rawPassword, user.getPassword());
    } else {
      // Nếu password chưa mã hóa, so sánh trực tiếp
      return user.getPassword().equals(rawPassword);
    }
  }

  private void updatePasswordWithEncryption(User user, String rawPassword) {
    user.setPassword(passwordEncoder.encode(rawPassword));
    userRepository.save(user);
  }
}
