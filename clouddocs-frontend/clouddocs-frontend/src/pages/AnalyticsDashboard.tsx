/* eslint-disable react-hooks/exhaustive-deps */
'use client';

import { useState, useMemo, useEffect, useCallback } from 'react';
import { Sparklines, SparklinesLine } from 'react-sparklines';
import { RefreshCw, Download, AlertCircle, Zap } from 'lucide-react';
import Sidebar from '../components/layout/Sidebar';
import { Button } from '../components/ui/button';
import { Card } from '../components/ui/card';
import { useAnalytics } from '../hooks/useAnalytics';
import { analyticsApi } from '../services/analyticsApi';
import { DateRange, ExportType } from '../types/analytics';
import '../AnalyticsDashboard.css';

/* ────────────────  types  ──────────────── */
type Tone = '' | 'success' | 'danger' | 'warning';

interface CardMeta {
  t: string;
  v: string | number;
  tone: Tone;
}

/* ✅ HELPER: Fix date conversion issues */
const formatDateForInput = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const parseInputDate = (dateString: string): Date => {
  // Parse as local date to avoid timezone issues
  const [year, month, day] = dateString.split('-').map(Number);
  return new Date(year, month - 1, day);
};

/* ────────────────  KPI CARD  ──────────────── */
function MetricCard({
  title,
  value,
  spark = [],
  tone = '',
}: {
  title: string;
  value: string | number;
  spark?: number[];
  tone?: Tone;
}) {
  return (
    <Card className={`metric-card border-l-4 ${tone && 'metric-' + tone}`}>
      <h3 className="text-sm text-gray-500 mb-1">{title}</h3>
      <div className="flex items-end space-x-2">
        <p className="text-2xl font-semibold">{value}</p>
        {spark.length > 1 && (
          <Sparklines data={spark} width={80} height={24} margin={0}>
            <SparklinesLine
              color="#4f46e5"
              style={{ strokeWidth: 1, fill: 'none' }}
            />
          </Sparklines>
        )}
      </div>
    </Card>
  );
}

/* ────────────────  SECTION HEADER  ──────────────── */
function SectionHeader({
  title,
  onExport,
}: {
  title: string;
  onExport?: () => void;
}) {
  return (
    <div className="flex items-center justify-between mb-3">
      <h2 className="text-lg font-medium">{title}</h2>
      {onExport && (
        <Button size="sm" variant="outline" onClick={onExport}>
          <Download className="w-4 h-4 mr-1" />
          CSV
        </Button>
      )}
    </div>
  );
}

/* ────────────────  MAIN PAGE  ──────────────── */
export default function AnalyticsDashboard() {
  /* ✅ FIXED: Proper date range initialization */
  const [range, setRange] = useState<DateRange>(() => {
    const today = new Date();
    const pastDate = new Date(today);
    pastDate.setDate(pastDate.getDate() - 30);
    
    return {
      from: formatDateForInput(pastDate),
      to: formatDateForInput(today),
    };
  });

  /* ✅ ADD: Manual refresh state */
  const [isManualRefreshing, setIsManualRefreshing] = useState(false);
  const [lastRefreshTime, setLastRefreshTime] = useState<Date | null>(null);

  /* data hook */
  const {
    overview,
    templateMetrics,
    stepMetrics,
    myMetrics,
    loading,
    error,
    refetch,
  } = useAnalytics(range);

  /* ✅ ADD: Debug logging */
  useEffect(() => {
    if (overview) {
      console.log('🔍 Analytics Overview Data:', overview);
      console.log('📊 Approved Workflows Count:', overview.approved);
      console.log('📊 Total Workflows Count:', overview.total);
      console.log('📅 Current Date Range:', range);
      
      if (overview.approved === 0) {
        console.warn('⚠️ No approved workflows found in current date range!');
      } else {
        console.log('✅ Found approved workflows:', overview.approved);
      }
    }
  }, [overview, range]);

  /* auto-refresh every 60 s */
  useEffect(() => {
    const id = setInterval(() => {
      console.log('🔄 Auto-refresh triggered');
      refetch();
    }, 60000);
    return () => clearInterval(id);
  }, [range]);

  /* ✅ FIXED: Proper date change handlers */
  const handleFromDateChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const newFromDate = event.target.value;
    console.log('📅 From date changed:', newFromDate);
    
    setRange(prevRange => {
      const newRange = { ...prevRange, from: newFromDate };
      console.log('📅 New range:', newRange);
      return newRange;
    });
  }, []);

  const handleToDateChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const newToDate = event.target.value;
    console.log('📅 To date changed:', newToDate);
    
    setRange(prevRange => {
      const newRange = { ...prevRange, to: newToDate };
      console.log('📅 New range:', newRange);
      return newRange;
    });
  }, []);

  /* ✅ ADD: Manual force refresh function */
  const forceRefresh = useCallback(async () => {
    try {
      setIsManualRefreshing(true);
      console.log('🔄 Manual force refresh triggered');
      
      // Clear all possible caches
      if (typeof window !== 'undefined') {
        Object.keys(localStorage).forEach(key => {
          if (key.includes('analytics') || key.includes('cache')) {
            localStorage.removeItem(key);
            console.log('🗑️ Cleared cache:', key);
          }
        });
        sessionStorage.clear();
      }
      
      // Call backend refresh endpoint if available
      try {
        const response = await fetch('/api/analytics/refresh', { 
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json'
          }
        });
        
        if (response.ok) {
          const result = await response.json();
          console.log('✅ Backend refresh successful:', result);
        }
      } catch (backendError) {
        console.warn('⚠️ Backend refresh failed, continuing with frontend refresh:', backendError);
      }
      
      await refetch();
      setLastRefreshTime(new Date());
      console.log('✅ Manual refresh completed');
      
    } catch (error) {
      console.error('❌ Manual refresh failed:', error);
    } finally {
      setIsManualRefreshing(false);
    }
  }, [refetch]);

  /* ✅ ADD: Expose refresh function globally */
  useEffect(() => {
    if (typeof window !== 'undefined') {
      (window as any).refreshAnalytics = forceRefresh;
    }
    return () => {
      if (typeof window !== 'undefined') {
        delete (window as any).refreshAnalytics;
      }
    };
  }, [forceRefresh]);

  /* helpers */
  const fmtH = (h?: number | null) =>
    h == null ? 'N/A' : `${h.toFixed(1)} h`;
  const fmtP = (p?: number | null) =>
    p == null ? 'N/A' : `${p.toFixed(1)} %`;

  const exportCsv = (t: ExportType) =>
    analyticsApi.exportCsv(t, range.from, range.to);

  /* overview + my-metrics arrays */
  const overviewCards: CardMeta[] = useMemo(
    () =>
      overview
        ? [
            { t: 'Total Workflows', v: overview.total, tone: '' },
            { t: 'Approved', v: overview.approved, tone: 'success' },
            { t: 'Rejected', v: overview.rejected, tone: 'danger' },
            { t: 'In Progress', v: overview.inProgress, tone: 'warning' },
            { t: 'Overdue Tasks', v: overview.overdueTasks, tone: 'danger' },
            {
              t: 'Avg Approval Time',
              v: fmtH(overview.avgApprovalHours),
              tone: '',
            },
          ]
        : [],
    [overview]
  );

  const myCards: CardMeta[] = useMemo(
    () =>
      myMetrics
        ? [
            { t: 'My Workflows', v: myMetrics.myInitiatedTotal, tone: '' },
            {
              t: 'My Pending Tasks',
              v: myMetrics.myPendingTasks,
              tone: 'warning',
            },
            {
              t: 'My Completed Tasks',
              v: myMetrics.myCompletedTasks,
              tone: 'success',
            },
            {
              t: 'My Avg Task Time',
              v: fmtH(myMetrics.myAvgTaskCompletionHours),
              tone: '',
            },
          ]
        : [],
    [myMetrics]
  );

  /* ✅ ENHANCED: Quick-range helper with proper date handling */
  const quick = (days: number | 'ytd' | 'all' | 'today') => () => {
    const today = new Date();
    let from: Date;
    let to: Date = today;
    
    if (days === 'ytd') {
      from = new Date(today.getFullYear(), 0, 1);
    } else if (days === 'all') {
      from = new Date(2020, 0, 1);
    } else if (days === 'today') {
      from = today;
    } else {
      from = new Date(today);
      from.setDate(from.getDate() - (days as number));
    }
    
    const newRange = {
      from: formatDateForInput(from),
      to: formatDateForInput(to),
    };
    
    console.log('📅 Quick range set:', newRange);
    setRange(newRange);
  };

  /* render */
  return (
    <div className="min-h-screen flex bg-gray-50">
      <Sidebar />

      <main className="flex-1 flex flex-col">
        {/* ✅ ENHANCED: sticky toolbar with fixed date inputs */}
        <div className="bg-white border-b px-8 py-4 sticky top-0 z-10 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold">Analytics</h1>
            <p className="text-sm text-gray-500 mt-1">
              {loading ? 'Loading...' : `Showing data from ${range.from} to ${range.to}`}
              {lastRefreshTime && (
                <span className="ml-2 text-gray-400">
                  • Last refreshed: {lastRefreshTime.toLocaleTimeString()}
                </span>
              )}
            </p>
          </div>

          <div className="flex items-center space-x-3">
            {/* ✅ FIXED: Proper controlled date inputs */}
            <input
              type="date"
              value={range.from}
              onChange={handleFromDateChange}
              className="border px-3 py-1 rounded-md"
              max={range.to} // Prevent from date being after to date
            />
            <span className="text-gray-500">–</span>
            <input
              type="date"
              value={range.to}
              onChange={handleToDateChange}
              className="border px-3 py-1 rounded-md"
              min={range.from} // Prevent to date being before from date
              max={formatDateForInput(new Date())} // Prevent future dates
            />

            {/* ✅ ENHANCED: Quick presets with Today button */}
            <div className="hidden md:flex space-x-1">
              <Button size="sm" variant="ghost" onClick={quick('today')}>
                Today
              </Button>
              <Button size="sm" variant="ghost" onClick={quick(7)}>
                7 d
              </Button>
              <Button size="sm" variant="ghost" onClick={quick(30)}>
                30 d
              </Button>
              <Button size="sm" variant="ghost" onClick={quick(90)}>
                90 d
              </Button>
              <Button size="sm" variant="ghost" onClick={quick(365)}>
                1 Year
              </Button>
              <Button size="sm" variant="ghost" onClick={quick('all')}>
                All Time
              </Button>
              <Button size="sm" variant="ghost" onClick={quick('ytd')}>
                YTD
              </Button>
            </div>

            {/* Refresh buttons */}
            <div className="flex space-x-2">
              <Button 
                size="sm" 
                variant="outline" 
                onClick={refetch}
                disabled={loading || isManualRefreshing}
              >
                <RefreshCw
                  className={`w-4 h-4 mr-1 ${(loading && !isManualRefreshing) && 'animate-spin'}`}
                />
                {loading && !isManualRefreshing ? 'Refreshing...' : 'Refresh'}
              </Button>
              
              <Button 
                size="sm" 
                variant="destructive" 
                onClick={forceRefresh}
                disabled={loading || isManualRefreshing}
                title="Clear cache and force reload fresh data"
              >
                <Zap className={`w-4 h-4 mr-1 ${isManualRefreshing && 'animate-pulse'}`} />
                {isManualRefreshing ? 'Force Refreshing...' : 'Force Refresh'}
              </Button>
            </div>
          </div>
        </div>

        {/* body */}
        <div className="flex-1 overflow-y-auto p-8">
          
          {/* ✅ ADD: Debug information panel */}
          {process.env.NODE_ENV === 'development' && overview && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
              <h3 className="font-semibold text-blue-800 mb-2">🔍 Debug Information</h3>
              <div className="text-sm space-y-2 text-blue-700">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p><strong>Date Range:</strong> {range.from} to {range.to}</p>
                    <p><strong>Loading:</strong> {loading ? 'Yes' : 'No'}</p>
                    <p><strong>Error:</strong> {error || 'None'}</p>
                  </div>
                  <div>
                    <p><strong>Last Refresh:</strong> {lastRefreshTime ? lastRefreshTime.toLocaleTimeString() : 'Never'}</p>
                    <p><strong>Manual Refreshing:</strong> {isManualRefreshing ? 'Yes' : 'No'}</p>
                  </div>
                </div>
                
                <div className="bg-white rounded p-3 mt-3">
                  <p className="font-medium mb-2">Current Analytics Data:</p>
                  <div className="grid grid-cols-4 gap-2 text-xs">
                    <div>Total: {overview.total}</div>
                    <div>Approved: {overview.approved}</div>
                    <div>Rejected: {overview.rejected}</div>
                    <div>In Progress: {overview.inProgress}</div>
                  </div>
                  
                  {overview.total === 0 && (
                    <div className="bg-red-100 border border-red-300 rounded p-2 mt-2">
                      <p className="text-red-800 font-medium">⚠️ No workflows found!</p>
                      <p className="text-red-700 text-xs">Try clicking "Today" or "All Time" to expand date range.</p>
                    </div>
                  )}
                </div>
                
                <div className="flex space-x-2 mt-3">
                  <Button size="sm" onClick={() => {
                    console.log('🔍 Current analytics state:', { overview, myMetrics, templateMetrics, stepMetrics });
                  }}>
                    Log State
                  </Button>
                  <Button size="sm" onClick={forceRefresh}>
                    Test Refresh
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* loading */}
          {loading && (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 animate-pulse">
              {Array.from({ length: 6 }).map((_, i) => (
                <Card key={i} className="h-24 bg-gray-200 rounded-lg" />
              ))}
            </div>
          )}

          {/* error */}
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
              <div className="flex items-center">
                <AlertCircle className="w-5 h-5 text-red-500 mr-2" />
                <div className="text-red-700">
                  <strong>Error loading analytics:</strong> {error}
                </div>
              </div>
              <div className="flex space-x-2 mt-3">
                <button 
                  onClick={refetch} 
                  className="text-red-600 hover:text-red-800 underline"
                >
                  Try again
                </button>
                <button 
                  onClick={forceRefresh}
                  className="text-red-600 hover:text-red-800 underline"
                >
                  Force refresh
                </button>
              </div>
            </div>
          )}

          {!loading && !error && (
            <>
              {/* OVERVIEW */}
              <SectionHeader
                title="Overview"
                onExport={() => exportCsv('overview')}
              />
              <div className="grid auto-fill-200 gap-4 mb-10">
                {overviewCards.map((c) => (
                  <MetricCard
                    key={c.t}
                    title={c.t}
                    value={c.v}
                    tone={c.tone}
                  />
                ))}
              </div>

              {/* MY METRICS */}
              <SectionHeader title="My Metrics" />
              <div className="grid auto-fill-200 gap-4 mb-10">
                {myCards.map((c) => (
                  <MetricCard
                    key={c.t}
                    title={c.t}
                    value={c.v}
                    tone={c.tone}
                  />
                ))}
              </div>

              {/* TEMPLATE TABLE */}
              <SectionHeader
                title="Template Performance"
                onExport={() => exportCsv('template')}
              />
              <div className="overflow-x-auto mb-10">
                {templateMetrics.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    <p>No template data available for the selected date range.</p>
                    <button 
                      onClick={quick('all')} 
                      className="text-blue-600 hover:text-blue-800 underline mt-2"
                    >
                      Try "All Time" range
                    </button>
                  </div>
                ) : (
                  <table className="metrics-table w-full">
                    <thead>
                      <tr>
                        <th>Template</th>
                        <th>Total</th>
                        <th className="text-green-700">Approved</th>
                        <th className="text-red-700">Rejected</th>
                        <th>Rate</th>
                        <th>Avg h</th>
                      </tr>
                    </thead>
                    <tbody>
                      {templateMetrics.map((t) => (
                        <tr
                          key={t.templateId}
                          className="hover:bg-gray-50 cursor-pointer"
                          onClick={() =>
                            (window.location.href = `/workflows?templateId=${t.templateId}`)
                          }
                        >
                          <td>{t.templateName}</td>
                          <td>{t.total}</td>
                          <td className="text-green-700">{t.approved}</td>
                          <td className="text-red-700">{t.rejected}</td>
                          <td>{fmtP(t.approvalRate)}</td>
                          <td>{fmtH(t.avgDurationHours)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>

              {/* STEP TABLE */}
              <SectionHeader
                title="Step Performance"
                onExport={() => exportCsv('step')}
              />
              <div className="overflow-x-auto mb-20">
                {stepMetrics.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    <p>No step data available for the selected date range.</p>
                    <button 
                      onClick={quick('all')} 
                      className="text-blue-600 hover:text-blue-800 underline mt-2"
                    >
                      Try "All Time" range
                    </button>
                  </div>
                ) : (
                  <table className="metrics-table w-full">
                    <thead>
                      <tr>
                        <th>Step</th>
                        <th>Total</th>
                        <th className="text-green-700">Done</th>
                        <th className="text-green-700">Approvals</th>
                        <th className="text-red-700">Rejects</th>
                        <th>Avg h</th>
                        <th>Rate</th>
                      </tr>
                    </thead>
                    <tbody>
                      {stepMetrics.map((s) => (
                        <tr key={s.stepOrder}>
                          <td>#{s.stepOrder}</td>
                          <td>{s.totalTasks}</td>
                          <td className="text-green-700">{s.completedTasks}</td>
                          <td className="text-green-700">{s.approvals}</td>
                          <td className="text-red-700">{s.rejections}</td>
                          <td>{fmtH(s.avgTaskCompletionHours)}</td>
                          <td>{fmtP(s.completionRate)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  );
}
