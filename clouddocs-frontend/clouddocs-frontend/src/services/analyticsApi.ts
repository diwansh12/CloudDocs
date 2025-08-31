import {
  OverviewMetricsDTO,
  TemplateMetricsDTO,
  StepMetricsDTO,
  MyMetricsDTO,
  ExportType
} from '../types/analytics';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'https://clouddocs.onrender.com/api';

class AnalyticsApiError extends Error {
  constructor(
    message: string, 
    public status?: number,
    public originalError?: Error
  ) {
    super(message);
    this.name = 'AnalyticsApiError';
  }
}

const getAuthHeaders = (): HeadersInit => {
  const token = localStorage.getItem('token');
  return {
    'Authorization': token ? `Bearer ${token}` : '',
    'Content-Type': 'application/json'
  };
};

/**
 * Convert date string to proper backend format (YYYY-MM-DDTHH:MM:SS)
 * Handles various input formats correctly
 */
const formatDateForBackend = (dateStr: string, isEndDate = false): string => {
  // Remove trailing 'Z' if present (from toISOString())
  let cleanDate = dateStr.endsWith('Z') ? dateStr.slice(0, -1) : dateStr;
  
  // If already has time component, replace it appropriately
  if (cleanDate.includes('T')) {
    const datePart = cleanDate.split('T')[0];
    const timePart = isEndDate ? 'T23:59:59' : 'T00:00:00';
    return `${datePart}${timePart}`;
  }
  
  // If just a date (YYYY-MM-DD), append time
  const timePart = isEndDate ? 'T23:59:59' : 'T00:00:00';
  return `${cleanDate}${timePart}`;
};

const handleApiResponse = async <T>(response: Response): Promise<T> => {
  if (response.ok) {
    return response.json();
  }

  let errorMessage: string;
  
  switch (response.status) {
    case 401:
      errorMessage = 'Authentication failed. Please login again.';
      break;
    case 403:
      errorMessage = 'Access denied. You need admin or manager privileges to view analytics.';
      break;
    case 404:
      errorMessage = 'Analytics endpoint not found.';
      break;
    case 500:
      errorMessage = 'Server error occurred while processing analytics data.';
      break;
    case 400:
      errorMessage = 'Invalid date range or parameters provided.';
      break;
    default:
      errorMessage = `API Error: ${response.status} ${response.statusText}`;
  }

  try {
    const errorData = await response.json();
    if (errorData.message) {
      errorMessage = errorData.message;
    }
  } catch {
    // Use default message if response body can't be parsed
  }

  throw new AnalyticsApiError(errorMessage, response.status);
};

export const analyticsApi = {
  async getOverview(from: string, to: string): Promise<OverviewMetricsDTO> {
    try {
      const fromParam = formatDateForBackend(from, false);
      const toParam = formatDateForBackend(to, true);
      
      const response = await fetch(
        `${API_BASE_URL}/workflows/metrics/overview?from=${fromParam}&to=${toParam}`,
        { headers: getAuthHeaders() }
      );
      return handleApiResponse<OverviewMetricsDTO>(response);
    } catch (error) {
      if (error instanceof AnalyticsApiError) {
        throw error;
      }
      throw new AnalyticsApiError('Failed to fetch overview metrics', undefined, error as Error);
    }
  },

  async getTemplateMetrics(from: string, to: string): Promise<TemplateMetricsDTO[]> {
    try {
      const fromParam = formatDateForBackend(from, false);
      const toParam = formatDateForBackend(to, true);
      
      const response = await fetch(
        `${API_BASE_URL}/workflows/metrics/by-template?from=${fromParam}&to=${toParam}`,
        { headers: getAuthHeaders() }
      );
      return handleApiResponse<TemplateMetricsDTO[]>(response);
    } catch (error) {
      if (error instanceof AnalyticsApiError) {
        throw error;
      }
      throw new AnalyticsApiError('Failed to fetch template metrics', undefined, error as Error);
    }
  },

  async getStepMetrics(from: string, to: string): Promise<StepMetricsDTO[]> {
    try {
      const fromParam = formatDateForBackend(from, false);
      const toParam = formatDateForBackend(to, true);
      
      const response = await fetch(
        `${API_BASE_URL}/workflows/metrics/by-step?from=${fromParam}&to=${toParam}`,
        { headers: getAuthHeaders() }
      );
      return handleApiResponse<StepMetricsDTO[]>(response);
    } catch (error) {
      if (error instanceof AnalyticsApiError) {
        throw error;
      }
      throw new AnalyticsApiError('Failed to fetch step metrics', undefined, error as Error);
    }
  },

  async getMyMetrics(from: string, to: string): Promise<MyMetricsDTO> {
    try {
      const fromParam = formatDateForBackend(from, false);
      const toParam = formatDateForBackend(to, true);
      
      const response = await fetch(
        `${API_BASE_URL}/workflows/metrics/my?from=${fromParam}&to=${toParam}`,
        { headers: getAuthHeaders() }
      );
      return handleApiResponse<MyMetricsDTO>(response);
    } catch (error) {
      if (error instanceof AnalyticsApiError) {
        throw error;
      }
      throw new AnalyticsApiError('Failed to fetch personal metrics', undefined, error as Error);
    }
  },

  async exportCsv(type: ExportType, from: string, to: string): Promise<void> {
    try {
      const fromParam = formatDateForBackend(from, false);
      const toParam = formatDateForBackend(to, true);
      
      let endpoint: string;
      let filename: string;
      
      switch (type) {
        case 'template':
          endpoint = 'by-template';
          filename = `template-metrics-${from}-${to}.csv`;
          break;
        case 'step':
          endpoint = 'by-step';
          filename = `step-metrics-${from}-${to}.csv`;
          break;
        case 'overview':
          endpoint = 'overview';
          filename = `overview-metrics-${from}-${to}.csv`;
          break;
        default:
          throw new AnalyticsApiError(`Invalid export type: ${type}`);
      }

      const response = await fetch(
        `${API_BASE_URL}/workflows/metrics/${endpoint}/export?from=${fromParam}&to=${toParam}`,
        { headers: getAuthHeaders() }
      );

      if (!response.ok) {
        let errorMessage: string;
        
        switch (response.status) {
          case 401:
            errorMessage = 'Authentication failed. Please login again.';
            break;
          case 403:
            errorMessage = 'Access denied. You need admin or manager privileges to export data.';
            break;
          case 404:
            errorMessage = `Export endpoint for ${type} metrics not found.`;
            break;
          case 400:
            errorMessage = 'Invalid date range provided for export.';
            break;
          default:
            errorMessage = `Export failed: ${response.status} ${response.statusText}`;
        }
        
        throw new AnalyticsApiError(errorMessage, response.status);
      }

      const blob = await response.blob();
      
      if (blob.size === 0) {
        throw new AnalyticsApiError('No data available for the selected date range');
      }

      // Create and trigger download
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      anchor.style.display = 'none';
      
      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);
      
      // Cleanup
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      
    } catch (error) {
      if (error instanceof AnalyticsApiError) {
        throw error;
      }
      throw new AnalyticsApiError(
        'CSV export failed due to network or server error', 
        undefined, 
        error as Error
      );
    }
  }
};

export const getErrorMessage = (error: unknown): string => {
  if (error instanceof AnalyticsApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred';
};
