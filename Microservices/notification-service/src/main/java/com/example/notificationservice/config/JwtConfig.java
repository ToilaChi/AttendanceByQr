package com.example.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.apigateway.security.JwtUtil;

@Configuration
public class JwtConfig {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Bean
  public JwtUtil jwtUtil() {
    return new JwtUtil(jwtSecret);
  }
}