import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import '../styles/LoginPage.css';

const LoginPage = () => {
  const [formData, setFormData] = useState({
    cic: '',
    password: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { login, isAuthenticated, user, loading: authLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Redirect nếu đã authenticated
  useEffect(() => {
    // Chỉ redirect khi không loading và đã authenticated
    if (!authLoading && isAuthenticated && user) {
      const from = location.state?.from?.pathname || '/dashboard';
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, user, authLoading, navigate, location]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    // Clear error when user starts typing
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setLoading(true);

    try {
      await login({
        cic: formData.cic.trim(),
        password: formData.password
      });
    } catch (err) {
      setError(err.message || 'Đã xảy ra lỗi');
    } finally {
      setLoading(false);
    }
  };

  // Show loading screen while checking auth status
  if (authLoading) {
    return (
      <div className="login-container">
        <div className="login-background">
          <div className="login-card">
            <div className="login-header">
              <img
                src="https://portal.ut.edu.vn/images/sv_logo_dashboard.png"
                alt="UTH Logo"
                className="login-logo"
              />
              <h1 className="login-title">ĐANG KIỂM TRA...</h1>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="login-container">
      <div className="login-background">
        <div className="login-card">
          <div className="login-header">
            <img
              src="https://portal.ut.edu.vn/images/sv_logo_dashboard.png"
              alt="UTH Logo"
              className="login-logo"
            />
            <h1 className="login-title">ĐĂNG NHẬP HỆ THỐNG</h1>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            {error && (
              <div className="error-message" role="alert">
                {error}
              </div>
            )}

            <div className="form-group">
              <input
                type="text"
                name="cic"
                placeholder="Tài khoản đăng nhập"
                value={formData.cic}
                onChange={handleInputChange}
                className="form-input"
                disabled={loading}
                autoComplete="cic"
                required
                maxLength={12}
                pattern="[0-9]{12}"
                title="CCCD phải là dãy 12 chữ số"
              />
            </div>

            <div className="form-group password-group">
              <input
                type={showPassword ? "text" : "password"}
                name="password"
                placeholder="Mật khẩu"
                value={formData.password}
                onChange={handleInputChange}
                className="form-input"
                disabled={loading}
                autoComplete="current-password"
                required
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                disabled={loading}
                aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              >
                {showPassword ? '👁️' : '👁️‍🗨️'}
              </button>
            </div>

            <button
              type="submit"
              className={`login-button ${loading ? 'loading' : ''}`}
              disabled={loading}
            >
              {loading ? 'ĐANG ĐĂNG NHẬP...' : 'ĐĂNG NHẬP'}
            </button>

            <div className="login-footer">
              <a href="#" className="forgot-password">
                QUÊN MẬT KHẨU?
              </a>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;