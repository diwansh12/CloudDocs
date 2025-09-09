// src/services/aiService.ts
type DocumentStatus = "PENDING" | "IN_REVIEW" | "APPROVED" | "REJECTED";
interface DocumentResult {
  id: number;
  originalFilename: string;
  description?: string;
  category?: string;
  uploadDate: string;
  uploadedByName: string;
  aiScore?: number;
  // Add other fields to match your Document interface
filename: string;
  fileSize: number;
  formattedFileSize: string;
  mimeType: string;
  documentType: string;
  status: DocumentStatus;
  versionNumber: number;
  uploadedById: number;
  lastModified: string;
  downloadCount: number;
  tags?: string[];
}

interface AISearchResponse {
  query: string;
  results: DocumentResult[];
  count: number;
  type: string;
  beta_user?: boolean;
}


const mapBackendResponseToDocument = (backendDoc: any): DocumentResult => ({
  id: backendDoc.id,
  filename: backendDoc.filename || backendDoc.originalFilename,
  originalFilename: backendDoc.originalFilename,
  description: backendDoc.description || '',
  category: backendDoc.category || '',
  uploadDate: backendDoc.uploadDate,
  uploadedByName: backendDoc.uploadedByName,
  fileSize: backendDoc.fileSize || 0,
  formattedFileSize: backendDoc.formattedFileSize || '0 B',
  mimeType: backendDoc.mimeType || '',
  documentType: backendDoc.documentType || '',
  status: backendDoc.status || 'PENDING',
  versionNumber: backendDoc.versionNumber || 1,
  uploadedById: backendDoc.uploadedById || 0,
  lastModified: backendDoc.lastModified || backendDoc.uploadDate,
  downloadCount: backendDoc.downloadCount || 0,
  tags: backendDoc.tags || [],
  aiScore: backendDoc.aiScore
});


const AI_API_BASE = process.env.REACT_APP_API_BASE_URL 
  ? `${process.env.REACT_APP_API_BASE_URL}/ai`
  : 'https://clouddocs.onrender.com/api/ai';


// ✅ Keep the rest of your aiService code exactly the same
const getAuthHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('token')}`
});

export const aiService = {
  async semanticSearch(query: string, limit: number = 12): Promise<DocumentResult[]> {
    try {
      const response = await fetch(`${AI_API_BASE}/search`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ query, limit })
      });

      if (!response.ok) {
        throw new Error(`AI search failed: ${response.statusText}`);
      }

      const data: AISearchResponse = await response.json();
      
      if ('message' in data && typeof data.message === 'string') {
        throw new Error(data.message);
      }

      return (data.results || []).map(mapBackendResponseToDocument);
    } catch (error) {
      console.error('AI Search Error:', error);
      throw error;
    }
  },

  async generateEmbeddings(): Promise<string> {
    try {
      const response = await fetch(`${AI_API_BASE}/generate-embeddings`, {
        method: 'POST',
        headers: getAuthHeaders()
      });

      if (!response.ok) {
        throw new Error(`Failed to generate embeddings: ${response.statusText}`);
      }

      const data = await response.json();
      return data.message || 'Embeddings generated successfully';
    } catch (error) {
      console.error('Generate Embeddings Error:', error);
      throw error;
    }
  },

  async getAIStatus(): Promise<any> {
    try {
      const response = await fetch(`${AI_API_BASE}/status`, {
        method: 'GET',
        headers: getAuthHeaders()
      });

      if (!response.ok) {
        return { aiSearchEnabled: false };
      }

      return await response.json();
    } catch (error) {
      return { aiSearchEnabled: false };
    }
  }
};