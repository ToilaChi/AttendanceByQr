package com.example.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

  private final SecretKey key;

  public JwtUtil(@Value("${jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public Claims extractAllClaims(String token) {
    try {
      return Jwts.parser()
              .verifyWith(key)
              .build()
              .parseSignedClaims(token)
              .getPayload();
    } catch (Exception e) {
      throw new JwtException("Token không hợp lệ: " + e.getMessage());
    }
  }

  public String extractCIC(String token) {
    Claims claims = extractAllClaims(token);
    if (!"Attendance By QR".equals(claims.getSubject())) {
      throw new JwtException("Token không hợp lệ: sai subject");
    }
    return claims.get("CIC", String.class);
  }

  public String extractRole(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("role", String.class);
  }

  public boolean validateToken(String token) {
    try {
      Claims claims = extractAllClaims(token);
      return "Attendance By QR".equals(claims.getSubject()) &&
              claims.getExpiration().after(new java.util.Date());
    } catch (Exception e) {
      return false;
    }
  }
}