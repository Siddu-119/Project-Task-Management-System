import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProjects, deleteProject } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import ProjectFormModal from '../components/ProjectFormModal';
import { Loader, EmptyState, ErrorBox, ProgressBar } from '../components/ui';
import { PROJECT_STATUSES, projectStatusLabel, projectStatusClass, errMsg, fmtDate } from '../utils/helpers';

export default function ProjectsPage() {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const isAdmin = user?.role === 'ADMIN';

  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [modal, setModal] = useState(null); // null | 'new' | project

  const load = () => {
    setError('');
    getProjects({ search: search || undefined, status: status || undefined })
      .then(r => setProjects(r.data))
      .catch(e => setError(errMsg(e, 'Could not load projects.')))
      .finally(() => setLoading(false));
  };
  useEffect(load, [search, status]);

  const handleDelete = async (p) => {
    if (!window.confirm(`Delete "${p.name}"? Its ${p.totalTasks} task(s), comments and team will also be deleted.`)) return;
    try { await deleteProject(p.id); toast.success('Project deleted.'); load(); }
    catch (e) { toast.error(errMsg(e, 'Failed to delete project.')); }
  };

  return (
    <div>
      <div className="section">
        <div className="section-header">
          <span className="section-title">📁 Projects</span>
          <div className="action-group wrap">
            <input className="form-control filter-input" placeholder="Search by name..." value={search} onChange={e => setSearch(e.target.value)} />
            <select className="status-select" value={status} onChange={e => setStatus(e.target.value)}>
              <option value="">All Statuses</option>
              {PROJECT_STATUSES.map(s => <option key={s} value={s}>{projectStatusLabel[s]}</option>)}
            </select>
            {isAdmin && <button className="btn btn-primary" onClick={() => setModal('new')}>+ New Project</button>}
          </div>
        </div>

        {error && <div style={{ padding: 16 }}><ErrorBox message={error} onRetry={load} /></div>}
        {loading ? <Loader />
          : projects.length === 0 && !error ? <EmptyState icon="📁" text={search || status ? 'No projects match your filters.' : (isAdmin ? 'No projects yet. Create your first one!' : 'You are not a member of any project yet.')} />
          : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>Project</th><th>Status</th><th>Progress</th><th>Tasks</th><th>Team</th><th>Start</th><th>End</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  {projects.map(p => (
                    <tr key={p.id}>
                      <td><span className="link fw-600" onClick={() => navigate(`/projects/${p.id}`)}>{p.name}</span>
                        <div className="text-muted clamp">{p.description || ''}</div></td>
                      <td><span className={`badge ${projectStatusClass[p.status]}`}>{projectStatusLabel[p.status]}</span></td>
                      <td style={{ minWidth: 120 }}><ProgressBar value={p.progress} /><span className="text-muted">{p.progress}%</span></td>
                      <td className="text-muted">{p.completedTasks}/{p.totalTasks}</td>
                      <td className="text-muted">{p.memberCount}</td>
                      <td className="text-muted">{fmtDate(p.startDate)}</td>
                      <td className="text-muted">{fmtDate(p.endDate)}</td>
                      <td>
                        <div className="action-group">
                          <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/projects/${p.id}`)}>👁 View</button>
                          {isAdmin && <button className="btn btn-ghost btn-sm" onClick={() => setModal(p)}>✏</button>}
                          {isAdmin && <button className="btn btn-danger btn-sm" onClick={() => handleDelete(p)}>🗑</button>}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
      </div>

      {modal && (
        <ProjectFormModal
          project={modal === 'new' ? null : modal}
          onClose={() => setModal(null)}
          onSaved={(msg) => { setModal(null); toast.success(msg); load(); }}
        />
      )}
    </div>
  );
}
