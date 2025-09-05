// src/services/api.ts
import axios, { AxiosInstance, AxiosResponse, AxiosRequestConfig, AxiosHeaders } from 'axios';

class ApiClient {
  private axiosInstance: AxiosInstance;

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: process.env.REACT_APP_API_BASE_URL || 'https://clouddocs.onrender.com/api',
      timeout: 60000,
      withCredentials: true,
      // ✅ CRITICAL: Add cache-busting headers
      headers: {
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
      }
    });

    // ✅ FIXED: Request interceptor with proper header handling
    this.axiosInstance.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          if (!config.headers) {
            config.headers = new AxiosHeaders();
          }
          config.headers.set('Authorization', `Bearer ${token}`);
          console.log('Adding token to request:', config.url);
        } else {
          console.log('No token found for request:', config.url);
        }

        // ✅ CRITICAL: Add timestamp to GET requests for cache-busting
        if (config.method === 'get') {
          const separator = config.url?.includes('?') ? '&' : '?';
          config.url = `${config.url}${separator}_t=${Date.now()}`;
          
          // ✅ FIXED: Properly set cache-busting headers
          if (!config.headers) {
            config.headers = new AxiosHeaders();
          }
          config.headers.set('Cache-Control', 'no-cache, must-revalidate');
          config.headers.set('Pragma', 'no-cache');
        }

        // ✅ CRITICAL FIX: Handle FormData properly
        if (config.data instanceof FormData) {
          // Remove Content-Type header to let browser set correct boundary
          if (config.headers && config.headers.hasContentType()) {
            config.headers.setContentType(false);
            console.log('📤 FormData detected - removed Content-Type header for proper boundary');
          }
        } else if (config.method !== 'get') {
          // Set JSON Content-Type for non-GET requests
          if (!config.headers) {
            config.headers = new AxiosHeaders();
          }
          config.headers.setContentType('application/json');
        }

        return config;
      },
      (error) => Promise.reject(error)
    );

    // ✅ ENHANCED: Response interceptor with better error handling
    this.axiosInstance.interceptors.response.use(
      (response) => {
        // ✅ Log successful responses for debugging timestamps
        if (response.config.url?.includes('workflow')) {
          console.log('📅 Workflow API Response received at:', new Date().toISOString());
        }
        return response;
      },
      (error) => {
        console.error('API Error:', {
          url: error.config?.url,
          status: error.response?.status,
          message: error.response?.data?.message || error.message,
          data: error.response?.data
        });

        if (error.response?.status === 401) {
          console.log('401 Unauthorized - clearing token and redirecting to login');
          localStorage.removeItem('token');
          localStorage.removeItem('user');
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

  // ✅ ENHANCED: GET method with force refresh option
  async get<T = any>(url: string, config?: AxiosRequestConfig & { forceRefresh?: boolean }): Promise<AxiosResponse<T>> {
    const enhancedConfig = { ...config };
    
    // ✅ Add extra cache-busting for force refresh
    if (config?.forceRefresh) {
      const separator = url.includes('?') ? '&' : '?';
      url = `${url}${separator}_refresh=${Date.now()}`;
      
      // ✅ FIXED: Properly handle headers for force refresh
      if (!enhancedConfig.headers) {
        enhancedConfig.headers = new AxiosHeaders();
      }
      
      const headers = enhancedConfig.headers as AxiosHeaders;
      headers.set('Cache-Control', 'no-cache, no-store, must-revalidate');
      headers.set('Pragma', 'no-cache');
      headers.set('Expires', '0');
      headers.set('If-Modified-Since', 'Thu, 01 Jan 1970 00:00:00 GMT');
    }
    
    return this.axiosInstance.get<T>(url, enhancedConfig);
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
      timeout: 60000,
      onUploadProgress: (progressEvent) => {
        if (config?.onUploadProgress) {
          config.onUploadProgress(progressEvent);
        }
        const percentCompleted = Math.round((progressEvent.loaded * 100) / (progressEvent.total || 1));
        console.log(`📤 Upload progress: ${percentCompleted}%`);
      }
    });
  }

  // ✅ NEW: Force refresh method for workflow data
  async getWithFreshData<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.get<T>(url, { ...config, forceRefresh: true });
  }

  // ✅ NEW: Clear browser cache
  async clearCache(): Promise<boolean> {
    try {
      if ('caches' in window) {
        const cacheNames = await caches.keys();
        await Promise.all(
          cacheNames.map(cacheName => caches.delete(cacheName))
        );
        console.log('✅ Browser cache cleared');
        return true;
      }
      return false;
    } catch (error) {
      console.warn('Failed to clear cache:', error);
      return false;
    }
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
