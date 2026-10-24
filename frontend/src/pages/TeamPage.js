import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getProjects, getProjectMembers, addProjectMember, removeProjectMember, getAllUsers, getTasks } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import { Loader, EmptyState, ErrorBox, ProgressBar } from '../components/ui';
import { statusLabel, statusClass, errMsg, fmtDate } from '../utils/helpers';

export default function TeamPage() {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const isAdmin = user?.role === 'ADMIN';

  const [projects, setProjects] = useState([]);
  const [users, setUsers] = useState([]);
  const [members, setMembers] = useState([]);
  const [projectId, setProjectId] = useState(params.get('project') || '');
  const [newUserId, setNewUserId] = useState('');
  const [expanded, setExpanded] = useState(null);   // userId whose tasks are shown
  const [memberTasks, setMemberTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getProjects().then(r => {
      setProjects(r.data);
      if (!projectId && r.data.length > 0) setProjectId(String(r.data[0].id));
      setLoading(false);
    }).catch(e => { setError(errMsg(e, 'Could not load projects.')); setLoading(false); });
    if (isAdmin) getAllUsers().then(r => setUsers(r.data)).catch(() => {});
  }, []);

  const loadMembers = useCallback(() => {
    if (!projectId) return;
    setError('');
    getProjectMembers(projectId).then(r => setMembers(r.data)).catch(e => setError(errMsg(e, 'Could not load team.')));
  }, [projectId]);

  useEffect(() => { loadMembers(); setExpanded(null); }, [loadMembers]);

  const pickProject = (e) => { setProjectId(e.target.value); setParams({ project: e.target.value }); };

  const handleAdd = async () => {
    if (!newUserId) return toast.error('Please choose a user to add.');
    try { await addProjectMember(projectId, Number(newUserId)); toast.success('Member added.'); setNewUserId(''); loadMembers(); }
    catch (e) { toast.error(errMsg(e, 'Failed to add member.')); }
  };

  const handleRemove = async (m) => {
    if (!window.confirm(`Remove ${m.name} from this project? Their tasks here will become unassigned.`)) return;
    try { await removeProjectMember(projectId, m.userId); toast.success('Member removed.'); loadMembers(); }
    catch (e) { toast.error(errMsg(e, 'Failed to remove member.')); }
  };

  const toggleTasks = async (m) => {
    if (expanded === m.userId) return setExpanded(null);
    try {
      const r = await getTasks({ projectId, assignedToId: m.userId });
      setMemberTasks(r.data); setExpanded(m.userId);
    } catch (e) { toast.error(errMsg(e, 'Could not load tasks.')); }
  };

  const available = users.filter(u => !members.some(m => m.userId === u.id));

  if (loading) return <Loader />;
  if (projects.length === 0) return <div className="section"><EmptyState icon="👥" text="No projects available yet, so there are no teams to show." /></div>;

  return (
    <div>
      <div className="section">
        <div className="section-header">
          <span className="section-title">👥 Team</span>
          <select className="status-select" value={projectId} onChange={pickProject}>
            {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>

        {isAdmin && (
          <div className="filter-bar">
            <select className="form-control" value={newUserId} onChange={e => setNewUserId(e.target.value)}>
              <option value="">— Add a member —</option>
              {available.map(u => <option key={u.id} value={u.id}>{u.name} ({u.role})</option>)}
            </select>
            <button className="btn btn-primary" onClick={handleAdd}>+ Add to Project</button>
          </div>
        )}

        {error && <div style={{ padding: 16 }}><ErrorBox message={error} onRetry={loadMembers} /></div>}
        {members.length === 0 && !error ? <EmptyState icon="👥" text="No members in this project yet." /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Member</th><th>Role</th><th>Workload</th><th>Open</th><th>Done</th><th>Overdue</th><th>Actions</th></tr></thead>
              <tbody>
                {members.map(m => (
                  <React.Fragment key={m.userId}>
                    <tr>
                      <td><span className="avatar sm">{m.name.charAt(0)}</span> <span className="fw-600">{m.name}</span>
                        <div className="text-muted">{m.email}</div></td>
                      <td><span className={`badge ${m.role === 'ADMIN' ? 'badge-admin' : 'badge-employee'}`}>{m.role}</span></td>
                      <td style={{ minWidth: 120 }}>
                        <ProgressBar value={m.totalTasks ? Math.round(m.completedTasks * 100 / m.totalTasks) : 0} />
                        <span className="text-muted">{m.totalTasks} task(s)</span></td>
                      <td>{m.openTasks}</td>
                      <td>{m.completedTasks}</td>
                      <td className={m.overdueTasks ? 'text-danger' : ''}>{m.overdueTasks}</td>
                      <td>
                        <div className="action-group">
                          <button className="btn btn-ghost btn-sm" onClick={() => toggleTasks(m)}>{expanded === m.userId ? 'Hide tasks' : 'View tasks'}</button>
                          {isAdmin && <button className="btn btn-danger btn-sm" onClick={() => handleRemove(m)}>Remove</button>}
                        </div>
                      </td>
                    </tr>
                    {expanded === m.userId && (
                      <tr><td colSpan={7} className="expand-cell">
                        {memberTasks.length === 0 ? <span className="text-muted">No tasks assigned in this project.</span>
                          : memberTasks.map(t => (
                            <div key={t.id} className="row-between list-row clickable" onClick={() => navigate(`/tasks/${t.id}`)}>
                              <span>{t.title} <span className="text-muted">· due {fmtDate(t.deadline)}</span></span>
                              <span className={`badge ${statusClass[t.status]}`}>{statusLabel[t.status]}</span>
                            </div>
                          ))}
                      </td></tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
