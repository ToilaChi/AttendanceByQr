package com.example.attendanceservice.repository;

import com.example.attendanceservice.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
  boolean existsByStudentCICAndScheduleId(String studentCIC, Integer scheduleId);
}
