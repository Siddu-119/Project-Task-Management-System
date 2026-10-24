export const STATUSES = ['TODO', 'IN_PROGRESS', 'REVIEW', 'COMPLETED'];
export const statusLabel = { TODO: 'To Do', IN_PROGRESS: 'In Progress', REVIEW: 'Review', COMPLETED: 'Completed' };
export const statusClass = { TODO: 'badge-pending', IN_PROGRESS: 'badge-progress', REVIEW: 'badge-review', COMPLETED: 'badge-completed' };

export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
export const priorityLabel = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', URGENT: 'Urgent' };
export const priorityClass = { LOW: 'prio-low', MEDIUM: 'prio-medium', HIGH: 'prio-high', URGENT: 'prio-urgent' };

export const PROJECT_STATUSES = ['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED'];
export const projectStatusLabel = { PLANNING: 'Planning', ACTIVE: 'Active', ON_HOLD: 'On Hold', COMPLETED: 'Completed' };
export const projectStatusClass = { PLANNING: 'badge-review', ACTIVE: 'badge-progress', ON_HOLD: 'badge-pending', COMPLETED: 'badge-completed' };

// Turns an Axios error into a friendly message
export const errMsg = (err, fallback = 'Something went wrong. Please try again.') => {
  if (err.response?.data?.error) return err.response.data.error;
  if (err.response?.status === 403) return 'You do not have permission to do this.';
  if (!err.response) return 'Cannot reach the server. Is the backend running?';
  return fallback;
};

export const fmtDate = (d) => (d ? new Date(d + (d.length === 10 ? 'T00:00:00' : '')).toLocaleDateString() : '—');
export const fmtDateTime = (d) => (d ? new Date(d).toLocaleString() : '—');
