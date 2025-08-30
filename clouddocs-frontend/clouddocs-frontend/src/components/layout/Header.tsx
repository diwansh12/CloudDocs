import React from 'react';
import { Search, Bell, Plus } from 'lucide-react';

interface HeaderProps {
  title: string;
  showUploadButton?: boolean;
  showExportButton?: boolean;
  onUpload?: () => void;
  onExport?: () => void;
}

const Header: React.FC<HeaderProps> = ({ 
  title, 
  showUploadButton = false, 
  showExportButton = false,
  onUpload,
  onExport 
}) => {
  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4">
      <div className="flex items-center justify-between">
        {/* Title */}
        <h1 className="text-2xl font-semibold text-gray-900">{title}</h1>
        
        {/* Search Bar */}
        <div className="flex-1 max-w-2xl mx-8">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="Search documents..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>
        
        {/* Action Buttons */}
        <div className="flex items-center space-x-4">
          <button className="p-2 text-gray-600 hover:text-gray-900 relative">
            <Bell className="w-5 h-5" />
            <span className="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full"></span>
          </button>
          
          {showExportButton && (
            <button 
              onClick={onExport}
              className="bg-gray-100 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-200 flex items-center space-x-2 text-sm font-medium"
            >
              <span>Export Log</span>
            </button>
          )}
          
          {showUploadButton && (
            <button 
              onClick={onUpload}
              className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 flex items-center space-x-2 text-sm font-medium"
            >
              <Plus className="w-4 h-4" />
              <span>Upload Document</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
