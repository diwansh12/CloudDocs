// src/services/ocrService.ts
import api from './api';
import { OCRResult, OCRStatistics, DocumentWithOCR } from './api';

export interface SearchResult {
  documents: DocumentWithOCR[];
  totalResults: number;
  searchType: 'semantic' | 'keyword' | 'hybrid';
  processingTime: number;
}

class OCRService {
  /**
   * ✅ Extract text from image using OCR (handles disabled state)
   */
  async extractText(file: File): Promise<OCRResult> {
    try {
      console.log('🔍 Starting OCR extraction for:', file.name);
      
      const formData = new FormData();
      formData.append('file', file);

      const response = await api.uploadFile<OCRResult>('/ocr/extract', formData, {
        timeout: 180000, // Extended timeout for OCR processing
        onUploadProgress: (progressEvent) => {
          const progress = Math.round((progressEvent.loaded * 100) / (progressEvent.total || 1));
          console.log(`📤 OCR Upload Progress: ${progress}%`);
        },
      });

      const result = response.data;
      
      // Handle OCR disabled response
      if (result.success === false) {
        console.log('ℹ️ OCR is disabled:', result.message);
        return {
          extractedText: '',
          confidence: 0,
          processingTimeMs: 0,
          filename: file.name,
          success: false,
          message: result.message || 'OCR service is currently unavailable',
          reason: result.reason || 'Feature temporarily disabled for memory optimization',
          alternative: result.alternative || 'Use regular document upload and AI search instead'
        };
      }

      console.log('✅ OCR extraction completed:', result);
      return result;
    } catch (error: any) {
      console.error('❌ OCR extraction failed:', error);
      
      // Handle timeout errors
      if (error.code === 'ECONNABORTED' || error.name === 'AbortError') {
        return {
          extractedText: '',
          confidence: 0,
          processingTimeMs: 0,
          filename: file.name,
          success: false,
          message: 'OCR processing timed out',
          reason: 'Server took too long to process the image',
          alternative: 'Try with a smaller image or use regular document upload'
        };
      }
      
      // Handle file size errors
      if (error.response?.status === 413) {
        return {
          extractedText: '',
          confidence: 0,
          processingTimeMs: 0,
          filename: file.name,
          success: false,
          message: 'File too large for OCR processing',
          reason: 'Image exceeds maximum size limit',
          alternative: 'Use an image smaller than 500KB or upload normally'
        };
      }
      
      // Handle unsupported file type
      if (error.response?.status === 415) {
        return {
          extractedText: '',
          confidence: 0,
          processingTimeMs: 0,
          filename: file.name,
          success: false,
          message: 'Unsupported file type for OCR',
          reason: 'Only image files are supported for text extraction',
          alternative: 'Upload as a regular document instead'
        };
      }

      // Handle OCR disabled response from backend
      if (error.response?.data) {
        const backendError = error.response.data;
        return {
          extractedText: '',
          confidence: 0,
          processingTimeMs: 0,
          filename: file.name,
          success: false,
          message: backendError.message || 'OCR processing failed',
          reason: backendError.reason || 'Server error or OCR disabled',
          alternative: backendError.alternative || 'Use regular document upload instead'
        };
      }

      // Generic error fallback
      return {
        extractedText: '',
        confidence: 0,
        processingTimeMs: 0,
        filename: file.name,
        success: false,
        message: 'OCR text extraction failed',
        reason: error.message || 'Unknown error occurred',
        alternative: 'Try again later or use regular document upload'
      };
    }
  }

  /**
   * ✅ Upload document with OCR processing (handles disabled state)
   */
  async uploadDocumentWithOCR(
    file: File, 
    description?: string, 
    category?: string
  ): Promise<any> {
    try {
      console.log('📄 Starting OCR document upload for:', file.name);
      
      const formData = new FormData();
      formData.append('file', file);
      
      if (description) {
        formData.append('description', description);
      }
      
      if (category) {
        formData.append('category', category);
      }

      const response = await api.uploadFile<any>('/ocr/upload', formData, {
        timeout: 240000, // Extended timeout for OCR + AI processing
        onUploadProgress: (progressEvent) => {
          const progress = Math.round((progressEvent.loaded * 100) / (progressEvent.total || 1));
          console.log(`📤 OCR Document Upload Progress: ${progress}%`);
        },
      });

      const result = response.data;

      // Handle OCR disabled response
      if (result.success === false) {
        console.log('ℹ️ OCR upload is disabled:', result.message);
        return {
          success: false,
          message: result.message || 'OCR document upload is currently unavailable',
          reason: result.reason || 'Feature temporarily disabled for memory optimization',
          alternative: result.alternative || 'Use regular document upload instead - all features except OCR text extraction remain available'
        };
      }

      console.log('✅ OCR document upload completed:', result);
      return {
        success: true,
        document: result,
        message: 'Document uploaded successfully with OCR processing'
      };
    } catch (error: any) {
      console.error('❌ OCR document upload failed:', error);
      
      // Handle OCR disabled response from backend
      if (error.response?.data) {
        const backendError = error.response.data;
        return {
          success: false,
          message: backendError.message || 'Document upload with OCR failed',
          reason: backendError.reason || 'OCR service unavailable',
          alternative: backendError.alternative || 'Use regular document upload - your document will still be stored and searchable'
        };
      }

      // Handle file size errors
      if (error.response?.status === 413) {
        return {
          success: false,
          message: 'File too large for OCR upload',
          reason: 'File exceeds maximum size limit',
          alternative: 'Use regular document upload for larger files'
        };
      }
      
      return {
        success: false,
        message: 'Document upload with OCR failed',
        reason: error.message || 'Network or server error',
        alternative: 'Use regular document upload - your document will still be stored and searchable'
      };
    }
  }

  /**
   * ✅ AI-powered semantic search (includes OCR text)
   */
  async semanticSearch(query: string, limit: number = 10): Promise<SearchResult> {
    try {
      console.log('🔍 Starting semantic search for:', query);
      
      const startTime = Date.now();
      
      const response = await api.post<any>('/search/semantic', {
        query,
        limit
      });
      
      const processingTime = Date.now() - startTime;
      console.log(`✅ Semantic search completed in ${processingTime}ms`);
      
      // Safe property access with null checks
      const backendData = response?.data || {};
      
      return {
        documents: Array.isArray(backendData.results) ? backendData.results : [],
        totalResults: typeof backendData.count === 'number' ? backendData.count : 0,
        searchType: 'semantic' as const,
        processingTime
      };
    } catch (error: any) {
      console.error('❌ Semantic search failed:', error);
      throw new Error(error.response?.data?.message || 'Semantic search failed');
    }
  }

  /**
   * ✅ Validate file for OCR processing
   */
  validateFileForOCR(file: File): { valid: boolean; error?: string } {
    // Check file size (500KB limit for OCR)
    const maxSize = 500 * 1024; // 500KB
    if (file.size > maxSize) {
      return {
        valid: false,
        error: 'File too large for OCR. Please select an image smaller than 500KB.'
      };
    }

    // Check file type
    const supportedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/bmp', 'image/tiff', 'image/gif'];
    if (!supportedTypes.includes(file.type.toLowerCase())) {
      return {
        valid: false,
        error: 'Unsupported file type. Please upload JPEG, PNG, BMP, TIFF, or GIF images.'
      };
    }

    return { valid: true };
  }

  /**
   * ✅ Format OCR confidence as percentage
   */
  formatConfidence(confidence: number): string {
    return `${(confidence * 100).toFixed(1)}%`;
  }

  /**
   * ✅ Get confidence color for UI
   */
  getConfidenceColor(confidence: number): string {
    if (confidence >= 0.8) return 'text-green-600';
    if (confidence >= 0.6) return 'text-yellow-600';
    return 'text-red-600';
  }

  /**
   * ✅ Get confidence badge class
   */
  getConfidenceBadge(confidence: number): string {
    if (confidence >= 0.8) return 'bg-green-100 text-green-800';
    if (confidence >= 0.6) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  }
}

export default new OCRService();
