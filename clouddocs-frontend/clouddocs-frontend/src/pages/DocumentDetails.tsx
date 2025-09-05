import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Download, Share, Edit, Trash2, AlertCircle, RefreshCw } from "lucide-react";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import { Card, CardContent } from "../components/ui/card";
import Sidebar from '../components/layout/Sidebar';
import ShareModal from '../components/ShareModal';
import EditMetadataModal from '../components/EditMetadataModal';
import documentService, { Document } from '../services/documentService';

export default function DocumentDetails() {
  const { documentId } = useParams<{ documentId: string }>();
  const navigate = useNavigate();

  const [document, setDocument] = useState<Document | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [showShareModal, setShowShareModal] = useState(false);
  const [showEditMetadataModal, setShowEditMetadataModal] = useState(false);
  const [updating, setUpdating] = useState(false);

  // Load document details
  useEffect(() => {
    if (documentId) {
      loadDocument(parseInt(documentId));
    }
  }, [documentId]);

  const loadDocument = async (id: number) => {
    try {
      setLoading(true);
      setError('');
      const doc = await documentService.getDocumentById(id);
      setDocument(doc);
    } catch (err: any) {
      console.error('Error loading document:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async () => {
    if (!document) return;

    try {
      await documentService.downloadDocument(document.id, document.originalFilename);
    } catch (err: any) {
      console.error('Download failed:', err);
    }
  };

  const handleDelete = async () => {
    if (!document || !window.confirm('Are you sure you want to delete this document? This action cannot be undone.')) {
      return;
    }

    try {
      setDeleting(true);
      await documentService.deleteDocument(document.id);
      navigate('/documents');
    } catch (err: any) {
      console.error('Delete failed:', err);
      alert('Failed to delete document: ' + err.message);
    } finally {
      setDeleting(false);
    }
  };

  const handleShare = () => {
    setShowShareModal(true);
  };

  // Edit Metadata handler  
  const handleEditMetadata = () => {
    setShowEditMetadataModal(true);
  };

  // Update metadata handler
  const handleUpdateMetadata = async (updatedMetadata: any) => {
    if (!document) return;

    try {
      setUpdating(true);
      await documentService.updateDocumentMetadata(document.id, updatedMetadata);

      // Reload document to show updated metadata
      await loadDocument(document.id);
      setShowEditMetadataModal(false);

      // Show success message
      alert('Metadata updated successfully!');
    } catch (err: any) {
      console.error('Failed to update metadata:', err);
      alert('Failed to update metadata: ' + err.message);
    } finally {
      setUpdating(false);
    }
  };

  const handleBackToDocuments = () => {
    navigate('/documents');
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return 'bg-green-100 text-green-800 hover:bg-green-100';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800 hover:bg-yellow-100';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 hover:bg-red-100';
      case 'IN_REVIEW':
        return 'bg-blue-100 text-blue-800 hover:bg-blue-100';
      default:
        return 'bg-gray-100 text-gray-800 hover:bg-gray-100';
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <Sidebar />
        <main className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <RefreshCw className="w-8 h-8 animate-spin text-blue-600 mx-auto mb-4" />
            <p className="text-gray-600">Loading document...</p>
          </div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <Sidebar />
        <main className="flex-1 flex items-center justify-center">
          <Card className="w-96 bg-white">
            <CardContent className="p-6 text-center">
              <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Error Loading Document</h3>
              <p className="text-gray-600 mb-4">{error}</p>
              <div className="space-x-2">
                <Button onClick={() => documentId && loadDocument(parseInt(documentId))}>
                  Try Again
                </Button>
                <Button variant="outline" onClick={handleBackToDocuments}>
                  Back to Documents
                </Button>
              </div>
            </CardContent>
          </Card>
        </main>
      </div>
    );
  }

  if (!document) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <Sidebar />
        <main className="flex-1 flex items-center justify-center">
          <p className="text-gray-500">Document not found</p>
        </main>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <Sidebar />

      <main className="flex-1 flex flex-col">
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-8 py-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Button
                variant="ghost"
                size="sm"
                className="p-2"
                onClick={handleBackToDocuments}
              >
                <ArrowLeft className="w-4 h-4" />
              </Button>
              <div>
                <h1 className="text-2xl font-semibold text-gray-900">{document.originalFilename}</h1>
                <p className="text-sm text-gray-500 mt-1">Document ID: {document.id}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Button
                onClick={handleDownload}
                className="bg-blue-600 hover:bg-blue-700 transition-colors duration-200"
              >
                <Download className="w-4 h-4 mr-2" />
                Download
              </Button>
              <Button
                variant="outline"
                className="transition-colors duration-200"
                onClick={handleShare} // ✅ Add click handler
              >
                <Share className="w-4 h-4 mr-2" />
                Share
              </Button>

              <Button
                variant="outline"
                className="transition-colors duration-200"
                onClick={handleEditMetadata} // ✅ Add click handler
              >
                <Edit className="w-4 h-4 mr-2" />
                Edit Metadata
              </Button>

              <Button
                variant="destructive"
                onClick={handleDelete}
                disabled={deleting}
                className="transition-colors duration-200"
              >
                <Trash2 className="w-4 h-4 mr-2" />
                {deleting ? 'Deleting...' : 'Delete'}
              </Button>
            </div>
          </div>
        </header>

        {/* Document Metadata */}
        <section className="bg-white border-b border-gray-200 px-8 py-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            <div>
              <span className="text-sm text-gray-500">Status</span>
              <div className="mt-1">
                <Badge className={getStatusColor(document.status)}>
                  {document.status}
                </Badge>
              </div>
            </div>
            <div>
              <span className="text-sm text-gray-500">Version</span>
              <div className="mt-1 font-medium text-gray-900">{document.versionNumber}</div>
            </div>
            <div>
              <span className="text-sm text-gray-500">Uploaded By</span>
              <div className="mt-1 font-medium text-gray-900">{document.uploadedByName}</div>
            </div>
            <div>
              <span className="text-sm text-gray-500">Upload Date</span>
              <div className="mt-1 font-medium text-gray-900">
                {new Date(document.uploadDate).toLocaleDateString()}
              </div>
            </div>
            <div>
              <span className="text-sm text-gray-500">File Size</span>
              <div className="mt-1 font-medium text-gray-900">{document.formattedFileSize}</div>
            </div>
            <div>
              <span className="text-sm text-gray-500">File Type</span>
              <div className="mt-1 font-medium text-gray-900">{document.mimeType}</div>
            </div>
            <div>
              <span className="text-sm text-gray-500">Category</span>
              <div className="mt-1 font-medium text-gray-900">{document.category || 'None'}</div>
            </div>
            <div>
              <span className="text-sm text-gray-500">Downloads</span>
              <div className="mt-1 font-medium text-gray-900">{document.downloadCount}</div>
            </div>
          </div>

          {document.description && (
            <div className="mt-6">
              <span className="text-sm text-gray-500">Description</span>
              <div className="mt-1 text-gray-900">{document.description}</div>
            </div>
          )}

          {document.tags && document.tags.length > 0 && (
            <div className="mt-4">
              <span className="text-sm text-gray-500">Tags</span>
              <div className="mt-1 flex flex-wrap gap-2">
                {document.tags.map((tag, index) => (
                  <Badge key={index} variant="secondary" className="text-xs">
                    {tag}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          {document.rejectionReason && (
            <div className="mt-4">
              <span className="text-sm text-gray-500">Rejection Reason</span>
              <div className="mt-1 text-red-600">{document.rejectionReason}</div>
            </div>
          )}

          {document.approvedByName && document.approvalDate && (
            <div className="mt-4">
              <span className="text-sm text-gray-500">Approved By</span>
              <div className="mt-1 text-gray-900">
                {document.approvedByName} on {new Date(document.approvalDate).toLocaleDateString()}
              </div>
            </div>
          )}
        </section>

        {/* Document Preview Area */}
        <section className="flex-1 p-8">
          <div className="h-full bg-white rounded-lg border-2 border-dashed border-gray-300 flex flex-col items-center justify-center hover:border-blue-300 transition-colors duration-200">
            <div className="w-16 h-16 bg-blue-100 rounded-lg flex items-center justify-center mb-4">
              <div className="w-8 h-8 text-blue-600">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M14,2H6A2,2 0 0,0 4,4V20A2,2 0 0,0 6,22H18A2,2 0 0,0 20,20V8L14,2M18,20H6V4H13V9H18V20Z" />
                  <path d="M8,12H16V14H8V12M8,16H13V18H8V16M8,8H10V10H8V8Z" />
                </svg>
              </div>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              Document Preview - {document.originalFilename}
            </h3>
            <p className="text-sm text-gray-500 mb-4">Preview functionality coming soon</p>
            <Button onClick={handleDownload} variant="outline">
              <Download className="w-4 h-4 mr-2" />
              Download to View
            </Button>
          </div>
        </section>
      </main>

      {document && (
  <>
    <ShareModal
      documentId={document.id}
      documentName={document.originalFilename}
      isOpen={showShareModal}
      onClose={() => setShowShareModal(false)}
    />
    
    <EditMetadataModal
      document={document}
      isOpen={showEditMetadataModal}
      onClose={() => setShowEditMetadataModal(false)}
      onSave={handleUpdateMetadata}
      updating={updating}
    />
  </>
)}
    </div>
  );
}
