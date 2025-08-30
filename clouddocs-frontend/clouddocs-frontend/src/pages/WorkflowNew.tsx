import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { ArrowLeft, FileText, Plus, AlertCircle } from 'lucide-react';
import Sidebar from '../components/layout/Sidebar';
import workflowService, { WorkflowTemplate } from '../services/workflowService';

export default function WorkflowNew() {
  // ✅ FIXED: Corrected initial state with proper priority values
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    documentId: '',
    templateId: '',
    priority: 'NORMAL' // ✅ Changed from MEDIUM to NORMAL to match expected values
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [templates, setTemplates] = useState<WorkflowTemplate[]>([]);
  const [templatesLoading, setTemplatesLoading] = useState(true);
  const [templatesError, setTemplatesError] = useState('');
  
  const navigate = useNavigate();

  // Load available templates on component mount
  useEffect(() => {
    loadTemplates();
  }, []);

  const loadTemplates = async () => {
    try {
      setTemplatesLoading(true);
      setTemplatesError('');
      
      const templatesData = await workflowService.getWorkflowTemplates();
      
      if (Array.isArray(templatesData)) {
        setTemplates(templatesData);
      } else {
        setTemplates([]);
        console.warn('Expected array of templates, got:', templatesData);
      }
      
    } catch (err: any) {
      console.error('Failed to load templates:', err);
      setTemplatesError('Failed to load workflow templates. Please refresh the page.');
      setTemplates([]);
    } finally {
      setTemplatesLoading(false);
    }
  };

  // ✅ ENHANCED: Better form data handling with debug logging
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    
    // ✅ ADD: Debug logging for priority changes
    if (name === 'priority') {
      console.log('🔄 Priority changed from', formData.priority, 'to', value);
    }
    
    setFormData(prev => {
      const newFormData = { ...prev, [name]: value };
      
      // ✅ ADD: Debug logging to verify state update
      if (name === 'priority') {
        console.log('✅ Priority state updated:', newFormData.priority);
      }
      
      return newFormData;
    });
  };

  // ✅ ENHANCED: Better form submission with priority verification
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // ✅ ADD: Debug logging for form submission
      console.log('🚀 Submitting workflow with data:', formData);
      console.log('📋 Priority being submitted:', formData.priority);

      // Validate required fields
      if (!formData.documentId) {
        throw new Error('Document ID is required');
      }
      if (!formData.templateId) {
        throw new Error('Please select a workflow template');
      }

      // ✅ CRITICAL: Ensure priority is included in the request
      const workflowRequest = {
        documentId: parseInt(formData.documentId),
        templateId: formData.templateId,
        title: formData.title.trim() || undefined,
        description: formData.description.trim() || undefined,
        priority: formData.priority // ✅ Explicitly include priority
      };

      console.log('📤 Final request data:', workflowRequest);

      // ✅ UPDATED: Pass priority to the service
      await workflowService.startWorkflow(
        workflowRequest.documentId,
        workflowRequest.templateId,
        workflowRequest.title,
        workflowRequest.description,
        workflowRequest.priority // ✅ ADD: Pass priority parameter
      );
      
      // Success - navigate back to workflows
      navigate('/workflow', { 
        state: { 
          message: 'Workflow created successfully!',
          type: 'success' 
        }
      });
    } catch (err: any) {
      console.error('❌ Failed to create workflow:', err);
      setError(err.message || 'Failed to create workflow');
    } finally {
      setLoading(false);
    }
  };

  const validateDocumentId = (value: string) => {
    const num = parseInt(value);
    return !isNaN(num) && num > 0;
  };

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      
      <main className="flex-1 flex flex-col">
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-8 py-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate('/workflow')}
                className="flex items-center"
              >
                <ArrowLeft className="w-4 h-4 mr-2" />
                Back to Workflows
              </Button>
              <div>
                <h1 className="text-2xl font-semibold text-gray-900">New Workflow</h1>
                <p className="text-sm text-gray-500 mt-1">Create a new workflow instance</p>
              </div>
            </div>
          </div>
        </header>

        {/* Form Content */}
        <div className="flex-1 p-8">
          <div className="max-w-2xl mx-auto">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <FileText className="w-5 h-5 mr-2" />
                  Workflow Details
                </CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* Error Display */}
                  {error && (
                    <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-start">
                      <AlertCircle className="w-5 h-5 text-red-500 mt-0.5 mr-3 flex-shrink-0" />
                      <div>
                        <p className="text-red-700 text-sm font-medium">Error</p>
                        <p className="text-red-600 text-sm mt-1">{error}</p>
                      </div>
                    </div>
                  )}

                  {/* Templates Error Display */}
                  {templatesError && (
                    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-start">
                      <AlertCircle className="w-5 h-5 text-yellow-500 mt-0.5 mr-3 flex-shrink-0" />
                      <div>
                        <p className="text-yellow-700 text-sm font-medium">Template Loading Issue</p>
                        <p className="text-yellow-600 text-sm mt-1">{templatesError}</p>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={loadTemplates}
                          className="mt-2"
                        >
                          Retry Loading Templates
                        </Button>
                      </div>
                    </div>
                  )}

                  {/* Workflow Title */}
                  <div className="space-y-2">
                    <Label htmlFor="title">Workflow Title</Label>
                    <Input
                      id="title"
                      name="title"
                      type="text"
                      placeholder="Enter workflow title (optional)"
                      value={formData.title}
                      onChange={handleChange}
                      className="w-full"
                    />
                    <p className="text-xs text-gray-500">
                      Optional: Custom title for this workflow instance
                    </p>
                  </div>

                  {/* Description */}
                  <div className="space-y-2">
                    <Label htmlFor="description">Description</Label>
                    <textarea
                      id="description"
                      name="description"
                      rows={3}
                      placeholder="Enter workflow description (optional)"
                      value={formData.description}
                      onChange={handleChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>

                  {/* Document ID */}
                  <div className="space-y-2">
                    <Label htmlFor="documentId">Document ID *</Label>
                    <Input
                      id="documentId"
                      name="documentId"
                      type="number"
                      min="1"
                      placeholder="Enter document ID"
                      value={formData.documentId}
                      onChange={handleChange}
                      required
                      className={`w-full ${
                        formData.documentId && !validateDocumentId(formData.documentId)
                          ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                          : ''
                      }`}
                    />
                    <p className="text-xs text-gray-500">
                      The ID of the document this workflow will process
                    </p>
                    {formData.documentId && !validateDocumentId(formData.documentId) && (
                      <p className="text-xs text-red-600">Please enter a valid document ID (positive number)</p>
                    )}
                  </div>

                  {/* Template Selection */}
                  <div className="space-y-2">
                    <Label htmlFor="templateId">Workflow Template *</Label>
                    {templatesLoading ? (
                      <div className="flex items-center justify-center py-8">
                        <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mr-3"></div>
                        <span className="text-gray-600">Loading templates...</span>
                      </div>
                    ) : (
                      <select
                        id="templateId"
                        name="templateId"
                        value={formData.templateId}
                        onChange={handleChange}
                        required
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
                      >
                        <option value="">Select a workflow template</option>
                        {Array.isArray(templates) && templates.length > 0 ? (
                          templates.map((template) => (
                            <option key={template.id} value={template.id}>
                              {template.name} - {template.type}
                            </option>
                          ))
                        ) : (
                          <option disabled>No templates available</option>
                        )}
                      </select>
                    )}
                    <p className="text-xs text-gray-500">
                      Choose the workflow template that defines the approval process
                    </p>
                    {!templatesLoading && Array.isArray(templates) && templates.length === 0 && !templatesError && (
                      <p className="text-xs text-yellow-600">
                        No active templates available. Contact an administrator.
                      </p>
                    )}
                  </div>

                  {/* ✅ FIXED: Priority Selection with Proper Controlled Component */}
                  <div className="space-y-2">
                    <Label htmlFor="priority">Priority</Label>
                    <select
                      id="priority"
                      name="priority"
                      value={formData.priority} // ✅ CRITICAL: Controlled value
                      onChange={handleChange} // ✅ CRITICAL: Proper onChange handler
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
                    >
                      <option value="LOW">Low</option>
                      <option value="NORMAL">Normal</option>
                      <option value="HIGH">High</option>
                      <option value="URGENT">Urgent</option>
                    </select>
                    
                    {/* ✅ ADD: Debug display (remove in production) */}
                    {process.env.NODE_ENV === 'development' && (
                      <p className="text-xs text-blue-600">
                        Debug: Current priority = "{formData.priority}"
                      </p>
                    )}
                    
                    <p className="text-xs text-gray-500">
                      Set the priority level for this workflow
                    </p>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex justify-end space-x-3 pt-6 border-t border-gray-200">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => navigate('/workflow')}
                      disabled={loading}
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      disabled={
                        loading || 
                        !formData.documentId || 
                        !formData.templateId ||
                        !validateDocumentId(formData.documentId) ||
                        templates.length === 0
                      }
                      className="bg-blue-600 hover:bg-blue-700 flex items-center"
                    >
                      {loading ? (
                        <>
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
                          Creating...
                        </>
                      ) : (
                        <>
                          <Plus className="w-4 h-4 mr-2" />
                          Create Workflow
                        </>
                      )}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
}
