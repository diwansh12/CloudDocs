import { useState, useEffect } from 'react';
import { X, Save, FileText, Tag, FolderOpen, AlignLeft } from 'lucide-react';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Document } from '../services/documentService';

interface EditMetadataModalProps {
  document: Document;
  isOpen: boolean;
  onClose: () => void;
  onSave: (metadata: any) => Promise<void>;
  updating: boolean;
}

export default function EditMetadataModal({ 
  document, 
  isOpen, 
  onClose, 
  onSave, 
  updating 
}: EditMetadataModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [tags, setTags] = useState('');

  useEffect(() => {
    if (document && isOpen) {
      setTitle(document.originalFilename || '');
      setDescription(document.description || '');
      setCategory(document.category || '');
      setTags(document.tags ? document.tags.join(', ') : '');
    }
  }, [document, isOpen]);

  const handleSave = async () => {
    const metadata = {
      title: title.trim(),
      description: description.trim(),
      category: category.trim() || null,
      tags: tags.split(',').map(tag => tag.trim()).filter(tag => tag)
    };
    
    await onSave(metadata);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-lg mx-auto bg-white shadow-2xl border-0 rounded-xl overflow-hidden">
        <CardContent className="p-0">
          {/* Header */}
          <div className="bg-gradient-to-r from-green-600 to-green-700 px-6 py-4 text-white">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="bg-white bg-opacity-20 p-2 rounded-lg">
                  <FileText className="w-5 h-5" />
                </div>
                <h3 className="text-xl font-semibold">Edit Document Metadata</h3>
              </div>
              <Button 
                variant="ghost" 
                size="sm" 
                onClick={onClose}
                className="text-white hover:bg-white hover:bg-opacity-20 rounded-full p-2"
                disabled={updating}
              >
                <X className="w-5 h-5" />
              </Button>
            </div>
          </div>

          {/* Content */}
          <div className="p-6 space-y-6">
            {/* Title Field */}
            <div>
              <label className="flex items-center text-sm font-semibold text-gray-700 mb-2">
                <FileText className="w-4 h-4 mr-2" />
                Document Title
              </label>
              <input 
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 transition-colors"
                placeholder="Enter document title"
                disabled={updating}
              />
            </div>

            {/* Description Field */}
            <div>
              <label className="flex items-center text-sm font-semibold text-gray-700 mb-2">
                <AlignLeft className="w-4 h-4 mr-2" />
                Description
              </label>
              <textarea 
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 transition-colors resize-none"
                rows={3}
                placeholder="Describe your document"
                disabled={updating}
              />
            </div>

            {/* Category Field */}
            <div>
              <label className="flex items-center text-sm font-semibold text-gray-700 mb-2">
                <FolderOpen className="w-4 h-4 mr-2" />
                Category
              </label>
              <select 
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-white transition-colors"
                disabled={updating}
              >
                <option value="">Select a category</option>
                <option value="Legal">📋 Legal</option>
                <option value="Financial">💰 Financial</option>
                <option value="HR">👥 HR</option>
                <option value="Marketing">📈 Marketing</option>
                <option value="Technical">⚙️ Technical</option>
                <option value="Reports">📊 Reports</option>
                <option value="Contracts">📄 Contracts</option>
                <option value="Other">🔖 Other</option>
              </select>
            </div>

            {/* Tags Field */}
            <div>
              <label className="flex items-center text-sm font-semibold text-gray-700 mb-2">
                <Tag className="w-4 h-4 mr-2" />
                Tags
              </label>
              <input 
                type="text"
                value={tags}
                onChange={(e) => setTags(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 transition-colors"
                placeholder="important, draft, review"
                disabled={updating}
              />
              <p className="text-xs text-gray-500 mt-2 flex items-center">
                <Tag className="w-3 h-3 mr-1" />
                Separate tags with commas for better organization
              </p>
            </div>

            {/* Action Buttons */}
            <div className="flex space-x-3 pt-4 border-t border-gray-200">
              <Button 
                onClick={handleSave} 
                disabled={updating || !title.trim()} 
                className="flex-1 bg-green-600 hover:bg-green-700 text-white py-3 rounded-lg font-semibold transition-colors shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Save className="w-5 h-5 mr-2" />
                {updating ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                    Saving...
                  </>
                ) : (
                  'Save Changes'
                )}
              </Button>
              <Button 
                variant="outline" 
                onClick={onClose} 
                disabled={updating}
                className="px-6 py-3 border-2 border-gray-300 text-gray-700 hover:bg-gray-50 rounded-lg font-semibold transition-colors disabled:opacity-50"
              >
                Cancel
              </Button>
            </div>

            {/* Helper Text */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
              <p className="text-xs text-blue-700">
                💡 <strong>Tip:</strong> Good metadata helps others find and understand your documents easily. 
                Make sure to add relevant tags and a clear description.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
