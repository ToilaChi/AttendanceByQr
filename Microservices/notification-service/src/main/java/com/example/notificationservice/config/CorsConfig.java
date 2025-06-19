package com.example.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);

    // Sử dụng allowedOriginPatterns thay vì allowedOrigins
    config.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",
            "http://192.168.1.4:5173",
            "http://192.168.161.1:5173",
            "http://192.168.174.1:5173",
            "https://*.ngrok-free.app",
            "https://*.ngrok.io"
    ));

    config.setAllowedHeaders(List.of("*"));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Cache-Control",
            "Content-Type",
            "ngrok-skip-browser-warning"
    ));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return new CorsFilter(source);
  }
}