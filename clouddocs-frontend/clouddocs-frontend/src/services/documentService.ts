import api from './api';

export interface Document {
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
}

export interface DocumentsResponse {
  documents: Document[];
  currentPage: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface DocumentUploadResponse {
  message: string;
  document: Document;
}

// ✅ NEW: Share link interfaces
export interface ShareLinkOptions {
  expiryHours?: number;
  allowDownload?: boolean;
  password?: string;
}

export interface ShareLinkResponse {
  shareUrl: string;
  expiresAt?: string;
  shareId: string;
}

export interface ShareLink {
  id: string;
  url: string;
  expiresAt: string;
  allowDownload: boolean;
  hasPassword: boolean;
  createdAt: string;
  accessCount: number;
}

// ✅ NEW: Metadata update interface
export interface DocumentMetadata {
  title?: string;
  description?: string;
  category?: string;
  tags?: string[];
}

class DocumentService {
  
  // Get all documents with pagination and filtering
  async getAllDocuments(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'uploadDate',
    sortDir: string = 'desc',
    search?: string,
    status?: string,
    category?: string
  ): Promise<DocumentsResponse> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sortBy,
        sortDir
      });

      if (search) params.append('search', search);
      if (status) params.append('status', status);
      if (category) params.append('category', category);

      const response = await api.get<DocumentsResponse>(`/documents?${params}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch documents');
    }
  }

  // Get user's own documents
  async getMyDocuments(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'uploadDate',
    sortDir: string = 'desc'
  ): Promise<DocumentsResponse> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sortBy,
        sortDir
      });

      const response = await api.get<DocumentsResponse>(`/documents/my-documents?${params}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch your documents');
    }
  }

  // Get document by ID
  async getDocumentById(id: number): Promise<Document> {
    try {
      const response = await api.get<Document>(`/documents/${id}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch document details');
    }
  }

  // Upload document
  async uploadDocument(file: File, description?: string, category?: string, tags?: string[]): Promise<Document> {
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      if (description) {
        formData.append('description', description);
      }
      if (category) {
        formData.append('category', category);
      }
      if (tags && tags.length > 0) {
        tags.forEach(tag => formData.append('tags', tag));
      }

      const response = await api.post<DocumentUploadResponse>('/documents/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      return response.data.document;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to upload document');
    }
  }

  // Download document
  async downloadDocument(id: number, filename: string): Promise<void> {
    try {
      const response = await api.get(`/documents/${id}/download`, {
        responseType: 'blob'
      });
      
      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = window.document.createElement('a');
      link.href = url;
      link.download = filename;
      window.document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to download document');
    }
  }

  // Delete document
  async deleteDocument(id: number): Promise<void> {
    try {
      await api.delete(`/documents/${id}`);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to delete document');
    }
  }

  // ✅ NEW: Update document metadata
  async updateDocumentMetadata(id: number, metadata: DocumentMetadata): Promise<Document> {
    try {
      const response = await api.put<Document>(`/documents/${id}/metadata`, metadata);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to update document metadata');
    }
  }

  // ✅ NEW: Generate share link
  async generateShareLink(id: number, options: ShareLinkOptions = {}): Promise<ShareLinkResponse> {
    try {
      const response = await api.post<ShareLinkResponse>(`/documents/${id}/share`, options);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to generate share link');
    }
  }

  // ✅ NEW: Get existing share links for a document
  async getShareLinks(id: number): Promise<ShareLink[]> {
    try {
      const response = await api.get<ShareLink[]>(`/documents/${id}/shares`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch share links');
    }
  }

  // ✅ NEW: Revoke share link
  async revokeShareLink(documentId: number, shareId: string): Promise<void> {
    try {
      await api.delete(`/documents/${documentId}/shares/${shareId}`);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to revoke share link');
    }
  }

  // ✅ NEW: Access shared document (public endpoint)
  async accessSharedDocument(shareId: string, password?: string): Promise<Document> {
    try {
      const payload = password ? { password } : {};
      const response = await api.post<Document>(`/documents/shared/${shareId}/access`, payload);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to access shared document');
    }
  }

  // ✅ NEW: Download shared document (public endpoint)
  async downloadSharedDocument(shareId: string, password?: string): Promise<void> {
    try {
      const payload = password ? { password } : {};
      const response = await api.post(`/documents/shared/${shareId}/download`, payload, {
        responseType: 'blob'
      });

      // Extract filename from response headers
      const disposition = response.headers['content-disposition'];
      let filename = 'document';
      if (disposition) {
        const filenameMatch = disposition.match(/filename="(.+)"/);
        if (filenameMatch) {
          filename = filenameMatch[1];
        }
      }

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to download shared document');
    }
  }

  // Update document status (Admin/Manager only)
  async updateDocumentStatus(id: number, status: string, rejectionReason?: string): Promise<Document> {
    try {
      const params = new URLSearchParams({ status });
      if (rejectionReason) {
        params.append('rejectionReason', rejectionReason);
      }

      const response = await api.put(`/documents/${id}/status?${params}`);
      return response.data.document;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to update document status');
    }
  }

  // Get categories
  async getCategories(): Promise<string[]> {
    try {
      const response = await api.get<string[]>('/documents/categories');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch categories');
    }
  }

  // Get tags
  async getTags(): Promise<string[]> {
    try {
      const response = await api.get<string[]>('/documents/tags');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch tags');
    }
  }

  // ✅ NEW: Get document statistics (Admin/Manager only)
  async getDocumentStats(): Promise<{
    total: number;
    byStatus: Record<string, number>;
    byCategory: Record<string, number>;
    recentUploads: number;
    totalDownloads: number;
  }> {
    try {
      const response = await api.get('/documents/stats');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch document statistics');
    }
  }

  // ✅ NEW: Bulk operations (Admin/Manager only)
  async bulkUpdateStatus(documentIds: number[], status: string, rejectionReason?: string): Promise<void> {
    try {
      const payload = {
        documentIds,
        status,
        rejectionReason
      };
      await api.put('/documents/bulk/status', payload);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to update document statuses');
    }
  }

  async bulkDelete(documentIds: number[]): Promise<void> {
    try {
      await api.delete('/documents/bulk', { data: { documentIds } });
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to delete documents');
    }
  }
}

export default new DocumentService();

