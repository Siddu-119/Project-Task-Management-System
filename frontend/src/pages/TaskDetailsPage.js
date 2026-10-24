import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getTask, getComments, addComment, updateTaskStatus, deleteTask, getProjects } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import TaskFormModal from '../components/TaskFormModal';
import { Loader, EmptyState, ErrorBox } from '../components/ui';
import { STATUSES, statusLabel, statusClass, priorityLabel, priorityClass, errMsg, fmtDate, fmtDateTime } from '../utils/helpers';

export default function TaskDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const toast = useToast();
  const isAdmin = user?.role === 'ADMIN';

  const [task, setTask] = useState(null);
  const [comments, setComments] = useState([]);
  const [projects, setProjects] = useState([]);
  const [text, setText] = useState('');
  const [posting, setPosting] = useState(false);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setError('');
    Promise.all([getTask(id), getComments(id)])
      .then(([t, c]) => { setTask(t.data); setComments(c.data); })
      .catch(e => setError(errMsg(e, 'Could not load this task.')));
  }, [id]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { if (isAdmin) getProjects().then(r => setProjects(r.data)).catch(() => {}); }, [isAdmin]);

  const handleStatus = async (status) => {
    try { await updateTaskStatus(task.id, status); toast.success('Status updated.'); load(); window.dispatchEvent(new Event('notifications-changed')); }
    catch (e) { toast.error(errMsg(e, 'Failed to update status.')); }
  };

  const handleDelete = async () => {
    if (!window.confirm('Delete this task and its comments?')) return;
    try { await deleteTask(task.id); toast.success('Task deleted.'); navigate('/tasks'); }
    catch (e) { toast.error(errMsg(e, 'Failed to delete task.')); }
  };

  const handleComment = async (e) => {
    e.preventDefault();
    if (!text.trim()) return toast.error('Comment cannot be empty.');
    setPosting(true);
    try { await addComment(task.id, text.trim()); setText(''); load(); }
    catch (err) { toast.error(errMsg(err, 'Failed to post comment.')); }
    finally { setPosting(false); }
  };

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!task) return <Loader text="Loading task..." />;

  const canUpdate = isAdmin || task.assignedToId === user.id;

  return (
    <div>
      <button className="btn btn-ghost btn-sm" style={{ marginBottom: 16 }} onClick={() => navigate(-1)}>← Back</button>

      <div className="section">
        <div className="section-header">
          <span className="section-title">✅ {task.title}</span>
          {isAdmin && (
            <div className="action-group">
              <button className="btn btn-ghost btn-sm" onClick={() => setEditing(true)}>✏ Edit</button>
              <button className="btn btn-danger btn-sm" onClick={handleDelete}>🗑 Delete</button>
            </div>
          )}
        </div>
        <div style={{ padding: 20 }}>
          <p style={{ marginBottom: 20 }}>{task.description || <span className="text-muted">No description.</span>}</p>
          <div className="detail-grid">
            <div><span className="text-muted">Project</span>
              <div>{task.projectId ? <span className="link" onClick={() => navigate(`/projects/${task.projectId}`)}>{task.projectName}</span> : '—'}</div></div>
            <div><span className="text-muted">Assigned to</span><div>{task.assignedToName || 'Unassigned'}</div></div>
            <div><span className="text-muted">Priority</span><div><span className={`badge ${priorityClass[task.priority]}`}>{priorityLabel[task.priority]}</span></div></div>
            <div><span className="text-muted">Due date</span><div className={task.overdue ? 'text-danger' : ''}>{fmtDate(task.deadline)}{task.overdue && ' ⚠ Overdue'}</div></div>
            <div><span className="text-muted">Status</span>
              <div>{canUpdate
                ? <select className="status-select" value={task.status} onChange={e => handleStatus(e.target.value)}>
                    {STATUSES.map(s => <option key={s} value={s}>{statusLabel[s]}</option>)}
                  </select>
                : <span className={`badge ${statusClass[task.status]}`}>{statusLabel[task.status]}</span>}</div></div>
            <div><span className="text-muted">Created</span><div>{fmtDateTime(task.createdAt)}</div></div>
            <div><span className="text-muted">Last updated</span><div>{fmtDateTime(task.updatedAt)}</div></div>
          </div>
        </div>
      </div>

      <div className="section">
        <div className="section-header"><span className="section-title">💬 Comments ({comments.length})</span></div>
        {comments.length === 0 ? <EmptyState icon="💬" text="No comments yet. Start the conversation!" />
          : comments.map(c => (
            <div key={c.id} className="comment">
              <span className="avatar sm">{c.userName.charAt(0)}</span>
              <div>
                <div><strong>{c.userName}</strong> <span className="text-muted">· {fmtDateTime(c.createdAt)}</span></div>
                <div className="comment-text">{c.text}</div>
              </div>
            </div>
          ))}
        <form className="comment-form" onSubmit={handleComment}>
          <textarea className="form-control" placeholder="Write a comment..." value={text} maxLength={2000} onChange={e => setText(e.target.value)} />
          <button className="btn btn-primary" disabled={posting}>{posting ? 'Posting...' : 'Post Comment'}</button>
        </form>
      </div>

      {editing && (
        <TaskFormModal task={task} projects={projects} onClose={() => setEditing(false)}
          onSaved={(m) => { setEditing(false); toast.success(m); load(); }} />
      )}
    </div>
  );
}
