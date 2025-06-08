package com.example.notificationservice.config;

import io.nats.client.*;
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
            .maxReconnects(-1) //Không giới hạn reconnect
            .connectionListener((conn, type) -> {
              log.info("Nats connection event: {}", type);
            })
            .errorListener(new ErrorListener() {
              @Override
              public void errorOccurred(Connection conn, String error) {
                log.error("Nats error occurred: {}", error);
              }

              @Override
              public void exceptionOccurred(Connection conn, Exception exp) {
                log.error("Nats exception occurred: {}", exp);
              }

              @Override
              public void slowConsumerDetected(Connection conn, Consumer consumer) {
                log.warn("Nats slow consumer detected: {}", consumer);
              }
            })
            .build();
    Connection connection = Nats.connect(options);
    log.info("Connected to NATS server at: {}", natsUrl);

    return connection;
  }
}
