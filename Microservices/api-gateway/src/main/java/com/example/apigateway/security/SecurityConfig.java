package com.example.apigateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http
            .cors(cors -> corsWebFilter())
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/auth/login", "/auth/logout").permitAll()
                    .pathMatchers("/ws-notifications/**").permitAll()
                    .pathMatchers("/**").permitAll()
            );
    return http.build();
  }

  @Bean
  public CorsWebFilter corsWebFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);

    config.setAllowedOriginPatterns(List.of(
            "https://localhost:5173",
            "https://192.168.1.4:5173",
            "https://192.168.161.1:5173",
            "https://192.168.174.246:5173",
            "https://192.168.114.246:5173",
            "https://talented-rare-iguana.ngrok-free.app"
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

    return new CorsWebFilter(source);
  }
}