package com.example.authservice.service;

import com.example.authservice.models.RefreshToken;
import com.example.authservice.models.User;
import com.example.authservice.repository.RefreshTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefreshTokenService {
  @Value("${jwt.refresh.duration}")
  private long refreshDuration;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtUtil jwtUtil;

  @Transactional
  public RefreshToken createRefreshToken(User user) {
    //Delete old refresh token
    refreshTokenRepository.deleteByUser(user);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);

    String refreshTokenString = jwtUtil.generateRefreshToken(user.getCIC(), user.getRole());
    refreshToken.setToken(refreshTokenString);

    refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshDuration));
    return refreshTokenRepository.save(refreshToken);
  }

  public void verifyExpiration(RefreshToken refreshToken) {
    if(refreshToken.getExpiresAt().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(refreshToken);
      throw new RuntimeException("Refresh token expired");
    }
  }

  @Transactional
  public void deleteByUser(User user) {
    refreshTokenRepository.deleteByUser(user);
  }

  public RefreshToken findByToken(String token) {
    return refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));
  }

  @Transactional
  public void deleteByToken(String token) {
    refreshTokenRepository.deleteByToken(token);
  }
}
