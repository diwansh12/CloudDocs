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
   * ✅ FIXED: Get paginated workflow instances with filtering
   * Changed from /workflows/mine to /workflow-instances/mine
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

      // ✅ FIXED: Changed endpoint to match backend controller
      const response: AxiosResponse<WorkflowPage> = await api.get<WorkflowPage>(
        `/workflow-instances/mine?${params.toString()}`
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
   * ✅ FIXED: Search workflow instances by document name
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

      // ✅ FIXED: Changed to workflow-instances search endpoint
      const response: AxiosResponse<WorkflowPage> = await api.get<WorkflowPage>(
        `/workflow-instances/search?${params.toString()}`
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
   * ✅ FIXED: Get user's pending tasks
   * Changed from /workflows/tasks/my to /workflows/tasks/user
   */
  async getMyTasks() {
    try {
      // ✅ FIXED: Updated endpoint path
      const response: AxiosResponse<any[]> = await api.get<any[]>('/workflows/tasks/user');
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
   * ✅ Update task status (approve/reject)
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
   * Download document by ID
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
   * ✅ FIXED: Get workflow instance details by ID
   * Changed from /workflows/{id} to /workflow-instances/{id}
   */
  async getWorkflowById(id: string) {
    try {
      // ✅ FIXED: Updated endpoint path
      const response: AxiosResponse<any> = await api.get(`/workflow-instances/${id}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch workflow details');
    }
  }

  /**
   * ✅ FIXED: Get workflow instance with complete task details
   */
  async getWorkflowWithTasks(id: string) {
    try {
      // ✅ FIXED: Updated endpoint path  
      const response: AxiosResponse<any> = await api.get(`/workflow-instances/${id}/details`);
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
      
      // ✅ FIXED: Updated endpoint path
      const response: AxiosResponse<any> = await api.put(
        `/workflow-instances/${id}/cancel?${params.toString()}`
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to cancel workflow');
    }
  }

  /**
   * ✅ Start a new workflow (stays on /workflows)
   * This is a workflow operation, not instance query
   */
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

      // ✅ CORRECT: Create workflow stays on /workflows
      const response: AxiosResponse<any> = await api.post('/workflows', requestData);
      
      console.log('✅ WorkflowService: Response received:', response.data);
      return response.data;
      
    } catch (error: any) {
      console.error('❌ WorkflowService: Creation failed:', error);
      throw new Error(error.response?.data?.message || 'Failed to create workflow');
    }
  }

  /**
   * Get available workflow templates
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
   * ✅ FIXED: Get workflow history
   */
  async getWorkflowHistory(workflowId: string) {
    try {
      // ✅ FIXED: Updated endpoint path
      const response: AxiosResponse<any[]> = await api.get(`/workflow-instances/${workflowId}/history`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch workflow history');
    }
  }

  /**
   * Get task details by ID (stays on /workflows)
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
   * ✅ FIXED: Get workflow metrics for analytics
   */
  async getWorkflowMetrics(workflowId: string) {
    try {
      // ✅ FIXED: Updated endpoint path
      const response: AxiosResponse<any> = await api.get(`/workflow-instances/${workflowId}/metrics`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch workflow metrics');
    }
  }

  /**
   * Reassign task to different user (stays on /workflows)
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
   * ✅ FIXED: Add comment to workflow instance
   */
  async addWorkflowComment(workflowId: string, comment: string) {
    try {
      // ✅ FIXED: Updated endpoint path
      const response: AxiosResponse<any> = await api.post(
        `/workflow-instances/${workflowId}/comments`,
        { comment: comment.trim() }
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to add comment');
    }
  }

  /**
   * ✅ FIXED: Get workflow instance comments
   */
  async getWorkflowComments(workflowId: string) {
    try {
      // ✅ FIXED: Updated endpoint path
      const response: AxiosResponse<any[]> = await api.get(`/workflow-instances/${workflowId}/comments`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to fetch comments');
    }
  }
}

export default new WorkflowService();
