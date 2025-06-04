import React, { useState, useEffect, useRef, useCallback } from 'react';
import api from '../api/api';
import '../styles/QRDisplay.css';

const QRDisplayPage = () => {
  const [qrData, setQrData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [timeRemaining, setTimeRemaining] = useState(0);
  
  // Refs để tránh duplicate api calls 
  const apiCallInProgressRef = useRef(false);
  const hasInitializedRef = useRef(false);
  const timerRef = useRef(null);

  const generateAndSetQrData = useCallback(async () => {
    if (apiCallInProgressRef.current) {
      return;
    }

    if (hasInitializedRef.current) {
      return;
    }

    apiCallInProgressRef.current = true;
    hasInitializedRef.current = true;
    setLoading(true);
    setError(null);

    try {
      const response = await api.post('/qr/generate');
      
      if (response.data) {
        if (expiredTime && typeof expiredTime === 'number') {
          const qrDataWithTime = {
            ...response.data,
            expiredTime: expiredTime 
          };
          
          setQrData(qrDataWithTime);
          
          const currentTime = Date.now();
          const remaining = Math.max(0, expiredTime - currentTime);
          setTimeRemaining(remaining);
        } else {
          console.error('QRDisplayPage: Invalid or missing expiredTime in response data:', response.data);
          console.error('Available fields:', Object.keys(response.data));
          setError('Không nhận được thông tin thời gian hết hạn từ server. Vui lòng thử lại.');
        }
      } else {
        console.error('QRDisplayPage: No data in response');
        setError('Không nhận được dữ liệu QR từ server.');
      }
    } catch (err) {
      console.error('QRDisplayPage: Error generating QR code:', err);
      console.error('Error response:', err.response?.data);
      setError(
        err.response?.data?.message ||  
        'Không thể tạo mã QR. Vui lòng thử lại.'
      );
      // Reset initialization để thử lại 
      hasInitializedRef.current = false;
    } finally {
      apiCallInProgressRef.current = false;
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;
    
    const initializeQR = async () => {
      if (isMounted && !hasInitializedRef.current) {
        await generateAndSetQrData();
      }
    };

    // Delay 100ms để chắc chắn component đã mounted
    const timeoutId = setTimeout(initializeQR, 100);

    return () => {
      isMounted = false;
      clearTimeout(timeoutId);
      // Không reset hasInitializedRef ở đây để tránh gọi lại generateAndSetQrData khi component unmount
    };
  }, []); 

  useEffect(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }

    if (!qrData?.expiredTime || typeof qrData.expiredTime !== 'number') {
      return;
    }

    const startTimer = () => {
      timerRef.current = setInterval(() => {
        const now = Date.now();
        const currentExpiredTime = qrData.expiredTime; 
        const remaining = Math.max(0, currentExpiredTime - now);
        
        setTimeRemaining(remaining);
        
        if (remaining === 0) {
          if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
          }
        }
      }, 1000);
    };

    const now = Date.now();
    const initialRemaining = Math.max(0, qrData.expiredTime - now);
    setTimeRemaining(initialRemaining);
    
    if (initialRemaining > 0) {
      startTimer();
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [qrData?.expiredTime]); 

  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, []);

  const formatTime = (milliseconds) => {
    if (typeof milliseconds !== 'number' || milliseconds < 0) return '00:00';
    const totalSeconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  const handleCloseWindow = () => window.close();

  const handleRetry = useCallback(() => {
    hasInitializedRef.current = false;
    apiCallInProgressRef.current = false;
    setError(null);
    setQrData(null);
    setTimeRemaining(0);
    
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    
    generateAndSetQrData();
  }, [generateAndSetQrData]);

  if (loading && !qrData && !error) { 
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
            <button className="retry-button" onClick={handleRetry}>
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
  
  if (!qrData) {
    return (
      <div className="qr-display-container">
        <div className="error-container">
          <div className="error-icon">🤔</div>
          <h2>Không có dữ liệu QR</h2>
          <p className="error-message">Không thể hiển thị mã QR. Vui lòng thử lại.</p>
          <div className="button-group">
            <button className="retry-button" onClick={handleRetry}>
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
              <div className="qr-placeholder">
                <div className="placeholder-icon">📱</div>
                <p>QR Code sẽ hiển thị ở đây</p>
              </div>
            )}
            {timeRemaining > 0 && qrData.qrImageBase64 && (
              <div className="timer-overlay">
                <div className={`timer ${timeRemaining < 60000 ? 'warning' : ''}`}>
                  ⏱️ {formatTime(timeRemaining)}
                </div>
              </div>
            )}
          </div>
          {qrData.qrImageBase64 && (
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