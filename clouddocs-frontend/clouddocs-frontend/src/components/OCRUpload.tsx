import React, { useState } from 'react';
import { Upload, FileImage, Brain, Check, AlertCircle } from 'lucide-react';

interface OCRResult {
  extractedText: string;
  confidence: number;
  processingTimeMs: number;
  filename: string;
  success: boolean;
  errorMessage?: string;
}

const OCRUpload: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [ocrResult, setOcrResult] = useState<OCRResult | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  
  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (selectedFile) {
      // Validate file type
      if (selectedFile.type.startsWith('image/')) {
        setFile(selectedFile);
        setOcrResult(null);
      } else {
        alert('Please select an image file (JPEG, PNG, BMP, TIFF, GIF)');
      }
    }
  };
  
  const extractText = async () => {
    if (!file) return;
    
    setIsProcessing(true);
    
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await fetch('/api/ocr/extract', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: formData
      });
      
      const result: OCRResult = await response.json();
      setOcrResult(result);
      
    } catch (error) {
      console.error('OCR extraction failed:', error);
      setOcrResult({
        extractedText: '',
        confidence: 0,
        processingTimeMs: 0,
        filename: file.name,
        success: false,
        errorMessage: 'OCR processing failed'
      });
    } finally {
      setIsProcessing(false);
    }
  };
  
  const uploadDocument = async () => {
    if (!file) return;
    
    setIsProcessing(true);
    
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('description', description);
      formData.append('category', category);
      
      const response = await fetch('/api/ocr/upload', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: formData
      });
      
      if (response.ok) {
        const document = await response.json();
        alert('Document uploaded with OCR processing!');
        // Reset form
        setFile(null);
        setOcrResult(null);
        setDescription('');
        setCategory('');
      } else {
        alert('Upload failed');
      }
      
    } catch (error) {
      console.error('Upload failed:', error);
      alert('Upload failed');
    } finally {
      setIsProcessing(false);
    }
  };
  
  return (
    <div className="max-w-2xl mx-auto p-6 bg-white rounded-lg shadow-lg">
      <div className="mb-6">
        <h3 className="text-xl font-bold text-gray-900 flex items-center">
          <FileImage className="mr-2 h-6 w-6 text-blue-600" />
          📖 AI-Powered OCR Upload
        </h3>
        <p className="text-gray-600 mt-2">
          Upload scanned documents or images - our AI will extract text and make them searchable!
        </p>
      </div>
      
      {/* File Upload */}
      <div className="mb-6">
        <label className="flex flex-col items-center justify-center w-full h-32 border-2 border-gray-300 border-dashed rounded-lg cursor-pointer bg-gray-50 hover:bg-gray-100">
          <div className="flex flex-col items-center justify-center pt-5 pb-6">
            <Upload className="w-8 h-8 mb-4 text-gray-500" />
            <p className="mb-2 text-sm text-gray-500">
              <span className="font-semibold">Click to upload</span> or drag and drop
            </p>
            <p className="text-xs text-gray-500">JPEG, PNG, BMP, TIFF, GIF</p>
          </div>
          <input 
            type="file" 
            className="hidden" 
            accept="image/*"
            onChange={handleFileSelect}
          />
        </label>
        
        {file && (
          <div className="mt-4 p-4 bg-blue-50 rounded-lg">
            <p className="text-sm font-medium text-blue-900">Selected: {file.name}</p>
            <p className="text-xs text-blue-700">Size: {(file.size / 1024 / 1024).toFixed(2)} MB</p>
          </div>
        )}
      </div>
      
      {/* OCR Preview */}
      {file && (
        <div className="mb-6">
          <button
            onClick={extractText}
            disabled={isProcessing}
            className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            <Brain className="mr-2 h-4 w-4" />
            {isProcessing ? 'Processing...' : '🔍 Preview OCR Text'}
          </button>
        </div>
      )}
      
      {/* OCR Results */}
      {ocrResult && (
        <div className="mb-6 p-4 border rounded-lg">
          <div className="flex items-center mb-3">
            {ocrResult.success ? (
              <Check className="h-5 w-5 text-green-600 mr-2" />
            ) : (
              <AlertCircle className="h-5 w-5 text-red-600 mr-2" />
            )}
            <span className="font-medium">
              OCR Result {ocrResult.success && `(${(ocrResult.confidence * 100).toFixed(1)}% confidence)`}
            </span>
          </div>
          
          {ocrResult.success ? (
            <div>
              <div className="mb-2 text-sm text-gray-600">
                Processed in {ocrResult.processingTimeMs}ms • {ocrResult.extractedText.length} characters
              </div>
              <textarea
                value={ocrResult.extractedText}
                readOnly
                rows={6}
                className="w-full p-3 border border-gray-300 rounded-lg bg-gray-50 text-sm"
                placeholder="Extracted text will appear here..."
              />
            </div>
          ) : (
            <p className="text-red-600 text-sm">{ocrResult.errorMessage}</p>
          )}
        </div>
      )}
      
      {/* Document Metadata */}
      {file && (
        <div className="space-y-4 mb-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Brief description of the document..."
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Category
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">Select category...</option>
              <option value="Identity Documents">Identity Documents</option>
              <option value="Financial Documents">Financial Documents</option>
              <option value="Medical Documents">Medical Documents</option>
              <option value="Legal Documents">Legal Documents</option>
              <option value="Tax Documents">Tax Documents</option>
              <option value="Other">Other</option>
            </select>
          </div>
        </div>
      )}
      
      {/* Upload Button */}
      {file && (
        <button
          onClick={uploadDocument}
          disabled={isProcessing}
          className="w-full flex items-center justify-center px-4 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 font-medium"
        >
          <Upload className="mr-2 h-5 w-5" />
          {isProcessing ? 'Uploading...' : '🚀 Upload with AI Processing'}
        </button>
      )}
    </div>
  );
};

export default OCRUpload;
