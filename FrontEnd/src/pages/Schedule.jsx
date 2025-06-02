import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import api from '../api/api';
import '../styles/Schedule.css'; 
import Header from '../components/layout/Header';

const Schedule = () => {
  const { user } = useAuth();
  const [scheduleData, setScheduleData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentDate, setCurrentDate] = useState(new Date());
  
  // ✅ FIX 1: Sử dụng ref để track API calls và prevent duplicate calls
  const isLoadingRef = useRef(false);
  const lastFetchedRef = useRef('');

  // ✅ FIX 2: Memoize user role để tránh re-render không cần thiết
  const userRole = useMemo(() => user.data?.role, [user.data?.role]);

  // Format date to YYYY-MM-DD
  const formatDate = useCallback((date) => {
    return date.toISOString().split('T')[0];
  }, []);

  // ✅ FIX 3: Memoize formatted current date
  const formattedCurrentDate = useMemo(() => formatDate(currentDate), [currentDate, formatDate]);

  // Get week dates based on current date
  const getWeekDates = useCallback((date) => {
    const currentDay = date.getDay();
    const monday = new Date(date);
    // Adjust to Monday (day 1)
    monday.setDate(date.getDate() - currentDay + 1);

    const weekDates = [];
    for (let i = 0; i < 7; i++) {
      const day = new Date(monday);
      day.setDate(monday.getDate() + i);
      weekDates.push(day);
    }
    return weekDates;
  }, []);

  const weekDates = useMemo(() => getWeekDates(currentDate), [currentDate, getWeekDates]);

  // Day names in Vietnamese
  const dayNames = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];

  // Time slots for the schedule
  const timeSlots = [
    { label: 'Sáng', periods: ['07:00 - 08:30', '8:30 - 10:00', '10:00 - 11:30'] },
    { label: 'Chiều', periods: ['13:00 - 14:30', '14:30 - 16:00', '16:00 - 17:30'] },
    { label: 'Tối', periods: ['18:00 - 19:30', '19:30 - 21:00', '21:00 - 22:30'] }
  ];

  // ✅ FIX 4: Enhanced fetchSchedule with duplicate call prevention
  const fetchSchedule = useCallback(async (date) => {
    const dateKey = `${userRole}-${formatDate(date)}`;
    
    // Prevent duplicate calls for same date and role
    if (isLoadingRef.current || lastFetchedRef.current === dateKey) {
      console.log('🚫 Prevented duplicate API call for:', dateKey);
      return;
    }

    console.log('📞 Making API call for:', dateKey);
    isLoadingRef.current = true;
    lastFetchedRef.current = dateKey;
    
    setLoading(true);
    setError(null);

    try {
      const endpoint = userRole === 'TEACHER' ? '/schedules/teacher' : '/schedules/student';
      const response = await api.get(`${endpoint}?currentDate=${formatDate(date)}`);

      if (response.data && response.data.data) {
        setScheduleData(response.data.data);
      } else {
        setScheduleData([]);
      }
    } catch (err) {
      console.error('Error fetching schedule:', err);
      setError('Không thể tải lịch học. Vui lòng thử lại.');
      setScheduleData([]);
      // Reset lastFetched on error to allow retry
      lastFetchedRef.current = '';
    } finally {
      setLoading(false);
      isLoadingRef.current = false;
    }
  }, [userRole, formatDate]);

  // ✅ FIX 5: Optimized useEffect with stable dependencies
  useEffect(() => {
    if (userRole && formattedCurrentDate) {
      fetchSchedule(currentDate);
    }
  }, [userRole, formattedCurrentDate, fetchSchedule, currentDate]);

  // ✅ FIX 6: Debounced date change to prevent rapid calls
  const [dateChangeTimeout, setDateChangeTimeout] = useState(null);
  
  const handleDateChange = useCallback((newDate) => {
    // Clear existing timeout
    if (dateChangeTimeout) {
      clearTimeout(dateChangeTimeout);
    }
    
    // Set new timeout to debounce date changes
    const timeout = setTimeout(() => {
      setCurrentDate(newDate);
      // Reset lastFetched when user manually changes date
      lastFetchedRef.current = '';
    }, 150);
    
    setDateChangeTimeout(timeout);
  }, [dateChangeTimeout]);

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (dateChangeTimeout) {
        clearTimeout(dateChangeTimeout);
      }
    };
  }, [dateChangeTimeout]);

  // Navigate to previous/next week
  const navigateWeek = useCallback((direction) => {
    const newDate = new Date(currentDate);
    newDate.setDate(currentDate.getDate() + (direction * 7));
    handleDateChange(newDate);
  }, [currentDate, handleDateChange]);

  // ✅ FIX 7: Memoized functions to prevent unnecessary re-renders
  const getClassForSlot = useCallback((dayIndex, timeRange) => {
    const targetDate = weekDates[dayIndex];
    const targetDateStr = formatDate(targetDate);

    return scheduleData.find(schedule => {
      if (schedule.date !== targetDateStr) return false;

      const scheduleStart = schedule.startTime;
      const scheduleEnd = schedule.endTime;

      const [rangeStart, rangeEnd] = timeRange.split(' - ');

      return scheduleStart >= rangeStart && scheduleEnd <= rangeEnd;
    });
  }, [weekDates, scheduleData, formatDate]);

  // Render schedule cell
  const renderScheduleCell = useCallback((dayIndex, timeRange) => {
    const classInfo = getClassForSlot(dayIndex, timeRange);

    if (!classInfo) {
      return <div className="schedule-cell empty"></div>;
    }

    return (
      <div className="schedule-cell filled">
        <div className="class-info">
          <div className="subject-name">{classInfo.subjectName}</div>
          <div className="class-code">Mã lớp: {classInfo.classCode}</div>
          <div className="class-details">
            <div className="time">Thời gian: {classInfo.startTime} - {classInfo.endTime}</div>
            <div className="room">Phòng: {classInfo.room}</div>
            {userRole === 'STUDENT' ? 
              <div className="attendance-link">Điểm danh</div> : 
              <div className="attendance-link">Tạo QR điểm danh</div>
            }
          </div>
        </div>
      </div>
    );
  }, [getClassForSlot, userRole]);

  const today = new Date();
  const todayStr = formatDate(today);

  return (
    <div className="schedule-container">
      <Header />

      <div className="schedule-content">
        <div className="schedule-header">
          <div className="date-picker">
            <input
              type="date"
              value={formattedCurrentDate}
              onChange={(e) => handleDateChange(new Date(e.target.value))}
              className="date-input"
            />
          </div>

          <div className="navigation-buttons">
            <button
              className="nav-button prev"
              onClick={() => navigateWeek(-1)}
              title="Tuần trước"
              disabled={loading}
            >
              ←
            </button>
            <button
              className="nav-button today"
              onClick={() => handleDateChange(new Date())}
              disabled={loading}
            >
              HIỆN TẠI
            </button>
            <button
              className="nav-button next"
              onClick={() => navigateWeek(1)}
              title="Tuần sau"
              disabled={loading}
            >
              →
            </button>
          </div>
        </div>

        {/* Loading indicator */}
        {loading && (
          <div className="loading-indicator">
            Đang tải lịch học...
          </div>
        )}

        {/* Error message */}
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {/* Schedule Table */}
        <div className="schedule-table">
          {/* Header Row */}
          <div className="schedule-row header-row">
            <div className="time-slot-header">Ca học</div>
            {weekDates.map((date, index) => (
              <div
                key={index}
                className={`day-header ${formatDate(date) === todayStr ? 'today' : ''}`}
              >
                <div className="day-name">{dayNames[index]}</div>
                <div className="day-date">{date.getDate()}/{String(date.getMonth() + 1).padStart(2, '0')}/{date.getFullYear()}</div>
              </div>
            ))}
          </div>

          {/* Time Slot Rows */}
          {timeSlots.map((slot, slotIndex) => (
            <div key={slotIndex}>
              {slot.periods.map((period, periodIndex) => (
                <div key={`${slotIndex}-${periodIndex}`} className="schedule-row">
                  {periodIndex === 0 && (
                    <div className="time-slot-label" rowSpan={slot.periods.length}>
                      {slot.label}
                    </div>
                  )}
                  {periodIndex > 0 && <div className="time-slot-spacer"></div>}

                  {weekDates.map((_, dayIndex) => (
                    <div key={dayIndex} className="schedule-cell-container">
                      {renderScheduleCell(dayIndex, period)}
                    </div>
                  ))}
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Schedule;