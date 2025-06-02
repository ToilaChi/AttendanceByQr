import api from '../api/api';

export const authService = {
  // Đăng nhập
  login: async (credentials) => {
    try {
      const response = await api.post('/auth/login', credentials);
      // Truy cập cấu trúc response
      const responseData = response.data.data;

      if (!responseData) {
        const serverMessage = response.data.message || 'Đăng nhập thất bại';
        console.warn('AuthService: Server returned null data:', serverMessage);
        throw new Error(serverMessage);
      }

      const accessToken = responseData.accessToken;
      const refreshToken = responseData.refreshToken;
      const account = responseData.account;

      if (!accessToken || !refreshToken) {
        throw new Error('Server response missing required authentication tokens');
      }

      // Lưu tokens
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      // Lấy thông tin user chi tiết từ /users/cic
      let userData = null;
      try {
        const userResponse = await api.get('/users/cic');
        userData = userResponse.data;
      } catch (userError) {
        // Fallback với thông tin từ account
        userData = {
          id: account.id,
          username: account.username,
          role: account.role,
          cic: credentials.cic
        };
      }

      // Lưu user data
      localStorage.setItem('user', JSON.stringify(userData));

      const result = {
        user: userData,
        accessToken,
        refreshToken,
        message: response.data.message
      };

      return result;
    } catch (error) {
      // Clear any partial data
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');

      // Xử lý error message từ server
      let errorMessage = 'Đăng nhập thất bại';
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }

      throw new Error(errorMessage);
    }
  },

  // Lấy thông tin user hiện tại
  getCurrentUser: async () => {
    try {
      const response = await api.get('/users/cic');

      // Xử lý response structure nếu cần
      const userData = response.data.data || response.data;
      return userData;
    } catch (error) {
      const errorMessage = error.response?.data?.message || 'Không thể lấy thông tin người dùng';
      throw new Error(errorMessage);
    }
  },

  // Đăng xuất
  logout: async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        await api.post('/auth/logout', { refreshToken });
      }
    } catch (error) {
      console.warn('AuthService: Logout API error (non-critical):', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },

  // Refresh token
  refreshToken: async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');

      if (!refreshToken) {
        throw new Error('No refresh token available');
      }

      const response = await api.post('/auth/refresh-token', { refreshToken });

      // Xử lý response structure: response.data.data
      const responseData = response.data.data;

      if (!responseData) {
        console.error('AuthService: Refresh response missing data field:', response.data);
        throw new Error('Invalid refresh response structure');
      }

      const newAccessToken = responseData.accessToken;
      const newRefreshToken = responseData.refreshToken;

      if (!newAccessToken) {
        throw new Error('Refresh response missing access token');
      }

      // Cập nhật cả access token và refresh token mới
      localStorage.setItem('accessToken', newAccessToken);
      if (newRefreshToken) {
        localStorage.setItem('refreshToken', newRefreshToken);
      } else {
        console.log('AuthService: Only access token refreshed');
      }
      return newAccessToken;
    } catch (error) {
      // Clear tokens on refresh failure
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');

      // Rethrow với message rõ ràng hơn
      const errorMessage = error.response?.data?.message || error.message || 'Token refresh failed';
      throw new Error(errorMessage);
    }
  },

  // Kiểm tra token có hợp lệ không
  isTokenValid: () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      return false;
    }

    try {
      // JWT token có 3 phần được phân tách bởi dấu "."
      const parts = token.split('.');
      if (parts.length !== 3) {
        return false;
      }

      const payload = JSON.parse(atob(parts[1]));
      const currentTime = Math.floor(Date.now() / 1000);
      const isValid = payload.exp > currentTime;

      console.log('AuthService: Token validation:', {
        exp: payload.exp,
        current: currentTime,
        isValid: isValid,
        timeLeft: payload.exp - currentTime
      });

      return isValid;
    } catch (error) {
      console.error('AuthService: Token validation error:', error);
      return false;
    }
  }
};