// Mở QR tab đơn giản
export const openQRTab = () => {
  const qrTab = window.open('/qr-display', '_blank');
  if (qrTab) {
    qrTab.focus();
  }
  return qrTab;
};

// Kiểm tra xem có phải giờ học không (15 phút trước giờ bắt đầu đến hết giờ)
export const canGenerateQR = (startTime, endTime, currentDate) => {
  const now = new Date();
  const todayStr = now.toISOString().split('T')[0];
  const classDateStr = currentDate;
  
  // Chỉ cho phép tạo QR trong ngày học
  if (todayStr !== classDateStr) {
    return {
      canGenerate: false,
      reason: 'Chỉ có thể tạo QR code trong ngày học'
    };
  }

  const [startHour, startMinute] = startTime.split(':').map(Number);
  const [endHour, endMinute] = endTime.split(':').map(Number);
  
  const classStart = new Date();
  classStart.setHours(startHour, startMinute, 0, 0);
  
  const classEnd = new Date();
  classEnd.setHours(endHour, endMinute, 0, 0);
  
  // Cho phép tạo QR từ 15 phút trước giờ học đến hết giờ học
  const allowedStart = new Date(classStart.getTime() - 15 * 60 * 1000);
  
  if (now < allowedStart) {
    const minutesUntilAllowed = Math.ceil((allowedStart - now) / (1000 * 60));
    return {
      canGenerate: false,
      reason: `Chưa thể tạo QR code. Vui lòng đợi ${minutesUntilAllowed} phút nữa`
    };
  }
  
  if (now > classEnd) {
    return {
      canGenerate: false,
      reason: 'Đã hết giờ học, không thể tạo QR code'
    };
  }
  
  return {
    canGenerate: true,
    reason: null
  };
};