package com.example.attendanceservice.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisService {
  private final StringRedisTemplate redisTemplate;
  private ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public void saveQrSession(String qrSignature, RedisQrSession session, long ttlSeconds) {
    try {
      String json = objectMapper.writeValueAsString(session);
      redisTemplate.opsForValue().set("qr:" + qrSignature, json, Duration.ofSeconds(ttlSeconds));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize RedisQrSession to JSON", e);
    }
  }

  public RedisQrSession getQrSession(String qrSignature) {
    String json = redisTemplate.opsForValue().get("qr:" + qrSignature);
    if (json == null) return null;

    try {
      return objectMapper.readValue(json, RedisQrSession.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse RedisQrSession from Redis", e);
    }
  }

  public void deleteQrSession(String qrSignature) {
    redisTemplate.delete("qr:" + qrSignature);
  }

  public String get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  public void setWithExpiry(String key, String value, long ttlSeconds) {
    redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
  }

  public void delete(String key) {
    redisTemplate.delete(key);
  }

  public Long addToSet(String key, String value) {
    try {
      return redisTemplate.opsForSet().add(key, value);
    } catch (Exception e) {
      return 0L;
    }
  }
  public Set<String> getSetMembers(String key) {
    try {
      Set<String> members = redisTemplate.opsForSet().members(key);
      return members != null ? members : new HashSet<>();
    } catch (Exception e) {
      return new HashSet<>();
    }
  }
  public Long removeFromSet(String key, String value) {
    try {
      return redisTemplate.opsForSet().remove(key, value);
    } catch (Exception e) {
      return 0L;
    }
  }
}