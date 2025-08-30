import React from 'react';
import { FileText, Download, Eye, MoreVertical } from 'lucide-react';

interface DocumentCardProps {
  document: {
    id: number;
    filename: string;
    status: 'APPROVED' | 'PENDING' | 'REJECTED';
    version: string;
    uploadDate: string;
    type: string;
  };
}

const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const getStatusStyles = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return 'bg-green-100 text-green-800';
      case 'PENDING':
        return 'bg-orange-100 text-orange-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <span className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusStyles(status)}`}>
      {status}
    </span>
  );
};

const DocumentCard: React.FC<DocumentCardProps> = ({ document }) => {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6 hover:shadow-md transition-shadow cursor-pointer group">
      <div className="flex flex-col items-center text-center">
        {/* Document Icon */}
        <div className="w-16 h-16 bg-blue-100 rounded-lg flex items-center justify-center mb-4">
          <FileText className="w-8 h-8 text-blue-600" />
        </div>
        
        {/* Document Info */}
        <h3 className="font-medium text-gray-900 mb-2 truncate w-full text-sm">
          {document.filename}
        </h3>
        
        <div className="text-xs text-gray-500 mb-3">
          Version: {document.version}
        </div>
        
        {/* Status Badge */}
        <StatusBadge status={document.status} />
        
        {/* Upload Date */}
        <div className="text-xs text-gray-400 mt-2">
          {document.uploadDate}
        </div>
        
        {/* Action Buttons - Show on Hover */}
        <div className="flex space-x-2 mt-4 opacity-0 group-hover:opacity-100 transition-opacity">
          <button className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg">
            <Eye className="w-4 h-4" />
          </button>
          <button className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg">
            <Download className="w-4 h-4" />
          </button>
          <button className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg">
            <MoreVertical className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default DocumentCard;
