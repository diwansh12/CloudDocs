import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Upload, 
  CheckCircle,
  AlertCircle,
  ArrowUpCircle,
  RefreshCw,
  TrendingUp,
  FileText,
  Users,
  Clock,
  Download,
  Eye,
  Brain, // ✅ NEW: For AI features
  Zap, // ✅ NEW: For search features
  Sparkles, // ✅ NEW: For AI indicators
  Search, // ✅ NEW: For search functionality
  FileImage, // ✅ NEW: For OCR uploads
  BarChart, // ✅ NEW: For analytics
  Target, // ✅ NEW: For accuracy
  Camera // ✅ NEW: For OCR scanning
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Card, CardContent, CardHeader } from '../components/ui/card';
import { Input } from '../components/ui/input';
import Sidebar from '../components/layout/Sidebar';
import DocumentUploadModal from '../components/documents/DocumentUpload';
import dashboardService, { DashboardStats, Document } from '../services/dashboardService';
import documentService from '../services/documentService'; // ✅ NEW: For OCR features

export default function Dashboard() {
  const navigate = useNavigate();
  
  // Existing state management
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentDocuments, setRecentDocuments] = useState<Document[]>([]);
  const [pendingWorkflows, setPendingWorkflows] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  // ✅ NEW: OCR and AI State
  const [ocrStats, setOcrStats] = useState<any>(null);
  const [aiReadyDocuments, setAiReadyDocuments] = useState<Document[]>([]);
  const [quickSearchQuery, setQuickSearchQuery] = useState('');
  const [quickSearchResults, setQuickSearchResults] = useState<Document[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);

  // ✅ ENHANCED: Load dashboard data with OCR
  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError('');

      const [
        statsData, 
        documentsData, 
        workflowsData,
        ocrStatsData, // ✅ NEW
        aiReadyData   // ✅ NEW
      ] = await Promise.all([
        dashboardService.getDashboardStats(),
        dashboardService.getRecentDocuments(4),
        dashboardService.getPendingWorkflows(3),
        documentService.getOCRStatistics().catch(() => null), // ✅ NEW: OCR stats
        documentService.getAIReadyDocuments().catch(() => []).then(docs => docs.slice(0, 3)) // ✅ NEW: AI ready docs
      ]);

      setStats(statsData);
      setRecentDocuments(documentsData);
      setPendingWorkflows(workflowsData);
      setOcrStats(ocrStatsData); // ✅ NEW
      setAiReadyDocuments(aiReadyData); // ✅ NEW
    } catch (err: any) {
      console.error('Error loading dashboard data:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // ✅ NEW: Quick search functionality
  const handleQuickSearch = async (query: string) => {
    if (query.length < 2) {
      setQuickSearchResults([]);
      return;
    }

    setSearchLoading(true);
    try {
      const results = await documentService.semanticSearch(query, 3);
      setQuickSearchResults(results.documents);
    } catch (error) {
      console.error('Quick search failed:', error);
      setQuickSearchResults([]);
    } finally {
      setSearchLoading(false);
    }
  };

  // ✅ NEW: Debounced search
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (quickSearchQuery.trim()) {
        handleQuickSearch(quickSearchQuery);
      } else {
        setQuickSearchResults([]);
      }
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [quickSearchQuery]);

  // Refresh data
  const handleRefresh = async () => {
    setRefreshing(true);
    await loadDashboardData();
    setRefreshing(false);
  };

  // Handle upload success
  const handleUploadSuccess = () => {
    loadDashboardData(); // Refresh data after upload
  };

  // Handle document download
  const handleDownload = async (document: Document) => {
    try {
      const blob = await dashboardService.downloadDocument(document.id);
      const url = window.URL.createObjectURL(blob);
      const link = window.document.createElement('a');
      link.href = url;
      link.download = document.originalFilename;
      window.document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      console.error('Download failed:', err);
    }
  };

  // Load data on component mount
  useEffect(() => {
    loadDashboardData();
  }, []);

  // Utility functions
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'PENDING':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'IN_REVIEW':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric', 
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="h-screen flex overflow-hidden bg-gray-50">
        <Sidebar />
        <main className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <RefreshCw className="w-8 h-8 animate-spin text-blue-600 mx-auto mb-4" />
            <p className="text-gray-600">Loading dashboard...</p>
          </div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-screen flex overflow-hidden bg-gray-50">
        <Sidebar />
        <main className="flex-1 flex items-center justify-center">
          <Card className="w-96 bg-white">
            <CardContent className="p-6 text-center">
              <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Error Loading Dashboard</h3>
              <p className="text-gray-600 mb-4">{error}</p>
              <Button onClick={handleRefresh} disabled={refreshing}>
                {refreshing ? 'Retrying...' : 'Try Again'}
              </Button>
            </CardContent>
          </Card>
        </main>
      </div>
    );
  }

  return (
    <div className="h-screen flex overflow-hidden bg-gray-50">
      <Sidebar />

      <main className="flex-1 flex flex-col">
        {/* ✅ ENHANCED Header with Quick Search */}
        <header className="bg-white border-b border-gray-200 px-8 py-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <h1 className="text-2xl text-gray-900 font-medium">Dashboard</h1>
              <Button
                onClick={handleRefresh}
                variant="outline"
                size="sm"
                disabled={refreshing}
                className="flex items-center"
              >
                <RefreshCw className={`w-4 h-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                Refresh
              </Button>
            </div>
            
            {/* ✅ NEW: Quick Search and Upload */}
            <div className="flex items-center space-x-4">
              {/* Quick Search */}
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                <Input
                  placeholder="Quick AI search..."
                  value={quickSearchQuery}
                  onChange={(e) => setQuickSearchQuery(e.target.value)}
                  className="pl-10 w-64 bg-gray-50"
                />
                {searchLoading && (
                  <RefreshCw className="w-4 h-4 absolute right-3 top-1/2 transform -translate-y-1/2 animate-spin text-gray-400" />
                )}
                
                {/* Quick Search Results */}
                {quickSearchResults.length > 0 && (
                  <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-10">
                    <div className="p-2">
                      {quickSearchResults.map((doc) => (
                        <button
                          key={doc.id}
                          onClick={() => navigate(`/documents/${doc.id}`)}
                          className="w-full text-left p-2 hover:bg-gray-50 rounded text-sm"
                        >
                          <div className="font-medium text-gray-900 truncate">{doc.originalFilename}</div>
                          <div className="text-xs text-gray-500">{doc.category}</div>
                        </button>
                      ))}
                      <div className="border-t mt-2 pt-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => navigate(`/documents?search=${quickSearchQuery}`)}
                          className="w-full text-xs"
                        >
                          View all results →
                        </Button>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              <Button 
                onClick={() => setUploadModalOpen(true)}
                className="bg-blue-900 hover:bg-blue-800 text-white px-6 py-2 rounded-lg text-sm font-medium flex items-center transition-colors duration-200"
              >
                <Upload className="w-4 h-4 mr-2" />
                Upload Document
              </Button>
            </div>
          </div>
        </header>

        {/* Dashboard Content */}
        <div className="flex-1 p-8 overflow-auto">
          {/* ✅ ENHANCED Stats Cards with OCR */}
          {stats && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-6 mb-8">
              {/* Existing Stats */}
              <Card className="bg-white border border-gray-200 rounded-xl hover:shadow-lg transition-shadow">
                <CardContent className="p-6">
                  <div className="flex items-center">
                    <div className="p-2 bg-blue-100 rounded-lg">
                      <FileText className="w-6 h-6 text-blue-600" />
                    </div>
                    <div className="ml-4">
                      <p className="text-sm font-medium text-gray-600">Total Documents</p>
                      <p className="text-2xl font-semibold text-gray-900">{stats.totalDocuments}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card className="bg-white border border-gray-200 rounded-xl hover:shadow-lg transition-shadow">
                <CardContent className="p-6">
                  <div className="flex items-center">
                    <div className="p-2 bg-orange-100 rounded-lg">
                      <Clock className="w-6 h-6 text-orange-600" />
                    </div>
                    <div className="ml-4">
                      <p className="text-sm font-medium text-gray-600">Pending Review</p>
                      <p className="text-2xl font-semibold text-gray-900">{stats.pendingDocuments}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card className="bg-white border border-gray-200 rounded-xl hover:shadow-lg transition-shadow">
                <CardContent className="p-6">
                  <div className="flex items-center">
                    <div className="p-2 bg-green-100 rounded-lg">
                      <CheckCircle className="w-6 h-6 text-green-600" />
                    </div>
                    <div className="ml-4">
                      <p className="text-sm font-medium text-gray-600">Approved</p>
                      <p className="text-2xl font-semibold text-gray-900">{stats.approvedDocuments}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card className="bg-white border border-gray-200 rounded-xl hover:shadow-lg transition-shadow">
                <CardContent className="p-6">
                  <div className="flex items-center">
                    <div className="p-2 bg-purple-100 rounded-lg">
                      <TrendingUp className="w-6 h-6 text-purple-600" />
                    </div>
                    <div className="ml-4">
                      <p className="text-sm font-medium text-gray-600">Recent Uploads</p>
                      <p className="text-2xl font-semibold text-gray-900">{stats.recentUploads}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* ✅ NEW: OCR Stats */}
              {ocrStats && (
                <>
                  <Card className="bg-white border border-green-200 rounded-xl hover:shadow-lg transition-shadow">
                    <CardContent className="p-6">
                      <div className="flex items-center">
                        <div className="p-2 bg-green-100 rounded-lg">
                          <Eye className="w-6 h-6 text-green-600" />
                        </div>
                        <div className="ml-4">
                          <p className="text-sm font-medium text-gray-600">OCR Processed</p>
                          <p className="text-2xl font-semibold text-gray-900">{ocrStats.documentsWithOCR}</p>
                          <p className="text-xs text-green-600">
                            {Math.round(ocrStats.ocrCoverage * 100)}% coverage
                          </p>
                        </div>
                      </div>
                    </CardContent>
                  </Card>

                  <Card className="bg-white border border-purple-200 rounded-xl hover:shadow-lg transition-shadow">
                    <CardContent className="p-6">
                      <div className="flex items-center">
                        <div className="p-2 bg-purple-100 rounded-lg">
                          <Brain className="w-6 h-6 text-purple-600" />
                        </div>
                        <div className="ml-4">
                          <p className="text-sm font-medium text-gray-600">AI Ready</p>
                          <p className="text-2xl font-semibold text-gray-900">{ocrStats.documentsWithEmbeddings}</p>
                          <p className="text-xs text-purple-600">
                            {ocrStats.averageOCRConfidence > 0 && `${Math.round(ocrStats.averageOCRConfidence * 100)}% avg confidence`}
                          </p>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </>
              )}
            </div>
          )}

          {/* ✅ NEW: Quick Actions Section */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
            <Card 
              className="bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 rounded-xl hover:shadow-lg transition-shadow cursor-pointer"
              onClick={() => navigate('/documents?mode=semantic')}
            >
              <CardContent className="p-4 text-center">
                <Brain className="w-8 h-8 text-blue-600 mx-auto mb-2" />
                <h3 className="font-medium text-gray-900">AI Search</h3>
                <p className="text-xs text-gray-600">Semantic document search</p>
              </CardContent>
            </Card>

            <Card 
              className="bg-gradient-to-r from-green-50 to-blue-50 border border-green-200 rounded-xl hover:shadow-lg transition-shadow cursor-pointer"
              onClick={() => navigate('/ocr-upload')}
            >
              <CardContent className="p-4 text-center">
                <Camera className="w-8 h-8 text-green-600 mx-auto mb-2" />
                <h3 className="font-medium text-gray-900">OCR Upload</h3>
                <p className="text-xs text-gray-600">Extract text from images</p>
              </CardContent>
            </Card>

            <Card 
              className="bg-gradient-to-r from-purple-50 to-pink-50 border border-purple-200 rounded-xl hover:shadow-lg transition-shadow cursor-pointer"
              onClick={() => navigate('/documents?mode=hybrid')}
            >
              <CardContent className="p-4 text-center">
                <Zap className="w-8 h-8 text-purple-600 mx-auto mb-2" />
                <h3 className="font-medium text-gray-900">Hybrid Search</h3>
                <p className="text-xs text-gray-600">AI + Keywords + OCR</p>
              </CardContent>
            </Card>

            <Card 
              className="bg-gradient-to-r from-orange-50 to-red-50 border border-orange-200 rounded-xl hover:shadow-lg transition-shadow cursor-pointer"
              onClick={() => navigate('/analytics')}
            >
              <CardContent className="p-4 text-center">
                <BarChart className="w-8 h-8 text-orange-600 mx-auto mb-2" />
                <h3 className="font-medium text-gray-900">Analytics</h3>
                <p className="text-xs text-gray-600">Performance insights</p>
              </CardContent>
            </Card>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Recent Documents */}
            <div className="lg:col-span-1">
              <Card className="bg-white border border-gray-200 rounded-xl h-fit hover:shadow-lg transition-shadow duration-300">
                <CardHeader className="pb-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-lg font-medium text-gray-900">Recent Documents</h3>
                    <Button 
                      onClick={() => navigate('/documents')}
                      variant="ghost"
                      size="sm"
                      className="text-blue-600 hover:text-blue-700"
                    >
                      View All
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {recentDocuments.length > 0 ? (
                    recentDocuments.map((doc) => (
                      <div key={doc.id} className="flex items-center justify-between py-3 border-b border-gray-100 last:border-b-0 hover:bg-gray-50 transition-colors duration-200 rounded px-2">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center mb-1">
                            <h4 className="font-medium text-gray-900 text-sm truncate">
                              {doc.originalFilename}
                            </h4>
                            {/* ✅ NEW: OCR Indicator */}
                            {(doc as any).hasOcr && (
                              <Eye className="w-3 h-3 text-green-600 ml-2 flex-shrink-0" />
                            )}
                            {(doc as any).embeddingGenerated && (
                              <Brain className="w-3 h-3 text-purple-600 ml-1 flex-shrink-0" />
                            )}
                          </div>
                          <p className="text-xs text-gray-500">
                            {doc.uploadedByName} • {formatDate(doc.uploadDate)} • {doc.formattedFileSize}
                          </p>
                          {doc.downloadCount > 0 && (
                            <p className="text-xs text-blue-600">
                              Downloaded {doc.downloadCount} times
                            </p>
                          )}
                        </div>
                        <div className="flex items-center space-x-2 ml-4">
                          <Button
                            onClick={() => handleDownload(doc)}
                            variant="ghost"
                            size="sm"
                            className="p-1 h-auto"
                          >
                            <Download className="w-4 h-4" />
                          </Button>
                          <Badge 
                            variant="outline" 
                            className={`text-xs px-2 py-1 font-medium ${getStatusColor(doc.status)}`}
                          >
                            {doc.status}
                          </Badge>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-8">
                      <FileText className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                      <p className="text-gray-500">No recent documents</p>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>

            {/* ✅ NEW: AI-Ready Documents */}
            <div className="lg:col-span-1">
              <Card className="bg-white border border-purple-200 rounded-xl hover:shadow-lg transition-shadow duration-300">
                <CardHeader className="pb-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-lg font-medium text-gray-900 flex items-center">
                      <Sparkles className="w-5 h-5 text-purple-600 mr-2" />
                      AI-Ready Documents
                    </h3>
                    <Button 
                      onClick={() => navigate('/documents?filter=ai-ready')}
                      variant="ghost"
                      size="sm"
                      className="text-purple-600 hover:text-purple-700"
                    >
                      View All
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {aiReadyDocuments.length > 0 ? (
                    aiReadyDocuments.map((doc) => (
                      <div key={doc.id} className="flex items-center justify-between py-3 border-b border-gray-100 last:border-b-0 hover:bg-purple-50 transition-colors duration-200 rounded px-2">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center mb-1">
                            <h4 className="font-medium text-gray-900 text-sm truncate">
                              {doc.originalFilename}
                            </h4>
                            {(doc as any).hasOcr && (
                              <Badge className="ml-2 bg-green-100 text-green-700 text-xs">
                                OCR {(doc as any).ocrConfidence && `${Math.round((doc as any).ocrConfidence * 100)}%`}
                              </Badge>
                            )}
                          </div>
                          <p className="text-xs text-gray-500">
                            {doc.category} • {formatDate(doc.uploadDate)}
                          </p>
                          <p className="text-xs text-purple-600">
                            Ready for AI search & analysis
                          </p>
                        </div>
                        <div className="flex items-center ml-4">
                          <Brain className="w-4 h-4 text-purple-600" />
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-8">
                      <Brain className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                      <p className="text-gray-500">No AI-ready documents</p>
                      <Button 
                        onClick={() => navigate('/ocr-upload')}
                        size="sm"
                        variant="outline"
                        className="mt-2"
                      >
                        Upload & Process
                      </Button>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>

            {/* Pending Workflows */}
            <div className="lg:col-span-1">
              <Card className="bg-white border border-gray-200 rounded-xl hover:shadow-lg transition-shadow duration-300">
                <CardHeader className="pb-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-lg font-medium text-gray-900">Pending Workflows</h3>
                    <Button 
                      onClick={() => navigate('/documents?status=PENDING')}
                      variant="ghost"
                      size="sm"
                      className="text-blue-600 hover:text-blue-700"
                    >
                      View All
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {pendingWorkflows.length > 0 ? (
                    pendingWorkflows.map((doc) => (
                      <div key={doc.id} className="flex items-center justify-between py-3 border-b border-gray-100 last:border-b-0 hover:bg-gray-50 transition-colors duration-200 rounded px-2">
                        <div className="flex-1 min-w-0">
                          <h4 className="font-medium text-gray-900 text-sm mb-1 truncate">
                            {doc.originalFilename}
                          </h4>
                          <p className="text-xs text-gray-500">
                            {doc.description || 'Awaiting review'} • {formatDate(doc.uploadDate)}
                          </p>
                          {doc.category && (
                            <p className="text-xs text-purple-600">
                              Category: {doc.category}
                            </p>
                          )}
                        </div>
                        <Badge 
                          variant="outline" 
                          className={`text-xs px-2 py-1 font-medium ml-4 ${getStatusColor(doc.status)}`}
                        >
                          {doc.status}
                        </Badge>
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-8">
                      <Clock className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                      <p className="text-gray-500">No pending workflows</p>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
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
