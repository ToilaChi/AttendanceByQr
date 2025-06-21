//package com.example.notificationservice.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//public class CorsConfig {
//
//  @Bean
//  public CorsConfigurationSource corsConfigurationSource() {
//    CorsConfiguration configuration = new CorsConfiguration();
//
//    // Cho phép tất cả origins (bao gồm ngrok)
//    configuration.setAllowedOriginPatterns(List.of("*"));
//
//    // Cho phép tất cả methods
//    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//
//    // Cho phép tất cả headers
//    configuration.setAllowedHeaders(List.of("*"));
//
//    // Cho phép credentials
//    configuration.setAllowCredentials(true);
//
//    // Expose headers
//    configuration.setExposedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
//
//    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//    source.registerCorsConfiguration("/**", configuration);
//
//    return source;
//  }
//}