import React, { useState, useEffect, useCallback, useRef } from 'react';
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
  Zap,
  Eye,
  Trash2,
  MoreVertical,
  ChevronDown
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
import { DeleteConfirmationModal } from '../components/ui/delete-confirmation-modal';

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

  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [documentToDelete, setDocumentToDelete] = useState<Document | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Search mode states with proper refs
  const [searchMode, setSearchMode] = useState<'regular' | 'semantic' | 'hybrid' | 'ocr'>('semantic');
  const [ocrStats, setOcrStats] = useState<any>(null);
  const [showSearchModes, setShowSearchModes] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

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

  // Enhanced loadDocuments function
  const loadDocuments = useCallback(async (page = 0, useAI = false) => {
    try {
      setLoading(true);
      setError('');
      setAiError('');

      console.log(`🔍 loadDocuments called: page=${page}, useAI=${useAI}, searchMode=${searchMode}, query="${searchQuery}"`);

      // Enhanced AI/OCR Search Logic
      if (searchQuery.trim() && searchQuery.length > 0) {
        setAiLoading(true);
        setIsAiSearch(true);

        try {
          let searchResults: any[] = [];
          let searchSuccessful = false;

          console.log(`🤖 Attempting ${searchMode} search for: "${searchQuery}"`);

          switch (searchMode) {
            case 'semantic':
              try {
                console.log('📡 Calling semantic search API...');
                const semanticResult = await documentService.semanticSearch(searchQuery, pageSize);
                console.log('📡 Semantic search response:', semanticResult);
                searchResults = semanticResult.documents || [];
                searchSuccessful = true;
                console.log(`✅ Semantic search returned ${searchResults.length} results`);
              } catch (semanticError: any) {
                console.error('❌ Semantic search failed:', semanticError.message);
                setAiError(`Semantic search failed: ${semanticError.message}`);
              }
              break;

            case 'hybrid':
              try {
                console.log('📡 Calling hybrid search API...');
                const hybridResult = await documentService.hybridSearch(searchQuery, pageSize);
                console.log('📡 Hybrid search response:', hybridResult);
                searchResults = hybridResult.documents || [];
                searchSuccessful = true;
                console.log(`✅ Hybrid search returned ${searchResults.length} results`);
              } catch (hybridError: any) {
                console.error('❌ Hybrid search failed:', hybridError.message);
                setAiError(`Hybrid search failed: ${hybridError.message}`);
              }
              break;

            case 'ocr':
              try {
                console.log('📡 Calling OCR search API...');
                searchResults = await documentService.searchOCRText(searchQuery);
                console.log('📡 OCR search response:', searchResults);
                searchSuccessful = true;
                console.log(`✅ OCR search returned ${searchResults.length} results`);
              } catch (ocrError: any) {
                console.error('❌ OCR search failed:', ocrError.message);
                setAiError(`OCR search failed: ${ocrError.message}`);
              }
              break;

            case 'regular':
            default:
              // Skip AI search, go directly to regular search
              console.log('📄 Regular search mode selected, skipping AI search');
              break;
          }

          // If AI/OCR search was successful and returned results, use them
          if (searchSuccessful && searchResults.length > 0) {
            console.log(`✅ ${searchMode} search successful, displaying ${searchResults.length} results`);
            setAiDocuments(searchResults);
            setDocuments([]);
            setAiLoading(false);
            setLoading(false);
            return;
          }

          // If AI/OCR search failed or returned no results, continue to regular search
          if (searchMode !== 'regular') {
            if (!searchSuccessful) {
              console.log(`⚠️ ${searchMode} search failed, falling back to regular search`);
            } else {
              console.log(`ℹ️ ${searchMode} search returned no results, falling back to regular search`);
              setAiError(`${searchMode} search found no results, showing regular search results instead`);
            }
          }

        } catch (aiErr: any) {
          console.error('❌ AI search error:', aiErr);
          setAiError(`${searchMode} search failed: ${aiErr.message}`);
          setAiLoading(false);
        }
      }

      // Regular search logic (fallback or default)
      console.log('📄 Executing regular document search');
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

      console.log(`✅ Regular search returned ${response.documents.length} documents`);
      setDocuments(response.documents);
      setCurrentPage(response.currentPage);
      setTotalPages(response.totalPages);
      setTotalItems(response.totalItems);

    } catch (err: any) {
      console.error('❌ Error loading documents:', err);
      setError(err.message);
      setDocuments([]);
      setAiDocuments([]);
    } finally {
      setLoading(false);
      setAiLoading(false);
    }
  }, [activeTab, searchQuery, selectedStatus, selectedCategory, searchMode, pageSize]);

  // Load documents on component mount and when dependencies change
  useEffect(() => {
    loadDocuments(0);
    setCurrentPage(0);
  }, [loadDocuments]);

  // Handle search with proper mode switching
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      console.log(`🔄 Search effect triggered: query="${searchQuery}", mode=${searchMode}`);

      if (searchQuery !== '') {
        loadDocuments(0, searchMode !== 'regular');
      } else {
        setAiDocuments([]);
        setIsAiSearch(false);
        setAiError('');
        loadDocuments(0, false);
      }
    }, 1500);

    return () => clearTimeout(timeoutId);
  }, [searchQuery, searchMode, loadDocuments]);

  // Search mode change handler that actually works
  const handleSearchModeChange = useCallback((newMode: 'regular' | 'semantic' | 'hybrid' | 'ocr') => {
    console.log(`🔄 Changing search mode from ${searchMode} to ${newMode}`);

    // Update the search mode state
    setSearchMode(newMode);
    setShowSearchModes(false); // Close dropdown immediately
    setAiError(''); // Clear previous errors
    setError('');

    // If there's a search query, immediately reload with new mode
    if (searchQuery.trim()) {
      console.log(`🔍 Reloading documents with ${newMode} mode for query: "${searchQuery}"`);
      setCurrentPage(0); // Reset to first page
      // Use setTimeout to ensure state is updated before loading
      setTimeout(() => {
        loadDocuments(0, newMode !== 'regular');
      }, 100);
    } else {
      // No search query, just reload regular documents
      setTimeout(() => {
        loadDocuments(0, false);
      }, 100);
    }
  }, [searchMode, searchQuery, loadDocuments]);

  // Close search modes dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowSearchModes(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Check AI status and load OCR stats on component mount
  useEffect(() => {
    checkAIStatus();
    loadOCRStats();
  }, []);

  const checkAIStatus = async () => {
    try {
      const status = await aiService.getAIStatus();
      setAiEnabled(true);
      console.log('🚀 Portfolio Mode: AI features enabled for all users');
    } catch (error) {
      console.warn('AI features may be limited, but still showing as available for portfolio demo');
      setAiEnabled(true);
    }
  };

  const loadOCRStats = async () => {
    try {
      const stats = await documentService.getOCRStatistics();
      setOcrStats(stats);
    } catch (error) {
      console.error('Failed to load OCR stats:', error);
    }
  };


  const handleDeleteDocument = async () => {
      if (!documentToDelete) return;

      setIsDeleting(true);
      try {
        await documentService.deleteDocument(documentToDelete.id);

        // Update the documents list by removing the deleted document
        if (isAiSearch) {
          setAiDocuments(prev => prev.filter(doc => doc.id !== documentToDelete.id));
        } else {
          setDocuments(prev => prev.filter(doc => doc.id !== documentToDelete.id));
        }

        // Update total count
        setTotalItems(prev => prev - 1);

        // Close modal and reset state
        setDeleteModalOpen(false);
        setDocumentToDelete(null);

        console.log(`✅ Document "${documentToDelete.originalFilename}" deleted successfully`);

        // Show success message (optional)
        // You could add a toast notification here

      } catch (error: any) {
        console.error('❌ Failed to delete document:', error);
        alert(`Failed to delete document: ${error.message}`);
      } finally {
        setIsDeleting(false);
      }
    };

    const openDeleteModal = (document: Document) => {
      setDocumentToDelete(document);
      setDeleteModalOpen(true);
    };

    const closeDeleteModal = () => {
      if (isDeleting) return; // Prevent closing while deleting
      setDeleteModalOpen(false);
      setDocumentToDelete(null);
    };

  const handleGenerateEmbeddings = async () => {
    try {
      setEmbeddingLoading(true);
      const message = await aiService.generateEmbeddings();
      alert(message + ' You can now use AI search!');
      await checkAIStatus();
      await loadOCRStats();
    } catch (error: any) {
      alert(`Failed to generate embeddings: ${error.message}`);
    } finally {
      setEmbeddingLoading(false);
    }
  };

  const handleUploadSuccess = () => {
    loadDocuments(currentPage, isAiSearch);
    loadOCRStats();
  };

  const handleDownload = async (doc: Document) => {
    try {
      await documentService.downloadDocument(doc.id, doc.originalFilename);
    } catch (err: any) {
      console.error('Download failed:', err);
    }
  };

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

  const handleDocumentClick = (documentId: number) => {
    navigate(`/documents/${documentId}`);
  };

  const clearAISearch = () => {
    setAiDocuments([]);
    setIsAiSearch(false);
    setAiError('');
    setSearchQuery('');
    loadDocuments(0, false);
  };

  const clearFilters = () => {
    setSearchQuery('');
    setSelectedStatus('');
    setSelectedCategory('');
    setShowFilters(false);
    setAiDocuments([]);
    setIsAiSearch(false);
  };

  const currentDocuments = isAiSearch ? aiDocuments : documents;
  const currentLoading = aiLoading || loading;

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

      {/* ✅ FIXED: Main content with better responsive layout */}
      <main className="flex-1 flex flex-col overflow-hidden min-w-0">
        {/* ✅ FIXED: Header with responsive design and better spacing */}
        <header className="bg-white border-b border-gray-200 px-4 sm:px-6 lg:px-8 py-4 sm:py-6 flex-shrink-0">
          <div className="flex flex-col space-y-4">
            {/* ✅ Title and info row - stack on mobile */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-3 sm:space-y-0">
              <div className="min-w-0 flex-1">
                <h1 className="text-xl sm:text-2xl font-semibold text-gray-900 mb-1">Document Management</h1>
                <div className="flex flex-wrap items-center gap-2 sm:gap-4">
                  <p className="text-sm text-gray-500">
                    {currentLoading ? 'Loading...' : `${totalItems} documents total`}
                  </p>

                  {/* AI Status Indicator - responsive */}
                  {aiEnabled && (
                    <div className="flex flex-wrap items-center gap-2">
                      <div className="flex items-center px-2 py-1 bg-purple-100 text-purple-700 rounded-full text-xs font-medium">
                        <Sparkles className="w-3 h-3 mr-1" />
                        <span className="hidden sm:inline">AI Search Enabled</span>
                        <span className="sm:hidden">AI ✓</span>
                      </div>

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
                        <span className="hidden sm:inline">
                          {embeddingLoading ? 'Generating...' : 'Prepare AI Search'}
                        </span>
                        <span className="sm:hidden">
                          {embeddingLoading ? 'Gen...' : 'Prep AI'}
                        </span>
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* ✅ Controls row - responsive layout */}
            <div className="flex flex-col lg:flex-row gap-3 lg:items-center lg:justify-between">
              {/* ✅ Left side - Search controls */}
              <div className="flex flex-col sm:flex-row gap-3 lg:flex-1 lg:max-w-4xl">
                {/* ✅ Search Mode Dropdown - improved responsive design */}
                <div className="relative flex-shrink-0" ref={dropdownRef}>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      console.log('🎯 Toggle dropdown clicked');
                      setShowSearchModes(!showSearchModes);
                    }}
                    className="flex items-center gap-2 w-full sm:w-auto sm:min-w-[140px] justify-between"
                  >
                    <div className="flex items-center gap-2">
                      {searchMode === 'semantic' && <Brain className="w-4 h-4 text-purple-600" />}
                      {searchMode === 'hybrid' && <Zap className="w-4 h-4 text-blue-600" />}
                      {searchMode === 'ocr' && <Eye className="w-4 h-4 text-green-600" />}
                      {searchMode === 'regular' && <Search className="w-4 h-4 text-gray-600" />}
                      <span className="capitalize">{searchMode}</span>
                    </div>
                    <ChevronDown className={`w-4 h-4 transition-transform ${showSearchModes ? 'rotate-180' : ''}`} />
                  </Button>

                  {/* ✅ Dropdown Menu with improved positioning for responsive design */}
                  {showSearchModes && (
                    <div className="absolute top-full left-0 right-0 sm:right-auto sm:w-64 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-50">
                      <div className="p-2">
                        <div className="text-xs font-medium text-gray-500 mb-2 px-2">Search Modes</div>

                        {[
                          {
                            mode: 'semantic',
                            icon: Brain,
                            label: 'AI Semantic',
                            desc: 'Meaning-based search',
                            color: 'text-purple-600'
                          },
                          {
                            mode: 'hybrid',
                            icon: Zap,
                            label: 'Hybrid Search',
                            desc: 'AI + Keywords + OCR',
                            color: 'text-blue-600'
                          },
                          {
                            mode: 'ocr',
                            icon: Eye,
                            label: 'OCR Text',
                            desc: 'Search extracted text',
                            color: 'text-green-600'
                          },
                          {
                            mode: 'regular',
                            icon: Search,
                            label: 'Regular',
                            desc: 'Standard keyword search',
                            color: 'text-gray-600'
                          }
                        ].map(({ mode, icon: Icon, label, desc, color }) => (
                          <button
                            key={mode}
                            type="button"
                            onClick={(e) => {
                              e.preventDefault();
                              e.stopPropagation();
                              console.log(`🎯 Clicked ${mode} mode button`);
                              handleSearchModeChange(mode as any);
                            }}
                            className={`w-full flex items-center p-3 rounded-md hover:bg-gray-50 transition-colors text-left ${searchMode === mode ? 'bg-blue-50 border-l-4 border-blue-500' : 'border-l-4 border-transparent'
                              }`}
                          >
                            <Icon className={`w-4 h-4 mr-3 flex-shrink-0 ${searchMode === mode ? 'text-blue-600' : color}`} />
                            <div className="flex-1 min-w-0">
                              <div className={`text-sm font-medium ${searchMode === mode ? 'text-blue-900' : 'text-gray-900'}`}>
                                {label}
                              </div>
                              <div className="text-xs text-gray-500 mt-0.5">{desc}</div>
                            </div>
                            {searchMode === mode && (
                              <div className="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0"></div>
                            )}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>

                {/* ✅ Search Input - improved responsive design */}
                <div className="relative flex-1 lg:max-w-md">
                  {searchQuery && searchMode !== 'regular' && (
                    <div className="absolute -top-6 left-3 text-xs font-medium flex items-center">
                      {searchMode === 'semantic' && <span className="text-purple-600"><Brain className="w-3 h-3 inline mr-1" />AI Semantic</span>}
                      {searchMode === 'hybrid' && <span className="text-blue-600"><Zap className="w-3 h-3 inline mr-1" />Hybrid Search</span>}
                      {searchMode === 'ocr' && <span className="text-green-600"><Eye className="w-3 h-3 inline mr-1" />OCR Search</span>}
                    </div>
                  )}

                  <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                  <Input
                    placeholder={
                      searchMode === 'semantic' ? "AI search: 'find contracts about payment'" :
                        searchMode === 'hybrid' ? "Hybrid search: combines AI + keywords + OCR" :
                          searchMode === 'ocr' ? "Search in extracted text from images/PDFs" :
                            "Search documents..."
                    }
                    value={searchQuery}
                    onChange={(e) => {
                      const newQuery = e.target.value;
                      console.log(`📝 Search query changing from "${searchQuery}" to "${newQuery}"`);
                      setSearchQuery(newQuery);
                    }}
                    className="pl-10 w-full bg-gray-50 border-gray-200"
                  />

                  {searchQuery && (
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => {
                        setSearchQuery('');
                        setAiDocuments([]);
                        setIsAiSearch(false);
                      }}
                      className="absolute right-2 top-1/2 transform -translate-y-1/2 h-6 w-6 p-0"
                    >
                      <X className="w-3 h-3" />
                    </Button>
                  )}
                </div>

                {/* ✅ OCR Stats - responsive display */}
                {ocrStats && (
                  <div className="text-xs text-gray-600 bg-gray-100 px-3 py-2 rounded-lg whitespace-nowrap">
                    <span className="hidden sm:inline">📊 </span>
                    {ocrStats.documentsWithOCR}/{ocrStats.totalDocuments}
                    <span className="hidden sm:inline"> with OCR</span>
                    <span className="sm:hidden"> OCR</span>
                    {ocrStats.averageOCRConfidence > 0 && (
                      <span className="ml-1 sm:ml-2">
                        <span className="hidden sm:inline">• Avg: </span>
                        {Math.round(ocrStats.averageOCRConfidence * 100)}%
                      </span>
                    )}
                  </div>
                )}
              </div>

              {/* ✅ Right side - Action buttons */}
              <div className="flex items-center gap-3 flex-shrink-0">
                <Button
                  variant="outline"
                  onClick={() => setShowFilters(!showFilters)}
                  className={showFilters ? "bg-blue-50 border-blue-200" : ""}
                  size="sm"
                >
                  <Filter className="w-4 h-4 sm:mr-2" />
                  <span className="hidden sm:inline">Filters</span>
                </Button>

                <Button
                  onClick={() => setUploadModalOpen(true)}
                  className="bg-blue-600 hover:bg-blue-700 text-white transition-colors duration-200"
                  size="sm"
                >
                  <Upload className="w-4 h-4 sm:mr-2" />
                  <span className="hidden sm:inline">Upload Document</span>
                  <span className="sm:hidden">Upload</span>
                </Button>
              </div>
            </div>
          </div>

          {/* AI Error Display */}
          {aiError && (
            <div className="mt-4 p-3 bg-orange-50 border border-orange-200 rounded-lg">
              <div className="flex items-center">
                <AlertCircle className="w-4 h-4 text-orange-600 mr-2 flex-shrink-0" />
                <span className="text-orange-700 text-sm">{aiError}</span>
                <span className="text-orange-600 text-sm ml-2">• Using regular search instead</span>
              </div>
            </div>
          )}

          {/* ✅ Filters Panel - improved responsive design */}
          {showFilters && (
            <div className="mt-4 p-4 bg-gray-50 rounded-lg border">
              <div className="flex flex-col sm:flex-row items-start sm:items-end space-y-3 sm:space-y-0 sm:space-x-4">
                <div className="flex-1 w-full sm:w-auto">
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

                <div className="flex-1 w-full sm:w-auto">
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

                <div className="flex w-full sm:w-auto">
                  <Button
                    variant="outline"
                    onClick={clearFilters}
                    className="flex items-center w-full sm:w-auto justify-center"
                    size="sm"
                  >
                    <X className="w-4 h-4 mr-1" />
                    Clear
                  </Button>
                </div>
              </div>
            </div>
          )}
        </header>

        {/* ✅ Content Section with proper padding and responsive grid */}
        <section className="flex-1 px-4 sm:px-6 lg:px-8 py-4 sm:py-6 overflow-auto">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="bg-gray-100 p-1 rounded-lg mb-6 sm:mb-8 w-full sm:w-auto">
              <TabsTrigger
                value="all"
                className="data-[state=active]:bg-white data-[state=active]:text-gray-900 px-4 sm:px-6 py-2 transition-colors duration-200 flex-1 sm:flex-none"
              >
                <span className="text-sm">All Documents</span>
              </TabsTrigger>
              <TabsTrigger
                value="my"
                className="data-[state=active]:bg-white data-[state=active]:text-gray-900 px-4 sm:px-6 py-2 text-gray-500 transition-colors duration-200 flex-1 sm:flex-none"
              >
                <span className="text-sm">My Documents</span>
              </TabsTrigger>
              <TabsTrigger
                value="shared"
                className="data-[state=active]:bg-white data-[state=active]:text-gray-900 px-4 sm:px-6 py-2 text-gray-500 transition-colors duration-200 flex-1 sm:flex-none"
              >
                <span className="text-sm">Shared with me</span>
              </TabsTrigger>
            </TabsList>

            <TabsContent value={activeTab} className="mt-0">
              {/* Search Results Header */}
              {isAiSearch && aiDocuments.length > 0 && (
                <div className="mb-6 p-4 bg-gradient-to-r from-purple-50 to-indigo-50 border border-purple-200 rounded-lg">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-3 sm:space-y-0">
                    <div className="flex items-center">
                      {searchMode === 'semantic' && <Brain className="w-5 h-5 text-purple-600 mr-2 flex-shrink-0" />}
                      {searchMode === 'hybrid' && <Zap className="w-5 h-5 text-blue-600 mr-2 flex-shrink-0" />}
                      {searchMode === 'ocr' && <Eye className="w-5 h-5 text-green-600 mr-2 flex-shrink-0" />}
                      {searchMode === 'regular' && <Search className="w-5 h-5 text-gray-600 mr-2 flex-shrink-0" />}

                      <span className="text-purple-900 font-medium text-sm sm:text-base">
                        {searchMode === 'semantic' && `🧠 AI found ${aiDocuments.length} semantically relevant results`}
                        {searchMode === 'hybrid' && `⚡ Hybrid search found ${aiDocuments.length} results`}
                        {searchMode === 'ocr' && `👁️ Found ${aiDocuments.length} documents with OCR text matching`}
                        {searchMode === 'regular' && `🔍 Found ${aiDocuments.length} results`}
                        {` for "${searchQuery}"`}
                      </span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Badge variant="outline" className="text-xs">
                        {searchMode.charAt(0).toUpperCase() + searchMode.slice(1)} Mode
                      </Badge>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={clearAISearch}
                        className="text-purple-700 border-purple-300"
                      >
                        <X className="w-4 h-4 mr-1" />
                        <span className="hidden sm:inline">Clear Search</span>
                        <span className="sm:hidden">Clear</span>
                      </Button>
                    </div>
                  </div>
                </div>
              )}

              {error && (
                <Card className="mb-6 border-red-200 bg-red-50">
                  <CardContent className="p-4 flex flex-col sm:flex-row sm:items-center space-y-2 sm:space-y-0">
                    <AlertCircle className="w-5 h-5 text-red-500 mr-2 flex-shrink-0" />
                    <span className="text-red-700 flex-1">{error}</span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => loadDocuments(currentPage, isAiSearch)}
                      className="w-full sm:w-auto sm:ml-auto"
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
                    {aiLoading ? `🤖 ${searchMode.toUpperCase()} search in progress...` : 'Loading documents...'}
                  </span>
                </div>
              ) : currentDocuments.length === 0 ? (
                <div className="text-center py-12">
                  <File className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-500 text-lg mb-2">
                    {searchQuery ? 'No documents match your search' : 'No documents found'}
                  </p>
                  <p className="text-gray-400 text-sm sm:text-base">
                    {isAiSearch
                      ? searchMode === 'ocr'
                        ? 'Try uploading images with text or use a different search mode'
                        : 'Try different keywords or generate embeddings first'
                      : activeTab === 'my'
                        ? 'Upload your first document to get started'
                        : 'Documents will appear here when available'
                    }
                  </p>
                </div>
              ) : (
                <>
                  {/* ✅ Responsive grid that adapts to container width */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-4 sm:gap-6">
                    {currentDocuments.map((doc) => {
                      const IconComponent = getFileIcon(doc);
                      return (
                        <Card
                          key={doc.id}
                          className={`bg-white border rounded-xl hover:shadow-lg hover:scale-105 transition-all duration-300 cursor-pointer group ${(doc as any).hasOcr
                            ? 'border-green-200 shadow-green-50'
                            : isAiSearch && (doc as any).aiScore
                              ? 'border-purple-200 shadow-purple-100'
                              : 'border-gray-200'
                            }`}
                          onClick={() => handleDocumentClick(doc.id)}
                        >
                          <CardContent className="p-4 sm:p-6">
                            <div className="flex flex-col items-center text-center space-y-3 sm:space-y-4">
                              {/* ✅ UPDATED: Badges Section with Delete Button */}
                              <div className="w-full flex justify-between items-start mb-2">
                                <div className="flex flex-wrap gap-1">
                                  {/* AI Score Badge */}
                                  {isAiSearch && (doc as any).aiScore && (
                                    <Badge className="bg-purple-100 text-purple-700 border-purple-200 text-xs">
                                      <Sparkles className="w-3 h-3 mr-1" />
                                      {((doc as any).aiScore * 100).toFixed(0)}% match
                                    </Badge>
                                  )}

                                  {/* OCR Badge */}
                                  {(doc as any).hasOcr && (
                                    <Badge className="bg-green-100 text-green-700 border-green-200 text-xs">
                                      <Eye className="w-3 h-3 mr-1" />
                                      {(doc as any).ocrConfidence ?
                                        `${Math.round((doc as any).ocrConfidence * 100)}% OCR` :
                                        'OCR ✓'
                                      }
                                    </Badge>
                                  )}

                                  {/* AI Ready Badge */}
                                  {(doc as any).embeddingGenerated && (
                                    <Badge className="bg-blue-100 text-blue-700 border-blue-200 text-xs">
                                      <Brain className="w-3 h-3 mr-1" />
                                      AI Ready
                                    </Badge>
                                  )}
                                </div>

                                {/* ✅ NEW: Delete Button (Top-Right Corner) */}
                                <div className="opacity-0 group-hover:opacity-100 transition-opacity">
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={(e) => {
                                      e.stopPropagation(); // Prevent card click
                                      openDeleteModal(doc);
                                    }}
                                    className="h-8 w-8 p-0 hover:bg-red-50"
                                    title={`Delete ${doc.originalFilename}`}
                                  >
                                    <Trash2 className="w-4 h-4 text-red-600" />
                                  </Button>
                                </div>
                              </div>

                              {/* File Icon with OCR indication */}
                              <div className={`w-12 h-12 sm:w-16 sm:h-16 rounded-lg flex items-center justify-center transition-colors duration-200 relative ${(doc as any).hasOcr
                                ? 'bg-green-50 group-hover:bg-green-100'
                                : isAiSearch
                                  ? 'bg-purple-50 group-hover:bg-purple-100'
                                  : 'bg-gray-100 group-hover:bg-blue-50'
                                }`}>
                                <IconComponent className={`w-6 h-6 sm:w-8 sm:h-8 transition-colors duration-200 ${(doc as any).hasOcr
                                  ? 'text-green-600 group-hover:text-green-700'
                                  : isAiSearch
                                    ? 'text-purple-600 group-hover:text-purple-700'
                                    : 'text-gray-600 group-hover:text-blue-600'
                                  }`} />

                                {/* OCR Indicator Dot */}
                                {(doc as any).hasOcr && (
                                  <div className="absolute -bottom-1 -right-1 w-3 h-3 sm:w-4 sm:h-4 bg-green-500 rounded-full flex items-center justify-center">
                                    <Eye className="w-1.5 h-1.5 sm:w-2 sm:h-2 text-white" />
                                  </div>
                                )}
                              </div>

                              {/* Document Info */}
                              <div className="space-y-2 w-full">
                                <h3 className="font-medium text-gray-900 text-xs sm:text-sm leading-tight line-clamp-2">
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

                                {/* OCR Text Preview (for OCR searches) */}
                                {searchMode === 'ocr' && (doc as any).ocrText && searchQuery && (
                                  <div className="text-xs text-gray-600 bg-gray-50 p-2 rounded border-l-2 border-green-400 mt-2">
                                    <div className="font-medium text-green-700 mb-1">📄 OCR Match:</div>
                                    <div className="line-clamp-2">
                                      {((doc as any).ocrText as string)
                                        .substring(0, 100)
                                        .replace(new RegExp(searchQuery, 'gi'), `**${searchQuery}**`)}
                                      {(doc as any).ocrText.length > 100 && '...'}
                                    </div>
                                  </div>
                                )}

                                {/* ✅ UPDATED: Action Buttons (Download + Delete) */}
                                <div className="flex justify-center items-center gap-2 pt-2">
                                  <Button
                                    size="sm"
                                    variant="ghost"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleDownload(doc);
                                    }}
                                    className="opacity-0 group-hover:opacity-100 transition-opacity"
                                    title="Download document"
                                  >
                                    <Download className="w-4 h-4" />
                                  </Button>
                                  <Button
                                    size="sm"
                                    variant="ghost"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      openDeleteModal(doc);
                                    }}
                                    className="opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-50"
                                    title="Delete document"
                                  >
                                    <Trash2 className="w-4 h-4 text-red-600" />
                                  </Button>
                                </div>
                              </div>
                            </div>
                          </CardContent>
                        </Card>
                      );
                    })}
                  </div>


                  {/* Only show pagination for regular search */}
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

      <DeleteConfirmationModal
  isOpen={deleteModalOpen}
  onClose={closeDeleteModal}
  onConfirm={handleDeleteDocument}
  isDeleting={isDeleting}
  itemName={documentToDelete?.originalFilename || ''}
  itemType="document"
/>


      {/* Upload Modal */}
      <DocumentUploadModal
        isOpen={uploadModalOpen}
        onClose={() => setUploadModalOpen(false)}
        onUploadSuccess={handleUploadSuccess}
      />
    </div>
  );
}
