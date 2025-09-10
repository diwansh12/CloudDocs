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
import OCRUpload from './components/OCRUpload'; // ✅ NEW: Import OCR Upload component
import Sidebar from './components/layout/Sidebar'; // ✅ NEW: Import Sidebar for OCR page
import authService from './services/auth';
import api from './services/api';
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

// ✅ NEW: OCR Layout Component for consistent styling
const OCRLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div className="flex h-screen bg-gray-50">
    <Sidebar />
    <main className="flex-1 flex flex-col">
      <header className="bg-white border-b border-gray-200 px-8 py-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 mb-1">
              📖 AI-Powered OCR Upload
            </h1>
            <p className="text-gray-600">
              Upload images and documents for intelligent text extraction and AI search
            </p>
          </div>
          <div className="flex items-center space-x-2">
            <div className="text-xs bg-green-100 text-green-700 px-3 py-1 rounded-full">
              ✨ OCR Active
            </div>
            <div className="text-xs bg-purple-100 text-purple-700 px-3 py-1 rounded-full">
              🤖 AI Ready
            </div>
          </div>
        </div>
      </header>
      <section className="flex-1 overflow-auto">
        {children}
      </section>
    </main>
  </div>
);

const App: React.FC = () => {
  // Cache clearing functionality
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

  // Periodic cache clearing for long-running sessions
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

          {/* ✅ NEW: OCR Upload Route */}
          <Route path="/ocr-upload" element={
            <ProtectedRoute>
              <OCRLayout>
                <div className="p-8">
                  <OCRUpload />
                </div>
              </OCRLayout>
            </ProtectedRoute>
          } />

          {/* ✅ NEW: Enhanced Search Route (optional) */}
          <Route path="/search" element={
            <ProtectedRoute>
              <Documents />
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

          {/* ✅ NEW: 404 Error Page */}
          <Route path="*" element={
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
              <div className="text-center">
                <div className="text-6xl mb-4">🤖</div>
                <h1 className="text-4xl font-bold text-gray-900 mb-2">404</h1>
                <p className="text-lg text-gray-600 mb-8">
                  Page not found in CloudDocs AI
                </p>
                <div className="space-x-4">
                  <button
                    onClick={() => window.history.back()}
                    className="px-6 py-3 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                  >
                    ← Go Back
                  </button>
                  <button
                    onClick={() => window.location.href = '/dashboard'}
                    className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    🏠 Dashboard
                  </button>
                </div>
              </div>
            </div>
          } />
        </Routes>
      </div>
    </Router>
  );
};

export default App;
