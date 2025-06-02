package com.example.classservice.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  private String message;
  private T data;
  private Boolean success;


  public ApiResponse(String message, T data) {
    this.message = message;
    this.data = data;
  }

  public ApiResponse(String message, boolean success) {
    this.message = message;
    this.success = success;
  }
}
