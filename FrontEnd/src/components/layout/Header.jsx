import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import '../../styles/Header.css'; 

const Header = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [showUserMenu, setShowUserMenu] = useState(false);

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/login');
    } catch (error) {
      console.error('Logout error:', error);
      navigate('/login');
    }
  };

  const toggleUserMenu = () => setShowUserMenu(!showUserMenu);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (showUserMenu && !event.target.closest('.user-menu-container')) {
        setShowUserMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showUserMenu]);

  return (
    <header className="dashboard-header">
      <div className="header-content">
        <div className="header-left">
          <a href="/dashboard" className="header-logo-link">
            <img
              src="https://portal.ut.edu.vn/images/sv_logo_dashboard.png"
              alt="UTH Logo"
              className="header-logo"
            />
          </a>
        </div>

        <div className="header-right">
          <div className="user-menu-container">
            <button className="user-button" onClick={toggleUserMenu}>
              <img
                src="https://i.pinimg.com/736x/36/53/e5/3653e53d2bdf1402859c6b95dbdae098.jpg"
                alt="User Avatar"
                className="user-avatar"
              />
              <span className="user-name">{user.data?.fullName}</span>
              <svg className="dropdown-icon" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
              </svg>
            </button>

            {showUserMenu && (
              <div className="user-dropdown">
                <a href="#" className="dropdown-item">Thông tin cá nhân</a>
                <a href="#" className="dropdown-item">Cài đặt</a>
                <hr className="dropdown-divider" />
                <button onClick={handleLogout} className="dropdown-item logout-item">
                  Đăng xuất
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
