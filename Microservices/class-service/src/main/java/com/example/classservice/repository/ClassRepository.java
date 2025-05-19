package com.example.classservice.repository;

import com.example.classservice.model.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, String> {
  Optional<ClassEntity> findByClassCode(String classCode);
}
