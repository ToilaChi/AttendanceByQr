import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import api from '../api/api';
import { openQRTab, canGenerateQR } from '../utils/qrUtils';
import QRScanner from '../components/common/QRScanner';
import '../styles/Schedule.css';
import Header from '../components/layout/Header';

const Schedule = () => {
  const { user } = useAuth();
  const [scheduleData, setScheduleData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentDate, setCurrentDate] = useState(new Date());
  const [qrGenerating, setQrGenerating] = useState(false);

  // QR Scanner states
  const [showQRScanner, setShowQRScanner] = useState(false);
  const [selectedClassForAttendance, setSelectedClassForAttendance] = useState(null);

  // Refs để prevent duplicate calls
  const fetchingRef = useRef(false);
  const lastFetchedDate = useRef(null);
  const lastFetchedRole = useRef(null);

  const userRole = useMemo(() => user?.data?.role || user?.role, [user]);

  // Format date to YYYY-MM-DD
  const formatDate = useCallback((date) => {
    return date.toISOString().split('T')[0];
  }, []);

  const formattedCurrentDate = useMemo(() => formatDate(currentDate), [currentDate, formatDate]);

  // Get tuần hiện tại dựa trên current date 
  const getWeekDates = useCallback((date) => {
    let currentDay = date.getDay(); // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    if (currentDay === 0) {
      currentDay = 7; // Đổi Chủ Nhật từ 0 → 7 để thuộc tuần hiện tại
    }
    const monday = new Date(date);
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
    { label: 'Sáng', periods: ['00:00 - 02:00', '06:45 - 09:15', '09:25 - 11:55'] },
    { label: 'Chiều', periods: ['12:00 - 14:40', '14:50 - 17:20'] },
    { label: 'Tối', periods: ['17:30 - 20:00', '20:30 - 23:55'] }
  ];

  // Optimized hàm gọi lịch và tránh duplicate calls
  const fetchSchedule = useCallback(async (date) => {
    const dateStr = formatDate(date);

    // Prevent duplicate calls
    if (fetchingRef.current ||
      (lastFetchedDate.current === dateStr && lastFetchedRole.current === userRole)) {
      return;
    }

    if (!userRole) {
      return;
    }

    fetchingRef.current = true;
    setLoading(true);
    setError(null);

    try {
      const endpoint = userRole === 'TEACHER' ? '/schedules/teacher' : '/schedules/student';
      const response = await api.get(`${endpoint}?currentDate=${dateStr}`);

      if (response.data && response.data.data) {
        setScheduleData(response.data.data);
        lastFetchedDate.current = dateStr;
        lastFetchedRole.current = userRole;
      } else {
        setScheduleData([]);
        lastFetchedDate.current = dateStr;
        lastFetchedRole.current = userRole;
      }
    } catch (err) {
      console.error('Schedule.jsx: Error fetching schedule:', err);
      setError('Không thể tải lịch học. Vui lòng thử lại.');
      setScheduleData([]);
    } finally {
      setLoading(false);
      fetchingRef.current = false;
    }
  }, [userRole, formatDate]);

  // Generate QR code (for teacher)
  const handleGenerateQR = useCallback(async (classInfo) => {
    if (qrGenerating) {
      return;
    }

    try {
      setQrGenerating(true);

      // Kiểm tra xem có thể tạo QR không
      const qrCheck = canGenerateQR(classInfo.startTime, classInfo.endTime, classInfo.date);
      if (!qrCheck.canGenerate) {
        alert(qrCheck.reason);
        return;
      }

      // Mở tab QR
      const qrTab = openQRTab();

      if (!qrTab) {
        alert('Không thể mở cửa sổ mới. Vui lòng kiểm tra cài đặt trình duyệt.');
        return;
      }

    } catch (error) {
      console.error('Schedule.jsx: Error generating QR:', error);
      alert('Có lỗi xảy ra khi mở tab QR code. Vui lòng thử lại.');
    } finally {
      setQrGenerating(false);
    }
  }, [qrGenerating]);

  // Handle attendance (for student) - Mở QR Scanner
  const handleAttendance = useCallback((classInfo) => {
    // Kiểm tra thời gian có hợp lệ để điểm danh không
    const now = new Date();
    const classDate = new Date(`${classInfo.date}T${classInfo.startTime}`);
    const endTime = new Date(`${classInfo.date}T${classInfo.endTime}`);

    // Cho phép điểm danh từ 30 phút trước đến 15 phút sau khi kết thúc lớp
    const canAttendFrom = new Date(classDate.getTime() - 30 * 60 * 1000); // 30 phút trước
    const canAttendUntil = new Date(endTime.getTime() + 15 * 60 * 1000); // 15 phút sau khi kết thúc

    if (now < canAttendFrom) {
      alert('Chưa đến thời gian điểm danh. Vui lòng thử lại sau.');
      return;
    }

    if (now > canAttendUntil) {
      alert('Đã hết thời gian điểm danh cho lớp học này.');
      return;
    }

    // Mở QR Scanner
    setSelectedClassForAttendance(classInfo);
    setShowQRScanner(true);
  }, []);

  // Close QR Scanner
  const handleCloseQRScanner = useCallback(() => {
    setShowQRScanner(false);
    setSelectedClassForAttendance(null);

    // Refresh schedule để cập nhật trạng thái điểm danh
    setTimeout(() => {
      refreshSchedule();
    }, 1000);
  }, []);

  useEffect(() => {
    let isMounted = true;

    const initiateFetch = async () => {
      if (isMounted && userRole && formattedCurrentDate) {
        await fetchSchedule(currentDate);
      }
    };

    initiateFetch();

    return () => {
      isMounted = false;
    };
  }, [userRole, formattedCurrentDate, fetchSchedule, currentDate]);

  // Navigate to previous/next week
  const navigateWeek = useCallback((direction) => {
    const newDate = new Date(currentDate);
    newDate.setDate(currentDate.getDate() + (direction * 7));
    setCurrentDate(newDate);

    // Reset fetch cache when navigating
    lastFetchedDate.current = null;
    lastFetchedRole.current = null;
  }, [currentDate]);

  // Refresh schedule
  const refreshSchedule = useCallback(() => {
    lastFetchedDate.current = null;
    lastFetchedRole.current = null;
    fetchSchedule(currentDate);
  }, [currentDate, fetchSchedule]);

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

  const canGenerateQRForClass = useCallback((classInfo) => {
    if (!classInfo) return false;
    return canGenerateQR(classInfo.startTime, classInfo.endTime, classInfo.date).canGenerate;
  }, []);

  // Check if student can attend class
  const canAttendClass = useCallback((classInfo) => {
    if (!classInfo) return { canAttend: false, reason: '' };

    const now = new Date();
    const classDate = new Date(`${classInfo.date}T${classInfo.startTime}`);
    const endTime = new Date(`${classInfo.date}T${classInfo.endTime}`);

    // Cho phép điểm danh từ 30 phút trước đến 15 phút sau khi kết thúc lớp
    const canAttendFrom = new Date(classDate.getTime() - 30 * 60 * 1000);
    const canAttendUntil = new Date(endTime.getTime() + 15 * 60 * 1000);

    if (now < canAttendFrom) {
      return { canAttend: false, reason: 'Chưa đến thời gian điểm danh' };
    }

    if (now > canAttendUntil) {
      return { canAttend: false, reason: 'Đã hết thời gian điểm danh' };
    }

    return { canAttend: true, reason: '' };
  }, []);

  // Render schedule cell
  const renderScheduleCell = useCallback((dayIndex, timeRange) => {
    const classInfo = getClassForSlot(dayIndex, timeRange);

    if (!classInfo) {
      return <div className="schedule-cell empty"></div>;
    }

    const canGenerate = canGenerateQRForClass(classInfo);
    const attendanceStatus = canAttendClass(classInfo);

    return (
      <div className="schedule-cell filled">
        <div className="class-info">
          <div className="subject-name">{classInfo.subjectName}</div>
          <div className="class-code">Mã lớp: {classInfo.classCode}</div>
          <div className="class-details">
            <div className="room">Phòng: {classInfo.room}</div>
            <div className="time">Thời gian: {classInfo.startTime} - {classInfo.endTime}</div>

            {userRole === 'TEACHER' ? (
              <button
                className={`qr-button ${canGenerate && !qrGenerating ? 'active' : 'disabled'}`}
                onClick={() => canGenerate && !qrGenerating && handleGenerateQR(classInfo)}
                disabled={!canGenerate || qrGenerating}
                title={
                  !canGenerate
                    ? canGenerateQR(classInfo.startTime, classInfo.endTime, classInfo.date).reason
                    : 'Tạo QR code điểm danh'
                }
              >
                {qrGenerating ? '⏳ Đang tạo...' : '📱 Tạo QR'}
              </button>
            ) : (
              <button
                className={`attendance-button ${attendanceStatus.canAttend ? 'active' : 'disabled'}`}
                onClick={() => attendanceStatus.canAttend && handleAttendance(classInfo)}
                disabled={!attendanceStatus.canAttend || classInfo.attendanceStatus === 'checked'}
                title={
                  classInfo.attendanceStatus === 'checked'
                    ? 'Đã điểm danh'
                    : !attendanceStatus.canAttend
                      ? attendanceStatus.reason
                      : 'Nhấn để điểm danh'
                }
              >
                {classInfo.attendanceStatus === 'checked' ? '✓ Đã điểm danh' : '📍 Điểm danh'}
              </button>
            )}
          </div>
        </div>
      </div>
    );
  }, [getClassForSlot, userRole, canGenerateQRForClass, canAttendClass, handleGenerateQR, handleAttendance, qrGenerating]);

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
              onChange={(e) => setCurrentDate(new Date(e.target.value))}
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
              onClick={() => setCurrentDate(new Date())}
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
            <button
              className="nav-button refresh"
              onClick={refreshSchedule}
              disabled={loading}
              title="Làm mới"
            >
              🔄
            </button>
          </div>
        </div>

        {loading && (
          <div className="loading-indicator">
            Đang tải lịch học...
          </div>
        )}

        {error && (
          <div className="error-message">
            {error}
            <button onClick={refreshSchedule} className="retry-button">
              Thử lại
            </button>
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

      {/* QR Scanner Modal */}
      <QRScanner
        isOpen={showQRScanner}
        onClose={handleCloseQRScanner}
        classInfo={selectedClassForAttendance}
      />
    </div>
  );
};

export default Schedule;