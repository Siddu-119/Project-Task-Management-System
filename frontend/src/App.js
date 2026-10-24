import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './components/Toast';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ProjectsPage from './pages/ProjectsPage';
import ProjectDetailsPage from './pages/ProjectDetailsPage';
import TasksPage from './pages/TasksPage';
import TaskDetailsPage from './pages/TaskDetailsPage';
import TeamPage from './pages/TeamPage';
import NotificationsPage from './pages/NotificationsPage';
import ReportsPage from './pages/ReportsPage';
import ProfilePage from './pages/ProfilePage';
import Layout from './components/Layout';

// Wraps a page: must be logged in (and have the right role, if one is given)
const Protected = ({ role, children }) => {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to="/dashboard" replace />;
  return <Layout>{children}</Layout>;
};

function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/dashboard" element={<Protected><DashboardPage /></Protected>} />
            <Route path="/projects" element={<Protected><ProjectsPage /></Protected>} />
            <Route path="/projects/:id" element={<Protected><ProjectDetailsPage /></Protected>} />
            <Route path="/tasks" element={<Protected><TasksPage /></Protected>} />
            <Route path="/tasks/:id" element={<Protected><TaskDetailsPage /></Protected>} />
            <Route path="/team" element={<Protected><TeamPage /></Protected>} />
            <Route path="/notifications" element={<Protected><NotificationsPage /></Protected>} />
            <Route path="/reports" element={<Protected role="ADMIN"><ReportsPage /></Protected>} />
            <Route path="/profile" element={<Protected><ProfilePage /></Protected>} />

            {/* Old V1 links still work */}
            <Route path="/admin-dashboard" element={<Navigate to="/dashboard" replace />} />
            <Route path="/employee-dashboard" element={<Navigate to="/dashboard" replace />} />
            <Route path="/admin/projects" element={<Navigate to="/projects" replace />} />
            <Route path="/admin/tasks" element={<Navigate to="/tasks" replace />} />
            <Route path="/employee/tasks" element={<Navigate to="/tasks" replace />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  );
}

export default App;
