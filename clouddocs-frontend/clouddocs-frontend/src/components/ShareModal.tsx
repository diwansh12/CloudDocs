import { useState, useEffect } from 'react';
import { X, Copy, Link, Calendar, Download, Lock, Shield } from 'lucide-react';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import documentService from '../services/documentService';

interface ShareModalProps {
  documentId: number;
  documentName: string;
  isOpen: boolean;
  onClose: () => void;
}

export default function ShareModal({ documentId, documentName, isOpen, onClose }: ShareModalProps) {
  const [shareUrl, setShareUrl] = useState('');
  const [expiryHours, setExpiryHours] = useState(24);
  const [allowDownload, setAllowDownload] = useState(true);
  const [password, setPassword] = useState('');
  const [generating, setGenerating] = useState(false);
  const [copied, setCopied] = useState(false);

  const generateLink = async () => {
    try {
      setGenerating(true);
      const result = await documentService.generateShareLink(documentId, {
        expiryHours,
        allowDownload,
        password: password || undefined
      });
      setShareUrl(result.shareUrl);
    } catch (error) {
      alert('Failed to generate share link');
    } finally {
      setGenerating(false);
    }
  };

  const copyToClipboard = async () => {
    if (shareUrl) {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-lg mx-auto bg-white shadow-2xl border-0 rounded-xl overflow-hidden">
        <CardContent className="p-0">
          {/* Header */}
          <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4 text-white">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="bg-white bg-opacity-20 p-2 rounded-lg">
                  <Link className="w-5 h-5" />
                </div>
                <h3 className="text-xl font-semibold">Share Document</h3>
              </div>
              <Button 
                variant="ghost" 
                size="sm" 
                onClick={onClose}
                className="text-white hover:bg-white hover:bg-opacity-20 rounded-full p-2"
              >
                <X className="w-5 h-5" />
              </Button>
            </div>
          </div>

          {/* Content */}
          <div className="p-6 space-y-6">
            {/* Document Info */}
            <div className="bg-gray-50 p-4 rounded-lg border border-gray-200">
              <p className="text-sm text-gray-600 font-medium mb-1">Document</p>
              <p className="text-gray-900 font-semibold truncate">{documentName}</p>
            </div>

            {/* Share Options */}
            <div className="space-y-5">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  <Calendar className="inline w-4 h-4 mr-2" />
                  Expires in
                </label>
                <select 
                  value={expiryHours} 
                  onChange={(e) => setExpiryHours(Number(e.target.value))}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-900 transition-colors"
                >
                  <option value={1}>1 hour</option>
                  <option value={24}>24 hours</option>
                  <option value={168}>1 week</option>
                  <option value={720}>30 days</option>
                </select>
              </div>

              <div className="bg-gray-50 p-4 rounded-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <Download className="w-5 h-5 text-gray-600" />
                    <div>
                      <p className="font-medium text-gray-900">Allow downloads</p>
                      <p className="text-sm text-gray-500">Recipients can download the file</p>
                    </div>
                  </div>
                  <div className="relative">
                    <input 
                      type="checkbox" 
                      checked={allowDownload}
                      onChange={(e) => setAllowDownload(e.target.checked)}
                      className="sr-only"
                      id="allowDownload"
                    />
                    <label
                      htmlFor="allowDownload"
                      className={`block w-12 h-7 rounded-full cursor-pointer transition-colors ${
                        allowDownload ? 'bg-blue-600' : 'bg-gray-300'
                      }`}
                    >
                      <div
                        className={`w-5 h-5 bg-white rounded-full shadow-md transform transition-transform ${
                          allowDownload ? 'translate-x-6' : 'translate-x-1'
                        } mt-1`}
                      ></div>
                    </label>
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  <Shield className="inline w-4 h-4 mr-2" />
                  Password protection (optional)
                </label>
                <input 
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Leave empty for no password"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                />
                <p className="text-xs text-gray-500 mt-1">Add a password for extra security</p>
              </div>
            </div>

            {/* Generate Button */}
            <Button 
              onClick={generateLink}
              disabled={generating}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-lg font-semibold transition-colors shadow-lg hover:shadow-xl"
            >
              <Link className="w-5 h-5 mr-2" />
              {generating ? 'Generating...' : 'Generate Share Link'}
            </Button>

            {/* Share URL Result */}
            {shareUrl && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-4 space-y-3">
                <label className="block text-sm font-semibold text-green-800">
                  Share URL Generated Successfully!
                </label>
                <div className="flex space-x-2">
                  <input 
                    type="text" 
                    value={shareUrl}
                    readOnly
                    className="flex-1 px-3 py-2 bg-white border border-green-300 rounded-lg text-sm text-gray-700 focus:outline-none"
                  />
                  <Button 
                    onClick={copyToClipboard}
                    variant="outline"
                    className={`px-4 py-2 rounded-lg font-medium transition-all ${
                      copied 
                        ? 'bg-green-100 text-green-700 border-green-300' 
                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    <Copy className="w-4 h-4 mr-1" />
                    {copied ? 'Copied!' : 'Copy'}
                  </Button>
                </div>
                <p className="text-xs text-green-600">Anyone with this link can access your document</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
