// src/pages/Dashboard.jsx
import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import '../styles/Dashboard.css';
import Header from '../components/layout/Header'; 

const Dashboard = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [showUserMenu, setShowUserMenu] = useState(false);

  const [userData, setUserData] = useState({
    mssv: '',
    canCuocCongDan: '',
    hoTen: '',
    soDienThoai: '',
    email: '',
    gioiTinh: '',
    lopChinhQuy: '',
    ngaySinh: '',
    khoa: '',
    nganh: '',
    role: ''
  });

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      const user = JSON.parse(storedUser).data || JSON.parse(storedUser);
      const role = user.role;

      if (role === 'STUDENT') {
        setUserData({
          mssv: user.studentCode || '',
          canCuocCongDan: user.cic || '',
          hoTen: user.fullName || '',
          soDienThoai: user.phone || '',
          email: user.email || '',
          ngaySinh: user.dateOfBirth ? new Date(user.dateOfBirth).toLocaleDateString('vi-VN') : '',
          gioiTinh: user.gender || '',
          lopChinhQuy: user.regularClassCode || '',
          khoa: 'Công nghệ thông tin',
          nganh: 'Công nghệ thông tin',
          role
        });
      } else if (role === 'TEACHER') {
        setUserData({
          mssv: user.teacherCode || '',
          canCuocCongDan: user.cic || '',
          hoTen: user.fullName || '',
          soDienThoai: user.phone || '',
          email: user.email || '',
          ngaySinh: user.dateOfBirth ? new Date(user.dateOfBirth).toLocaleDateString('vi-VN') : '',
          gioiTinh: user.gender || '',
          lopChinhQuy: '', 
          khoa: 'Công nghệ thông tin',
          nganh: '',
          role
        });
      }
    }
  }, []);

  return (
    <div className="dashboard-container">
      {/* Header */}
      <Header />

      {/* Main Content */}
      <main className="dashboard-main">
        <div className="dashboard-content">
          {/* Student Info Card */}
          <div className="info-card">
            <div className="card-header">
              <h2>Thông tin sinh viên</h2>
            </div>

            <div className="student-info">
              <div className="student-avatar">
                <img
                  src="https://i.pinimg.com/736x/36/53/e5/3653e53d2bdf1402859c6b95dbdae098.jpg"
                  alt="Student Photo"
                  className="avatar-image"
                />
              </div>

              <div className="student-details">
                <div className="details-grid">
                  <div className="detail-item">
                    <label>{userData.role === 'TEACHER' ? 'MSGV:' : 'MSSV:'}</label>
                    <span>{userData.mssv}</span>
                  </div>
                  <div className="detail-item">
                    <label>CCCD:</label>
                    <span>{userData.canCuocCongDan}</span>
                  </div>
                  <div className="detail-item">
                    <label>Họ tên:</label>
                    <span>{userData.hoTen}</span>
                  </div>
                  <div className="detail-item">
                    <label>Email:</label>
                    <span>{userData.email}</span>
                  </div>
                  <div className="detail-item">
                    <label>Số điện thoại:</label>
                    <span>{userData.soDienThoai}</span>
                  </div>
                  {userData.role === 'STUDENT' && (
                    <>
                      <div className="detail-item">
                        <label>Lớp chính quy:</label>
                        <span>{userData.lopChinhQuy}</span>
                      </div>
                      <div className="detail-item">
                        <label>Ngành:</label>
                        <span>{userData.nganh}</span>
                      </div>
                    </>
                  )}
                  <div className="detail-item">
                    <label>Khoa:</label>
                    <span>{userData.khoa}</span>
                  </div>
                  <div className="detail-item">
                    <label>Ngày sinh:</label>
                    <span>{userData.ngaySinh}</span>
                  </div>
                  <div className="detail-item">
                    <label>Giới tính:</label>
                    <span>{userData.gioiTinh}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Dashboard Cards */}
          <div className="dashboard-cards">
            <div className="dashboard-card schedule-card">
              <div className="card-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7z" />
                </svg>
              </div>
              <div className="card-content">
                {userData.role === 'STUDENT' ? <h3>Lịch học trong tuần</h3> : <h3>Lịch dạy trong tuần</h3>}
                <button
                  className="card-button"
                  onClick={() => navigate('/schedule')}
                >
                  Xem chi tiết
                </button>

              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;