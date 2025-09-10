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
   * ✅ Extract text from image using OCR
   */
  async extractText(file: File): Promise<OCRResult> {
    try {
      console.log('🔍 Starting OCR extraction for:', file.name);
      
      const formData = new FormData();
      formData.append('file', file);

      const response = await api.uploadFile<OCRResult>('/ocr/extract', formData, {
        timeout: 45000, // Extended timeout for OCR processing
        onUploadProgress: (progressEvent) => {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total!);
          console.log(`📤 OCR Upload Progress: ${progress}%`);
        },
      });

      console.log('✅ OCR extraction completed:', response.data);
      return response.data;
    } catch (error: any) {
      console.error('❌ OCR extraction failed:', error);
      
      if (error.response?.status === 413) {
        throw new Error('File too large. Please select a smaller image (max 10MB).');
      }
      
      if (error.response?.status === 415) {
        throw new Error('Unsupported file type. Please upload an image file (JPEG, PNG, BMP, TIFF, GIF).');
      }
      
      throw new Error(error.response?.data?.message || 'OCR text extraction failed');
    }
  }

  /**
   * ✅ Upload document with OCR processing
   */
  async uploadDocumentWithOCR(
    file: File, 
    description?: string, 
    category?: string
  ): Promise<DocumentWithOCR> {
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

      const response = await api.uploadFile<DocumentWithOCR>('/ocr/upload', formData, {
        timeout: 60000, // Extended timeout for OCR + AI processing
        onUploadProgress: (progressEvent) => {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total!);
          console.log(`📤 OCR Document Upload Progress: ${progress}%`);
        },
      });

      console.log('✅ OCR document upload completed:', response.data);
      return response.data;
    } catch (error: any) {
      console.error('❌ OCR document upload failed:', error);
      
      if (error.response?.status === 413) {
        throw new Error('File too large. Please select a smaller image (max 10MB).');
      }
      
      throw new Error(error.response?.data?.message || 'Document upload with OCR failed');
    }
  }

  /**
   * ✅ Get OCR statistics for current user
   */
  async getOCRStatistics(): Promise<OCRStatistics> {
    try {
      const response = await api.get<OCRStatistics>('/ocr/stats');
      return response.data;
    } catch (error: any) {
      console.error('❌ Failed to fetch OCR statistics:', error);
      throw new Error(error.response?.data?.message || 'Failed to fetch OCR statistics');
    }
  }

  /**
   * ✅ AI-powered semantic search (includes OCR text)
   */
  async semanticSearch(query: string, limit: number = 10): Promise<SearchResult> {
    try {
      console.log('🔍 Starting semantic search for:', query);
      
      const startTime = Date.now();
      
      const response = await api.post<SearchResult>('/search/semantic', {
        query,
        limit
      });
      
      const processingTime = Date.now() - startTime;
      console.log(`✅ Semantic search completed in ${processingTime}ms`);
      
      return {
        ...response.data,
        processingTime
      };
    } catch (error: any) {
      console.error('❌ Semantic search failed:', error);
      throw new Error(error.response?.data?.message || 'Semantic search failed');
    }
  }

  /**
   * ✅ Hybrid search (semantic + keyword + OCR text)
   */
  async hybridSearch(query: string, limit: number = 10): Promise<SearchResult> {
    try {
      console.log('🔍 Starting hybrid search for:', query);
      
      const startTime = Date.now();
      
      const response = await api.post<SearchResult>('/search/hybrid', {
        query,
        limit
      });
      
      const processingTime = Date.now() - startTime;
      console.log(`✅ Hybrid search completed in ${processingTime}ms`);
      
      return {
        ...response.data,
        processingTime
      };
    } catch (error: any) {
      console.error('❌ Hybrid search failed:', error);
      throw new Error(error.response?.data?.message || 'Hybrid search failed');
    }
  }

  /**
   * ✅ Search specifically in OCR text
   */
  async searchOCRText(query: string): Promise<DocumentWithOCR[]> {
    try {
      const response = await api.get<DocumentWithOCR[]>(`/search/ocr?q=${encodeURIComponent(query)}`);
      return response.data;
    } catch (error: any) {
      console.error('❌ OCR text search failed:', error);
      throw new Error(error.response?.data?.message || 'OCR text search failed');
    }
  }

  /**
   * ✅ Get documents with high OCR confidence
   */
  async getHighConfidenceOCRDocuments(minConfidence: number = 0.8): Promise<DocumentWithOCR[]> {
    try {
      const response = await api.get<DocumentWithOCR[]>(`/documents/ocr/high-confidence?minConfidence=${minConfidence}`);
      return response.data;
    } catch (error: any) {
      console.error('❌ Failed to fetch high confidence OCR documents:', error);
      throw new Error(error.response?.data?.message || 'Failed to fetch OCR documents');
    }
  }

  /**
   * ✅ Get AI-ready documents (both OCR and embeddings)
   */
  async getAIReadyDocuments(): Promise<DocumentWithOCR[]> {
    try {
      const response = await api.get<DocumentWithOCR[]>('/documents/ai-ready');
      return response.data;
    } catch (error: any) {
      console.error('❌ Failed to fetch AI-ready documents:', error);
      throw new Error(error.response?.data?.message || 'Failed to fetch AI-ready documents');
    }
  }

  /**
   * ✅ Validate file for OCR processing
   */
  validateFileForOCR(file: File): { valid: boolean; error?: string } {
    // Check file size (10MB limit)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      return {
        valid: false,
        error: 'File too large. Please select an image smaller than 10MB.'
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
