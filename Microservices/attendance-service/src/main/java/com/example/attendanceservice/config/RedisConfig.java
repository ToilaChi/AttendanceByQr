package com.example.attendanceservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}

