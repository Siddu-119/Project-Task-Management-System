import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDashboard } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Loader, EmptyState, ErrorBox, ProgressBar } from '../components/ui';
import { statusLabel, statusClass, priorityClass, priorityLabel, projectStatusLabel, projectStatusClass, errMsg, fmtDate } from '../utils/helpers';

const COLORS = { TODO: '#f59e0b', IN_PROGRESS: '#4f8ef7', REVIEW: '#7c5cfc', COMPLETED: '#22c55e' };

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = () => {
    setError('');
    getDashboard().then(r => setData(r.data)).catch(e => setError(errMsg(e, 'Could not load the dashboard.')));
  };
  useEffect(load, []);

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!data) return <Loader text="Loading dashboard..." />;

  const mine = data.scope === 'MINE';
  const cards = [
    { label: 'Total Projects', value: data.totalProjects, sub: mine ? 'Projects you are in' : 'All projects', color: 'var(--accent)' },
    { label: 'Active Projects', value: data.activeProjects, sub: 'Currently running', color: 'var(--accent2)' },
    { label: 'Completed Projects', value: data.completedProjects, sub: 'Finished', color: 'var(--success)' },
    { label: 'Total Tasks', value: data.totalTasks, sub: mine ? 'Assigned to you' : 'Across all projects', color: 'var(--accent)' },
    { label: 'Pending Tasks', value: data.pendingTasks, sub: 'To do', color: 'var(--warning)' },
    { label: 'In Progress', value: data.inProgressTasks, sub: 'Being worked on', color: 'var(--accent)' },
    { label: 'In Review', value: data.reviewTasks, sub: 'Waiting for review', color: 'var(--accent2)' },
    { label: 'Completed Tasks', value: data.completedTasks, sub: 'Done ✓', color: 'var(--success)' },
    { label: 'Overdue Tasks', value: data.overdueTasks, sub: 'Past due date', color: 'var(--danger)' },
  ];

  // Donut chart built with CSS conic-gradient (no chart library)
  const parts = [['TODO', data.pendingTasks], ['IN_PROGRESS', data.inProgressTasks], ['REVIEW', data.reviewTasks], ['COMPLETED', data.completedTasks]];
  let acc = 0;
  const stops = data.totalTasks === 0 ? 'var(--bg3) 0 100%' : parts.map(([k, v]) => {
    const start = acc; acc += (v / data.totalTasks) * 100;
    return `${COLORS[k]} ${start}% ${acc}%`;
  }).join(', ');

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h2 style={{ fontSize: 20, fontWeight: 700 }}>Good day, {user?.name} 👋</h2>
        <p className="text-muted">{mine ? 'Here is a summary of your work.' : "Here's what's happening across your projects."}</p>
      </div>

      <div className="stats-row">
        {cards.map(c => (
          <div key={c.label} className="stat-card" style={{ '--accent-color': c.color }}>
            <div className="stat-label">{c.label}</div>
            <div className="stat-value">{c.value}</div>
            <div className="stat-sub">{c.sub}</div>
          </div>
        ))}
      </div>

      <div className="grid-2">
        <div className="section">
          <div className="section-header"><span className="section-title">🍩 Task Status</span></div>
          <div className="donut-wrap">
            <div className="donut" style={{ background: `conic-gradient(${stops})` }}>
              <div className="donut-hole"><strong>{data.totalTasks}</strong><span>tasks</span></div>
            </div>
            <div className="legend">
              {parts.map(([k, v]) => (
                <div key={k} className="legend-item"><span className="dot" style={{ background: COLORS[k] }} />{statusLabel[k]}: <strong>{v}</strong></div>
              ))}
            </div>
          </div>
        </div>

        <div className="section">
          <div className="section-header"><span className="section-title">📈 Project Progress</span></div>
          {data.projectProgress.length === 0
            ? <EmptyState icon="📁" text="No projects yet." />
            : data.projectProgress.map(p => (
              <div key={p.id} className="list-row clickable" onClick={() => navigate(`/projects/${p.id}`)}>
                <div className="row-between">
                  <span className="fw-600">{p.name}</span>
                  <span className="text-muted">{p.progress}% · {p.completedTasks}/{p.totalTasks} tasks</span>
                </div>
                <ProgressBar value={p.progress} />
              </div>
            ))}
        </div>
      </div>

      <div className="grid-2">
        <div className="section">
          <div className="section-header"><span className="section-title">📁 Recent Projects</span></div>
          {data.recentProjects.length === 0
            ? <EmptyState icon="📁" text="No projects yet." />
            : data.recentProjects.map(p => (
              <div key={p.id} className="list-row clickable" onClick={() => navigate(`/projects/${p.id}`)}>
                <div className="row-between">
                  <span className="fw-600">{p.name}</span>
                  <span className={`badge ${projectStatusClass[p.status]}`}>{projectStatusLabel[p.status]}</span>
                </div>
                <span className="text-muted">{p.totalTasks} tasks · {p.memberCount} members</span>
              </div>
            ))}
        </div>

        <div className="section">
          <div className="section-header"><span className="section-title">✅ Recent Tasks</span></div>
          {data.recentTasks.length === 0
            ? <EmptyState icon="✅" text="No tasks yet." />
            : data.recentTasks.map(t => (
              <div key={t.id} className="list-row clickable" onClick={() => navigate(`/tasks/${t.id}`)}>
                <div className="row-between">
                  <span className="fw-600">{t.title}</span>
                  <span className={`badge ${statusClass[t.status]}`}>{statusLabel[t.status]}</span>
                </div>
                <span className="text-muted">
                  {t.projectName || 'No project'} · <span className={`badge ${priorityClass[t.priority]}`}>{priorityLabel[t.priority]}</span>
                  {t.deadline && <> · due <span className={t.overdue ? 'text-danger' : ''}>{fmtDate(t.deadline)}</span></>}
                </span>
              </div>
            ))}
        </div>
      </div>
    </div>
  );
}
