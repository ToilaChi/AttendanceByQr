package com.example.attendanceservice.repository;

import com.example.attendanceservice.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
  boolean existsByStudentCICAndScheduleId(String studentCIC, Integer scheduleId);
  @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.studentCIC = :studentCic " +
          "AND a.scheduleId = :scheduleId AND a.timestamp >= :startOfDay AND a.timestamp < :startOfNextDay")
  boolean existsByStudentCicAndScheduleIdAndDateRange(
          @Param("studentCic") String studentCic,
          @Param("scheduleId") int scheduleId,
          @Param("startOfDay") java.time.LocalDateTime startOfDay,
          @Param("startOfNextDay") java.time.LocalDateTime startOfNextDay
  );

  @Query("SELECT a FROM Attendance a WHERE a.ipAddress = :ipAddress AND a.deviceInfo = :deviceInfo " +
          "AND a.timestamp >= :startOfDay AND a.timestamp < :startOfNextDay")
  List<Attendance> findByIpAndDeviceAndDateRange(
          @Param("ipAddress") String ipAddress,
          @Param("deviceInfo") String deviceInfo,
          @Param("startOfDay") LocalDateTime startOfDay,
          @Param("startOfNextDay") LocalDateTime startOfNextDay
  );
}
