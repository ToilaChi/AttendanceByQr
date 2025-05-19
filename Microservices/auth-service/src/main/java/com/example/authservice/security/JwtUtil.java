package com.example.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.example.authservice.models.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
  private final SecretKey secretKey;

  private static final long EXPIRATION_TIME = 30 * 60 * 1000;
  private static final long REFRESH_TIME = 604800000;

  public JwtUtil(@Value("${jwt.secret}") String secretKey) {
    this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(String cic, Role role) {
    return Jwts.builder()
            .subject("Attendance By QR")
            .claim("CIC", cic)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
  }

  public String generateRefreshToken(String cic, Role role) {
    return Jwts.builder()
            .subject("Attendance By QR")
            .claim("CIC", cic)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + REFRESH_TIME))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
  }

  public String validateTokenAndRetrieveSubject(String token) throws JwtException {
    Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

    if(!"Attendance By QR".equals(claims.getSubject())) {
      throw new JwtException("Token không hợp lệ!!!");
    }

    return claims.get("CIC", String.class);
  }

  public String extractRole(String token) {
    Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

    return claims.get("role", String.class);
  }
}
