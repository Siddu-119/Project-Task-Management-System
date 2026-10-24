import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTasks, deleteTask, updateTaskStatus, getProjects, getAllUsers } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import TaskFormModal from '../components/TaskFormModal';
import { Loader, EmptyState, ErrorBox } from '../components/ui';
import { STATUSES, statusLabel, statusClass, PRIORITIES, priorityLabel, priorityClass, errMsg, fmtDate } from '../utils/helpers';

const emptyFilters = { search: '', status: '', priority: '', assignedToId: '', projectId: '', dueFrom: '', dueTo: '', overdue: false };

export default function TasksPage() {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const isAdmin = user?.role === 'ADMIN';

  const [tasks, setTasks] = useState([]);
  const [projects, setProjects] = useState([]);
  const [users, setUsers] = useState([]);
  const [filters, setFilters] = useState(emptyFilters);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null); // null | 'new' | task

  const setF = (k) => (e) => setFilters({ ...filters, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const load = () => {
    setError('');
    const params = {};
    Object.entries(filters).forEach(([k, v]) => { if (v !== '' && v !== false) params[k] = v; });
    getTasks(params)
      .then(r => setTasks(r.data))
      .catch(e => setError(errMsg(e, 'Could not load tasks.')))
      .finally(() => setLoading(false));
  };
  useEffect(load, [filters]);

  useEffect(() => {
    getProjects().then(r => setProjects(r.data)).catch(() => {});
    getAllUsers().then(r => setUsers(r.data)).catch(() => {});
  }, []);

  const handleDelete = async (t) => {
    if (!window.confirm(`Delete task "${t.title}"?`)) return;
    try { await deleteTask(t.id); toast.success('Task deleted.'); load(); }
    catch (e) { toast.error(errMsg(e, 'Failed to delete task.')); }
  };

  const handleStatus = async (t, status) => {
    try { await updateTaskStatus(t.id, status); toast.success('Status updated.'); load(); window.dispatchEvent(new Event('notifications-changed')); }
    catch (e) { toast.error(errMsg(e, 'Failed to update status.')); }
  };

  const filtered = JSON.stringify(filters) !== JSON.stringify(emptyFilters);

  return (
    <div>
      <div className="section">
        <div className="section-header">
          <span className="section-title">✅ Tasks ({tasks.length})</span>
          {isAdmin && <button className="btn btn-primary" onClick={() => setModal('new')}>+ New Task</button>}
        </div>

        <div className="filter-bar">
          <input className="form-control" placeholder="Search by title..." value={filters.search} onChange={setF('search')} />
          <select className="form-control" value={filters.status} onChange={setF('status')}>
            <option value="">All Statuses</option>
            {STATUSES.map(s => <option key={s} value={s}>{statusLabel[s]}</option>)}
          </select>
          <select className="form-control" value={filters.priority} onChange={setF('priority')}>
            <option value="">All Priorities</option>
            {PRIORITIES.map(p => <option key={p} value={p}>{priorityLabel[p]}</option>)}
          </select>
          <select className="form-control" value={filters.assignedToId} onChange={setF('assignedToId')}>
            <option value="">All Members</option>
            {users.map(u => <option key={u.id} value={u.id}>{u.name}</option>)}
          </select>
          <select className="form-control" value={filters.projectId} onChange={setF('projectId')}>
            <option value="">All Projects</option>
            {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
          <label className="filter-date">Due from <input type="date" className="form-control" value={filters.dueFrom} onChange={setF('dueFrom')} /></label>
          <label className="filter-date">Due to <input type="date" className="form-control" value={filters.dueTo} onChange={setF('dueTo')} /></label>
          <label className="filter-check"><input type="checkbox" checked={filters.overdue} onChange={setF('overdue')} /> Overdue only</label>
          {filtered && <button className="btn btn-ghost btn-sm" onClick={() => setFilters(emptyFilters)}>Clear filters</button>}
        </div>

        {error && <div style={{ padding: 16 }}><ErrorBox message={error} onRetry={load} /></div>}
        {loading ? <Loader />
          : tasks.length === 0 && !error ? <EmptyState icon="✅" text={filtered ? 'No tasks match your filters.' : 'No tasks to show yet.'} />
          : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>Task</th><th>Project</th><th>Assigned To</th><th>Priority</th><th>Due Date</th><th>Status</th>{isAdmin && <th>Actions</th>}</tr>
                </thead>
                <tbody>
                  {tasks.map(t => {
                    const canUpdate = isAdmin || t.assignedToId === user.id;
                    return (
                      <tr key={t.id}>
                        <td><span className="link fw-600" onClick={() => navigate(`/tasks/${t.id}`)}>{t.title}</span></td>
                        <td className="text-muted">{t.projectName || '—'}</td>
                        <td>{t.assignedToName ? <span className="chip">👤 {t.assignedToName}</span> : <span className="text-muted">Unassigned</span>}</td>
                        <td><span className={`badge ${priorityClass[t.priority]}`}>{priorityLabel[t.priority]}</span></td>
                        <td className={t.overdue ? 'text-danger' : 'text-muted'}>{fmtDate(t.deadline)}{t.overdue && ' ⚠'}</td>
                        <td>
                          {canUpdate
                            ? <select className="status-select" value={t.status} onChange={e => handleStatus(t, e.target.value)}>
                                {STATUSES.map(s => <option key={s} value={s}>{statusLabel[s]}</option>)}
                              </select>
                            : <span className={`badge ${statusClass[t.status]}`}>{statusLabel[t.status]}</span>}
                        </td>
                        {isAdmin && (
                          <td>
                            <div className="action-group">
                              <button className="btn btn-ghost btn-sm" onClick={() => setModal(t)}>✏</button>
                              <button className="btn btn-danger btn-sm" onClick={() => handleDelete(t)}>🗑</button>
                            </div>
                          </td>
                        )}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
      </div>

      {modal && (
        <TaskFormModal
          task={modal === 'new' ? null : modal}
          projects={projects}
          onClose={() => setModal(null)}
          onSaved={(msg) => { setModal(null); toast.success(msg); load(); }}
        />
      )}
    </div>
  );
}
