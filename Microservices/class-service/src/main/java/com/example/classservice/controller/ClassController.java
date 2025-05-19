package com.example.classservice.controller;

import com.example.classservice.dto.ClassResponse;
import com.example.classservice.service.ClassService;
import com.example.classservice.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/classes")
public class ClassController {
  private final ClassService classService;

  public ClassController(ClassService classService) {
    this.classService = classService;
  }

  @GetMapping("/{classCode}")
  public ResponseEntity<ApiResponse<ClassResponse>> getClassByCode(@PathVariable String classCode){
    return ResponseEntity.ok(classService.getClassByCode(classCode));
  }
}
