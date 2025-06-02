import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const initializeAuth = async () => {
      setLoading(true);

      try {
        const accessToken = localStorage.getItem('accessToken');
        const refreshToken = localStorage.getItem('refreshToken');
        const storedUser = localStorage.getItem('user');

        if (refreshToken) {
          // Nếu có accessToken và còn hạn → dùng
          if (accessToken && authService.isTokenValid()) {
            if (storedUser) {
              try {
                const userData = JSON.parse(storedUser);
                setUser(userData);
              } catch (parseError) {
                console.error('AuthContext: Error parsing stored user data:', parseError);
                localStorage.removeItem('user');
                await fetchUserData();
              }
            } else {
              console.log('AuthContext: No stored user, fetching from API');
              await fetchUserData();
            }
          } else {
            // Không có accessToken hoặc hết hạn → cố gắng refresh
            console.log('AuthContext: Access token missing/expired, attempting refresh...');
            try {
              await authService.refreshToken();
              await fetchUserData();
            } catch (refreshError) {
              console.log('AuthContext: Refresh failed, clearing auth data');
              await logout();
            }
          }
        } else {
          console.log('AuthContext: No refresh token available. User not authenticated.');
        }
      } catch (err) {
        console.error('AuthContext: Initialize error:', err);
        await logout();
      } finally {
        setLoading(false);
        console.log('AuthContext: Auth initialization complete');
      }
    };

    const fetchUserData = async () => {
      try {
        const raw = await authService.getCurrentUser();
        const userData = raw?.data || raw; 
        setUser(userData);
      } catch (fetchError) {
        console.error('AuthContext: Failed to fetch user data:', fetchError);
        await logout();
      }
    };

    initializeAuth();
  }, []);

  const login = async (credentials) => {
    try {
      setError(null);
      setLoading(true);

      const response = await authService.login(credentials);

      setUser(response.user);
      return response;
    } catch (err) {
      const errorMessage = err.message || 'Đăng nhập thất bại';
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      setLoading(true);
      await authService.logout();
    } catch (err) {
      console.error('AuthContext: Logout error:', err);
    } finally {
      setUser(null);
      setError(null);
      setLoading(false);
    }
  };

  const value = {
    user,
    error,
    loading,
    login,
    logout,
    isAuthenticated: !!user && !!localStorage.getItem('accessToken')
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};