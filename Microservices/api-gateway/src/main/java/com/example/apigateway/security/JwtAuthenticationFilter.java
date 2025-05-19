package com.example.apigateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private RouterValidator routerValidator;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    System.out.println("Gateway Filter đang xử lý request path: " + request.getURI().getPath());

    // Kiểm tra xem endpoint này có cần xác thực không
    if (routerValidator.isSecured.test(request)) {
      // Kiểm tra authorization header
      if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
        return onError(exchange, "Missing authorization header");
      }

      String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return onError(exchange, "Invalid authorization header format");
      }

      // Xác thực JWT
      String token = authHeader.substring(7);
      try {
        // Xác thực token
        if (!jwtUtil.validateToken(token)) {
          return onError(exchange, "Invalid JWT token");
        }

        // Extract thông tin và thêm vào header
        String cic = jwtUtil.extractCIC(token);
        String role = jwtUtil.extractRole(token);

        System.out.println(">> JWT OK: CIC = " + cic + ", role = " + role);
        // Thêm thông tin người dùng vào headers để các service sau có thể sử dụng
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-CIC", cic)
                .header("X-User-Role", role)
                .build();

        // Tiếp tục với request đã được sửa đổi
        System.out.println("JWT hợp lệ – CIC: " + cic + ", role: " + role);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
      } catch (Exception e) {
        return onError(exchange, "Invalid token: " + e.getMessage());
      }
    }

    // Nếu không cần xác thực, chỉ cần forward request
    return chain.filter(exchange);
  }

  private Mono<Void> onError(ServerWebExchange exchange, String err) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().add("Content-Type", "application/json");
    String responseBody = "{\"message\": \"" + err + "\"}";
    return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.getBytes())));
  }

  @Override
  public int getOrder() {
    return -100; // Đảm bảo filter này chạy trước các filter khác
  }
}
