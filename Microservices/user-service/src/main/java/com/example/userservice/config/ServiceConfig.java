package com.example.userservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class ServiceConfig {
  @Bean
  @LoadBalanced
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    RestTemplate restTemplate = builder.build();

    // Đảm bảo các converter sử dụng UTF-8
    restTemplate.getMessageConverters().stream()
            .filter(converter -> converter instanceof StringHttpMessageConverter)
            .forEach(converter -> ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8));

    // Thêm interceptor để đảm bảo encoding cho header
    ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
      request.getHeaders().set("Accept-Charset", "UTF-8");
      return execution.execute(request, body);
    };

    restTemplate.getInterceptors().add(interceptor);

    return restTemplate;
  }
}
