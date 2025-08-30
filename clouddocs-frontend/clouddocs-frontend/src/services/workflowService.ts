import api from './api';
import { AxiosResponse } from 'axios';

export interface WorkflowItem {
  id: string;
  title: string;
   initiatedByName: string;    
  updatedDate: string;        
  assignedTo: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'IN_PROGRESS' | 'CANCELLED' | 'COMPLETED';
  documentName?: string;
  templateName?: string;
   tasks?: Array<{
    id: string;
    assignedToName?: string;
    assignedToId?: string;
    status: string;
    stepName?: string;
  }>;
}

export interface WorkflowPage {
  workflows: WorkflowItem[];
  totalItems: number;
  totalPages: number;
  currentPage: number;
  hasNext: boolean;
  hasPrevious: boolean;
  pageSize: number;
}

export interface WorkflowTemplate {
  id: string;
  name: string;
  description: string;
  type: string;
  isActive: boolean;
}

class WorkflowService {
  /**
   * Get paginated workflows with filtering
   */
  async getWorkflows(
    page: number = 0,
    size: number = 10,
    search?: string,
    status?: string,
    templateId?: string
  ): Promise<WorkflowPage> {
    try {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', size.toString());
      
      if (search?.trim()) params.append('q', search.trim());
      if (status && status !== 'All Statuses') params.append('status', status);
      if (templateId) params.append('templateId', templateId);

      const response: AxiosResponse<WorkflowPage> = await api.get<WorkflowPage>(
        `/workflows/mine?${params.toString()}`
      );
      
      return {
        workflows: response.data.workflows || [],
        totalItems: response.data.totalItems || 0,
        totalPages: response.data.totalPages || 1,
        currentPage: response.data.currentPage || 0,
        hasNext: response.data.hasNext || false,
        hasPrevious: response.data.hasPrevious || false,
        pageSize: response.data.pageSize || size
      };
    } catch (error: any) {
      console.error('Workflow service error:', error);
      throw new Error(error.response?.data?.message || 'Failed to fetch workflows');
    }
  }

  /**
   * Search workflows by document name
   */
  async searchWorkflows(
    query: string,
    page: number = 0,
    size: number = 10
  ): Promise<WorkflowPage> {
    try {
      const params = new URLSearchParams();
      params.append('q', query.trim());
      params.append('page', page.toString());
      params.append('size', size.toString());

      const response: AxiosResponse<WorkflowPage> = await api.get<WorkflowPage>(
        `/workflows/search?${params.toString()}`
      );
      
      return {
        workflows: response.data.workflows || [],
        totalItems: response.data.totalItems || 0,
        totalPages: response.data.totalPages || 1,
        currentPage: response.data.currentPage || 0,
        hasNext: response.data.hasNext || false,
        hasPrevious: response.data.hasPrevious || false,
        pageSize: response.data.pageSize || size
      };
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to search workflows');
    }
  }

  /**
   * Get user's pending tasks
   */
  async getMyTasks() {
    try {
      const response: AxiosResponse<any[]> = await api.get<any[]>('/workflows/tasks/my');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch tasks');
    }
  }

  /**
   * Complete a workflow task
   */
  async completeTask(taskId: string, action: string, comments?: string) {
    try {
      const params = new URLSearchParams();
      params.append('action', action);
      if (comments?.trim()) params.append('comments', comments.trim());
      
      const response: AxiosResponse<any> = await api.put(
        `/workflows/tasks/${taskId}/complete?${params.toString()}`
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to complete task');
    }
  }

  /**
   * ✅ NEW: Update task status (approve/reject)
   */
  async updateTaskStatus(taskId: string, action: 'approve' | 'reject', comments?: string) {
    try {
      const params = new URLSearchParams();
      params.append('action', action.toUpperCase()); // Backend expects uppercase
      if (comments?.trim()) params.append('comments', comments.trim());
      
      const response: AxiosResponse<any> = await api.put(
        `/workflows/tasks/${taskId}/action?${params.toString()}`
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || `Failed to ${action} task`);
    }
  }

  /**
   * ✅ NEW: Download document by ID
   */
  async downloadDocument(documentId: number) {
    try {
      const response = await api.get(`/documents/${documentId}/download`, {
        responseType: 'blob' // Important for file downloads
      });
      return response;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to download document');
    }
  }

  /**
   * Get workflow details by ID
   */
  async getWorkflowById(id: string) {
    try {
      const response: AxiosResponse<any> = await api.get(`/workflows/${id}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch workflow details');
    }
  }

  /**
   * Get workflow with complete task details
   */
  async getWorkflowWithTasks(id: string) {
    try {
      const response: AxiosResponse<any> = await api.get(`/workflows/${id}/details`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch workflow tasks');
    }
  }

  /**
   * Cancel a workflow instance
   */
  async cancelWorkflow(id: string, reason?: string) {
    try {
      const params = new URLSearchParams();
      if (reason?.trim()) params.append('reason', reason.trim());
      
      const response: AxiosResponse<any> = await api.put(
        `/workflows/${id}/cancel?${params.toString()}`
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to cancel workflow');
    }
  }

  /**
   * Start a new workflow
   */
// In workflowService.ts
async startWorkflow(
  documentId: number, 
  templateId: string, 
  title?: string, 
  description?: string,
  priority?: string
): Promise<any> {
  try {
    const requestData = {
      documentId,
      templateId,
      title: title?.trim() || undefined,
      description: description?.trim() || undefined,
      priority: priority || 'NORMAL'
    };

    console.log('📤 WorkflowService: Sending request with data:', requestData);

    // ✅ FIXED: Use api instance like other methods, not native fetch
    const response: AxiosResponse<any> = await api.post('/workflows', requestData);
    
    console.log('✅ WorkflowService: Response received:', response.data);
    return response.data;
    
  } catch (error: any) {
    console.error('❌ WorkflowService: Creation failed:', error);
    throw new Error(error.response?.data?.message || 'Failed to create workflow');
  }
}


  /**
   * Get available workflow templates - FIXED: Proper typing and safe response handling
   */
  async getWorkflowTemplates(): Promise<WorkflowTemplate[]> {
    try {
      const response: AxiosResponse<WorkflowTemplate[] | { templates: WorkflowTemplate[] } | { data: WorkflowTemplate[] }> = 
        await api.get('/workflow-templates/active');
      
      const responseData = response.data;
      
      // Handle different possible response structures safely
      if (Array.isArray(responseData)) {
        return responseData;
      } else if (responseData && typeof responseData === 'object') {
        // Check for nested structures
        if ('templates' in responseData && Array.isArray(responseData.templates)) {
          return responseData.templates;
        } else if ('data' in responseData && Array.isArray(responseData.data)) {
          return responseData.data;
        }
      }
      
      console.warn('Unexpected response structure for templates:', responseData);
      return [];
      
    } catch (error: any) {
      console.error('Error fetching templates:', error);
      throw new Error(error.response?.data?.message || 'Failed to fetch workflow templates');
    }
  }

  /**
   * Get workflow statistics (admin/manager only)
   */
  async getWorkflowStats() {
    try {
      const response: AxiosResponse<any> = await api.get('/workflows/stats');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch workflow statistics');
    }
  }

  /**
   * ✅ NEW: Get workflow history
   */
  async getWorkflowHistory(workflowId: string) {
    try {
      const response: AxiosResponse<any[]> = await api.get(`/workflows/${workflowId}/history`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch workflow history');
    }
  }

  /**
   * ✅ NEW: Get task details by ID
   */
  async getTaskById(taskId: string) {
    try {
      const response: AxiosResponse<any> = await api.get(`/workflows/tasks/${taskId}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch task details');
    }
  }

  /**
   * ✅ NEW: Get workflow metrics for analytics
   */
  async getWorkflowMetrics(workflowId: string) {
    try {
      const response: AxiosResponse<any> = await api.get(`/workflows/${workflowId}/metrics`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch workflow metrics');
    }
  }

  /**
   * ✅ NEW: Reassign task to different user
   */
  async reassignTask(taskId: string, newAssigneeId: string, reason?: string) {
    try {
      const params = new URLSearchParams();
      params.append('assigneeId', newAssigneeId);
      if (reason?.trim()) params.append('reason', reason.trim());
      
      const response: AxiosResponse<any> = await api.put(
        `/workflows/tasks/${taskId}/reassign?${params.toString()}`
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to reassign task');
    }
  }

  /**
   * ✅ NEW: Add comment to workflow
   */
  async addWorkflowComment(workflowId: string, comment: string) {
    try {
      const response: AxiosResponse<any> = await api.post(
        `/workflows/${workflowId}/comments`,
        { comment: comment.trim() }
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to add comment');
    }
  }

  /**
   * ✅ NEW: Get workflow comments
   */
  async getWorkflowComments(workflowId: string) {
    try {
      const response: AxiosResponse<any[]> = await api.get(`/workflows/${workflowId}/comments`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch comments');
    }
  }
}

export default new WorkflowService();
