// Create src/components/TimestampDebug.tsx
import React, { useState } from 'react';
import WorkflowService from '../services/workflowService';

const TimestampDebug: React.FC<{ workflowId: string }> = ({ workflowId }) => {
  const [debug, setDebug] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const checkTimestamp = async () => {
    setLoading(true);
    try {
      console.log('🔍 DEBUG: Fetching workflow data at:', new Date().toISOString());
      
      // Force fresh data
      const workflow = await WorkflowService.getWorkflowById(workflowId, true);
      
      setDebug({
        fetchTime: new Date().toISOString(),
        workflowData: workflow,
        updatedDate: workflow.updatedDate,
        browserTime: new Date().toISOString(),
        cacheHeaders: 'Check network tab for cache headers'
      });
      
      console.log('🔍 DEBUG: Workflow data received:', workflow);
    } catch (error) {
      console.error('❌ DEBUG: Error fetching workflow:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ border: '2px solid red', padding: '10px', margin: '10px' }}>
      <h3>🔍 Timestamp Debug</h3>
      <button onClick={checkTimestamp} disabled={loading}>
        {loading ? 'Checking...' : 'Check Current Timestamp'}
      </button>
      
      {debug && (
        <div style={{ marginTop: '10px', fontFamily: 'monospace' }}>
          <div><strong>Fetch Time:</strong> {debug.fetchTime}</div>
          <div><strong>Updated Date:</strong> {debug.updatedDate}</div>
          <div><strong>Browser Time:</strong> {debug.browserTime}</div>
          <div><strong>Workflow ID:</strong> {workflowId}</div>
          <div><strong>Status:</strong> {debug.workflowData?.status}</div>
          <details>
            <summary>Full Data</summary>
            <pre>{JSON.stringify(debug.workflowData, null, 2)}</pre>
          </details>
        </div>
      )}
    </div>
  );
};

export default TimestampDebug;
