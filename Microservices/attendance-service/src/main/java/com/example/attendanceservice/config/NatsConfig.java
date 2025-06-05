package com.example.attendanceservice.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
@Slf4j
public class NatsConfig {
  @Value("${nats.server.url}")
  private String natsUrl;

  @Bean
  public Connection natsConnection() throws IOException, InterruptedException {
    Options options = new Options.Builder()
            .server(natsUrl)
            .connectionTimeout(Duration.ofSeconds(10))
            .pingInterval(Duration.ofSeconds(2))
            .reconnectWait(Duration.ofSeconds(1))
            .maxReconnects(-1) // không giới hạn reconnect
            .connectionListener((conn, type) -> {
              log.info("NATS connection event: {}", type);
            })
    .build();

    Connection connection = Nats.connect(options);
    log.info("Connected to NATS server at: {}", natsUrl);

    return connection;
  }
}
