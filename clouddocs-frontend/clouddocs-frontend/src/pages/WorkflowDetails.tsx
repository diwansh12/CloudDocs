import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, 
  FileText, 
  Clock, 
  User, 
  Calendar, 
  AlertCircle, 
  CheckCircle, 
  XCircle,
  Download
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import Sidebar from '../components/layout/Sidebar';
import workflowService from '../services/workflowService';
import { formatDate } from '../utils/dateUtils';

export interface WorkflowDetails {
  id: string;
  title?: string;
  description?: string;
  status: string;
  initiator: string;
  initiatedBy: string;
  initiatedById: string;
  initiatedByName: string;
  startDate: string;
  endDate?: string;
  dueDate?: string;
  documentId: number;
  documentName?: string;
  templateId: string;
  templateName?: string;
  currentStepOrder: number;
  priority: number | string;
  tasks: WorkflowTask[];
  steps?: WorkflowStep[];
  history?: WorkflowHistory[];
}

interface WorkflowTask {
  id: string;
  title: string;
  description?: string;
  status: string;
  action?: string;
  assignedToId: string;
  assignedToName: string;
  createdDate: string;
  dueDate?: string;
  completedDate?: string;
  stepOrder: number;
  stepName: string;
}

interface WorkflowHistory {
  id: string;
  actionDate: string;
  details: string;
  action: string;
  performedById: string;
  performedByName: string;
}

interface WorkflowStep {
  stepOrder: number;
  name: string;
  stepType: string;
  slaHours?: number;
  requiredRoles: string[];
}

// Progress Component
const WorkflowProgress = ({ workflow }: { workflow: WorkflowDetails }) => {
  const completedTasks = workflow.tasks.filter(task => 
    task.status === 'COMPLETED' || task.status === 'APPROVED'
  ).length;
  const totalTasks = workflow.tasks.length;
  const progressPercentage = totalTasks > 0 ? (completedTasks / totalTasks) * 100 : 0;

  return (
    <div className="mb-4">
      <div className="flex justify-between text-sm text-gray-600 mb-1">
        <span>Progress</span>
        <span>{completedTasks}/{totalTasks} tasks completed</span>
      </div>
      <div className="w-full bg-gray-200 rounded-full h-2">
        <div 
          className="bg-blue-600 h-2 rounded-full transition-all duration-300"
          style={{ width: `${progressPercentage}%` }}
        ></div>
      </div>
    </div>
  );
};

export default function WorkflowDetails() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [workflow, setWorkflow] = useState<WorkflowDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancelling, setCancelling] = useState(false);
  const [taskActionLoading, setTaskActionLoading] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadWorkflow(id);
    }
  }, [id]);

  const loadWorkflow = async (workflowId: string) => {
    try {
      setLoading(true);
      setError('');
      const data = await workflowService.getWorkflowWithTasks(workflowId);
      setWorkflow(data);
    } catch (err: any) {
      console.error('Failed to load workflow:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelWorkflow = async () => {
    if (!workflow || !id) return;

    const reason = prompt('Please provide a reason for cancellation (optional):');
    if (reason === null) return; // User clicked cancel

    try {
      setCancelling(true);
      await workflowService.cancelWorkflow(id, reason || undefined);
      
      // Reload workflow to show updated status
      await loadWorkflow(id);
      
      alert('Workflow cancelled successfully');
    } catch (err: any) {
      console.error('Failed to cancel workflow:', err);
      alert(`Failed to cancel workflow: ${err.message}`);
    } finally {
      setCancelling(false);
    }
  };

  const handleTaskAction = async (taskId: string, action: 'approve' | 'reject') => {
    try {
      setTaskActionLoading(taskId);
      await workflowService.updateTaskStatus(taskId, action);
      // Reload workflow to show updated status
      if (id) await loadWorkflow(id);
      alert(`Task ${action}d successfully`);
    } catch (error: any) {
      console.error(`Failed to ${action} task:`, error);
      alert(`Failed to ${action} task: ${error.message}`);
    } finally {
      setTaskActionLoading(null);
    }
  };

  const handleDocumentDownload = async (documentId: number, documentName: string) => {
    try {
      const response = await workflowService.downloadDocument(documentId);
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = documentName || 'document';
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Download failed:', error);
      alert('Failed to download document');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case 'APPROVED':
      case 'COMPLETED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'PENDING':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'CANCELLED':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

const getPriorityColor = (priority: string | number | null | undefined) => {
    // Handle null/undefined priority with fallback
    if (priority === null || priority === undefined) {
      return 'bg-gray-100 text-gray-800 border-gray-200';
    }
    
    const priorityStr = priority.toString().toUpperCase();
    switch (priorityStr) {
      case 'URGENT':
      case '4':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'HIGH':
      case '3':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'NORMAL':
      case '2':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'LOW':
      case '1':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status.toUpperCase()) {
      case 'APPROVED':
      case 'COMPLETED':
        return <CheckCircle className="w-4 h-4" />;
      case 'REJECTED':
      case 'CANCELLED':
        return <XCircle className="w-4 h-4" />;
      case 'PENDING':
      case 'IN_PROGRESS':
        return <Clock className="w-4 h-4" />;
      default:
        return <AlertCircle className="w-4 h-4" />;
    }
  };

  const isDueSoon = (dueDate: string) => {
    const due = new Date(dueDate);
    const now = new Date();
    const diffInHours = (due.getTime() - now.getTime()) / (1000 * 60 * 60);
    return diffInHours <= 24 && diffInHours > 0; // Due within 24 hours
  };

  const isOverdue = (dueDate: string) => {
    const due = new Date(dueDate);
    const now = new Date();
    return due.getTime() < now.getTime();
  };

  if (loading) {
    return (
      <div className="flex h-screen bg-gray-50">
        <Sidebar />
        <main className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <div className="w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
            <p className="text-gray-600">Loading workflow details...</p>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      
      <main className="flex-1 flex flex-col">
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-4 md:px-8 py-4 md:py-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between space-y-4 md:space-y-0">
            <div className="flex items-center space-x-4">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate('/workflow')}
                className="flex items-center"
              >
                <ArrowLeft className="w-4 h-4 mr-2" />
                <span className="hidden sm:inline">Back to Workflows</span>
                <span className="sm:hidden">Back</span>
              </Button>
              <div>
                <h1 className="text-xl md:text-2xl font-semibold text-gray-900">
                  Workflow Details
                </h1>
                <p className="text-sm text-gray-500 mt-1">ID: {id}</p>
              </div>
            </div>
            
            {/* Actions */}
            {workflow && workflow.status === 'IN_PROGRESS' && (
              <Button
                variant="outline"
                onClick={handleCancelWorkflow}
                disabled={cancelling}
                className="text-red-600 border-red-200 hover:bg-red-50"
              >
                {cancelling ? (
                  <>
                    <div className="w-4 h-4 border-2 border-red-600 border-t-transparent rounded-full animate-spin mr-2" />
                    Cancelling...
                  </>
                ) : (
                  'Cancel Workflow'
                )}
              </Button>
            )}
          </div>
        </header>

        <div className="flex-1 p-4 md:p-8 overflow-auto">
          {error ? (
            <Card className="border-red-200 bg-red-50">
              <CardContent className="p-6 flex items-center">
                <AlertCircle className="w-5 h-5 text-red-500 mr-3" />
                <div>
                  <p className="text-red-700 font-medium">Error Loading Workflow</p>
                  <p className="text-red-600 text-sm mt-1">{error}</p>
                </div>
                <Button
                  variant="outline"
                  onClick={() => id && loadWorkflow(id)}
                  className="ml-auto"
                >
                  Retry
                </Button>
              </CardContent>
            </Card>
          ) : workflow ? (
            <div className="space-y-6">
              {/* Workflow Overview */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center justify-between">
                    <div className="flex items-center">
                      <FileText className="w-5 h-5 mr-2" />
                      Workflow Information
                    </div>
                    <Badge className={`${getStatusColor(workflow.status)} flex items-center`}>
                      {getStatusIcon(workflow.status)}
                      <span className="ml-1">{workflow.status.replace('_', ' ')}</span>
                    </Badge>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {/* Progress Bar */}
                  <WorkflowProgress workflow={workflow} />
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-3">
                      <div>
                        <label className="text-sm font-medium text-gray-500">Document</label>
                        {workflow.documentName ? (
                          <p className="text-gray-900 flex items-center">
                            <FileText className="w-4 h-4 mr-1" />
                            <button 
                              onClick={() => handleDocumentDownload(workflow.documentId, workflow.documentName || 'document')}
                              className="text-blue-600 hover:text-blue-800 underline cursor-pointer flex items-center"
                            >
                              {workflow.documentName}
                              <Download className="w-3 h-3 ml-1" />
                            </button>
                          </p>
                        ) : (
                          <p className="text-gray-500">No document attached</p>
                        )}
                      </div>
                      <div>
                        <label className="text-sm font-medium text-gray-500">Initiated By</label>
                        <p className="text-gray-900 flex items-center">
                          <User className="w-4 h-4 mr-1" />
                          {workflow.initiatedByName}
                        </p>
                      </div>
                      <div>
                        <label className="text-sm font-medium text-gray-500">Priority</label>
                        <Badge className={`ml-2 ${getPriorityColor(workflow.priority)}`}>
                          {workflow.priority}
                        </Badge>
                      </div>
                    </div>
                    <div className="space-y-3">
                      <div>
                        <label className="text-sm font-medium text-gray-500">Started</label>
                        <p className="text-gray-900 flex items-center">
                          <Calendar className="w-4 h-4 mr-1" />
                          {formatDate(workflow.startDate)}
                        </p>
                      </div>
                      {workflow.dueDate && (
                        <div>
                          <label className="text-sm font-medium text-gray-500">Due Date</label>
                          <p className={`text-gray-900 flex items-center ${
                            isOverdue(workflow.dueDate) ? 'text-red-600' : 
                            isDueSoon(workflow.dueDate) ? 'text-orange-600' : ''
                          }`}>
                            <Clock className="w-4 h-4 mr-1" />
                            {formatDate(workflow.dueDate)}
                            {isOverdue(workflow.dueDate) && (
                              <Badge className="ml-2 bg-red-100 text-red-800">Overdue</Badge>
                            )}
                            {isDueSoon(workflow.dueDate) && !isOverdue(workflow.dueDate) && (
                              <Badge className="ml-2 bg-orange-100 text-orange-800">Due Soon</Badge>
                            )}
                          </p>
                        </div>
                      )}
                      {workflow.endDate && (
                        <div>
                          <label className="text-sm font-medium text-gray-500">Completed</label>
                          <p className="text-gray-900 flex items-center">
                            <CheckCircle className="w-4 h-4 mr-1" />
                            {formatDate(workflow.endDate)}
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Tasks */}
              {workflow.tasks && workflow.tasks.length > 0 && (
                <Card>
                  <CardHeader>
                    <CardTitle>Tasks</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {workflow.tasks.map((task) => (
                        <div
                          key={task.id}
                          className="border border-gray-200 rounded-lg p-4 bg-gray-50"
                        >
                          <div className="flex items-center justify-between mb-2">
                            <h4 className="font-medium text-gray-900">{task.title}</h4>
                            <Badge className={getStatusColor(task.status)}>
                              {task.status}
                            </Badge>
                          </div>
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                            <div>
                              <span className="font-medium text-gray-500">Assigned to:</span>
                              <span className="ml-2 text-gray-900">{task.assignedToName}</span>
                            </div>
                            <div>
                              <span className="font-medium text-gray-500">Step:</span>
                              <span className="ml-2 text-gray-900">{task.stepName}</span>
                            </div>
                            <div>
                              <span className="font-medium text-gray-500">Created:</span>
                              <span className="ml-2 text-gray-900">{formatDate(task.createdDate)}</span>
                            </div>
                            {task.completedDate && (
                              <div>
                                <span className="font-medium text-gray-500">Completed:</span>
                                <span className="ml-2 text-gray-900">{formatDate(task.completedDate)}</span>
                              </div>
                            )}
                          </div>
                          
                          {/* Task Action Buttons */}
                          {task.status === 'PENDING' && (
                            <div className="mt-3 flex space-x-2">
                              <Button 
                                size="sm" 
                                onClick={() => handleTaskAction(task.id, 'approve')}
                                disabled={taskActionLoading === task.id}
                                className="bg-green-600 hover:bg-green-700 text-white"
                              >
                                {taskActionLoading === task.id ? (
                                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-1" />
                                ) : (
                                  <CheckCircle className="w-4 h-4 mr-1" />
                                )}
                                Approve
                              </Button>
                              <Button 
                                size="sm" 
                                variant="outline"
                                onClick={() => handleTaskAction(task.id, 'reject')}
                                disabled={taskActionLoading === task.id}
                                className="text-red-600 border-red-200 hover:bg-red-50"
                              >
                                <XCircle className="w-4 h-4 mr-1" />
                                Reject
                              </Button>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* History */}
              {workflow.history && workflow.history.length > 0 && (
                <Card>
                  <CardHeader>
                    <CardTitle>Workflow History</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {workflow.history.map((entry) => (
                        <div
                          key={entry.id}
                          className="border-l-4 border-blue-200 pl-4 py-2"
                        >
                          <div className="flex items-center justify-between">
                            <p className="font-medium text-gray-900">{entry.action.replace('_', ' ')}</p>
                            <span className="text-sm text-gray-500">{formatDate(entry.actionDate)}</span>
                          </div>
                          <p className="text-gray-700 text-sm mt-1">{entry.details}</p>
                          <p className="text-gray-500 text-xs mt-1">by {entry.performedByName}</p>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          ) : (
            <Card>
              <CardContent className="p-6 text-center">
                <AlertCircle className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-500">No workflow data available</p>
              </CardContent>
            </Card>
          )}
        </div>
      </main>
    </div>
  );
}
