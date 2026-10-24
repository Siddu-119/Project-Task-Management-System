import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Attach the JWT (saved at login) to every request
api.interceptors.request.use((config) => {
  const stored = localStorage.getItem('user');
  const token = stored ? JSON.parse(stored).token : null;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// If the token is missing/expired, send the user back to the login page
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isLoginCall = err.config?.url?.includes('/auth/login');
    if (err.response?.status === 401 && !isLoginCall) {
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const login = (email, password) =>
  api.post('/auth/login', { email, password });

export const getProjects = (params) => api.get('/projects', { params });
export const getProject = (id) => api.get(`/projects/${id}`);
export const createProject = (data) => api.post('/projects', data);
export const updateProject = (id, data) => api.put(`/projects/${id}`, data);
export const deleteProject = (id) => api.delete(`/projects/${id}`);

export const getAllTasks = () => api.get('/tasks');
export const getTasks = (params) => api.get('/tasks', { params });
export const getTask = (id) => api.get(`/tasks/${id}`);
export const getTasksByUser = (userId) => api.get(`/tasks/user/${userId}`);
export const getTasksByProject = (projectId) => api.get(`/tasks/project/${projectId}`);
export const createTask = (data) => api.post('/tasks', data);
export const updateTask = (id, data) => api.put(`/tasks/${id}`, data);
export const updateTaskStatus = (id, status) => api.patch(`/tasks/${id}/status`, { status });
export const deleteTask = (id) => api.delete(`/tasks/${id}`);

export const getEmployees = () => api.get('/users/employees');
export const getAllUsers = () => api.get('/users');


// ----- V2 -----
export const getProjectTasks = (id) => api.get(`/projects/${id}/tasks`);
export const getProjectMembers = (id) => api.get(`/projects/${id}/members`);
export const addProjectMember = (id, userId) => api.post(`/projects/${id}/members`, { userId });
export const removeProjectMember = (id, userId) => api.delete(`/projects/${id}/members/${userId}`);
export const getProjectActivity = (id) => api.get(`/projects/${id}/activity`);

export const getComments = (taskId) => api.get(`/tasks/${taskId}/comments`);
export const addComment = (taskId, text) => api.post(`/tasks/${taskId}/comments`, { text });

export const getNotifications = () => api.get('/notifications');
export const getUnreadCount = () => api.get('/notifications/unread-count');
export const markNotificationRead = (id) => api.patch(`/notifications/${id}/read`);
export const markAllNotificationsRead = () => api.patch('/notifications/read-all');

export const getDashboard = () => api.get('/dashboard');
export const getReports = () => api.get('/reports');

export const getMe = () => api.get('/users/me');
export const changePassword = (currentPassword, newPassword) =>
  api.put('/users/me/password', { currentPassword, newPassword });

export default api;
