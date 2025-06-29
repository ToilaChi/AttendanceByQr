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
  const currentStreamRef = useRef(null);
  const isVideoReadyRef = useRef(false);

  // Simplified states - giữ nguyên các state cần thiết cho UI
  const [isScanning, setIsScanning] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [cameraPermission, setCameraPermission] = useState('prompt');
  const [attendanceStatus, setAttendanceStatus] = useState('not_checked');
  const [isConnected, setIsConnected] = useState(false);
  const [currentLocation, setCurrentLocation] = useState(null);
  const [currentStep, setCurrentStep] = useState('qr');
  const [correlationId, setCorrelationId] = useState(null);
  const [faceRegistrationStatus, setFaceRegistrationStatus] = useState('unknown');
  const [cameraType, setCameraType] = useState('environment');
  const [tempSuccess, setTempSuccess] = useState('');
  const [showTempSuccess, setShowTempSuccess] = useState(false);

  // WebSocket setup (for notifications only)
  const token = localStorage.getItem('accessToken');
  const WS_URL = `${API_NOTIFICATIONS.replace('https', 'wss')}/ws-notifications?token=${encodeURIComponent(token)}`;

  // Enhanced stop camera with proper cleanup
  const stopCamera = useCallback(() => {
    console.log('Stopping camera...');

    // Stop video scanning interval
    if (scanningIntervalRef.current) {
      clearInterval(scanningIntervalRef.current);
      scanningIntervalRef.current = null;
    }

    // Stop all media tracks properly
    if (currentStreamRef.current) {
      console.log('Stopping current stream tracks...');
      currentStreamRef.current.getTracks().forEach(track => {
        console.log(`Stopping track: ${track.kind}, state: ${track.readyState}`);
        if (track.readyState === 'live') {
          track.stop();
        }
      });
      currentStreamRef.current = null;
    }

    // Clean up video element
    if (videoRef.current) {
      videoRef.current.pause();
      videoRef.current.srcObject = null;
      videoRef.current.load();
    }

    isVideoReadyRef.current = false;
    setIsScanning(false);
  }, []);

  // Enhanced wait for video ready with better error handling
  const waitForVideoReady = useCallback((timeoutMs = 10000) => {
    return new Promise((resolve, reject) => {
      const startTime = Date.now();

      const checkReady = () => {
        if (!videoRef.current || !canvasRef.current) {
          if (Date.now() - startTime > timeoutMs) {
            reject(new Error('Timeout: Video refs not available'));
            return;
          }
          setTimeout(checkReady, 100);
          return;
        }

        const video = videoRef.current;
        const isReady = video.readyState >= 3 && // HAVE_FUTURE_DATA or better
          video.videoWidth > 0 &&
          video.videoHeight > 0 &&
          !video.paused &&
          !video.ended;

        if (isReady) {
          console.log('Video is ready:', {
            readyState: video.readyState,
            width: video.videoWidth,
            height: video.videoHeight,
            paused: video.paused,
            ended: video.ended
          });
          isVideoReadyRef.current = true;
          resolve();
        } else {
          if (Date.now() - startTime > timeoutMs) {
            reject(new Error(`Timeout waiting for video. Current state: readyState=${video.readyState}, width=${video.videoWidth}, height=${video.videoHeight}`));
            return;
          }
          setTimeout(checkReady, 100);
        }
      };

      checkReady();
    });
  }, []);

  // Enhanced start camera with better error handling and stream management
  const startCamera = useCallback(async (facingMode = 'environment') => {
    try {
      setError('');
      setLoading(true);
      setCameraType(facingMode);

      console.log(`Starting camera with facingMode: ${facingMode}`);

      // Stop existing camera first
      stopCamera();

      // Wait a bit for cleanup to complete
      await new Promise(resolve => setTimeout(resolve, 300));

      // Check attendance status first (only for initial QR scan)
      if (facingMode === 'environment') {
        const alreadyCompleted = await checkAttendanceStatus();
        if (alreadyCompleted) {
          setLoading(false);
          return;
        }
      }

      // Get location for QR scanning
      if (facingMode === 'environment') {
        try {
          await getCurrentLocation();
        } catch (locationError) {
          console.warn('Could not get location:', locationError);
          setCurrentLocation({ latitude: '0', longitude: '0' });
        }
      }

      // Request camera with constraints
      const constraints = {
        video: {
          facingMode: facingMode,
          width: { ideal: facingMode === 'environment' ? 1280 : 640, max: 1920 },
          height: { ideal: facingMode === 'environment' ? 720 : 480, max: 1080 }
        }
      };

      console.log('Requesting camera with constraints:', constraints);

      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      currentStreamRef.current = stream;

      if (!videoRef.current) {
        throw new Error('Video element not available');
      }

      // Set up video element
      videoRef.current.srcObject = stream;

      // Wait for metadata to load
      await new Promise((resolve, reject) => {
        const video = videoRef.current;
        const timeout = setTimeout(() => {
          reject(new Error('Timeout loading video metadata'));
        }, 5000);

        const handleLoadedMetadata = () => {
          clearTimeout(timeout);
          video.removeEventListener('loadedmetadata', handleLoadedMetadata);
          resolve();
        };

        video.addEventListener('loadedmetadata', handleLoadedMetadata);

        if (video.readyState >= 1) {
          handleLoadedMetadata();
        }
      });

      // Play video
      await videoRef.current.play();

      // Wait for video to be fully ready
      await waitForVideoReady();

      console.log('Camera started successfully');
      setCameraPermission('granted');
      setIsScanning(true);
      setLoading(false);

      // Start appropriate scanning
      if (currentStep === 'qr') {
        startQRScanning();
      }

    } catch (err) {
      console.error('Error starting camera:', err);
      stopCamera();

      if (err.name === 'NotAllowedError') {
        setCameraPermission('denied');
        setError('Cần quyền truy cập camera để quét QR code');
      } else if (err.name === 'NotFoundError') {
        setError('Không tìm thấy camera');
      } else if (err.name === 'OverconstrainedError') {
        setError('Camera không hỗ trợ cấu hình yêu cầu');
      } else {
        setError('Không thể khởi động camera: ' + err.message);
      }
      setLoading(false);
      setIsScanning(false);
    }
  }, [currentStep, stopCamera, waitForVideoReady]);

  // Enhanced camera switching with proper async handling
  const switchToFrontCamera = useCallback(async () => {
    try {
      console.log('Switching to front camera...');
      setCurrentStep('face-check');

      await startCamera('user');
      return true;
    } catch (err) {
      console.error('Error switching to front camera:', err);
      setError('Không thể chuyển sang camera trước: ' + err.message);
      return false;
    }
  }, [startCamera]);

  const switchToBackCamera = useCallback(async () => {
    try {
      console.log('Switching to back camera...');
      setCurrentStep('qr');

      await startCamera('environment');
      return true;
    } catch (err) {
      console.error('Error switching to back camera:', err);
      setError('Không thể chuyển sang camera sau: ' + err.message);
      return false;
    }
  }, [startCamera]);

  // Enhanced image capture with better validation
  const captureImageFromVideo = useCallback(async () => {
    return new Promise((resolve, reject) => {
      // Comprehensive validation
      if (!videoRef.current || !canvasRef.current) {
        reject(new Error('Video hoặc canvas element không khả dụng'));
        return;
      }

      if (!isVideoReadyRef.current) {
        reject(new Error('Video chưa sẵn sàng để capture'));
        return;
      }

      const video = videoRef.current;
      const canvas = canvasRef.current;

      // Additional video validation
      if (video.readyState < 3) {
        reject(new Error(`Video chưa sẵn sàng - readyState: ${video.readyState}`));
        return;
      }

      if (video.videoWidth === 0 || video.videoHeight === 0) {
        reject(new Error(`Video không có kích thước hợp lệ: ${video.videoWidth}x${video.videoHeight}`));
        return;
      }

      if (video.paused || video.ended) {
        reject(new Error('Video đã bị dừng hoặc kết thúc'));
        return;
      }

      try {
        const context = canvas.getContext('2d');
        if (!context) {
          reject(new Error('Không thể lấy context của canvas'));
          return;
        }

        // Set canvas dimensions
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        console.log(`Capturing image: ${canvas.width}x${canvas.height}`);

        // Draw video frame to canvas
        context.drawImage(video, 0, 0, canvas.width, canvas.height);

        // Convert to blob
        canvas.toBlob((blob) => {
          if (blob && blob.size > 0) {
            console.log(`Image captured successfully: ${blob.size} bytes, type: ${blob.type}`);
            resolve(blob);
          } else {
            reject(new Error('Không thể tạo ảnh từ video - blob empty'));
          }
        }, 'image/jpeg', 0.8);

      } catch (error) {
        console.error('Error in captureImageFromVideo:', error);
        reject(new Error('Lỗi khi chụp ảnh: ' + error.message));
      }
    });
  }, []);

  // Face detection functions
  const checkFaceRegistration = useCallback(async (studentCIC) => {
    try {
      const response = await api.get(`/face/check/${studentCIC}`);
      return response.data.registered;
    } catch (error) {
      console.error('Error checking face registration:', error);
      throw new Error('Không thể kiểm tra trạng thái đăng ký khuôn mặt');
    }
  }, []);

  const registerFace = useCallback(async (studentCIC, imageBlob, correlationId) => {
    try {
      const formData = new FormData();
      formData.append('student_cic', studentCIC);
      formData.append('file', imageBlob, 'face.jpg');
      formData.append('correlation_id', correlationId);

      const response = await api.post('/face/register', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      return response.data;
    } catch (err) {
      console.error('Error registering face:', err);
      throw new Error(err.response?.data?.detail || 'Lỗi đăng ký khuôn mặt');
    }
  }, []);

  const verifyFace = useCallback(async (studentCIC, imageBlob, correlationId) => {
    try {
      const formData = new FormData();
      formData.append('student_cic', studentCIC);
      formData.append('file', imageBlob, 'face.jpg');
      formData.append('correlation_id', correlationId);

      const response = await api.post('/face/verify', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      return response.data;
    } catch (err) {
      console.error('Error verifying face:', err);
      throw new Error(err.response?.data?.detail || 'Lỗi xác thực khuôn mặt');
    }
  }, []);

  // Enhanced face detection with better error handling and timing
  const handleFaceDetection = useCallback(async (correlationIdParam = null) => {
    const activeCorrelationId = correlationIdParam || correlationId;

    if (!user?.data?.cic || !activeCorrelationId) {
      setError('Thiếu thông tin cần thiết cho xác thực khuôn mặt');
      return;
    }

    try {
      setLoading(true);
      setError('');

      console.log('Starting face detection process...');

      // Wait a bit more for camera to fully stabilize
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Ensure video is still ready
      if (!isVideoReadyRef.current) {
        throw new Error('Video không còn sẵn sàng');
      }

      // Step 1: Check if face is registered
      setCurrentStep('face-check');
      const isRegistered = await checkFaceRegistration(user.data.cic);
      setFaceRegistrationStatus(isRegistered ? 'registered' : 'not_registered');

      // Step 2: Capture image with retry logic
      let imageBlob;
      let captureAttempts = 0;
      const maxAttempts = 3;

      while (captureAttempts < maxAttempts) {
        try {
          imageBlob = await captureImageFromVideo();
          break;
        } catch (captureError) {
          captureAttempts++;
          console.warn(`Capture attempt ${captureAttempts} failed:`, captureError.message);

          if (captureAttempts >= maxAttempts) {
            throw new Error(`Không thể chụp ảnh sau ${maxAttempts} lần thử: ${captureError.message}`);
          }

          // Wait before retry
          await new Promise(resolve => setTimeout(resolve, 500));
        }
      }

      // Step 3: Register or Verify
      let result;
      if (isRegistered) {
        setCurrentStep('face-verify');
        result = await verifyFace(user.data.cic, imageBlob, activeCorrelationId);
      } else {
        setCurrentStep('face-register');
        result = await registerFace(user.data.cic, imageBlob, activeCorrelationId);
      }

      // Step 4: Handle result
      if (result.success || result.message?.includes('thành công')) {
        setCurrentStep('completed');
        setSuccess(result.message || 'Điểm danh hoàn tất thành công!');
        setAttendanceStatus('checked');
        setTimeout(() => {
          onClose();
        }, 3000);
      } else {
        throw new Error(result.message || 'Xác thực khuôn mặt thất bại');
      }

    } catch (err) {
      console.error('Face detection error:', err);
      setError(err.message);
      setCurrentStep('qr');
      setAttendanceStatus('not_checked');

      // Restart QR scanning
      setTimeout(async () => {
        const switched = await switchToBackCamera();
        if (switched) {
          // Wait for camera to be ready before starting scanning
          setTimeout(() => {
            if (isVideoReadyRef.current) {
              startQRScanning();
            }
          }, 1000);
        }
      }, 2000);
    } finally {
      setLoading(false);
    }
  }, [user?.data?.cic, correlationId, checkFaceRegistration, captureImageFromVideo, registerFace, verifyFace, onClose, switchToBackCamera]);

  // Enhanced QR processing with better camera switching
  const processQRScanResponse = useCallback(async (response) => {
    if (response.status === 200 && response.data) {
      const { correlation_id, message } = response.data;

      if (correlation_id) {
        setCorrelationId(correlation_id);
        setSuccess('QR Code hợp lệ! Đang chuyển sang xác thực khuôn mặt...');

        // Switch to front camera and wait for it to be ready
        setTimeout(async () => {
          try {
            const switched = await switchToFrontCamera();
            if (switched) {
              // Wait longer for camera to fully stabilize
              setTimeout(async () => {
                if (isVideoReadyRef.current) {
                  await handleFaceDetection(correlation_id);
                } else {
                  // Retry after a bit more time
                  setTimeout(async () => {
                    if (isVideoReadyRef.current) {
                      await handleFaceDetection(correlation_id);
                    } else {
                      setError('Camera chưa sẵn sàng cho face detection');
                    }
                  }, 2000);
                }
              }, 3000); // Increased delay
            }
          } catch (err) {
            console.error('Error in camera switching:', err);
            setError('Lỗi khi chuyển camera: ' + err.message);
          }
        }, 1000);
      } else {
        throw new Error(message || 'QR scan thành công nhưng thiếu correlation_id');
      }
    } else {
      throw new Error('QR scan không thành công');
    }
  }, [switchToFrontCamera, handleFaceDetection]);

  // Initialize WebSocket for notifications only
  const initWebSocket = useCallback(() => {
    if (!user?.data?.cic) return;

    try {
      const stompClient = new Client({
        brokerURL: WS_URL,
        connectHeaders: {},
        debug: function (str) {
          console.log('STOMP: ' + str);
        },
        reconnectDelay: 2000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        maxReconnectAttempts: 5,
      });

      stompClient.onConnect = (frame) => {
        console.log('Connected: ' + frame);
        setIsConnected(true);

        stompClient.subscribe(`/topic/student/${user.data.cic}`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('Received notification:', data);
            handleWebSocketNotification(data);
          } catch (err) {
            console.error('Error parsing message:', err);
          }
        });
      };

      stompClient.onDisconnect = () => {
        console.log('Disconnected');
        setIsConnected(false);
      };

      stompClient.onWebSocketError = (error) => {
        console.error('WebSocket error:', error);
        setIsConnected(false);
      };

      stompClient.activate();
      wsRef.current = stompClient;
    } catch (err) {
      console.error('Error initializing WebSocket:', err);
      setIsConnected(false);
    }
  }, [user?.data?.cic, WS_URL]);

  // Handle WebSocket notifications (for UI updates only)
  const handleWebSocketNotification = useCallback((data) => {
    if (data.type === 'ATTENDANCE_FINAL_SUCCESS') {
      setSuccess('Điểm danh hoàn tất thành công!');
      setAttendanceStatus('checked');
      setTimeout(() => onClose(), 2000);
    } else if (data.type === 'ATTENDANCE_FINAL_FAILED') {
      setError(data.message || 'Điểm danh cuối cùng thất bại');
    }
  }, [onClose]);

  // Get current location
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

  // Check attendance status
  const checkAttendanceStatus = useCallback(async () => {
    if (!classInfo || !user) return false;

    try {
      setAttendanceStatus('loading');
      const response = await api.get(`/attendances/status?studentCIC=${user.data.cic}&scheduleId=${classInfo.scheduleId}&date=${classInfo.date}`);

      if (response.data && response.data.message === 'Đã điểm danh') {
        setAttendanceStatus('checked');
        setSuccess('Bạn đã điểm danh rồi!');
        return true;
      }
      setAttendanceStatus('not_checked');
      return false;
    } catch (err) {
      console.error('Error checking attendance status:', err);
      setAttendanceStatus('not_checked');
      return false;
    }
  }, [classInfo, user]);

  // Validate QR data
  const validateQRData = useCallback((qrData) => {
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

    return qrSignature;
  }, []);

  // Handle QR code detection - xử lý response trực tiếp
  const handleQRCodeDetected = useCallback(async (qrData) => {
    if (loading || attendanceStatus === 'checked' || currentStep !== 'qr') {
      return;
    }

    try {
      setLoading(true);
      setError('');
      setSuccess('');

      // Stop scanning temporarily
      if (scanningIntervalRef.current) {
        clearInterval(scanningIntervalRef.current);
        scanningIntervalRef.current = null;
      }

      // Validate QR data
      const qrSignature = validateQRData(qrData);

      // Get current location
      let coords = currentLocation;
      if (!coords) {
        try {
          coords = await getCurrentLocation();
        } catch (locationError) {
          console.warn('Using default location:', locationError);
          coords = { latitude: '0', longitude: '0' };
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

      console.log('Submitting QR attendance:', attendanceData);

      // Submit QR attendance and process response directly
      const response = await api.post('/attendances/qr-scan', attendanceData);
      await processQRScanResponse(response);

    } catch (err) {
      console.error('Error in QR detection:', err);
      setError(err.response?.data?.message || err.message || 'Có lỗi xảy ra khi quét QR');

      // Restart scanning after error
      setTimeout(() => {
        if (isScanning && currentStep === 'qr') {
          startQRScanning();
        }
      }, 2000);
    } finally {
      setLoading(false);
    }
  }, [loading, attendanceStatus, currentStep, currentLocation, user?.data?.cic, validateQRData, getCurrentLocation, processQRScanResponse, isScanning]);

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
    }, 300);
  }, [handleQRCodeDetected]);

  // Enhanced cleanup
  const cleanup = useCallback(() => {
    console.log('Cleaning up QRScanner...');

    stopCamera();

    if (wsRef.current) {
      wsRef.current.deactivate();
      wsRef.current = null;
    }

    if (locationWatchRef.current) {
      navigator.geolocation.clearWatch(locationWatchRef.current);
      locationWatchRef.current = null;
    }

    // Reset all states
    setCurrentStep('qr');
    setCorrelationId(null);
    setFaceRegistrationStatus('unknown');
    setAttendanceStatus('not_checked');
    setError('');
    setSuccess('');
    isVideoReadyRef.current = false;
  }, [stopCamera]);

  // Enhanced useEffect with proper dependency management
  useEffect(() => {
    if (isOpen) {
      console.log('QRScanner opened, initializing...');

      // Reset states
      setAttendanceStatus('not_checked');
      setError('');
      setSuccess('');
      setLoading(false);
      setCurrentStep('qr');

      // Initialize WebSocket
      initWebSocket();

      // Start camera with delay
      const initTimer = setTimeout(() => {
        startCamera('environment').catch(err => {
          console.error('Failed to start camera on init:', err);
        });
      }, 200);

      return () => {
        clearTimeout(initTimer);
        cleanup();
      };
    } else {
      cleanup();
    }
  }, [isOpen]);

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
                {/* <div className="success-icon">✓</div> */}
                <p>{success}</p>
              </div>
            )}

            {attendanceStatus === 'checked' && !success && (
              <div className="already-attended">
                {/* <div className="success-icon">✓</div> */}
                <p>Bạn đã điểm danh cho lớp này rồi!</p>
                <small>Không thể điểm danh lại</small>
              </div>
            )}

            {(attendanceStatus === 'not_checked' || currentStep !== 'qr') && (
              <>
                <video
                  ref={videoRef}
                  className={`camera-video ${cameraType === 'user' ? 'front-camera' : ''} ${isScanning ? 'active' : ''}`}
                  autoPlay
                  playsInline
                  muted
                />

                <canvas
                  ref={canvasRef}
                  className={`camera-canvas ${cameraType === 'user' ? 'front-camera' : ''}`}
                  style={{ display: 'none' }}
                />

                {/* QR Scanning Overlay */}
                {isScanning && currentStep === 'qr' && (
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

                {/* Face Detection Overlay */}
                {isScanning && currentStep !== 'qr' && currentStep !== 'completed' && (
                  <div className="scan-overlay">
                    <div className="face-frame">
                      <div className="face-circle"></div>
                    </div>
                    <p className="scan-instruction">
                      {currentStep === 'face-check' && 'Đang kiểm tra trạng thái đăng ký...'}
                      {currentStep === 'face-register' && 'Đăng ký khuôn mặt - Nhìn thẳng vào camera'}
                      {currentStep === 'face-verify' && 'Xác thực khuôn mặt - Nhìn thẳng vào camera'}
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