import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Search, 
  Upload, 
  File,
  FileSpreadsheet,
  Archive,
  Code,
  FileText,
  Image,
  Video,
  Music,
  RefreshCw,
  AlertCircle,
  Download,
  Filter,
  X,
  Sparkles,
  Brain,
  Zap
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { Card, CardContent } from '../components/ui/card';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../components/ui/tabs';
import Sidebar from '../components/layout/Sidebar';
import DocumentUploadModal from '../components/documents/DocumentUpload';
import documentService, { Document, DocumentsResponse } from '../services/documentService';
import { aiService } from '../services/aiService';

export default function Documents() {
  const [activeTab, setActiveTab] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [documents, setDocuments] = useState<Document[]>([]);
  const [aiDocuments, setAiDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [embeddingLoading, setEmbeddingLoading] = useState(false);
  const [error, setError] = useState('');
  const [aiError, setAiError] = useState('');
  const [aiEnabled, setAiEnabled] = useState(false);
  const [isAiSearch, setIsAiSearch] = useState(false);
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  
  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  
  // Filter state
  const [selectedStatus, setSelectedStatus] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  
  const navigate = useNavigate();
  const pageSize = 12;

  // ✅ ENHANCED: Load documents with AI search capability
  const loadDocuments = useCallback(async (page = 0, useAI = false) => {
    try {
      setLoading(true);
      setError('');
      setAiError('');
      
      // ✅ Try AI search first if enabled and has search query
      if (useAI && aiEnabled && searchQuery.trim() && searchQuery.length > 2) {
        setAiLoading(true);
        setIsAiSearch(true);
        
        try {
          const aiResults = await aiService.semanticSearch(searchQuery, pageSize);
          
          if (aiResults.length > 0) {
            setAiDocuments(aiResults);
            setDocuments([]);
            setAiLoading(false);
            setLoading(false);
            return; // Exit early - we have AI results
          }
        } catch (aiErr: any) {
          setAiError(aiErr.message || 'AI search failed');
          setAiLoading(false);
          // Continue to regular search on AI failure
        }
      }

      // ✅ Regular search logic (fallback or default)
      setIsAiSearch(false);
      setAiDocuments([]);
      
      let response: DocumentsResponse;
      
      if (activeTab === 'my') {
        response = await documentService.getMyDocuments(page, pageSize);
      } else {
        response = await documentService.getAllDocuments(
          page, 
          pageSize, 
          'uploadDate', 
          'desc',
          searchQuery || undefined,
          selectedStatus || undefined,
          selectedCategory || undefined
        );
      }
      
      setDocuments(response.documents);
      setCurrentPage(response.currentPage);
      setTotalPages(response.totalPages);
      setTotalItems(response.totalItems);
      
    } catch (err: any) {
      console.error('Error loading documents:', err);
      setError(err.message);
      setDocuments([]);
      setAiDocuments([]);
    } finally {
      setLoading(false);
      setAiLoading(false);
    }
  }, [activeTab, searchQuery, selectedStatus, selectedCategory, aiEnabled]);

  // Load documents on component mount and when dependencies change
  useEffect(() => {
    loadDocuments(0);
    setCurrentPage(0);
  }, [loadDocuments]);

  // ✅ ENHANCED: Handle search with AI-first approach
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (searchQuery !== '') {
        loadDocuments(0, true); // Try AI search first
      } else {
        setAiDocuments([]);
        setIsAiSearch(false);
        loadDocuments(0, false); // Regular search
      }
    }, 500);
    
    return () => clearTimeout(timeoutId);
  }, [searchQuery, loadDocuments]);

  // ✅ NEW: Check AI status on component mount
  useEffect(() => {
    checkAIStatus();
  }, []);

  const checkAIStatus = async () => {
    try {
      const status = await aiService.getAIStatus();
      setAiEnabled(status.aiSearchEnabled || false);
    } catch (error) {
      setAiEnabled(false);
    }
  };

  // ✅ NEW: Generate embeddings function
  const handleGenerateEmbeddings = async () => {
    try {
      setEmbeddingLoading(true);
      const message = await aiService.generateEmbeddings();
      alert(message + ' You can now use AI search!');
      await checkAIStatus();
    } catch (error: any) {
      alert(`Failed to generate embeddings: ${error.message}`);
    } finally {
      setEmbeddingLoading(false);
    }
  };

  // Handle upload success
  const handleUploadSuccess = () => {
    loadDocuments(currentPage, isAiSearch);
  };

  // Handle document download
  const handleDownload = async (doc: Document) => {
    try {
      await documentService.downloadDocument(doc.id, doc.originalFilename);
    } catch (err: any) {
      console.error('Download failed:', err);
    }
  };

  // Get status color
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'IN_REVIEW':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  // Get file icon based on document type or mime type
  const getFileIcon = (doc: Document) => {
    const type = doc.documentType || doc.mimeType;
    
    if (type?.includes('pdf')) return File;
    if (type?.includes('spreadsheet') || type?.includes('excel')) return FileSpreadsheet;
    if (type?.includes('zip') || type?.includes('archive')) return Archive;
    if (type?.includes('json') || type?.includes('code')) return Code;
    if (type?.includes('text') || type?.includes('document')) return FileText;
    if (type?.includes('image')) return Image;
    if (type?.includes('video')) return Video;
    if (type?.includes('audio')) return Music;
    
    return File;
  };

  // Handle document click
  const handleDocumentClick = (documentId: number) => {
    navigate(`/documents/${documentId}`);
  };

  // ✅ NEW: Clear AI search and return to regular search
  const clearAISearch = () => {
    setAiDocuments([]);
    setIsAiSearch(false);
    setAiError('');
    setSearchQuery('');
    loadDocuments(0, false);
  };

  // Clear all filters
  const clearFilters = () => {
    setSearchQuery('');
    setSelectedStatus('');
    setSelectedCategory('');
    setShowFilters(false);
    setAiDocuments([]);
    setIsAiSearch(false);
  };

  // ✅ NEW: Get current documents to display
  const currentDocuments = isAiSearch ? aiDocuments : documents;
  const currentLoading = aiLoading || loading;

  // Render pagination
  const renderPagination = () => {
    if (totalPages <= 1 || isAiSearch) return null;

    const pages = [];
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
      pages.push(
        <Button
          key={i}
          variant={currentPage === i ? "default" : "outline"}
          size="sm"
          onClick={() => loadDocuments(i, false)}
          className="mx-1"
        >
          {i + 1}
        </Button>
      );
    }

    return (
      <div className="flex items-center justify-between mt-8">
        <div className="text-sm text-gray-700">
          Showing {currentPage * pageSize + 1} to {Math.min((currentPage + 1) * pageSize, totalItems)} of {totalItems} documents
        </div>
        <div className="flex items-center space-x-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadDocuments(currentPage - 1, false)}
            disabled={currentPage === 0}
          >
            Previous
          </Button>
          {pages}
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadDocuments(currentPage + 1, false)}
            disabled={currentPage >= totalPages - 1}
          >
            Next
          </Button>
        </div>
      </div>
    );
  };

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />

      <main className="flex-1 flex flex-col">
        {/* ✅ ENHANCED Header with AI features */}
        <header className="bg-white border-b border-gray-200 px-8 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl text-gray-900 mb-1">Document Management</h1>
              <div className="flex items-center space-x-4">
                <p className="text-sm text-gray-500">
                  {currentLoading ? 'Loading...' : `${totalItems} documents total`}
                </p>
                
                {/* ✅ NEW: AI Status Indicator */}
                {aiEnabled && (
                  <div className="flex items-center space-x-2">
                    <div className="flex items-center px-2 py-1 bg-purple-100 text-purple-700 rounded-full text-xs font-medium">
                      <Sparkles className="w-3 h-3 mr-1" />
                      AI Search Enabled
                    </div>
                    
                    {/* ✅ NEW: Generate Embeddings Button */}
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={handleGenerateEmbeddings}
                      disabled={embeddingLoading}
                      className="text-xs"
                    >
                      {embeddingLoading ? (
                        <RefreshCw className="w-3 h-3 animate-spin mr-1" />
                      ) : (
                        <Brain className="w-3 h-3 mr-1" />
                      )}
                      {embeddingLoading ? 'Generating...' : 'Prepare AI Search'}
                    </Button>
                  </div>
                )}
              </div>
            </div>
            
            <div className="flex items-center space-x-4">
              {/* ✅ ENHANCED: Search Input with AI indicator */}
              <div className="relative">
                {aiEnabled && searchQuery && isAiSearch && (
                  <div className="absolute -top-6 left-3 text-xs text-purple-600 font-medium">
                    <Sparkles className="w-3 h-3 inline mr-1" />
                    AI Search Active
                  </div>
                )}
                <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                <Input
                  placeholder={aiEnabled ? "AI-powered search... (e.g., 'find contracts about payment')" : "Search documents..."}
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10 w-80 bg-gray-50 border-gray-200"
                />
                {isAiSearch && searchQuery && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={clearAISearch}
                    className="absolute right-2 top-1/2 transform -translate-y-1/2 h-6 w-6 p-0"
                  >
                    <X className="w-3 h-3" />
                  </Button>
                )}
              </div>
              
              <Button 
                variant="outline"
                onClick={() => setShowFilters(!showFilters)}
                className={showFilters ? "bg-blue-50 border-blue-200" : ""}
              >
                <Filter className="w-4 h-4 mr-2" />
                Filters
              </Button>
              
              <Button
                onClick={() => setUploadModalOpen(true)}
                className="bg-blue-600 hover:bg-blue-700 text-white px-6 transition-colors duration-200"
              >
                <Upload className="w-4 h-4 mr-2" />
                Upload Document
              </Button>
            </div>
          </div>

          {/* ✅ NEW: AI Error Display */}
          {aiError && (
            <div className="mt-4 p-3 bg-orange-50 border border-orange-200 rounded-lg">
              <div className="flex items-center">
                <AlertCircle className="w-4 h-4 text-orange-600 mr-2" />
                <span className="text-orange-700 text-sm">{aiError}</span>
                <span className="text-orange-600 text-sm ml-2">• Using regular search instead</span>
              </div>
            </div>
          )}

          {/* Filters Panel */}
          {showFilters && (
            <div className="mt-4 p-4 bg-gray-50 rounded-lg border">
              <div className="flex items-center space-x-4">
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
                  <select
                    value={selectedStatus}
                    onChange={(e) => setSelectedStatus(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    <option value="">All Statuses</option>
                    <option value="PENDING">Pending</option>
                    <option value="IN_REVIEW">In Review</option>
                    <option value="APPROVED">Approved</option>
                    <option value="REJECTED">Rejected</option>
                  </select>
                </div>
                
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                  <select
                    value={selectedCategory}
                    onChange={(e) => setSelectedCategory(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    <option value="">All Categories</option>
                    <option value="Legal">Legal</option>
                    <option value="Marketing">Marketing</option>
                    <option value="HR">HR</option>
                    <option value="Finance">Finance</option>
                    <option value="Technical">Technical</option>
                    <option value="General">General</option>
                  </select>
                </div>
                
                <div className="flex items-end">
                  <Button
                    variant="outline"
                    onClick={clearFilters}
                    className="flex items-center"
                  >
                    <X className="w-4 h-4 mr-1" />
                    Clear
                  </Button>
                </div>
              </div>
            </div>
          )}
        </header>

        {/* ✅ ENHANCED Content Section */}
        <section className="flex-1 px-8 py-6">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="bg-gray-100 p-1 rounded-lg mb-8">
              <TabsTrigger 
                value="all" 
                className="data-[state=active]:bg-white data-[state=active]:text-gray-900 px-6 py-2 transition-colors duration-200"
              >
                All Documents
              </TabsTrigger>
              <TabsTrigger 
                value="my" 
                className="data-[state=active]:bg-white data-[state=active]:text-gray-900 px-6 py-2 text-gray-500 transition-colors duration-200"
              >
                My Documents
              </TabsTrigger>
              <TabsTrigger 
                value="shared" 
                className="data-[state=active]:bg-white data-[state=active]:text-gray-900 px-6 py-2 text-gray-500 transition-colors duration-200"
              >
                Shared with me
              </TabsTrigger>
            </TabsList>

            <TabsContent value={activeTab} className="mt-0">
              {/* ✅ NEW: AI Results Header */}
              {isAiSearch && aiDocuments.length > 0 && (
                <div className="mb-6 p-4 bg-gradient-to-r from-purple-50 to-indigo-50 border border-purple-200 rounded-lg">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center">
                      <Zap className="w-5 h-5 text-purple-600 mr-2" />
                      <span className="text-purple-900 font-medium">
                        🤖 AI found {aiDocuments.length} relevant results for "{searchQuery}"
                      </span>
                    </div>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={clearAISearch}
                      className="text-purple-700 border-purple-300"
                    >
                      <X className="w-4 h-4 mr-1" />
                      Clear AI Search
                    </Button>
                  </div>
                </div>
              )}

              {error && (
                <Card className="mb-6 border-red-200 bg-red-50">
                  <CardContent className="p-4 flex items-center">
                    <AlertCircle className="w-5 h-5 text-red-500 mr-2" />
                    <span className="text-red-700">{error}</span>
                    <Button 
                      variant="outline" 
                      size="sm" 
                      onClick={() => loadDocuments(currentPage, isAiSearch)}
                      className="ml-auto"
                    >
                      <RefreshCw className="w-4 h-4 mr-1" />
                      Retry
                    </Button>
                  </CardContent>
                </Card>
              )}

              {currentLoading ? (
                <div className="flex items-center justify-center py-12">
                  <RefreshCw className="w-8 h-8 animate-spin text-blue-600 mr-3" />
                  <span className="text-gray-600">
                    {aiLoading ? '🤖 AI is searching your documents...' : 'Loading documents...'}
                  </span>
                </div>
              ) : currentDocuments.length === 0 ? (
                <div className="text-center py-12">
                  <File className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-500 text-lg mb-2">
                    {searchQuery ? 'No documents match your search' : 'No documents found'}
                  </p>
                  <p className="text-gray-400">
                    {isAiSearch 
                      ? 'Try different keywords or generate embeddings first' 
                      : activeTab === 'my' 
                        ? 'Upload your first document to get started' 
                        : 'Documents will appear here when available'
                    }
                  </p>
                </div>
              ) : (
                <>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                    {currentDocuments.map((doc) => {
                      const IconComponent = getFileIcon(doc);
                      return (
                        <Card 
                          key={doc.id} 
                          className={`bg-white border rounded-xl hover:shadow-lg hover:scale-105 transition-all duration-300 cursor-pointer group ${
                            isAiSearch && (doc as any).aiScore 
                              ? 'border-purple-200 shadow-purple-100' 
                              : 'border-gray-200'
                          }`}
                          onClick={() => handleDocumentClick(doc.id)}
                        >
                          <CardContent className="p-6">
                            <div className="flex flex-col items-center text-center space-y-4">
                              {/* ✅ NEW: AI Score Badge */}
                              {isAiSearch && (doc as any).aiScore && (
                                <div className="w-full flex justify-end mb-2">
                                  <Badge className="bg-purple-100 text-purple-700 border-purple-200 text-xs">
                                    <Sparkles className="w-3 h-3 mr-1" />
                                    {((doc as any).aiScore * 100).toFixed(0)}% match
                                  </Badge>
                                </div>
                              )}
                              
                              {/* File Icon */}
                              <div className={`w-16 h-16 rounded-lg flex items-center justify-center transition-colors duration-200 ${
                                isAiSearch 
                                  ? 'bg-purple-50 group-hover:bg-purple-100' 
                                  : 'bg-gray-100 group-hover:bg-blue-50'
                              }`}>
                                <IconComponent className={`w-8 h-8 transition-colors duration-200 ${
                                  isAiSearch 
                                    ? 'text-purple-600 group-hover:text-purple-700' 
                                    : 'text-gray-600 group-hover:text-blue-600'
                                }`} />
                              </div>
                              
                              {/* Document Info */}
                              <div className="space-y-2 w-full">
                                <h3 className="font-medium text-gray-900 text-sm leading-tight">
                                  {doc.originalFilename}
                                </h3>
                                <p className="text-xs text-gray-500">
                                  v{doc.versionNumber} • {doc.formattedFileSize}
                                </p>
                                
                                {/* Status Badge */}
                                <Badge 
                                  variant="outline" 
                                  className={`text-xs px-2 py-1 ${getStatusColor(doc.status)}`}
                                >
                                  {doc.status}
                                </Badge>
                                
                                <p className="text-xs text-gray-500">
                                  {new Date(doc.uploadDate).toLocaleDateString()}
                                </p>
                                <p className="text-xs text-gray-500">
                                  by {doc.uploadedByName}
                                </p>

                                {/* Download button */}
                                <div className="flex justify-center pt-2">
                                  <Button
                                    size="sm"
                                    variant="ghost"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleDownload(doc);
                                    }}
                                    className="opacity-0 group-hover:opacity-100 transition-opacity"
                                  >
                                    <Download className="w-4 h-4" />
                                  </Button>
                                </div>
                              </div>
                            </div>
                          </CardContent>
                        </Card>
                      );
                    })}
                  </div>

                  {/* ✅ Modified: Only show pagination for regular search */}
                  {!isAiSearch && renderPagination()}
                </>
              )}
            </TabsContent>

            {/* Shared Documents - Placeholder */}
            <TabsContent value="shared" className="mt-0">
              <div className="text-center py-12">
                <p className="text-gray-500">Shared documents feature coming soon</p>
              </div>
            </TabsContent>
          </Tabs>
        </section>
      </main>

      {/* Upload Modal */}
      <DocumentUploadModal
        isOpen={uploadModalOpen}
        onClose={() => setUploadModalOpen(false)}
        onUploadSuccess={handleUploadSuccess}
      />
    </div>
  );
}
