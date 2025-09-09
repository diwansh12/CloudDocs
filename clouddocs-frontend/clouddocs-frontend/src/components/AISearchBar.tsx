// src/components/AISearchBar.tsx
import React, { useState } from 'react';
import { Search, Sparkles, Loader } from 'lucide-react';
import { Button } from './ui/button';

interface AISearchBarProps {
  onSearch: (query: string) => void;
  loading?: boolean;
}

export const AISearchBar: React.FC<AISearchBarProps> = ({ onSearch, loading }) => {
  const [query, setQuery] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      onSearch(query.trim());
    }
  };

  return (
    <form onSubmit={handleSubmit} className="relative">
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
          <Sparkles className="h-5 w-5 text-purple-500" />
        </div>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Ask anything about your documents... (e.g., 'find contracts about payment')"
          className="w-full pl-12 pr-32 py-4 text-lg border-2 border-purple-200 rounded-xl focus:ring-4 focus:ring-purple-100 focus:border-purple-500 bg-white text-slate-800 placeholder-slate-500"
          disabled={loading}
        />
        <div className="absolute inset-y-0 right-0 flex items-center pr-2">
          <Button
            type="submit"
            disabled={loading || !query.trim()}
            className="bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 text-white px-6 py-2 rounded-lg font-semibold transition-all duration-200"
          >
            {loading ? (
              <Loader className="w-5 h-5 animate-spin" />
            ) : (
              <>
                <Search className="w-5 h-5 mr-2" />
                AI Search
              </>
            )}
          </Button>
        </div>
      </div>
      <div className="mt-2 flex flex-wrap gap-2">
        {['find tax documents', 'contracts expiring soon', 'meeting notes from last week'].map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            onClick={() => setQuery(suggestion)}
            className="text-sm px-3 py-1 bg-purple-50 text-purple-700 rounded-full hover:bg-purple-100 transition-colors"
          >
            {suggestion}
          </button>
        ))}
      </div>
    </form>
  );
};
