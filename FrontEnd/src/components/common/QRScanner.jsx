import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../api/api';
import jsQR from 'jsqr';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import '../../styles/QRScanner.css';
import { API_NOTIFICATIONS } from '../../config';

const QRScanner = ({ isOpen, onClose, classInfo }) => {
  const { user } = useAuth();
  const [isScanning, setIsScanning] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [loading, setLoading] = useState(false);
  const [cameraPermission, setCameraPermission] = useState(null);
  const [location, setLocation] = useState({ latitude: null, longitude: null });
  const [attendanceStatus, setAttendanceStatus] = useState(null);
  const [stompClient, setStompClient] = useState(null);
  const [isConnected, setIsConnected] = useState(false);

  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const scanIntervalRef = useRef(null);

  // Handle attendance notification từ WebSocket
  const handleAttendanceNotification = useCallback((notification) => {
    console.log('Received attendance notification:', notification);

    if (notification.type === 'ATTENDANCE_SUCCESS') {
      setSuccess(notification.message);
      setAttendanceStatus('checked');
      setLoading(false);

      // Auto close after 2 seconds
      setTimeout(() => {
        onClose();
      }, 2000);

    } else if (notification.type === 'ATTENDANCE_FAILED') {
      setError(notification.message);
      setLoading(false);

      // Retry scanning after 3 seconds
      setTimeout(() => {
        if (isOpen && cameraPermission === 'granted' && attendanceStatus !== 'checked') {
          setError(null);
          setIsScanning(true);
          startScanning();
        }
      }, 3000);
    }
  }, [isOpen, cameraPermission, attendanceStatus, onClose]);

  // Native WebSocket connection function
  const connectWebSocket = useCallback(async () => {
    try {
      const token = localStorage.getItem('accessToken');
      console.log('Token:', token ? token.substring(0, 20) + '...' : 'null');
      if (!token) {
        throw new Error('No access token available');
      }
      const wssUrl = `${API_NOTIFICATIONS.replace('https', 'wss')}/ws-notifications?token=${encodeURIComponent(token)}`;
      console.log('Connecting to WebSocket:', wssUrl);
      const nativeSocket = new WebSocket(wssUrl);
      const client = Stomp.over(nativeSocket);
      client.debug = (str) => console.log('STOMP DEBUG:', str);
      client.heartbeatIncoming = 4000;
      client.heartbeatOutgoing = 4000;
      client.connect({}, (frame) => {
        console.log('WebSocket connected:', frame);
        setIsConnected(true);
        reconnectAttempts.current = 0;
        if (!user?.data?.cic) {
          console.error('Missing user.data.cic');
          setError('Không tìm thấy thông tin người dùng');
          return;
        }
        client.subscribe(`/topic/student/${user.data.cic}`, (message) => {
          const notification = JSON.parse(message.body);
          console.log('Received personal notification:', notification);
          handleAttendanceNotification(notification);
        });
        client.subscribe('/topic/attendance', (message) => {
          const notification = JSON.parse(message.body);
          console.log('Received general notification:', notification);
          if (notification.studentCIC === user.data.cic) {
            handleAttendanceNotification(notification);
          }
        });
        if (user.data.currentClass) {
          client.subscribe(`/topic/class/${user.data.currentClass}`, (message) => {
            const notification = JSON.parse(message.body);
            console.log('Received class notification:', notification);
            if (notification.studentCIC === user.data.cic) {
              handleAttendanceNotification(notification);
            }
          });
        }
        client.send("/app/connect", {}, JSON.stringify({
          studentCIC: user.data.cic,
          timestamp: new Date().toISOString()
        }));
        client.send(`/app/subscribe/student/${user.data.cic}`, {}, JSON.stringify({
          studentCIC: user.data.cic,
          timestamp: new Date().toISOString()
        }));
        setStompClient(client);
      }, (error) => {
        console.error('WebSocket connection error:', error);
        setIsConnected(false);
        handleReconnect();
      });
      nativeSocket.onopen = () => console.log('WebSocket connection opened');
      nativeSocket.onclose = (event) => {
        console.error('WebSocket closed. Code:', event.code, 'Reason:', event.reason);
        setIsConnected(false);
        if (event.code !== 1000) handleReconnect();
      };
      nativeSocket.onerror = (error) => {
        console.error('WebSocket error:', error);
        setIsConnected(false);
        // handleReconnect();
      };
    } catch (error) {
      console.error('Failed to initialize WebSocket:', error);
      setIsConnected(false);
      handleReconnect();
    }
  }, [user, isOpen, handleAttendanceNotification]);

  // Cleanup function
  const disconnectWebSocket = useCallback(() => {
    if (stompClient && stompClient.connected) {
      try {
        // Send disconnect message
        stompClient.send("/app/disconnect", {}, JSON.stringify({
          studentCIC: user.data.cic,
          timestamp: new Date().toISOString()
        }));

        // Disconnect
        stompClient.disconnect(() => {
          console.log('WebSocket disconnected');
          setIsConnected(false);
          setStompClient(null);
        });
      } catch (error) {
        console.error('Error disconnecting WebSocket:', error);
      }
    }
  }, [stompClient, user]);

  // Kiểm tra trạng thái điểm danh
  const checkAttendanceStatus = useCallback(async () => {
    if (!classInfo || !user) return;

    try {
      setAttendanceStatus('loading');
      setLoading(true);

      // Gọi API để kiểm tra trạng thái điểm danh
      const response = await api.get(`/attendances/status?studentCIC=${user.cic}&classCode=${classInfo.classCode}&date=${classInfo.date}`);

      if (response.data && response.data.success) {
        if (response.data.data.hasAttended) {
          setAttendanceStatus('checked');
          setSuccess('Bạn đã điểm danh cho lớp này rồi!');
        } else {
          setAttendanceStatus('not_checked');
        }
      } else {
        setAttendanceStatus('not_checked');
      }
    } catch (err) {
      console.error('Error checking attendance status:', err);
      setAttendanceStatus('not_checked'); // Cho phép tiếp tục nếu không check được
    } finally {
      setLoading(false);
    }
  }, [classInfo, user]);

  // Khởi động camera
  const startCamera = useCallback(async () => {
    if (attendanceStatus === 'checked') {
      return;
    }

    try {
      setError(null);
      setLoading(true);

      // Kiểm tra hỗ trợ getUserMedia
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('Trình duyệt không hỗ trợ camera');
      }

      // Yêu cầu quyền truy cập camera với fallback options
      const constraints = {
        video: {
          facingMode: { ideal: 'environment' }, // Sử dụng ideal thay vì exact
          width: { ideal: 1280, max: 1920 },
          height: { ideal: 720, max: 1080 }
        }
      };

      let stream;
      try {
        stream = await navigator.mediaDevices.getUserMedia(constraints);
      } catch (err) {
        // Fallback với constraints đơn giản hơn
        console.warn('Fallback to basic camera constraints:', err);
        stream = await navigator.mediaDevices.getUserMedia({ video: true });
      }

      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;

        const trackSettings = stream.getVideoTracks()[0]?.getSettings();
        const facing = trackSettings?.facingMode || 'unknown';

        // Nếu là cam trước (user) → lật lại ảnh để không bị ngược
        if (facing === 'user') {
          videoRef.current.style.transform = 'scaleX(-1)';
        } else {
          videoRef.current.style.transform = 'none';
        }

        // Đợi video load
        await new Promise((resolve, reject) => {
          const timeout = setTimeout(() => {
            reject(new Error('Video load timeout'));
          }, 5000);

          videoRef.current.onloadedmetadata = () => {
            clearTimeout(timeout);
            resolve();
          };

          videoRef.current.onerror = () => {
            clearTimeout(timeout);
            reject(new Error('Video load error'));
          };
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

      let errorMessage = 'Không thể khởi động camera';

      if (err.name === 'NotAllowedError') {
        errorMessage = 'Vui lòng cho phép truy cập camera để quét QR code';
      } else if (err.name === 'NotFoundError') {
        errorMessage = 'Không tìm thấy camera trên thiết bị';
      } else if (err.name === 'NotReadableError') {
        errorMessage = 'Camera đang được sử dụng bởi ứng dụng khác';
      } else if (err.name === 'OverconstrainedError') {
        errorMessage = 'Camera không hỗ trợ cấu hình yêu cầu';
      } else {
        errorMessage = errorMessage + ': ' + err.message;
      }

      setError(errorMessage);
    }
  }, [attendanceStatus]);

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
    if (!videoRef.current || !canvasRef.current) {
      console.error('Missing required elements for scanning');
      return;
    }

    const canvas = canvasRef.current;
    const context = canvas.getContext('2d');

    // Fix Canvas warning bằng cách set attribute trực tiếp
    try {
      if (canvas.willReadFrequently !== undefined) {
        canvas.willReadFrequently = true;
      }
    } catch (e) {
      console.warn('willReadFrequently not supported:', e);
    }

    scanIntervalRef.current = setInterval(async () => {
      try {
        const video = videoRef.current;

        if (!video || video.readyState !== video.HAVE_ENOUGH_DATA) return;

        // Tối ưu kích thước canvas
        const videoWidth = video.videoWidth;
        const videoHeight = video.videoHeight;

        if (videoWidth === 0 || videoHeight === 0) return;

        // Chỉ resize canvas khi cần thiết
        if (canvas.width !== videoWidth || canvas.height !== videoHeight) {
          canvas.width = videoWidth;
          canvas.height = videoHeight;
        }

        context.drawImage(video, 0, 0, videoWidth, videoHeight);

        const imageData = context.getImageData(0, 0, videoWidth, videoHeight);

        // Decode QR code với jsQR đã import
        const code = jsQR(imageData.data, imageData.width, imageData.height, {
          inversionAttempts: "dontInvert",
        });

        if (code && code.data) {
          console.log('QR Code detected:', code.data);
          setIsScanning(false);
          clearInterval(scanIntervalRef.current);
          scanIntervalRef.current = null;
          await handleQRDetected(code.data);
        }

      } catch (err) {
        console.error('Error scanning QR:', err);
      }
    }, 150); // 150ms interval để balance performance
  }, []);

  // Hàm lấy vị trí
  const getCurrentLocation = () => {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Trình duyệt không hỗ trợ định vị'));
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          const coords = {
            latitude: parseFloat(position.coords.latitude.toFixed(6)),
            longitude: parseFloat(position.coords.longitude.toFixed(6))
          };
          setLocation(coords);
          resolve(coords);
        },
        (error) => {
          console.warn('Geolocation error:', error);
          reject(error);
        }
      );
    });
  };

  // Xử lý khi phát hiện QR code - SỬA ĐỔI ĐỂ DÙNG WEBSOCKET
  const handleQRDetected = useCallback(async (qrData) => {
    try {
      setLoading(true);
      setError(null);

      console.log('Processing QR data:', qrData);

      // Parse QR data
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

      // Get location
      let coords;
      try {
        coords = await getCurrentLocation();
      } catch (locationErr) {
        throw new Error('Không thể lấy vị trí hiện tại');
      }

      const attendanceData = {
        qrSignature: qrSignature,
        studentCIC: user.data.cic,
        deviceInfo: navigator.userAgent || 'Unknown Device',
        latitude: coords.latitude,
        longitude: coords.longitude,
      };

      console.log('Sending attendance data:', attendanceData);

      // Gửi request và đợi notification qua WebSocket
      const response = await api.post('/attendances', attendanceData);

      if (response.status === 202) {
        // Request được accept, đợi notification qua WebSocket
        console.log('Attendance request accepted, waiting for WebSocket notification...');
        // setLoading sẽ được set false khi nhận notification
      } else {
        throw new Error('Unexpected response status');
      }

    } catch (err) {
      console.error('Error processing QR:', err);
      setError(err.message || 'Có lỗi xảy ra khi điểm danh');
      setLoading(false);

      // Retry after 3 seconds
      setTimeout(() => {
        if (isOpen && cameraPermission === 'granted' && attendanceStatus !== 'checked') {
          setError(null);
          setIsScanning(true);
          startScanning();
        }
      }, 3000);
    }
  }, [user, isOpen, cameraPermission, attendanceStatus, startScanning]);

  // Effect để connect/disconnect WebSocket
  useEffect(() => {
    if (isOpen && attendanceStatus === 'not_checked') {
      connectWebSocket();
    }

    return () => {
      if (!isOpen) {
        disconnectWebSocket();
      }
    };
  }, [isOpen, attendanceStatus, connectWebSocket, disconnectWebSocket]);

  // Kiểm tra trạng thái điểm danh khi mở modal
  useEffect(() => {
    if (isOpen) {
      checkAttendanceStatus();
    } else {
      stopCamera();
      disconnectWebSocket();
      // Reset states when closing
      setAttendanceStatus(null);
      setSuccess(null);
      setError(null);
    }

    return () => {
      if (!isOpen) {
        stopCamera();
        disconnectWebSocket();
      }
    };
  }, [isOpen, checkAttendanceStatus, stopCamera, disconnectWebSocket]);

  // Khởi động camera sau khi check attendance status
  useEffect(() => {
    if (isOpen && attendanceStatus === 'not_checked') {
      startCamera();
    } else if (attendanceStatus === 'checked') {
      // Không khởi động camera nếu đã điểm danh
      stopCamera();
    }
  }, [isOpen, attendanceStatus, startCamera, stopCamera]);

  // Cleanup khi component unmount
  useEffect(() => {
    return () => {
      stopCamera();
      disconnectWebSocket();
    };
  }, [stopCamera, disconnectWebSocket]);

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

            {loading && attendanceStatus !== 'loading' && (
              <div className="camera-loading">
                <div className="spinner"></div>
                <p>Đang xử lý điểm danh...</p>
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
              onClick={onClose}
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