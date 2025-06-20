package com.example.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class CorsConfig implements WebFluxConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins(
                    "http://localhost:5173",
                    "http://192.168.1.4:5173",
                    "http://192.168.161.1:5173",
                    "http://192.168.174.1:5173",
//                    "https://*.ngrok-free.app",
//                    "https://*.ngrok.io",
                    "https://talented-rare-iguana.ngrok-free.app"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            .exposedHeaders(
                    "Authorization", "Cache-Control", "Content-Type", "ngrok-skip-browser-warning"
            )
            .allowCredentials(true);
  }
}