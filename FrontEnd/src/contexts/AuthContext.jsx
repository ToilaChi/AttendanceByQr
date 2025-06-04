import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
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
  const effectRan = useRef(false);

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        console.log('AuthContext: Fetching user data after token validation/refresh.');
        const rawUserData = await authService.getCurrentUser();
        const userDataToSet = rawUserData?.data || rawUserData;
        setUser(userDataToSet);
      } catch (fetchError) {
        console.error('AuthContext: Failed to fetch user data:', fetchError);
        throw fetchError;
      }
    };

    const initializeAuth = async () => {
      setLoading(true);

      try {
        const accessToken = localStorage.getItem('accessToken');
        const refreshToken = localStorage.getItem('refreshToken');
        const storedUserJson = localStorage.getItem('user');

        if (accessToken && authService.isTokenValid()) {
          if (storedUserJson) {
            try {
              setUser(JSON.parse(storedUserJson));
            } catch (parseError) {
              console.error('AuthContext: Error parsing stored user, fetching from API.', parseError);
              localStorage.removeItem('user');
              await fetchUserData();
            }
          } else {
            await fetchUserData();
          }
        } else if (refreshToken) {
          try {
            await authService.refreshToken();
            await fetchUserData();
          } catch (refreshError) {
            console.error('AuthContext: Refresh token failed.', refreshError);
            await authService.logout();
            setUser(null);
          }
        } else {
          console.log('AuthContext: No tokens found. User is not authenticated.');
          setUser(null);
        }
      } catch (err) {
        console.error('AuthContext: Critical error during initialization:', err);
        await authService.logout();
        setUser(null);
      } finally {
        setLoading(false);
        console.log('AuthContext: Auth initialization process complete.');
      }
    };

    // Chỉ chạy effect một lần trong môi trường development, giống như môi trường production
    // Để tránh gọi lại khi hot reload xảy ra
    if (process.env.NODE_ENV === 'development') {
      if (effectRan.current === true) {
        initializeAuth();
      }
      return () => {
        effectRan.current = true;
      };
    } else {
      initializeAuth();
    }

    // Dọn dẹp khi component unmount
    return () => {
    };
  }, []);

  const login = async (credentials) => {
    setLoading(true);
    setError(null);
    try {
      const response = await authService.login(credentials);
      setUser(response.user);
      return response;
    } catch (err) {
      const errorMessage = err.message || 'Đăng nhập thất bại';
      console.error('AuthContext: Login failed:', errorMessage);
      setError(errorMessage);
      setUser(null);
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    setLoading(true);
    try {
      await authService.logout();
    } catch (err) {
      console.error('AuthContext: Error during API logout (non-critical):', err);
    } finally {
      setUser(null);
      setError(null);
      setLoading(false);
    }
  };

  const isAuthenticated = !!user && !!localStorage.getItem('accessToken') && authService.isTokenValid();

  const value = {
    user,
    error,
    loading,
    isAuthenticated,
    login,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};