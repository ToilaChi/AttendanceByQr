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
          "AND (COALESCE(:dayOfWeek, s.day_of_week) = s.day_of_week) " +
          "AND (COALESCE(:hasDate, false) = false OR (s.start_date <= :date AND s.end_date >= :date))")
  List<Schedule> findScheduleByStudent(
          @Param("studentCIC") String studentCIC,
          @Param("dayOfWeek") Integer dayOfWeek,
          @Param("date") LocalDate date,
          @Param("hasDate") Boolean hasDate
  );

  @Query("SELECT s FROM Schedule s " +
          "WHERE s.teacherCIC = :teacherCIC " +
          "AND (:dayOfWeek IS NULL OR s.day_of_week = :dayOfWeek) " +
          "AND (:hasDate = FALSE OR (s.start_date <= :date AND s.end_date >= :date))")
  List<Schedule> findScheduleByTeacher(
          @Param("teacherCIC") String teacherCIC,
          @Param("dayOfWeek") Integer dayOfWeek,
          @Param("date") LocalDate date,
          @Param("hasDate") Boolean hasDate
  );
}
