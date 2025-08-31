// src/services/api.ts
import axios, { AxiosInstance, AxiosResponse, AxiosRequestConfig } from 'axios';

class ApiClient {
  private axiosInstance: AxiosInstance;

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: process.env.REACT_APP_API_BASE_URL || 'https://clouddocs.onrender.com/api',
      timeout: 60000,
      withCredentials: true, // ✅ Increased timeout for file uploads
      // ✅ REMOVED: Default Content-Type header - set dynamically per request
    });

    // ✅ ENHANCED: Request interceptor with FormData support
    this.axiosInstance.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers = config.headers || {};
          config.headers.Authorization = `Bearer ${token}`;
          console.log('Adding token to request:', config.url);
        } else {
          console.log('No token found for request:', config.url);
        }

        // ✅ CRITICAL FIX: Handle FormData properly
        if (config.data instanceof FormData) {
          // Remove Content-Type header to let browser set correct boundary
          if (config.headers['Content-Type']) {
            delete config.headers['Content-Type'];
            console.log('📤 FormData detected - removed Content-Type header for proper boundary');
          }
        } else {
          // Set JSON Content-Type for regular requests
          config.headers['Content-Type'] = 'application/json';
        }

        return config;
      },
      (error) => Promise.reject(error)
    );

    // ✅ ENHANCED: Response interceptor with better error handling
    this.axiosInstance.interceptors.response.use(
      (response) => response,
      (error) => {
        console.error('API Error:', {
          url: error.config?.url,
          status: error.response?.status,
          message: error.response?.data?.message || error.message,
          data: error.response?.data
        });

        if (error.response?.status === 401) {
          console.log('401 Unauthorized - clearing token and redirecting to login');
          // ✅ FIXED: Use consistent token key names
          localStorage.removeItem('token'); // Fixed: was 'authToken'
          localStorage.removeItem('user'); // Fixed: was 'userData'
          window.location.href = '/login';
        }
        
        if (error.response?.status === 403) {
          console.log('403 Forbidden - insufficient permissions');
        }

        if (error.response?.status >= 500) {
          console.error('Server error:', error.response?.status);
        }
        
        return Promise.reject(error);
      }
    );
  }

  // ✅ ENHANCED: Generic GET method with better typing
  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.axiosInstance.get<T>(url, config);
  }

  // ✅ ENHANCED: POST method optimized for both JSON and FormData
  async post<T = any, D = any>(
    url: string, 
    data?: D, 
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> {
    return this.axiosInstance.post<T>(url, data, config);
  }

  // ✅ ENHANCED: PUT method with proper typing
  async put<T = any, D = any>(
    url: string, 
    data?: D, 
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> {
    return this.axiosInstance.put<T>(url, data, config);
  }

  // ✅ ENHANCED: DELETE method with proper typing
  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.axiosInstance.delete<T>(url, config);
  }

  // ✅ NEW: Specialized method for file uploads
  async uploadFile<T = any>(
    url: string, 
    formData: FormData, 
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> {
    return this.axiosInstance.post<T>(url, formData, {
      ...config,
      headers: {
        ...config?.headers,
        // Explicitly ensure Content-Type is not set for FormData
      },
      timeout: 60000, // Longer timeout for file uploads
      onUploadProgress: (progressEvent) => {
        if (config?.onUploadProgress) {
          config.onUploadProgress(progressEvent);
        }
        const percentCompleted = Math.round((progressEvent.loaded * 100) / (progressEvent.total || 1));
        console.log(`📤 Upload progress: ${percentCompleted}%`);
      }
    });
  }

  // ✅ NEW: Helper method to check if token exists
  hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  // ✅ NEW: Helper method to get token
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  // ✅ NEW: Helper method to set token
  setToken(token: string): void {
    localStorage.setItem('token', token);
  }

  // ✅ NEW: Helper method to clear auth data
  clearAuth(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }
}

const api = new ApiClient();
export default api;
