package com.example.classservice.repository;

import com.example.classservice.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {
  @Query("SELECT e.studentCIC FROM Enrollment e WHERE e.classEntity.classCode = :classCode")
  List<String> findStudentCICByClassCode(@Param("classCode") String classCode);

  @Query("SELECT COUNT(e) > 0 FROM Enrollment e " +
          "JOIN e.classEntity c WHERE e.studentCIC = :studentCIC AND c.classCode = :classCode")
  boolean existsByStudentCICAndClassCode(String studentCIC, String classCode);
}
