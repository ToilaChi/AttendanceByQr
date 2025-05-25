package com.example.classservice.repository;

import com.example.classservice.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, String> {
  @Query("SELECT s FROM Schedule s " +
          "JOIN Enrollment e ON s.classEntity.classCode = e.classEntity.classCode " +
          "WHERE e.studentCIC = :studentCIC " +
          "AND s.start_date <= :endOfWeek AND s.end_date >= :startOfWeek")
  List<Schedule> findScheduleByStudent(
          @Param("studentCIC") String studentCIC,
          @Param("startOfWeek") LocalDate startOfWeek,
          @Param("endOfWeek") LocalDate endOfWeek);

  @Query("SELECT s FROM Schedule s " +
          "WHERE s.teacherCIC = :teacherCIC " +
          "AND s.start_date <= :endOfWeek AND s.end_date >= :startOfWeek")
  List<Schedule> findScheduleByTeacher(
          @Param("teacherCIC") String teacherCIC,
          @Param("startOfWeek") LocalDate startOfWeek,
          @Param("endOfWeek") LocalDate endOfWeek);
}
