package com.example.classservice.service;

import com.example.classservice.dto.ScheduleResponse;
import com.example.classservice.model.Schedule;
import com.example.classservice.repository.ScheduleRepository;
import com.example.classservice.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduleService {
  @Autowired
  ScheduleRepository scheduleRepository;

  private final String[] DAY_NAMES = {"", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};

  public ApiResponse<List<ScheduleResponse>> getStudentSchedule
          (String studentCIC, LocalDate currentDate) {
    //Tính đầu tuần và cuối tuần
    LocalDate startOfWeek = currentDate.with(DayOfWeek.MONDAY);
    LocalDate endOfWeek = currentDate.with(DayOfWeek.SUNDAY);

    List<Schedule> studentSchedule = scheduleRepository.findScheduleByStudent(studentCIC, startOfWeek, endOfWeek);

    List<ScheduleResponse> studentScheduleResponseList = studentSchedule.stream()
            .map(schedule -> convertToScheduleResponse(schedule, currentDate))
            .sorted((s1, s2) -> {
              int dayCompare = getDayOfWeekNumber(s1.getDay_of_week()).compareTo(getDayOfWeekNumber(s2.getDay_of_week()));
              if(dayCompare != 0) return dayCompare;
              return s1.getStartTime().compareTo(s2.getStartTime());
            })
            .toList();

    return new ApiResponse<>("Lấy lịch học cho sinh viên thành công!!!",  studentScheduleResponseList);
  }

  public ApiResponse<List<ScheduleResponse>> getTeacherSchedule
          (String teacherCIC, LocalDate currentDate) {
    LocalDate startOfWeek = currentDate.with(DayOfWeek.MONDAY);
    LocalDate endOfWeek = currentDate.with(DayOfWeek.SUNDAY);

    List<Schedule> teacherSchedule = scheduleRepository.findScheduleByTeacher(teacherCIC, startOfWeek, endOfWeek);

    List<ScheduleResponse> teacherScheduleResponseList = teacherSchedule.stream()
            .map(schedule -> convertToScheduleResponse(schedule, currentDate))
            .sorted((s1, s2) -> {
              int dayCompare = getDayOfWeekNumber(s1.getDay_of_week()).compareTo(getDayOfWeekNumber( s2.getDay_of_week()));
              if(dayCompare != 0) return dayCompare;
              return s1.getStartTime().compareTo(s2.getStartTime());
            })
            .toList();

    return new ApiResponse<>("Lấy lịch dạy cho giảng viên thành công!!!", teacherScheduleResponseList);
  }

  private ScheduleResponse convertToScheduleResponse(Schedule schedule, LocalDate currentDate) {
    ScheduleResponse scheduleResponse = new ScheduleResponse();
    scheduleResponse.setScheduleId(Math.toIntExact(schedule.getId()));
    scheduleResponse.setClassCode(schedule.getClassEntity().getClassCode());
    scheduleResponse.setClassName(schedule.getClassEntity().getClassName());
    scheduleResponse.setSubjectName(schedule.getClassEntity().getSubjectName());

    // Chuyển đổi số thứ trong tuần sang tên thứ
    Integer dayNumber = schedule.getDay_of_week();
    String dayName = (dayNumber != null && dayNumber >= 1 && dayNumber <= 7) ?
            DAY_NAMES[dayNumber] : String.valueOf(dayNumber);
    scheduleResponse.setDay_of_week(dayName);

    //Tính ngày học
    if(dayNumber != null && dayNumber >= 1 && dayNumber <= 7) {
      LocalDate startDayOfWeek = currentDate.with(DayOfWeek.MONDAY);
      LocalDate specificDate = startDayOfWeek.plusDays(dayNumber - 1);
      scheduleResponse.setDate(specificDate.toString());
    }
    else {
      scheduleResponse.setDate(null);
    }

    scheduleResponse.setStartTime(schedule.getStart_time().toString());
    scheduleResponse.setEndTime(schedule.getEnd_time().toString());
    scheduleResponse.setRoom(schedule.getRoom());
    return scheduleResponse;
  }

  private Integer getDayOfWeekNumber(String dayName) {
    for(int i = 0; i < DAY_NAMES.length; i++) {
      if(DAY_NAMES[i].equals(dayName)) {
        return i;
      }
    }
    return 0;
  }
}
