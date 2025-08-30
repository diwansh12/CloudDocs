'use client';

import { useEffect, useState, useMemo } from 'react';
import { Search, ChevronDown, Calendar, RefreshCw } from 'lucide-react';
import Sidebar from '../components/layout/Sidebar';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { auditApi, AuditLogItem } from '../services/auditApi';

// ✅ FIXED: Change interface to match expected API signature
interface ApiResponse {
  success: boolean;
  count: number;
  data: AuditLogItem[];
  timestamp: string;
  [key: string]: any;
}

export default function AuditTrail() {
  /* ---------- local state ---------- */
  const [logs, setLogs] = useState<AuditLogItem[]>([]);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<'SUCCESS' | 'FAILED' | ''>('');
  const [user, setUser] = useState('');
  const [date, setDate] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | undefined>();

  /* ---------- data fetch ---------- */
  const fetchLogs = async () => {
    setLoading(true);
    setError(undefined);
    try {
      console.log('🔍 Fetching audit logs...');
      
      // ✅ FIXED: Build params as Record<string, string | undefined>
      const params: Record<string, string | undefined> = {};
      
      // Only add non-empty values
      if (q.trim()) params.q = q.trim();
      if (user.trim()) params.user = user.trim();
      if (status) params.type = status;
      if (date) {
        params.from = date;
        params.to = date;
      }
      
      console.log('📤 API params:', params);
      
      // Now this will work without TypeScript errors
      const response = await auditApi.list(params);
      
      console.log('📥 Raw API response:', response);
      
      // Handle different response formats
      let logsData: AuditLogItem[] = [];
      
      if (Array.isArray(response)) {
        logsData = response as AuditLogItem[];
      } else if (response && typeof response === 'object') {
        const apiResponse = response as ApiResponse;
        if (apiResponse.data && Array.isArray(apiResponse.data)) {
          logsData = apiResponse.data;
        } else {
          const values = Object.values(response);
          if (values.every(item => item && typeof item === 'object')) {
            logsData = values as AuditLogItem[];
          }
        }
      }
      
      if (!Array.isArray(logsData)) {
        console.warn('⚠️ Converting non-array data to array:', typeof logsData);
        logsData = [];
      }
      
      console.log('✅ Final processed logs:', logsData.length, 'entries');
      setLogs(logsData);
      setError(undefined);
      
    } catch (e: any) {
      console.error('❌ Error fetching audit logs:', e);
      setError(e.message || 'Failed to fetch audit logs');
      setLogs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  /* ---------- derived list ---------- */
  const filtered = useMemo(() => {
    if (!logs || !Array.isArray(logs)) {
      console.warn('⚠️ Logs is not an array in useMemo:', typeof logs);
      return [];
    }
    
    if (!q.trim()) {
      return logs;
    }
    
    const searchTerm = q.toLowerCase();
    return logs.filter((log: AuditLogItem) => {
      if (!log || typeof log !== 'object') return false;
      
      return (
        (log.activity && log.activity.toLowerCase().includes(searchTerm)) ||
        (log.linkedItem && log.linkedItem.toLowerCase().includes(searchTerm)) ||
        (log.user && log.user.toLowerCase().includes(searchTerm))
      );
    });
  }, [logs, q]);

  /* ---------- helper function for export ---------- */
  const handleExportCsv = () => {
    const exportParams: Record<string, string | undefined> = {};
    if (q.trim()) exportParams.q = q.trim();
    if (user.trim()) exportParams.user = user.trim();
    if (status) exportParams.type = status;
    if (date) {
      exportParams.from = date;
      exportParams.to = date;
    }
    auditApi.exportCsv(exportParams);
  };

  /* ---------- render ---------- */
  return (
    <div className="min-h-screen bg-gray-50 flex">
      <Sidebar />

      <main className="flex-1 flex flex-col">
        {/* header */}
        <header className="bg-white border-b px-8 py-6 flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-semibold">Audit Trail</h1>
            <p className="text-sm text-gray-600 mt-1">
              Showing {filtered.length} of {logs.length} audit records
            </p>
          </div>
          <div className="flex space-x-2">
            <Button
              onClick={fetchLogs}
              disabled={loading}
              variant="outline"
              size="sm"
            >
              <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
            <Button
              onClick={handleExportCsv}
              disabled={loading}
            >
              Export CSV
            </Button>
          </div>
        </header>

        {/* filters */}
        <section className="bg-white px-8 py-4 border-b space-x-4 flex items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
            <Input
              placeholder="Search activities, users, or items..."
              value={q}
              onChange={e => setQ(e.target.value)}
              className="pl-10"
            />
          </div>

          {/* status select */}
          <div className="relative">
            <select
              value={status}
              onChange={e => setStatus(e.target.value as 'SUCCESS' | 'FAILED' | '')}
              className="appearance-none border px-4 py-2 pr-8 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">All Status</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="FAILED">FAILED</option>
            </select>
            <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          </div>

          {/* date filter */}
          <div className="relative">
            <input
              type="date"
              value={date}
              onChange={e => setDate(e.target.value)}
              className="border px-4 py-2 pr-8 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
            <Calendar className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          </div>

          <Button onClick={fetchLogs} disabled={loading}>
            Apply Filters
          </Button>
        </section>

        {/* list */}
        <section className="flex-1 p-8 overflow-y-auto">
          {/* Loading State */}
          {loading && (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              <span className="ml-3 text-gray-600">Loading audit logs...</span>
            </div>
          )}

          {/* Error State */}
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-md p-4 mb-6">
              <div className="flex">
                <div className="text-red-700">
                  <strong>Error:</strong> {error}
                </div>
              </div>
              <button 
                onClick={fetchLogs}
                className="mt-2 text-red-600 hover:text-red-800 underline"
              >
                Try Again
              </button>
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && filtered.length === 0 && (
            <div className="text-center py-12">
              <div className="text-gray-400 text-lg mb-2">
                {logs.length === 0 ? '📋 No audit records found' : '🔍 No records match your filters'}
              </div>
              <p className="text-gray-500 text-sm">
                {logs.length === 0 
                  ? 'Audit logs will appear here as system activities are recorded.'
                  : 'Try adjusting your search terms or filters.'}
              </p>
              {logs.length === 0 && (
                <Button 
                  onClick={fetchLogs}
                  className="mt-4"
                  variant="outline"
                >
                  Refresh
                </Button>
              )}
            </div>
          )}

          {/* Audit Logs List */}
          {!loading && !error && filtered.length > 0 && (
            <div className="space-y-4">
              {filtered.map((log: AuditLogItem, index: number) => (
                <Card key={log.id || index} className="border hover:shadow-lg transition-shadow">
                  <CardContent className="p-6 flex justify-between items-start">
                    <div className="flex-1">
                      <h3 className="font-medium text-gray-900 mb-2">
                        {log.activity}
                        {log.linkedItem && (
                          <span className="text-blue-600 ml-2 font-normal">
                            → {log.linkedItem}
                          </span>
                        )}
                      </h3>
                      <div className="flex items-center text-sm text-gray-600 space-x-4">
                        <span className="flex items-center">
                          <strong className="mr-1">User:</strong> {log.user}
                        </span>
                        <span className="text-gray-400">|</span>
                        <span>
                          {new Date(log.timestamp).toLocaleString('en-US', {
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                            second: '2-digit'
                          })}
                        </span>
                      </div>
                    </div>
                    <Badge
                      className={
                        log.status === 'SUCCESS'
                          ? 'bg-green-100 text-green-700 border-green-200'
                          : 'bg-red-100 text-red-700 border-red-200'
                      }
                    >
                      {log.status}
                    </Badge>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

