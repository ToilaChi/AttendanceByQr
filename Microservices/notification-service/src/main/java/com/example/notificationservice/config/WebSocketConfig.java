package com.example.notificationservice.config;

import com.example.apigateway.security.JwtUtil;
import io.micrometer.common.lang.NonNull;
import io.micrometer.common.lang.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Autowired
  private JwtUtil jwtUtil;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-notifications")
            .setAllowedOrigins(
                    "http://localhost:5173",
                    "http://192.168.1.4:5173",
                    "http://192.168.161.1:5173",
                    "http://192.168.174.1:5173",
//                    "https://*.ngrok-free.app",
//                    "https://*.ngrok.io",
                    "https://talented-rare-iguana.ngrok-free.app"
            )
            .addInterceptors(new WebSocketAuthInterceptor());

  }

  public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) throws Exception {
      System.out.println("📥 WebSocket handshake for: " + request.getURI());

      // Thêm ngrok bypass headers
      response.getHeaders().add("ngrok-skip-browser-warning", "true");

      // CORS headers
      String origin = request.getHeaders().getFirst("Origin");
      if (origin != null) {
        response.getHeaders().add("Access-Control-Allow-Origin", origin);
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        response.getHeaders().add("Access-Control-Allow-Headers", "ngrok-skip-browser-warning, Content-Type, Authorization");
      }

      // Bỏ qua xác thực cho endpoint /info
      String path = request.getURI().getPath();
      if (path.endsWith("/info")) {
        System.out.println("✅ Allowing /info endpoint without authentication");
        return true;
      }

      // Xác thực token cho các endpoint khác
      String query = request.getURI().getQuery();
      String token = extractTokenFromQuery(query);

      if (token == null || token.isEmpty()) {
        System.out.println("❌ Missing token in query parameter for: " + path);
        return false;
      }

      try {
        if (!jwtUtil.validateToken(token)) {
          System.out.println("❌ Invalid JWT token for: " + path);
          return false;
        }

        String cic = jwtUtil.extractCIC(token);
        String role = jwtUtil.extractRole(token);

        attributes.put("CIC", cic);
        attributes.put("ROLE", role);

        System.out.println("✅ WebSocket handshake successful - CIC: " + cic + ", Role: " + role);
        return true;
      } catch (Exception e) {
        System.out.println("❌ WebSocket handshake failed for " + path + ": " + e.getMessage());
        return false;
      }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               @Nullable Exception exception) {
      if (exception != null) {
        System.out.println("❌ WebSocket handshake error: " + exception.getMessage());
      }
    }

    private String extractTokenFromQuery(String query) {
      if (query == null) return null;
      return Arrays.stream(query.split("&"))
              .filter(param -> param.startsWith("token="))
              .map(param -> param.substring(6))
              .findFirst()
              .orElse(null);
    }
  }
}