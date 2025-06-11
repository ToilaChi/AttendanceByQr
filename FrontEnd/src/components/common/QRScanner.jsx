import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../api/api';
import '../../styles/QRScanner.css';

const QRScanner = ({ isOpen, onClose, classInfo }) => {
  const { user } = useAuth();
  const [isScanning, setIsScanning] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [loading, setLoading] = useState(false);
  const [cameraPermission, setCameraPermission] = useState(null);
  
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const scanIntervalRef = useRef(null);

  // Khởi động camera
  const startCamera = useCallback(async () => {
    try {
      setError(null);
      setLoading(true);

      // Kiểm tra hỗ trợ getUserMedia
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('Trình duyệt không hỗ trợ camera');
      }

      // Yêu cầu quyền truy cập camera
      const constraints = {
        video: {
          facingMode: 'environment', // Camera sau (tốt hơn cho quét QR)
          width: { ideal: 1280 },
          height: { ideal: 720 }
        }
      };

      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      streamRef.current = stream;
      
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await new Promise((resolve) => {
          videoRef.current.onloadedmetadata = resolve;
        });
        await videoRef.current.play();
      }

      setCameraPermission('granted');
      setIsScanning(true);
      setLoading(false);
      
      // Bắt đầu quét QR
      startScanning();

    } catch (err) {
      console.error('Error starting camera:', err);
      setCameraPermission('denied');
      setLoading(false);
      
      if (err.name === 'NotAllowedError') {
        setError('Vui lòng cho phép truy cập camera để quét QR code');
      } else if (err.name === 'NotFoundError') {
        setError('Không tìm thấy camera trên thiết bị');
      } else if (err.name === 'NotReadableError') {
        setError('Camera đang được sử dụng bởi ứng dụng khác');
      } else {
        setError('Không thể khởi động camera: ' + err.message);
      }
    }
  }, []);

  // Dừng camera
  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }

    if (scanIntervalRef.current) {
      clearInterval(scanIntervalRef.current);
      scanIntervalRef.current = null;
    }

    setIsScanning(false);
    setCameraPermission(null);
  }, []);

  // Bắt đầu quét QR code
  const startScanning = useCallback(() => {
    if (!videoRef.current || !canvasRef.current) return;

    scanIntervalRef.current = setInterval(async () => {
      try {
        const video = videoRef.current;
        const canvas = canvasRef.current;
        
        if (video.readyState !== video.HAVE_ENOUGH_DATA) return;

        const context = canvas.getContext('2d');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        
        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        
        const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
        
        // Sử dụng thư viện jsQR để decode QR code
        // Cần cài đặt: npm install jsqr
        const code = window.jsQR ? window.jsQR(imageData.data, imageData.width, imageData.height) : null;
        
        if (code) {
          setIsScanning(false);
          clearInterval(scanIntervalRef.current);
          await handleQRDetected(code.data);
        }

      } catch (err) {
        console.error('Error scanning QR:', err);
      }
    }, 100); // Quét mỗi 100ms
  }, []);

  // Xử lý khi phát hiện QR code
  const handleQRDetected = useCallback(async (qrData) => {
    try {
      setLoading(true);
      setError(null);

      // Parse QR data
      let qrInfo;
      try {
        qrInfo = JSON.parse(qrData);
      } catch {
        throw new Error('QR code không hợp lệ');
      }

      // Validate QR data
      if (!qrInfo.classCode || !qrInfo.date || !qrInfo.startTime || !qrInfo.endTime) {
        throw new Error('QR code thiếu thông tin cần thiết');
      }

      // Kiểm tra thời gian hợp lệ (trong vòng 30 phút)
      const now = new Date();
      const qrTime = new Date(`${qrInfo.date}T${qrInfo.startTime}`);
      const timeDiff = Math.abs(now - qrTime) / (1000 * 60); // phút

      if (timeDiff > 30) {
        throw new Error('QR code đã hết hạn');
      }

      // Gửi request điểm danh
      const attendanceData = {
        classCode: qrInfo.classCode,
        date: qrInfo.date,
        startTime: qrInfo.startTime,
        endTime: qrInfo.endTime,
        studentId: user?.data?.id || user?.id,
        timestamp: new Date().toISOString()
      };

      const response = await api.post('/attendance/checkin', attendanceData);

      if (response.data && response.data.success) {
        setSuccess('Điểm danh thành công!');
        setTimeout(() => {
          onClose();
        }, 2000);
      } else {
        throw new Error(response.data?.message || 'Điểm danh thất bại');
      }

    } catch (err) {
      console.error('Error processing QR:', err);
      setError(err.message || 'Có lỗi xảy ra khi điểm danh');
      
      // Tiếp tục quét sau 3 giây
      setTimeout(() => {
        setError(null);
        if (isOpen && cameraPermission === 'granted') {
          setIsScanning(true);
          startScanning();
        }
      }, 3000);
    } finally {
      setLoading(false);
    }
  }, [user, onClose, isOpen, cameraPermission]);

  // Load jsQR library
  useEffect(() => {
    if (isOpen && !window.jsQR) {
      const script = document.createElement('script');
      script.src = 'https://cdnjs.cloudflare.com/ajax/libs/jsQR/1.4.0/jsQR.min.js';
      script.onload = () => {
        console.log('jsQR loaded successfully');
      };
      script.onerror = () => {
        setError('Không thể tải thư viện quét QR');
      };
      document.head.appendChild(script);

      return () => {
        document.head.removeChild(script);
      };
    }
  }, [isOpen]);

  // Khởi động camera khi mở modal
  useEffect(() => {
    if (isOpen) {
      startCamera();
    } else {
      stopCamera();
    }

    return () => {
      stopCamera();
    };
  }, [isOpen, startCamera, stopCamera]);

  // Cleanup khi component unmount
  useEffect(() => {
    return () => {
      stopCamera();
    };
  }, [stopCamera]);

  if (!isOpen) return null;

  return (
    <div className="qr-scanner-overlay">
      <div className="qr-scanner-modal">
        <div className="qr-scanner-header">
          <h3>Quét QR Code Điểm Danh</h3>
          <button className="close-button" onClick={onClose} disabled={loading}>
            ✕
          </button>
        </div>

        <div className="qr-scanner-body">
          {classInfo && (
            <div className="class-info-display">
              <p><strong>Môn học:</strong> {classInfo.subjectName}</p>
              <p><strong>Mã lớp:</strong> {classInfo.classCode}</p>
              <p><strong>Thời gian:</strong> {classInfo.startTime} - {classInfo.endTime}</p>
            </div>
          )}

          <div className="camera-container">
            {loading && (
              <div className="camera-loading">
                <div className="spinner"></div>
                <p>Đang khởi động camera...</p>
              </div>
            )}

            {error && (
              <div className="camera-error">
                <p>{error}</p>
                {cameraPermission === 'denied' && (
                  <button onClick={startCamera} className="retry-button">
                    Thử lại
                  </button>
                )}
              </div>
            )}

            {success && (
              <div className="camera-success">
                <div className="success-icon">✓</div>
                <p>{success}</p>
              </div>
            )}

            <video
              ref={videoRef}
              className={`camera-video ${isScanning ? 'active' : ''}`}
              autoPlay
              playsInline
              muted
            />

            <canvas
              ref={canvasRef}
              className="camera-canvas"
              style={{ display: 'none' }}
            />

            {isScanning && (
              <div className="scan-overlay">
                <div className="scan-frame">
                  <div className="scan-corners">
                    <div className="corner top-left"></div>
                    <div className="corner top-right"></div>
                    <div className="corner bottom-left"></div>
                    <div className="corner bottom-right"></div>
                  </div>
                  <div className="scan-line"></div>
                </div>
                <p className="scan-instruction">
                  Đưa QR code vào khung để quét
                </p>
              </div>
            )}
          </div>

          <div className="scanner-controls">
            <button
              className="control-button cancel"
              onClick={onClose}
              disabled={loading}
            >
              Hủy
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default QRScanner;