// src/services/api.ts
import axios, { AxiosInstance, AxiosResponse, AxiosRequestConfig, AxiosHeaders } from 'axios';

// ✅ ADD: OCR-specific interfaces
export interface OCRResult {
  extractedText: string;
  confidence: number;
  processingTimeMs: number;
  filename: string;
  success: boolean;
  errorMessage?: string;
}

export interface DocumentWithOCR {
  id: number;
  filename: string;
  originalFilename: string;
  description?: string;
  fileSize: number;
  formattedFileSize: string;
  mimeType: string;
  status: 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';
  versionNumber: number;
  uploadedByName: string;
  uploadedById: number;
  uploadDate: string;
  lastModified: string;
  downloadCount: number;
  tags?: string[];
  category?: string;
  documentType?: string;
  approvedByName?: string;
  approvalDate?: string;
  rejectionReason?: string;
  // ✅ OCR fields
  hasOcr?: boolean;
  ocrText?: string;
  ocrConfidence?: number;
  ocrProcessingTime?: number;
  // ✅ AI fields
  embeddingGenerated?: boolean;
  aiScore?: number;
  searchType?: 'semantic' | 'keyword' | 'hybrid';
}

export interface OCRStatistics {
  totalDocuments: number;
  documentsWithOCR: number;
  documentsWithEmbeddings: number;
  ocrCoverage: number;
  averageOCRConfidence: number;
  aiReadyDocuments?: number;
}

class ApiClient {
  private axiosInstance: AxiosInstance;

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: process.env.REACT_APP_API_BASE_URL || 'https://clouddocs-production.up.railway.app/api',
      timeout: 60000, // Extended for OCR processing
      withCredentials: true,
      headers: {
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
      }
    });

    // ✅ Your existing interceptors are perfect - keeping them unchanged
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

        if (config.method === 'get') {
          const separator = config.url?.includes('?') ? '&' : '?';
          config.url = `${config.url}${separator}_t=${Date.now()}`;
          
          if (!config.headers) {
            config.headers = new AxiosHeaders();
          }
          config.headers.set('Cache-Control', 'no-cache, must-revalidate');
          config.headers.set('Pragma', 'no-cache');
        }

        if (config.data instanceof FormData) {
          if (config.headers && config.headers.hasContentType()) {
            config.headers.setContentType(false);
            console.log('📤 FormData detected - removed Content-Type header for proper boundary');
          }
        } else if (config.method !== 'get') {
          if (!config.headers) {
            config.headers = new AxiosHeaders();
          }
          config.headers.setContentType('application/json');
        }

        return config;
      },
      (error) => Promise.reject(error)
    );

    this.axiosInstance.interceptors.response.use(
      (response) => {
        if (response.config.url?.includes('workflow') || response.config.url?.includes('ocr')) {
          console.log('📅 API Response received at:', new Date().toISOString());
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

  // ✅ Keep all your existing methods - they're excellent
  async get<T = any>(url: string, config?: AxiosRequestConfig & { forceRefresh?: boolean }): Promise<AxiosResponse<T>> {
    const enhancedConfig = { ...config };
    
    if (config?.forceRefresh) {
      const separator = url.includes('?') ? '&' : '?';
      url = `${url}${separator}_refresh=${Date.now()}`;
      
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

  async post<T = any, D = any>(url: string, data?: D, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.axiosInstance.post<T>(url, data, config);
  }

  async put<T = any, D = any>(url: string, data?: D, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.axiosInstance.put<T>(url, data, config);
  }

  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.axiosInstance.delete<T>(url, config);
  }

  async uploadFile<T = any>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.axiosInstance.post<T>(url, formData, {
      ...config,
      headers: {
        ...config?.headers,
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

  async getWithFreshData<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.get<T>(url, { ...config, forceRefresh: true });
  }

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

  hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  setToken(token: string): void {
    localStorage.setItem('token', token);
  }

  clearAuth(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }
}

const api = new ApiClient();
export default api;
