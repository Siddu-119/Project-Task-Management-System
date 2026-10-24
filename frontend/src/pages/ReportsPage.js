import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getReports } from '../services/api';
import { Loader, ErrorBox, ProgressBar, EmptyState } from '../components/ui';
import { projectStatusLabel, projectStatusClass, errMsg } from '../utils/helpers';

export default function ReportsPage() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = () => {
    setError('');
    getReports().then(r => setData(r.data)).catch(e => setError(errMsg(e, 'Could not load reports.')));
  };
  useEffect(load, []);

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!data) return <Loader text="Building reports..." />;

  const cards = [
    ['Total Projects', data.totalProjects, 'var(--accent)'],
    ['Completed Projects', data.completedProjects, 'var(--success)'],
    ['Active Projects', data.activeProjects, 'var(--accent2)'],
    ['Total Tasks', data.totalTasks, 'var(--accent)'],
    ['Completed Tasks', data.completedTasks, 'var(--success)'],
    ['Pending Tasks', data.pendingTasks, 'var(--warning)'],
    ['Overdue Tasks', data.overdueTasks, 'var(--danger)'],
  ];

  return (
    <div>
      <div className="stats-row">
        {cards.map(([label, value, color]) => (
          <div key={label} className="stat-card" style={{ '--accent-color': color }}>
            <div className="stat-label">{label}</div><div className="stat-value">{value}</div>
          </div>
        ))}
      </div>

      <div className="section">
        <div className="section-header"><span className="section-title">👤 Member-wise Task Statistics</span></div>
        {data.memberStats.length === 0 ? <EmptyState text="No members yet." /> : (
          <div className="table-wrap"><table>
            <thead><tr><th>Member</th><th>Total</th><th>To Do</th><th>In Progress</th><th>Review</th><th>Completed</th><th>Overdue</th></tr></thead>
            <tbody>{data.memberStats.map(m => (
              <tr key={m.userId}>
                <td className="fw-600">{m.name} <span className="text-muted">({m.role})</span></td>
                <td>{m.total}</td><td>{m.todo}</td><td>{m.inProgress}</td><td>{m.review}</td><td>{m.completed}</td>
                <td className={m.overdue ? 'text-danger' : ''}>{m.overdue}</td>
              </tr>))}
            </tbody></table></div>
        )}
      </div>

      <div className="section">
        <div className="section-header"><span className="section-title">📈 Project-wise Progress</span></div>
        {data.projectProgress.length === 0 ? <EmptyState icon="📁" text="No projects yet." /> : (
          <div className="table-wrap"><table>
            <thead><tr><th>Project</th><th>Status</th><th style={{ minWidth: 200 }}>Progress</th><th>Tasks Done</th></tr></thead>
            <tbody>{data.projectProgress.map(p => (
              <tr key={p.id}>
                <td><span className="link fw-600" onClick={() => navigate(`/projects/${p.id}`)}>{p.name}</span></td>
                <td><span className={`badge ${projectStatusClass[p.status]}`}>{projectStatusLabel[p.status]}</span></td>
                <td><ProgressBar value={p.progress} /><span className="text-muted">{p.progress}%</span></td>
                <td className="text-muted">{p.completedTasks}/{p.totalTasks}</td>
              </tr>))}
            </tbody></table></div>
        )}
      </div>
    </div>
  );
}
