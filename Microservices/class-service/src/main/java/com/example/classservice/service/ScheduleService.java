package com.example.classservice.service;

import com.example.classservice.dto.ScheduleResponse;
import com.example.classservice.model.Schedule;
import com.example.classservice.repository.ScheduleRepository;
import com.example.classservice.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduleService {
  @Autowired
  ScheduleRepository scheduleRepository;

  private final String[] DAY_NAMES = {"", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};

  public ApiResponse<List<ScheduleResponse>> getStudentSchedule
          (String studentCIC, Integer day_of_week, LocalDate date) {
    boolean hasDate = date != null;
    List<Schedule> studentSchedule = scheduleRepository.findScheduleByStudent(studentCIC, day_of_week, date, hasDate);

    if (date != null) {
      // Lấy thứ trong tuần của ngày được chọn
      int dayOfWeekFromDate = date.getDayOfWeek().getValue();

      // Lọc lại các lịch học chỉ đúng vào thứ của ngày được chọn
      studentSchedule = studentSchedule.stream()
              .filter(schedule -> schedule.getDay_of_week().equals(dayOfWeekFromDate))
              .toList();
    }

    List<ScheduleResponse> studentScheduleResponses = studentSchedule.stream()
            .map(this::convertToScheduleResponse)
            .toList();

    return new ApiResponse<>("Lấy lịch học cho sinh viên thành công!!!", studentScheduleResponses);
  }

  public ApiResponse<List<ScheduleResponse>> getTeacherSchedule
          (String teacherCIC, Integer day_of_week, LocalDate date) {
    boolean hasDate = date != null;
    List<Schedule> teacherSchedule = scheduleRepository.findScheduleByTeacher(teacherCIC, day_of_week, date, hasDate);

    if (date != null) {
      int dayOfWeekFromDate = date.getDayOfWeek().getValue();

      teacherSchedule = teacherSchedule.stream()
              .filter(schedule -> schedule.getDay_of_week().equals(dayOfWeekFromDate))
              .toList();
    }

    List<ScheduleResponse> teacherScheduleResponses =  teacherSchedule.stream()
            .map(this::convertToScheduleResponse)
            .toList();

    return new ApiResponse<>("Lấy lịch dạy cho giảng viên thành công!!!", teacherScheduleResponses);
  }

  private ScheduleResponse convertToScheduleResponse(Schedule schedule) {
    ScheduleResponse scheduleResponse = new ScheduleResponse();
    scheduleResponse.setClassCode(schedule.getClassEntity().getClassCode());
    scheduleResponse.setClassName(schedule.getClassEntity().getClassName());
    scheduleResponse.setSubjectName(schedule.getClassEntity().getSubjectName());

    // Chuyển đổi số thứ trong tuần sang tên thứ
    Integer dayNumber = schedule.getDay_of_week();
    String dayName = (dayNumber != null && dayNumber >= 1 && dayNumber <= 7) ?
            DAY_NAMES[dayNumber] : String.valueOf(dayNumber);
    scheduleResponse.setDay_of_week(dayName);

    scheduleResponse.setStartTime(schedule.getStart_time().toString());
    scheduleResponse.setEndTime(schedule.getEnd_time().toString());
    scheduleResponse.setStartDate(schedule.getStart_date().toString());
    scheduleResponse.setEndDate(schedule.getEnd_date().toString());
    scheduleResponse.setRoom(schedule.getRoom());
    return scheduleResponse;
  }
}
