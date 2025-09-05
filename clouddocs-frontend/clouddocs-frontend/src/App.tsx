import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Register from './pages/Register';
import Login from './pages/Login';
import Documents from './pages/Documents';
import DocumentDetails from './pages/DocumentDetails';
import Dashboard from './pages/Dashboard';
import Workflow from './pages/Workflow';
import WorkflowNew from './pages/WorkflowNew';
import WorkflowDetails from './pages/WorkflowDetails';
import AnalyticsDashboard from './pages/AnalyticsDashboard';
import AuditTrail from './pages/AuditTrail';
import Settings from './pages/Settings';
import ProfilePage from './pages/ProfilePage';
import authService from './services/auth';
import api from './services/api'; // ✅ Import the API client
import './styles/globals.css';

// Protected Route Component
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = authService.isAuthenticated();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  return <>{children}</>;
};

// Public Route Component (redirect if already authenticated)
const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = authService.isAuthenticated();
  
  if (isAuthenticated) {
    return <Navigate to="/documents" replace />;
  }
  
  return <>{children}</>;
};

const App: React.FC = () => {
  // ✅ MOVED: useEffect inside the App component
  useEffect(() => {
    // Clear cache on app start to ensure fresh data
    const clearCacheOnStart = async () => {
      try {
        await api.clearCache();
        console.log('✅ App started with fresh cache');
      } catch (error) {
        console.warn('⚠️ Failed to clear cache on start:', error);
      }
    };

    clearCacheOnStart();
  }, []);

  // ✅ OPTIONAL: Add periodic cache clearing for long-running sessions
  useEffect(() => {
    const interval = setInterval(async () => {
      try {
        await api.clearCache();
        console.log('🔄 Periodic cache clear completed');
      } catch (error) {
        console.warn('⚠️ Periodic cache clear failed:', error);
      }
    }, 5 * 60 * 1000); // Every 5 minutes

    return () => clearInterval(interval);
  }, []);

  return (
    <Router>
      <div className="App">
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          } />
          
          <Route path="/register" element={
            <PublicRoute>
              <Register />
            </PublicRoute>
          } />
          
          {/* Protected Routes */}
          <Route path="/documents" element={
            <ProtectedRoute>
              <Documents />
            </ProtectedRoute>
          } />
          
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } />
          
          <Route path="/workflow" element={
            <ProtectedRoute>
              <Workflow />
            </ProtectedRoute>
          } />

          <Route path="/workflow/new" element={
            <ProtectedRoute>
              <WorkflowNew />
            </ProtectedRoute>
          } />

          <Route path="/workflow/:id" element={
            <ProtectedRoute>
              <WorkflowDetails />
            </ProtectedRoute>
          } />

          <Route path="/audit-trail" element={
            <ProtectedRoute>
              <AuditTrail />
            </ProtectedRoute>
          } />

          <Route path="/settings" element={
            <ProtectedRoute>
              <Settings />
            </ProtectedRoute>
          } />

          <Route path="/documents/:documentId" element={
            <ProtectedRoute>
              <DocumentDetails />
            </ProtectedRoute>
          } />

          <Route path="/analytics" element={
            <ProtectedRoute>
              <AnalyticsDashboard />
            </ProtectedRoute>
          } />

          <Route path="/profile" element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          } />
          
          {/* Default redirect */}
          <Route path="/" element={<Navigate to="/documents" replace />} />
        </Routes>
      </div>
    </Router>
  );
};

export default App;
