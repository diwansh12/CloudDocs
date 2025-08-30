import { useState, useEffect, useCallback } from 'react';
import { analyticsApi } from '../services/analyticsApi';
import {
  OverviewMetricsDTO,
  TemplateMetricsDTO,
  StepMetricsDTO,
  MyMetricsDTO,
  DateRange
} from '../types/analytics';

interface State {
  overview:         OverviewMetricsDTO | null;
  templateMetrics:  TemplateMetricsDTO[];
  stepMetrics:      StepMetricsDTO[];
  myMetrics:        MyMetricsDTO | null;
  loading:          boolean;
  error:            string | null;
}

interface Return extends State {
  refetch: () => void;
}

/* --------  in-memory 30 s cache keyed by range -------- */
const cache = new Map<string, { data: Omit<State,'loading'|'error'>; ts: number }>();
const TTL = 30_000;   // 30 seconds

export const useAnalytics = (range: DateRange): Return => {
  const [state, set] = useState<State>({
    overview: null,
    templateMetrics: [],
    stepMetrics: [],
    myMetrics: null,
    loading: true,
    error: null
  });

  const fetchAll = useCallback(async (forceRefresh = false) => {
    const key = `${range.from}_${range.to}`;
    
    // ✅ CRITICAL: Always bypass cache for fresh data
    if (!forceRefresh) {
      console.log('🔄 Bypassing cache to ensure fresh data');
    }

    console.log('🔄 Fetching fresh analytics data for range:', range);
    set(s => ({ ...s, loading: true, error: null }));
    
    try {
      const fromISO = new Date(range.from).toISOString();
      const toISO = new Date(range.to).toISOString();

      console.log('📡 API Request params:', { fromISO, toISO });

      const [ov, templ, step, mine] = await Promise.all([
        analyticsApi.getOverview(fromISO, toISO),
        analyticsApi.getTemplateMetrics(fromISO, toISO),
        analyticsApi.getStepMetrics(fromISO, toISO),
        analyticsApi.getMyMetrics(fromISO, toISO)
      ]);

      console.log('📊 Fresh API Response - Overview:', ov);
      console.log('✅ Approved workflows from API:', ov.approved);

      const payload = {
        overview: ov,
        templateMetrics: templ,
        stepMetrics: step,
        myMetrics: mine
      };

      // ✅ Don't cache for now to ensure fresh data
      // cache.set(key, { data: payload, ts: Date.now() });
      set({ ...payload, loading: false, error: null });

    } catch (e: any) {
      console.error('❌ Analytics API Error:', e);
      set(s => ({ ...s, loading: false, error: e.message ?? 'Unknown error' }));
    }

  }, [range]);

  useEffect(() => { fetchAll(true); }, [fetchAll]); // ✅ Always force refresh

  return { ...state, refetch: () => fetchAll(true) };
};
