"use client"

import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../components/ui/button"
import { Card, CardContent } from "../components/ui/card"
import { Input } from "../components/ui/input"
import { Badge } from "../components/ui/badge"
import { Search, ChevronDown, RefreshCw, AlertCircle, FileText } from "lucide-react"
import Sidebar from '../components/layout/Sidebar';
import workflowService, { WorkflowItem } from '../services/workflowService';
import { formatDate } from '../utils/dateUtils';

export default function Workflow() {
  const [workflows, setWorkflows] = useState<WorkflowItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("All Statuses");

  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [hasPrevious, setHasPrevious] = useState(false);

  const navigate = useNavigate();

  // Load workflows with current filters and pagination
  const loadWorkflows = useCallback(async () => {
    try {
      setLoading(true);
      setError('');

      console.log('🔍 Loading workflows:', {
        page: currentPage,
        search: searchQuery,
        status: statusFilter
      });

      const data = await workflowService.getWorkflows(
        currentPage,
        10,
        searchQuery || undefined,
        statusFilter === 'All Statuses' ? undefined : statusFilter
      );

      console.log('✅ Workflows loaded:', data);

      setWorkflows(data.workflows || []);
      setTotalPages(data.totalPages || 1);
      setTotalItems(data.totalItems || 0);
      setHasNext(data.hasNext || false);
      setHasPrevious(data.hasPrevious || false);

    } catch (err: any) {
      console.error('❌ Error loading workflows:', err);
      setError(err.message);
      setWorkflows([]);
    } finally {
      setLoading(false);
    }
  }, [currentPage, searchQuery, statusFilter]);

  // Load workflows when dependencies change
  useEffect(() => {
    loadWorkflows();
  }, [loadWorkflows]);

  // Handle search with debouncing
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (currentPage !== 0) {
        setCurrentPage(0); // Reset to first page when searching
      } else {
        loadWorkflows();
      }
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  // Handle status filter change
  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setStatusFilter(e.target.value);
    setCurrentPage(0); // Reset to first page when filtering
  };

  // Handle search input change
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
  };

  // Navigate to workflow details
  const handleViewDetails = (id: string) => {
    navigate(`/workflow/${id}`);
  };

  // Handle page navigation
  const handlePreviousPage = () => {
    if (hasPrevious) {
      setCurrentPage(prev => Math.max(prev - 1, 0));
    }
  };

  const handleNextPage = () => {
    if (hasNext) {
      setCurrentPage(prev => Math.min(prev + 1, totalPages - 1));
    }
  };

  // Refresh workflows
  const handleRefresh = () => {
    loadWorkflows();
  };

  const handleNewWorkflow = () => {
    navigate('/workflow/new');
  };


  // Get status color styling
  const getStatusColor = (status: string) => {
    switch (status) {
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

  // ✅ Add this helper function inside the component
  const getAssignedTo = (workflow: WorkflowItem) => {
    // Check if workflow has tasks
    if (!workflow.tasks || workflow.tasks.length === 0) {
      return 'Unassigned';
    }

    // Find the current pending task
    const currentTask = workflow.tasks.find(task => task.status === 'PENDING');
    if (currentTask && currentTask.assignedToName) {
      return currentTask.assignedToName;
    }

    // Fallback to first task if no pending task
    const firstTask = workflow.tasks[0];
    if (firstTask && firstTask.assignedToName) {
      return firstTask.assignedToName;
    }

    return 'Unassigned';
  };


  return (
    <div className="min-h-screen bg-gray-50 flex">
      <Sidebar />

      <main className="flex-1 flex flex-col">
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-8 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-semibold text-gray-900">Workflows</h1>
              {!loading && (
                <p className="text-sm text-gray-500 mt-1">
                  {totalItems === 0 ? 'No workflows found' : `${totalItems} workflow${totalItems !== 1 ? 's' : ''} total`}
                </p>
              )}
            </div>
            <div className="flex items-center space-x-3">
              <Button
                variant="outline"
                onClick={handleRefresh}
                disabled={loading}
                className="flex items-center"
              >
                <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                Refresh
              </Button>
              <Button onClick={handleNewWorkflow} className="bg-blue-600 hover:bg-blue-700 text-white transition-colors duration-200">
                New Workflow
              </Button>
            </div>
          </div>
        </header>

        {/* Search and Filter */}
        <div className="bg-white px-8 py-4 border-b border-gray-200">
          <div className="flex items-center space-x-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                placeholder="Search workflows..."
                value={searchQuery}
                onChange={handleSearchChange}
                className="pl-10 bg-white border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors duration-200"
              />
            </div>
            <div className="relative">
              <select
                value={statusFilter}
                onChange={handleStatusChange}
                className="appearance-none bg-white border border-gray-300 rounded-md px-4 py-2 pr-8 text-sm text-gray-700 hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors duration-200"
              >
                <option>All Statuses</option>
                <option>PENDING</option>
                <option>IN_PROGRESS</option>
                <option>APPROVED</option>
                <option>REJECTED</option>
                <option>COMPLETED</option>
                <option>CANCELLED</option>
              </select>
              <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4 pointer-events-none" />
            </div>
          </div>
        </div>

        {/* Workflow List */}
        <div className="flex-1 p-8">
          {/* Error State */}
          {error && (
            <Card className="mb-6 border-red-200 bg-red-50">
              <CardContent className="p-4 flex items-center">
                <AlertCircle className="w-5 h-5 text-red-500 mr-2" />
                <span className="text-red-700">{error}</span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleRefresh}
                  className="ml-auto"
                >
                  <RefreshCw className="w-4 h-4 mr-1" />
                  Retry
                </Button>
              </CardContent>
            </Card>
          )}

          {/* Loading State */}
          {loading && (
            <div className="flex items-center justify-center py-12">
              <RefreshCw className="w-8 h-8 animate-spin text-blue-600 mr-3" />
              <span className="text-gray-600">Loading workflows...</span>
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && workflows.length === 0 && (
            <div className="text-center py-12">
              <FileText className="w-16 h-16 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-500 text-lg mb-2">
                {searchQuery ? 'No workflows match your search' : 'No workflows found'}
              </p>
              <p className="text-gray-400">
                {searchQuery
                  ? 'Try adjusting your search terms or filters'
                  : 'Workflows you initiate will appear here'}
              </p>
              {!searchQuery && (
                <Button
                  onClick={handleNewWorkflow}
                  className="mt-4 bg-blue-600 hover:bg-blue-700 text-white"
                >
                  Create Your First Workflow
                </Button>
              )}
            </div>
          )}

          {/* Workflow Items */}
          {!loading && !error && workflows.length > 0 && (
            <div className="space-y-4">


              {workflows.map((workflow) => (
                <Card
                  key={workflow.id}
                  className="bg-white border border-gray-200 hover:shadow-lg hover:border-blue-200 transition-all duration-300 cursor-pointer"
                  onClick={() => handleViewDetails(workflow.id)}
                >
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <div className="flex items-center space-x-4 mb-2">
                          <h3 className="text-lg font-medium text-gray-900">{workflow.title || `Workflow #${workflow.id}`}</h3>
                          <Badge className={`${getStatusColor(workflow.status)} font-medium transition-colors duration-200`}>
                            {workflow.status.replace('_', ' ')}
                          </Badge>
                        </div>
                        <div className="flex items-center space-x-6 text-sm text-gray-600">
                          <span>
                            <span className="font-medium">Initiator:</span> {workflow.initiatedByName || 'Unknown'}
                          </span>
                          <span>
                            <span className="font-medium">Assigned To:</span> {getAssignedTo(workflow)}
                          </span>
                          <span>
                            <span className="font-medium">Last Updated:</span> {formatDate(workflow.updatedDate)}
                          </span>
                        </div>
                        {workflow.documentName && (
                          <div className="mt-2 text-sm text-gray-500">
                            <span className="font-medium">Document:</span> {workflow.documentName}
                          </div>
                        )}
                      </div>
                      <Button
                        variant="outline"
                        className="text-blue-600 border-blue-200 hover:bg-blue-50 bg-transparent transition-colors duration-200"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleViewDetails(workflow.id);
                        }}
                      >
                        View Details
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))}

            </div>
          )}

          {/* Pagination */}
          {!loading && !error && workflows.length > 0 && (
            <div className="flex items-center justify-between mt-8 pt-6 border-t border-gray-200">
              <div className="text-sm text-gray-700">
                Showing page {currentPage + 1} of {totalPages} ({totalItems} total workflows)
              </div>
              <div className="flex items-center space-x-2">
                <Button
                  variant="outline"
                  onClick={handlePreviousPage}
                  disabled={!hasPrevious || loading}
                  className="flex items-center"
                >
                  Previous
                </Button>
                <span className="text-sm text-gray-600 px-3">
                  {currentPage + 1} / {totalPages}
                </span>
                <Button
                  variant="outline"
                  onClick={handleNextPage}
                  disabled={!hasNext || loading}
                  className="flex items-center"
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
