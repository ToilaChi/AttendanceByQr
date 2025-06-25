package com.example.notificationservice.config;

import io.micrometer.common.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  @Autowired
  private JwtUtil jwtUtil;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-notifications")
            .setAllowedOrigins(
                    "http://localhost:5173",
                    "https://talented-rare-iguana.ngrok-free.app",
                    "https://192.168.1.4:5173",
                    "https://192.168.114.246:5173",
                    "https://192.168.174.246:5173"
            )
            .addInterceptors(new WebSocketAuthInterceptor());
    System.out.println("Registered endpoints");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new JwtChannelInterceptor());
  }

  public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
      log.debug("📥 WebSocket handshake for URI: {}", request.getURI());
      log.debug("Headers: {}", request.getHeaders());
      log.debug("Query params: {}", request.getURI().getQuery());
      String token = extractTokenFromQuery(request.getURI().getQuery());
      if (token == null) {
        log.warn("⚠️ No token in query param");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
      }
      try {
        if (jwtUtil.validateToken(token)) {
          String cic = jwtUtil.extractCIC(token);
          String role = jwtUtil.extractRole(token);
          attributes.put("CIC", cic);
          attributes.put("ROLE", role);
          attributes.put("TOKEN", token);
          log.info("✅ Handshake successful - CIC: {}, Role: {}", cic, role);
          return true;
        } else {
          log.error("❌ Invalid token: {}", token.substring(0, Math.min(20, token.length())));
          response.setStatusCode(HttpStatus.UNAUTHORIZED);
          return false;
        }
      } catch (Exception e) {
        log.error("❌ Handshake error: {}", e.getMessage(), e);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
      }
    }
    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, Exception exception) {
      if (exception != null) {
        log.error("❌ WebSocket handshake failed for URI: {}. Error: {}", request.getURI(), exception.getMessage(), exception);
      } else {
        log.info("✅ WebSocket handshake completed for URI: {}", request.getURI());
      }
    }

    private String extractTokenFromQuery(String query) {
      if (query == null) return null;
      log.debug("🔍 Extracting token from query: {}", query);
      String token = Arrays.stream(query.split("&"))
              .filter(param -> param.startsWith("token="))
              .map(param -> param.substring(6))
              .findFirst()
              .orElse(null);
      log.debug("🔍 Extracted token: {}", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");
      return token;
    }
  }

  public class JwtChannelInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
      StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        assert sessionAttributes != null;
        String token = (String) sessionAttributes.get("TOKEN");
        if (token != null) {
          try {
            if (jwtUtil.validateToken(token)) {
              String cic = jwtUtil.extractCIC(token);
              String role = jwtUtil.extractRole(token);
              UsernamePasswordAuthenticationToken authentication =
                      new UsernamePasswordAuthenticationToken(
                              cic,
                              null,
                              List.of(new SimpleGrantedAuthority("ROLE_" + role))
                      );
              accessor.setUser(authentication);
              log.info("✅ STOMP Authentication successful - CIC: {}, Role: {}", cic, role);
              return message;
            } else {
              log.error("❌ Invalid JWT token in STOMP CONNECT");
              return null;
            }
          } catch (Exception e) {
            log.error("❌ STOMP Authentication error: {}", e.getMessage(), e);
            return null;
          }
        } else {
          log.error("❌ No token found in session attributes");
          return null;
        }
      }
      return message;
    }
  }
}