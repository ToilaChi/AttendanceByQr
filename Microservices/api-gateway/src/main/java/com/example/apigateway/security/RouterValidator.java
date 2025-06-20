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
          "/ws-notifications/**",
          "/notifications/**"
//          "/ws-notifications/info"
  );

  public Predicate<ServerHttpRequest> isSecured = request -> {
    String path = request.getURI().getPath();
    System.out.println("🧭 RouterValidator path: " + path);

    // Kiểm tra xem path có bắt đầu bằng bất kỳ endpoint nào trong danh sách không
    boolean isOpenEndpoint = openEndpoints.stream().anyMatch(path::startsWith);

    if (isOpenEndpoint) {
      System.out.println("🔓 Open endpoint detected: " + path);
      return false; // Không cần authentication
    }

    // Tất cả các endpoint khác (bao gồm ws-notifications) đều cần authentication
    System.out.println("🔒 Secured endpoint detected: " + path);
    return true; // Cần authentication
  };
}