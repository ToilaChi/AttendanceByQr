package com.example.classservice.service;

import com.example.classservice.dto.ClassResponse;
import com.example.classservice.model.ClassEntity;
import com.example.classservice.repository.ClassRepository;
import com.example.classservice.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassService {
  @Autowired
  private ClassRepository classRepository;

  public ApiResponse<ClassResponse> getClassByCode(String classCode){
    ClassEntity classEntity = classRepository.findByClassCode(classCode)
            .orElseThrow(() -> new RuntimeException("Class not found"));

    ClassResponse classResponse = new ClassResponse();
    classResponse.setClassCode(classEntity.getClassCode());
    classResponse.setClassName(classEntity.getClassName());
    classResponse.setSubjectName(classEntity.getSubjectName());
    classResponse.setTeacherCIC(classEntity.getTeacherCIC());

    return new ApiResponse<>("Lấy lớp thành công!!!", classResponse);
  }
}
