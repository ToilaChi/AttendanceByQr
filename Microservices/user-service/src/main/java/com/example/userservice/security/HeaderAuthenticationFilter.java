package com.example.userservice.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Collections;

@Component
public class HeaderAuthenticationFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;

    String cic = httpRequest.getHeader("X-User-CIC");
    String role = httpRequest.getHeader("X-User-Role");

    if (cic != null && role != null) {
      System.out.println("🔐 Setting security context from headers - username: " + cic + ", role: " + role);

      // Tạo authentication object với thông tin từ header
      UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                      cic,
                      null,
                      Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
              );

      // Đặt vào SecurityContextHolder
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } else {
      System.out.println("⚠️ Missing user headers");
    }

    chain.doFilter(request, response);
  }
}