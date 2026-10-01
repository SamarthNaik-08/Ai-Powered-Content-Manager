import React, { Suspense, lazy } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, Spin } from 'antd';
import { AuthProvider } from '@/features/auth/context/AuthContext';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { DashboardLayout } from '@/layouts/DashboardLayout';
import { PlaceholderPage } from '@/components/PlaceholderPage';

// Code-split page components for production performance
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage').then(m => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('@/features/auth/pages/RegisterPage').then(m => ({ default: m.RegisterPage })));
const ForgotPasswordPage = lazy(() => import('@/features/auth/pages/ForgotPasswordPage').then(m => ({ default: m.ForgotPasswordPage })));
const ResetPasswordPage = lazy(() => import('@/features/auth/pages/ResetPasswordPage').then(m => ({ default: m.ResetPasswordPage })));
const DashboardPage = lazy(() => import('@/features/dashboard/pages/DashboardPage').then(m => ({ default: m.DashboardPage })));
const ContentListPage = lazy(() => import('@/features/content/pages/ContentListPage').then(m => ({ default: m.ContentListPage })));
const ContentEditorPage = lazy(() => import('@/features/content/pages/ContentEditorPage').then(m => ({ default: m.ContentEditorPage })));
const ContentDetailPage = lazy(() => import('@/features/content/pages/ContentDetailPage').then(m => ({ default: m.ContentDetailPage })));
const CategoriesPage = lazy(() => import('@/features/content/pages/CategoriesPage').then(m => ({ default: m.CategoriesPage })));
const TagsPage = lazy(() => import('@/features/content/pages/TagsPage').then(m => ({ default: m.TagsPage })));
const MediaLibraryPage = lazy(() => import('@/features/media/pages/MediaLibraryPage').then(m => ({ default: m.MediaLibraryPage })));
const UserManagementPage = lazy(() => import('@/features/users/pages/UserManagementPage').then(m => ({ default: m.UserManagementPage })));
const AuditLogsPage = lazy(() => import('@/features/audit/pages/AuditLogsPage').then(m => ({ default: m.AuditLogsPage })));
const AnalyticsPage = lazy(() => import('@/features/analytics/pages/AnalyticsPage').then(m => ({ default: m.AnalyticsPage })));

const PageFallback: React.FC = () => (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
    <Spin size="large" />
  </div>
);

export const App: React.FC = () => {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#2563eb',
          borderRadius: 8,
          fontFamily:
            '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
        },
      }}
    >
      <AuthProvider>
        <Suspense fallback={<PageFallback />}>
          <Routes>
            {/* Public Authentication Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />

            {/* Authenticated Dashboard Routes */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <DashboardLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<DashboardPage />} />

              {/* Phase 2: Content Management */}
              <Route path="content" element={<ContentListPage />} />
              <Route path="content/create" element={<ContentEditorPage mode="create" />} />
              <Route path="content/:id" element={<ContentDetailPage />} />
              <Route path="content/:id/edit" element={<ContentEditorPage mode="edit" />} />

              {/* Taxonomies */}
              <Route path="categories" element={<CategoriesPage />} />
              <Route path="tags" element={<TagsPage />} />

              {/* Phase 3 & 4: Enterprise Media, Users, Audit & Analytics */}
              <Route
                path="media"
                element={
                  <ProtectedRoute requiredPermission="MEDIA_MANAGE">
                    <MediaLibraryPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="users"
                element={
                  <ProtectedRoute requiredRole="ROLE_ADMIN">
                    <UserManagementPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="audit-logs"
                element={
                  <ProtectedRoute requiredRole="ROLE_ADMIN">
                    <AuditLogsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="analytics"
                element={
                  <ProtectedRoute requiredRole="ROLE_EDITOR">
                    <AnalyticsPage />
                  </ProtectedRoute>
                }
              />

              {/* Settings Placeholder */}
              <Route
                path="settings"
                element={
                  <PlaceholderPage
                    moduleName="Settings & Platform Configuration"
                    targetPhase="Phase 5"
                    description="System configuration, AI model parameters, quota defaults, and integrations."
                    features={[
                      'LLM Provider Switcher (OpenAI / Claude / Local)',
                      'Default Token Quotas & Role Rate Limits',
                      'Email & Webhook Notification Endpoints',
                      'Media Storage Provider (Local / S3 / Cloudflare R2)',
                      'Security & Session Lifetime Policies',
                    ]}
                  />
                }
              />
            </Route>

            {/* Catch-all redirect */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Suspense>
      </AuthProvider>
    </ConfigProvider>
  );
};

export default App;
