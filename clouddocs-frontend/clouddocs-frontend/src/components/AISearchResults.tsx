// src/components/AISearchResults.tsx
import React from 'react';
import { FileText, Calendar, User, Star } from 'lucide-react';
import { Card, CardContent } from './ui/card';

interface DocumentResult {
  id: number;
  originalFilename: string;  // ✅ Matches backend
  description?: string;
  category?: string;
  uploadDate: string;        // ✅ Matches backend
  uploadedByName: string;    // ✅ Matches backend
  aiScore?: number;
}

interface AISearchResultsProps {
  results: DocumentResult[];
  query: string;
  onDocumentClick: (doc: DocumentResult) => void;
}

export const AISearchResults: React.FC<AISearchResultsProps> = ({ 
  results, 
  query, 
  onDocumentClick 
}) => {
  const getScoreColor = (score: number) => {
    if (score >= 0.9) return 'text-green-600 bg-green-100';
    if (score >= 0.8) return 'text-blue-600 bg-blue-100';
    return 'text-orange-600 bg-orange-100';
  };

  const getScoreLabel = (score: number) => {
    if (score >= 0.9) return 'Excellent Match';
    if (score >= 0.8) return 'Good Match';
    return 'Relevant';
  };

  if (results.length === 0) {
    return (
      <div className="text-center py-12">
        <FileText className="mx-auto h-16 w-16 text-gray-400 mb-4" />
        <h3 className="text-lg font-semibold text-gray-900 mb-2">No results found</h3>
        <p className="text-gray-600">
          Try different keywords or{' '}
          <button className="text-purple-600 underline">generate embeddings</button>{' '}
          for your documents first.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900">
          🤖 AI found {results.length} results for "{query}"
        </h3>
      </div>
      
      {results.map((doc) => (
        <Card 
          key={doc.id} 
          className="hover:shadow-lg transition-shadow cursor-pointer border-l-4 border-l-purple-500"
          onClick={() => onDocumentClick(doc)}
        >
          <CardContent className="p-6">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <FileText className="h-5 w-5 text-purple-600" />
                  <h4 className="text-xl font-semibold text-gray-900 hover:text-purple-600">
                    {doc.originalFilename}
                  </h4>
                  {doc.aiScore && (
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getScoreColor(doc.aiScore)}`}>
                      <Star className="inline w-3 h-3 mr-1" />
                      {getScoreLabel(doc.aiScore)}
                    </span>
                  )}
                </div>
                
                {doc.description && (
                  <p className="text-gray-600 mb-3 line-clamp-2">
                    {doc.description}
                  </p>
                )}
                
                <div className="flex items-center gap-4 text-sm text-gray-500">
                  <span className="flex items-center gap-1">
                    <Calendar className="h-4 w-4" />
                    {new Date(doc.uploadDate).toLocaleDateString()}
                  </span>
                  <span className="flex items-center gap-1">
                    <User className="h-4 w-4" />
                    {doc.uploadedByName}
                  </span>
                  {doc.category && (
                    <span className="px-2 py-1 bg-gray-100 rounded-full text-xs">
                      {doc.category}
                    </span>
                  )}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};
