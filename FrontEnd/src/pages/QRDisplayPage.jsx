import React, { useState, useEffect, useRef } from 'react';
import api from '../api/api';
import '../styles/QRDisplay.css';

const QRDisplayPage = () => {
  const [qrData, setQrData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [timeRemaining, setTimeRemaining] = useState(0);
  const apiCallInProgressRef = useRef(false); 
  const initialApiCallDoneRef = useRef(false); 

  const performQRCodeGeneration = async () => {
    if (apiCallInProgressRef.current) {
      return; // Đang có một lệnh gọi API khác, không làm gì cả
    }

    apiCallInProgressRef.current = true;
    setLoading(true);
    setError(null);

    try {
      const response = await api.post('/qr/generate');
      if (response.data) {
        console.log('QRDisplayPage: Raw expiredTime from server:', response.data.expiredTime);
        console.log('QRDisplayPage: Converted server expiredTime to Date:', new Date(response.data.expiredTime));
        console.log('QRDisplayPage: Client Date.now() at receive time:', Date.now());
        console.log('QRDisplayPage: Converted client Date.now() to Date:', new Date(Date.now()));

        setQrData(response.data);
        const remaining = response.data.expiredTime - Date.now();
        setTimeRemaining(Math.max(0, remaining));
        initialApiCallDoneRef.current = true; // Đánh dấu gọi API ban đầu đã hoàn tất
      } else {
        setError('Không nhận được dữ liệu QR hợp lệ.');
        initialApiCallDoneRef.current = true; // Vẫn đánh dấu để useEffect không cố gọi lại
      }
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Không thể tạo mã QR. Vui lòng thử lại.'
      );
      initialApiCallDoneRef.current = true; // Vẫn đánh dấu để useEffect không cố gọi lại
    } finally {
      apiCallInProgressRef.current = false;
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!initialApiCallDoneRef.current) {
      performQRCodeGeneration();
    }
  }, []);

  useEffect(() => {
    if (!qrData?.expiredTime) return;

    const timer = setInterval(() => {
      const now = Date.now();
      const remaining = Math.max(0, qrData.expiredTime - now);
      setTimeRemaining(remaining);

      if (remaining === 0) {
        clearInterval(timer);
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [qrData?.expiredTime]);

  const handleRetryGenerate = () => {
    // Không cần reset initialApiCallDoneRef vì performQRCodeGeneration sẽ tự xử lý logic loading và apiCallInProgressRef
    performQRCodeGeneration();
  };

  const handleCloseWindow = () => {
    window.close();
  };

  const formatTime = (milliseconds) => {
    const totalSeconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  if (loading && !initialApiCallDoneRef.current) { // Chỉ hiển thị loading ban đầu
    return (
      <div className="qr-display-container">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <h2>Đang tạo mã QR điểm danh...</h2>
          <p>Vui lòng đợi trong giây lát</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="qr-display-container">
        <div className="error-container">
          <div className="error-icon">⚠️</div>
          <h2>Không thể tạo mã QR</h2>
          <p className="error-message">{error}</p>
          <div className="button-group">
            <button className="retry-button" onClick={handleRetryGenerate}>
              🔄 Thử lại
            </button>
            <button className="close-button" onClick={handleCloseWindow}>
              ✕ Đóng
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!qrData && initialApiCallDoneRef.current && !error) {
    return (
      <div className="qr-display-container">
        <div className="error-container">
          <div className="error-icon">🤔</div>
          <h2>Không có dữ liệu QR</h2>
          <p className="error-message">Không thể hiển thị mã QR. Vui lòng thử lại.</p>
          <div className="button-group">
            <button className="retry-button" onClick={handleRetryGenerate}>
              🔄 Thử lại
            </button>
            <button className="close-button" onClick={handleCloseWindow}>
              ✕ Đóng
            </button>
          </div>
        </div>
      </div>
    );
  }


  return (
    <div className="qr-display-container">
      <div className="qr-content">
        <div className="qr-header">
          <h1>📱 Mã QR Điểm Danh</h1>
          <button className="close-btn" onClick={handleCloseWindow} title="Đóng cửa sổ">
            ✕
          </button>
        </div>

        <div className="qr-section">
          <div className="qr-code-container">
            {qrData?.qrImageBase64 ? (
              <img
                src={`data:image/png;base64,${qrData.qrImageBase64}`}
                alt="QR Code Điểm Danh"
                className="qr-code-image"
              />
            ) : (
              // Đoạn này có thể sẽ không bao giờ được hiển thị nếu logic ở trên xử lý hết các trường hợp
              <div className="qr-placeholder">
                <div className="placeholder-icon">📱</div>
                <p>QR Code sẽ hiển thị ở đây</p>
              </div>
            )}

            {timeRemaining > 0 && qrData && (
              <div className="timer-overlay">
                <div className={`timer ${timeRemaining < 60000 ? 'warning' : ''}`}>
                  ⏱️ {formatTime(timeRemaining)}
                </div>
              </div>
            )}
          </div>

          {qrData && (
            <div className="qr-info">
              <div className="info-item">
                <span className="info-label">Mã định danh QR:</span>
                <span className="info-value">#{qrData.qrSignature?.slice(-8).toUpperCase()}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Trạng thái:</span>
                <span className={`info-value status ${timeRemaining > 0 ? 'active' : 'expired'}`}>
                  {timeRemaining > 0 ? '🟢 Hoạt động' : '🔴 Hết hạn'}
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default QRDisplayPage;