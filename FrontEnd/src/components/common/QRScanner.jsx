import React, { useState, useRef, useEffect, useCallback } from 'react';
import jsQR from 'jsqr';
import { useAuth } from '../../contexts/AuthContext';
import '../../styles/QRScanner.css';
import api from '../../api/api';
import { API_NOTIFICATIONS } from '../../config';
import { Client } from '@stomp/stompjs';

const QRScanner = ({ isOpen, onClose, classInfo }) => {
  const { user } = useAuth();
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const wsRef = useRef(null);
  const scanningIntervalRef = useRef(null);
  const locationWatchRef = useRef(null);
  const attendanceStatusRef = useRef('not_checked');

  // States
  const [isScanning, setIsScanning] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [cameraPermission, setCameraPermission] = useState('prompt'); // 'granted', 'denied', 'prompt'
  const [attendanceStatus, setAttendanceStatus] = useState('not_checked'); // 'not_checked', 'loading', 'waiting', 'checked'
  const [isConnected, setIsConnected] = useState(false);
  const [currentLocation, setCurrentLocation] = useState(null);

  // WebSocket URL - adjust according to your backend configuration
  const token = localStorage.getItem('accessToken');
  const WS_URL = `${API_NOTIFICATIONS.replace('https', 'wss')}/ws-notifications?token=${encodeURIComponent(token)}`;

  // Initialize WebSocket connection
  const initWebSocket = useCallback(() => {
    if (!user?.data?.cic) return;

    try {
      const stompClient = new Client({
        brokerURL: WS_URL,
        connectHeaders: {},
        debug: function (str) {
          console.log('STOMP: ' + str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      stompClient.onConnect = (frame) => {
        console.log('Connected: ' + frame);
        setIsConnected(true);

        // Subscribe to student's personal topic
        stompClient.subscribe(`/topic/student/${user.data.cic}`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('Received notification:', data);
            handleWebSocketMessage(data);
          } catch (err) {
            console.error('Error parsing message:', err);
          }
        });
      };

      stompClient.onDisconnect = () => {
        console.log('Disconnected');
        setIsConnected(false);
      };

      stompClient.activate();
      wsRef.current = stompClient;

    } catch (err) {
      console.error('Error initializing WebSocket:', err);
      setIsConnected(false);
    }
  }, [user?.data?.cic, WS_URL]);

  // Stop camera
  const stopCamera = useCallback(() => {
    if (videoRef.current && videoRef.current.srcObject) {
      const tracks = videoRef.current.srcObject.getTracks();
      tracks.forEach(track => track.stop());
      videoRef.current.srcObject = null;
    }
    setIsScanning(false);

    if (scanningIntervalRef.current) {
      clearInterval(scanningIntervalRef.current);
      scanningIntervalRef.current = null;
    }
  }, []);

  const handleWebSocketMessage = useCallback((data) => {
    if (data.type === 'ATTENDANCE_SUCCESS') {
      attendanceStatusRef.current = 'checked';
      setAttendanceStatus('checked');
      setSuccess(data.message || 'Điểm danh thành công!');
      setLoading(false);
      setError('');
      stopCamera();

      // Auto close after 3 seconds on success
      setTimeout(() => {
        onClose();
      }, 3000);
    } else if (data.type === 'ATTENDANCE_FAILED') {
      attendanceStatusRef.current = 'not_checked';
      setAttendanceStatus('not_checked');
      setError(data.message || 'Điểm danh thất bại!');
      setLoading(false);
      setSuccess('');
      stopCamera();
    } else if (data.type === 'SUBSCRIPTION_ACK') {
      console.log('Subscribed to notifications successfully');
    }
  }, [onClose, stopCamera]);

  const reconnectWebSocket = useCallback(() => {
    console.log('🔄 Attempting to reconnect WebSocket...');
    if (wsRef.current) {
      wsRef.current.close();
    }
    setTimeout(() => {
      initWebSocket();
    }, 1000);
  }, [initWebSocket]);

  // Get user location
  const getCurrentLocation = useCallback(() => {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocation is not supported by this browser'));
        return;
      }

      const options = {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000
      };

      navigator.geolocation.getCurrentPosition(
        (position) => {
          const coords = {
            latitude: position.coords.latitude.toString(),
            longitude: position.coords.longitude.toString()
          };
          setCurrentLocation(coords);
          resolve(coords);
        },
        (error) => {
          console.error('Error getting location:', error);
          reject(error);
        },
        options
      );
    });
  }, []);

  // Check attendance status first
  const checkAttendanceStatus = useCallback(async () => {
    if (!classInfo || !user) return;

    try {
      setAttendanceStatus('loading');
      setLoading(true);

      // Gọi API để kiểm tra trạng thái điểm danh
      const response = await api.get(`/attendances/status?studentCIC=${user.data.cic}&scheduleId=${classInfo.scheduleId}&date=${classInfo.date}`);

      if (response.data && response.data.message === 'Đã điểm danh') {
        setAttendanceStatus('checked');
        setLoading(false);
        setSuccess('Bạn đã điểm danh rồi!');
        return;
      }
    } catch (err) {
      console.error('Error checking attendance status:', err);
    }
  }, [classInfo, user]);

  // Start camera
  const startCamera = useCallback(async () => {
    try {
      setError('');
      setLoading(true);

      checkAttendanceStatus();

      setAttendanceStatus('not_checked');
      console.log('attendanceStatus after check:', attendanceStatus);

      // Get user location
      try {
        await getCurrentLocation();
      } catch (locationError) {
        console.warn('Could not get location:', locationError);
        // Continue without location
        setCurrentLocation({ latitude: '0', longitude: '0' });
      }

      // Request camera permission
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: 'environment', // Use back camera if available
          width: { ideal: 1280, max: 1920 },
          height: { ideal: 720, max: 1080 }
        }
      });

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
        setCameraPermission('granted');
        setIsScanning(true);
        setLoading(false);

        // Start QR scanning
        startQRScanning();
      }
    } catch (err) {
      console.error('Error starting camera:', err);
      if (err.name === 'NotAllowedError') {
        setCameraPermission('denied');
        setError('Cần quyền truy cập camera để quét QR code');
      } else if (err.name === 'NotFoundError') {
        setError('Không tìm thấy camera');
      } else {
        setError('Không thể khởi động camera: ' + err.message);
      }
      setLoading(false);
      setIsScanning(false);
    }
  }, [classInfo, user?.data?.cic, getCurrentLocation]);

  // Start QR scanning
  const startQRScanning = useCallback(() => {
    if (scanningIntervalRef.current) {
      clearInterval(scanningIntervalRef.current);
    }

    scanningIntervalRef.current = setInterval(() => {
      if (videoRef.current && canvasRef.current && videoRef.current.readyState === 4) {
        const canvas = canvasRef.current;
        const video = videoRef.current;
        const context = canvas.getContext('2d');

        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        context.drawImage(video, 0, 0, canvas.width, canvas.height);

        try {
          const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
          const code = jsQR(imageData.data, imageData.width, imageData.height);

          if (code) {
            console.log('QR Code detected:', code.data);
            handleQRCodeDetected(code.data);
          }
        } catch (err) {
          console.error('Error scanning QR code:', err);
        }
      }
    }, 300); // Scan every 300ms
  }, []);

  // Handle QR code detection
  const handleQRCodeDetected = useCallback(async (qrData) => {
    if (loading || attendanceStatusRef.current === 'waiting' || attendanceStatusRef.current === 'checked') {
      console.log('Skipping QR detection - current status:', attendanceStatusRef.current);
      return;
    }

    try {
      attendanceStatusRef.current = 'waiting';
      setAttendanceStatus('waiting');
      setLoading(true);
      setError('');
      setSuccess('');

      if (!wsRef.current || !wsRef.current.connected) {
        console.log('WebSocket not connected, reconnecting...');
        initWebSocket();
      }

      await new Promise(resolve => setTimeout(resolve, 100));

      console.log('📝 Attendance status should be waiting now');

      // Stop scanning temporarily
      if (scanningIntervalRef.current) {
        clearInterval(scanningIntervalRef.current);
        scanningIntervalRef.current = null;
      }

      // Use current location or get it again
      let coords = currentLocation;
      if (!coords) {
        try {
          coords = await getCurrentLocation();
        } catch (locationError) {
          console.warn('Using default location due to error:', locationError);
          coords = { latitude: '0', longitude: '0' };
        }
      }

      //Parse QR data
      let qrSignature;
      if (qrData.startsWith('http') && qrData.includes('signature=')) {
        const url = new URL(qrData);
        qrSignature = url.searchParams.get('signature');
        if (!qrSignature) {
          throw new Error('Không tìm thấy signature trong QR code');
        }
      } else {
        try {
          const qrInfo = JSON.parse(qrData);
          qrSignature = qrInfo.signature;
          if (!qrSignature) {
            throw new Error('QR code thiếu thông tin signature');
          }
        } catch (parseErr) {
          throw new Error('QR code không đúng định dạng');
        }
      }

      // Prepare attendance data
      const attendanceData = {
        qrSignature: qrSignature,
        studentCIC: user.data.cic,
        deviceInfo: navigator.userAgent || 'Unknown Device',
        latitude: coords.latitude,
        longitude: coords.longitude,
      };

      console.log('Submitting attendance:', attendanceData);

      // Submit attendance
      const response = await api.post('/attendances', attendanceData);
      console.log('📡 Attendance API response:', response.status);
      console.log('WebSocket connection status:', isConnected ? 'Connected' : 'Disconnected');

      if (response.status === 202) {
        console.log('Attendance request accepted, waiting for WebSocket notification...');
      } else {
        throw new Error('Unexpected response status');
      }

      // The response will come via WebSocket, so we just wait
      console.log('Attendance submitted, waiting for WebSocket response...');

    } catch (err) {
      console.error('Error submitting attendance:', err);
      attendanceStatusRef.current = 'not_checked';
      setAttendanceStatus('not_checked');
      setError(err.response?.data?.message || 'Có lỗi xảy ra khi điểm danh');
      setLoading(false);

      // Restart scanning after error
      setTimeout(() => {
        if (isScanning) {
          startQRScanning();
        }
      }, 2000);
    }
  }, [loading, attendanceStatus, currentLocation, user?.data?.cic, getCurrentLocation, isScanning, startQRScanning, initWebSocket]);

  // Cleanup function
  const cleanup = useCallback(() => {
    stopCamera();

    if (wsRef.current) {
      wsRef.current.deactivate();
      wsRef.current = null;
    }

    if (locationWatchRef.current) {
      navigator.geolocation.clearWatch(locationWatchRef.current);
      locationWatchRef.current = null;
    }
    attendanceStatusRef.current = 'not_checked';
  }, [stopCamera]);

  // Initialize when component opens
  useEffect(() => {
    if (isOpen) {
      setAttendanceStatus('not_checked');
      setError('');
      setSuccess('');
      setLoading(false);

      // Initialize WebSocket
      initWebSocket();

      // Start camera
      setTimeout(() => {
        startCamera();
      }, 100);
    } else {
      cleanup();
    }

    return cleanup;
  }, [isOpen, initWebSocket, startCamera, cleanup]);

  // Handle close
  const handleClose = useCallback(() => {
    cleanup();
    onClose();
  }, [cleanup, onClose]);

  if (!isOpen) return null;

  return (
    <div className="qr-scanner-overlay">
      <div className="qr-scanner-modal">
        <div className="qr-scanner-header">
          <h3>Quét QR Code Điểm Danh</h3>
          <button className="close-button" onClick={handleClose} disabled={loading}>
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

          {/* WebSocket Connection Status */}
          {attendanceStatus === 'not_checked' && (
            <div className="connection-status">
              <small style={{ color: isConnected ? 'green' : 'orange' }}>
                {isConnected ? '🟢 Kết nối real-time' : '🟡 Đang kết nối...'}
              </small>
            </div>
          )}

          <div className="camera-container">
            {loading && attendanceStatus === 'loading' && (
              <div className="camera-loading">
                <div className="spinner"></div>
                <p>Đang kiểm tra trạng thái điểm danh...</p>
              </div>
            )}

            {loading && (attendanceStatus === 'loading' || attendanceStatus === 'waiting') && (
              <div className="camera-loading">
                <div className="spinner"></div>
                <p>{attendanceStatus === 'loading' ? 'Đang kiểm tra trạng thái điểm danh...' : 'Đang xử lý điểm danh...'}</p>
              </div>
            )}

            {error && (
              <div className="camera-error">
                <p>{error}</p>
                {cameraPermission === 'denied' && attendanceStatus !== 'checked' && (
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

            {attendanceStatus === 'checked' && !success && (
              <div className="already-attended">
                <div className="success-icon">✓</div>
                <p>Bạn đã điểm danh cho lớp này rồi!</p>
                <small>Không thể điểm danh lại</small>
              </div>
            )}

            {attendanceStatus === 'not_checked' && (
              <>
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
              </>
            )}
          </div>

          <div className="scanner-controls">
            <button
              className="control-button cancel"
              onClick={handleClose}
              disabled={loading}
            >
              {attendanceStatus === 'checked' ? 'Đóng' : 'Hủy'}
            </button>
            {error && attendanceStatus !== 'checked' && (
              <button
                className="control-button retry"
                onClick={startCamera}
                disabled={loading}
              >
                Thử lại
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default QRScanner;