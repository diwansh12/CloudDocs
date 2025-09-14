// src/components/OCRUpload.tsx
import React, { useState } from 'react';
import { Upload, FileImage, Brain, Check, AlertCircle, Info, ArrowRight, Search, FileText } from 'lucide-react';
import ocrService from '../services/ocrService';

interface OCRResult {
  extractedText?: string;
  confidence?: number;
  processingTimeMs?: number;
  filename?: string;
  success: boolean;
  message?: string;
  reason?: string;
  alternative?: string;
  errorMessage?: string;
}

const OCRUpload: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [ocrResult, setOcrResult] = useState<OCRResult | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [message, setMessage] = useState<string>('');
  const [messageType, setMessageType] = useState<'success' | 'error' | 'info' | 'warning'>('info');
  
  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (selectedFile) {
      // Validate file type
      if (selectedFile.type.startsWith('image/')) {
        setFile(selectedFile);
        setOcrResult(null);
        setMessage('');
      } else {
        setMessage('Please select an image file (JPEG, PNG, BMP, TIFF, GIF)');
        setMessageType('error');
      }
    }
  };
  
  const extractText = async () => {
    if (!file) return;

    // Validate file first
    const validation = ocrService.validateFileForOCR(file);
    if (!validation.valid) {
      setMessage(validation.error || 'Invalid file selected');
      setMessageType('error');
      return;
    }

    setIsProcessing(true);
    setMessage('Processing your image...');
    setMessageType('info');

    try {
      const result = await ocrService.extractText(file);
      setOcrResult(result);
      
      if (result.success) {
        // OCR succeeded (if ever re-enabled)
        setMessage(`Text extracted successfully! Confidence: ${ocrService.formatConfidence(result.confidence || 0)}`);
        setMessageType('success');
        console.log('✅ OCR Success:', {
          text: result.extractedText,
          confidence: ocrService.formatConfidence(result.confidence || 0),
          processingTime: `${result.processingTimeMs}ms`
        });
      } else {
        // OCR is disabled - show helpful message
        const disabledMessage = `🚫 OCR Feature Currently Unavailable

${result.message || 'OCR processing is temporarily disabled'}

${result.reason ? `Reason: ${result.reason}` : 'Feature optimization for better performance'}

💡 Don't worry! You can still:
• Upload your document using regular document upload
• Use AI-powered semantic search to find content
• Organize and manage your documents effectively

${result.alternative || 'All other features remain fully functional'}`;

        setMessage(disabledMessage);
        setMessageType('warning');
      }
    } catch (error) {
      console.error('❌ OCR Error:', error);
      setOcrResult({
        extractedText: '',
        confidence: 0,
        processingTimeMs: 0,
        filename: file.name,
        success: false,
        errorMessage: error instanceof Error ? error.message : 'OCR processing failed'
      });
      
      setMessage(`❌ Processing Failed

${error instanceof Error ? error.message : 'OCR processing encountered an error'}

💡 No problem! You can:
• Upload this document normally via document upload
• Search existing documents with AI
• Try again with a different image`);
      setMessageType('error');
    } finally {
      setIsProcessing(false);
    }
  };

  const uploadDocument = async () => {
    if (!file) return;

    setIsProcessing(true);
    setMessage('Uploading document...');
    setMessageType('info');

    try {
      const document = await ocrService.uploadDocumentWithOCR(file, description, category);
      
      if (document.success) {
        setMessage('🎉 Document uploaded successfully!');
        setMessageType('success');
        console.log('✅ Upload Success:', document);
        
        // Reset form after success
        setTimeout(() => {
          setFile(null);
          setOcrResult(null);
          setDescription('');
          setCategory('');
          setMessage('');
        }, 3000);
      } else {
        // OCR upload is disabled
        const uploadDisabledMessage = `🚫 OCR Upload Currently Unavailable

${document.message || 'OCR document upload is temporarily disabled'}

${document.reason ? `Reason: ${document.reason}` : 'Feature optimization in progress'}

💡 Alternative Options:
• Use regular document upload (all features work)
• Your document will still be stored securely
• AI search will still work on uploaded content

${document.alternative || 'All other features remain available'}`;

        setMessage(uploadDisabledMessage);
        setMessageType('warning');
      }
    } catch (error) {
      console.error('❌ Upload Error:', error);
      setMessage(`Upload failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
      setMessageType('error');
    } finally {
      setIsProcessing(false);
    }
  };

  const getMessageIcon = () => {
    switch (messageType) {
      case 'success': return <Check className="h-5 w-5 text-green-600" />;
      case 'error': return <AlertCircle className="h-5 w-5 text-red-600" />;
      case 'warning': return <Info className="h-5 w-5 text-yellow-600" />;
      default: return <Info className="h-5 w-5 text-blue-600" />;
    }
  };

  const navigateToDocumentUpload = () => {
    // Navigate to regular document upload
    window.location.href = '/documents/upload';
  };

  const navigateToSearch = () => {
    // Navigate to search page
    window.location.href = '/search';
  };
  
  return (
    <div className="max-w-2xl mx-auto p-6 bg-white rounded-lg shadow-lg">
      <div className="mb-6">
        <h3 className="text-xl font-bold text-gray-900 flex items-center">
          <FileImage className="mr-2 h-6 w-6 text-blue-600" />
          📖 OCR Text Extraction
        </h3>
        <p className="text-gray-600 mt-2">
          Extract text from scanned documents and images using AI-powered OCR technology.
        </p>
      </div>
      
      {/* File Upload */}
      <div className="mb-6">
        <label className="flex flex-col items-center justify-center w-full h-32 border-2 border-gray-300 border-dashed rounded-lg cursor-pointer bg-gray-50 hover:bg-gray-100 transition-colors">
          <div className="flex flex-col items-center justify-center pt-5 pb-6">
            <Upload className="w-8 h-8 mb-4 text-gray-500" />
            <p className="mb-2 text-sm text-gray-500">
              <span className="font-semibold">Click to upload</span> or drag and drop
            </p>
            <p className="text-xs text-gray-500">JPEG, PNG, BMP, TIFF, GIF (Max 500KB)</p>
          </div>
          <input 
            type="file" 
            className="hidden" 
            accept="image/*"
            onChange={handleFileSelect}
            disabled={isProcessing}
          />
        </label>
        
        {file && (
          <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <div className="flex items-center">
              <FileImage className="h-8 w-8 text-blue-600 mr-3" />
              <div>
                <p className="text-sm font-medium text-blue-900">{file.name}</p>
                <p className="text-xs text-blue-700">
                  Size: {(file.size / 1024 / 1024).toFixed(2)} MB • Type: {file.type}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
      
      {/* OCR Preview */}
      {file && (
        <div className="mb-6">
          <button
            onClick={extractText}
            disabled={isProcessing}
            className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <Brain className="mr-2 h-4 w-4" />
            {isProcessing ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent mr-2"></div>
                Processing...
              </>
            ) : (
              '🔍 Preview OCR Text'
            )}
          </button>
        </div>
      )}

      {/* Message Display */}
      {message && (
        <div className={`message-container ${messageType} mb-6`}>
          <div className="flex items-start">
            {getMessageIcon()}
            <div className="ml-3 flex-1">
              <pre className="message-text text-sm whitespace-pre-line">{message}</pre>
            </div>
          </div>
        </div>
      )}
      
      {/* OCR Results (if successful) */}
      {ocrResult && ocrResult.success && ocrResult.extractedText && (
        <div className="mb-6 p-4 border border-green-200 rounded-lg bg-green-50">
          <div className="flex items-center mb-3">
            <Check className="h-5 w-5 text-green-600 mr-2" />
            <span className="font-medium text-green-800">
              OCR Result ({((ocrResult.confidence || 0) * 100).toFixed(1)}% confidence)
            </span>
          </div>
          
          <div className="mb-2 text-sm text-green-700">
            Processed in {ocrResult.processingTimeMs}ms • {ocrResult.extractedText.length} characters
          </div>
          <textarea
            value={ocrResult.extractedText}
            readOnly
            rows={6}
            className="w-full p-3 border border-green-300 rounded-lg bg-white text-sm"
            placeholder="Extracted text will appear here..."
          />
        </div>
      )}

      {/* Alternative Actions (when OCR is disabled) */}
      {(messageType === 'warning' || (messageType === 'error' && message.includes('Processing Failed'))) && (
        <div className="alternative-actions mb-6">
          <h4 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <ArrowRight className="h-5 w-5 mr-2 text-blue-600" />
            💡 Alternative Actions
          </h4>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <button
              onClick={navigateToDocumentUpload}
              className="alt-button bg-green-600 hover:bg-green-700"
            >
              <FileText className="h-5 w-5 mr-2" />
              📤 Upload Document Normally
              <p className="text-sm mt-1 opacity-90">Store your document securely and make it searchable</p>
            </button>
            
            <button
              onClick={navigateToSearch}
              className="alt-button bg-blue-600 hover:bg-blue-700"
            >
              <Search className="h-5 w-5 mr-2" />
              🔍 Search Existing Documents
              <p className="text-sm mt-1 opacity-90">Find content with AI-powered semantic search</p>
            </button>
          </div>
        </div>
      )}
      
      {/* Document Metadata */}
      {file && (
        <div className="space-y-4 mb-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              📝 Description
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Brief description of the document..."
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 transition-colors"
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              🏷️ Category
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 transition-colors"
            >
              <option value="">Select category...</option>
              <option value="Identity Documents">📋 Identity Documents</option>
              <option value="Financial Documents">💰 Financial Documents</option>
              <option value="Medical Documents">🏥 Medical Documents</option>
              <option value="Legal Documents">⚖️ Legal Documents</option>
              <option value="Tax Documents">📊 Tax Documents</option>
              <option value="Other">📎 Other</option>
            </select>
          </div>
        </div>
      )}
      
      {/* Upload Button */}
      {file && (
        <button
          onClick={uploadDocument}
          disabled={isProcessing}
          className="w-full flex items-center justify-center px-4 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed font-medium transition-colors"
        >
          <Upload className="mr-2 h-5 w-5" />
          {isProcessing ? (
            <>
              <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent mr-2"></div>
              Uploading...
            </>
          ) : (
            '🚀 Upload with Processing'
          )}
        </button>
      )}

      {/* Information Box */}
      <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
        <h4 className="font-medium text-blue-900 mb-2">ℹ️ About OCR Processing:</h4>
        <p className="text-sm text-blue-700">
          OCR (Optical Character Recognition) extracts text from images and scanned documents. 
          While this feature may be temporarily unavailable, all other document management 
          and AI search features remain fully functional.
        </p>
      </div>
    </div>
  );
};

export default OCRUpload;
