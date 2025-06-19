package com.example.apigateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouterValidator {

  private static final List<String> openEndpoints = List.of(
          "/auth/login",
          "/auth/logout",
          "/auth/refresh-token",
          "/eureka",
          "/ws-notifications",
          "/ws-notifications/**",
          "/ws-notifications/info"
  );

  public Predicate<ServerHttpRequest> isSecured = request -> {
    String path = request.getURI().getPath();
    System.out.println("🧭 RouterValidator path: " + path);

    // Kiểm tra xem path có bắt đầu bằng bất kỳ endpoint nào trong danh sách không
    return openEndpoints.stream().noneMatch(path::startsWith);
  };
}