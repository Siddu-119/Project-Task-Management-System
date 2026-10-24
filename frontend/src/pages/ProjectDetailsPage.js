import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getProject, getProjectTasks, getProjectMembers, getProjectActivity, updateTaskStatus, getProjects } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import KanbanBoard from '../components/KanbanBoard';
import ProjectFormModal from '../components/ProjectFormModal';
import TaskFormModal from '../components/TaskFormModal';
import { Loader, EmptyState, ErrorBox, ProgressBar } from '../components/ui';
import { projectStatusLabel, projectStatusClass, errMsg, fmtDate, fmtDateTime } from '../utils/helpers';

export default function ProjectDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const toast = useToast();
  const isAdmin = user?.role === 'ADMIN';

  const [project, setProject] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [members, setMembers] = useState([]);
  const [activity, setActivity] = useState([]);
  const [allProjects, setAllProjects] = useState([]);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null); // 'edit' | 'task'

  const load = useCallback(() => {
    setError('');
    Promise.all([getProject(id), getProjectTasks(id), getProjectMembers(id), getProjectActivity(id)])
      .then(([p, t, m, a]) => { setProject(p.data); setTasks(t.data); setMembers(m.data); setActivity(a.data); })
      .catch(e => setError(errMsg(e, 'Could not load this project.')));
  }, [id]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { if (isAdmin) getProjects().then(r => setAllProjects(r.data)).catch(() => {}); }, [isAdmin]);

  const canMove = (t) => isAdmin || t.assignedToId === user.id;

  const handleMove = async (task, status) => {
    // show the move instantly, then confirm with the server
    setTasks(ts => ts.map(t => t.id === task.id ? { ...t, status } : t));
    try {
      await updateTaskStatus(task.id, status);
      toast.success('Task moved.');
    } catch (e) {
      toast.error(errMsg(e, 'Could not move the task.'));
    }
    load();
    window.dispatchEvent(new Event('notifications-changed'));
  };

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!project) return <Loader text="Loading project..." />;

  const stats = [
    ['TODO', project.todoTasks], ['IN PROGRESS', project.inProgressTasks],
    ['REVIEW', project.reviewTasks], ['COMPLETED', project.completedTasks],
  ];

  return (
    <div>
      <button className="btn btn-ghost btn-sm" style={{ marginBottom: 16 }} onClick={() => navigate('/projects')}>← Back to Projects</button>

      <div className="section">
        <div className="section-header">
          <span className="section-title">📁 {project.name}</span>
          <div className="action-group wrap">
            <span className={`badge ${projectStatusClass[project.status]}`}>{projectStatusLabel[project.status]}</span>
            {isAdmin && <button className="btn btn-primary btn-sm" onClick={() => setModal('task')}>+ Add Task</button>}
            {isAdmin && <button className="btn btn-ghost btn-sm" onClick={() => setModal('edit')}>✏ Edit</button>}
          </div>
        </div>
        <div style={{ padding: 20 }}>
          <p style={{ marginBottom: 16 }}>{project.description || <span className="text-muted">No description.</span>}</p>
          <div className="row-between" style={{ marginBottom: 6 }}>
            <strong>Project Progress: {project.progress}%</strong>
            <span className="text-muted">📅 {fmtDate(project.startDate)} → {fmtDate(project.endDate)}</span>
          </div>
          <ProgressBar value={project.progress} />
          <div className="mini-stats">
            {stats.map(([label, v]) => <div key={label} className="mini-stat"><span>{label}</span><strong>{v}</strong></div>)}
            <div className="mini-stat danger"><span>OVERDUE</span><strong>{project.overdueTasks}</strong></div>
          </div>
        </div>
      </div>

      <div className="section">
        <div className="section-header"><span className="section-title">🗂 Kanban Board</span>
          <span className="text-muted">{isAdmin ? 'Drag cards between columns' : 'You can move your own tasks'}</span></div>
        <div style={{ padding: 16 }}>
          {tasks.length === 0 ? <EmptyState icon="🗂" text="No tasks in this project yet." />
            : <KanbanBoard tasks={tasks} canMove={canMove} onMove={handleMove} />}
        </div>
      </div>

      <div className="grid-2">
        <div className="section">
          <div className="section-header">
            <span className="section-title">👥 Team ({members.length})</span>
            <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/team?project=${id}`)}>Manage</button>
          </div>
          {members.length === 0 ? <EmptyState icon="👥" text="No team members yet." />
            : members.map(m => (
              <div key={m.userId} className="list-row">
                <div className="row-between">
                  <span><span className="avatar sm">{m.name.charAt(0)}</span> <span className="fw-600">{m.name}</span></span>
                  <span className="text-muted">{m.openTasks} open · {m.completedTasks} done</span>
                </div>
              </div>
            ))}
        </div>

        <div className="section">
          <div className="section-header"><span className="section-title">🕒 Recent Activity</span></div>
          {activity.length === 0 ? <EmptyState icon="🕒" text="No activity yet." />
            : activity.map((a, i) => (
              <div key={i} className="list-row">
                <div>{a.type === 'COMMENT' ? '💬' : a.type === 'MEMBER' ? '👤' : '✅'} {a.message}</div>
                <span className="text-muted">{fmtDateTime(a.time)}</span>
              </div>
            ))}
        </div>
      </div>

      {modal === 'edit' && (
        <ProjectFormModal project={project} onClose={() => setModal(null)}
          onSaved={(m) => { setModal(null); toast.success(m); load(); }} />
      )}
      {modal === 'task' && (
        <TaskFormModal defaultProjectId={project.id} projects={allProjects} onClose={() => setModal(null)}
          onSaved={(m) => { setModal(null); toast.success(m); load(); }} />
      )}
    </div>
  );
}
