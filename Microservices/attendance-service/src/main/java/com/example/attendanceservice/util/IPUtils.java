package com.example.attendanceservice.util;

import jakarta.servlet.http.HttpServletRequest;

public class IPUtils {
  public static String getClientIP(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    String xRealIP = request.getHeader("X-Real-IP");
    String xForwardedProto = request.getHeader("X-Forwarded-Proto");

    if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      // Lấy IP đầu tiên trong danh sách (real client IP)
      return xForwardedFor.split(",")[0].trim();
    }

    if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
      return xRealIP;
    }

    return request.getRemoteAddr();
  }
}