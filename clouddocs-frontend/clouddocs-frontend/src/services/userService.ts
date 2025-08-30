import api from './api';

export interface UserProfile {
  id: number;
  fullName: string;
  username: string;
  email: string;
  role: 'USER' | 'MANAGER' | 'ADMIN';
  profilePicture?: string;
  createdAt: string;
  lastLoginAt?: string;
  active: boolean;
}

class UserService {
  
  // ✅ FIX: Remove /api prefix since it's already in baseURL
  async getCurrentUser(): Promise<UserProfile> {
    try {
      const response = await api.get('/users/profile'); // ✅ Changed from '/api/users/profile'
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch user profile');
    }
  }

  async uploadProfilePicture(file: File): Promise<UserProfile> {
    try {
      const formData = new FormData();
      formData.append('profilePicture', file);

      const response = await api.post('/users/profile/picture', formData, { // ✅ Removed /api prefix
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      return response.data.user || response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to upload profile picture');
    }
  }

  async updateProfile(profileData: {
    fullName: string;
    email: string;
  }): Promise<UserProfile> {
    try {
      const response = await api.put('/users/profile', profileData); // ✅ Removed /api prefix
      return response.data.user || response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to update profile');
    }
  }

  async logout(): Promise<void> {
    try {
      await api.post('/users/logout'); // ✅ Removed /api prefix
    } catch (error: any) {
      console.error('Logout API error:', error);
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    try {
      await api.post('/users/change-password', { // ✅ Removed /api prefix
        currentPassword,
        newPassword
      });
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to change password');
    }
  }
}

export default new UserService();
