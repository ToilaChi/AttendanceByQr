package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.models.RefreshToken;
import com.example.authservice.models.User;
import com.example.authservice.security.JwtUtil;
import com.example.authservice.service.AuthService;
import com.example.authservice.service.RefreshTokenService;
import com.example.authservice.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;
  private final RefreshTokenService refreshTokenService;
  private final JwtUtil jwtUtil;

  public AuthController(AuthService authService, RefreshTokenService refreshTokenService, JwtUtil jwtUtil) {
    this.authService = authService;
    this.refreshTokenService = refreshTokenService;
    this.jwtUtil = jwtUtil;
  }

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest loginRequest) {
    return authService.login(loginRequest);
  }

  @PostMapping("/logout")
  public LogoutResponse logout(@RequestBody LogoutRequest logoutRequest) {
    return authService.logout(logoutRequest);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
    try {
      RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenRequest.getRefreshToken());
      refreshTokenService.verifyExpiration(refreshToken);

      //Create new accessToken
      User user = refreshToken.getUser();
      String newAccessToken = jwtUtil.generateAccessToken(user.getCIC(), user.getRole());

      return ResponseEntity.ok(new ApiResponse<>
              ("", new RefreshTokenResponse(refreshToken.getToken(), newAccessToken)));
    }
    catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
