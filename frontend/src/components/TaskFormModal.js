import React, { useEffect, useState } from 'react';
import { createTask, updateTask, getProjectMembers } from '../services/api';
import { Modal } from './ui';
import { STATUSES, statusLabel, PRIORITIES, priorityLabel, errMsg } from '../utils/helpers';

export default function TaskFormModal({ task, defaultProjectId, projects, onClose, onSaved }) {
  const [form, setForm] = useState({
    title: task?.title || '',
    description: task?.description || '',
    status: task?.status || 'TODO',
    priority: task?.priority || 'MEDIUM',
    deadline: task?.deadline || '',
    projectId: task?.projectId || defaultProjectId || '',
    assignedToId: task?.assignedToId || '',
  });
  const [members, setMembers] = useState([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  // Only members of the chosen project can be assigned
  useEffect(() => {
    if (!form.projectId) { setMembers([]); return; }
    getProjectMembers(form.projectId).then(r => setMembers(r.data)).catch(() => setMembers([]));
  }, [form.projectId]);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!form.title.trim()) return setError('Task title is required.');
    if (!form.projectId) return setError('Please choose a project.');
    setSaving(true); setError('');
    try {
      const payload = {
        ...form,
        projectId: Number(form.projectId),
        assignedToId: form.assignedToId ? Number(form.assignedToId) : null,
        deadline: form.deadline || null,
      };
      if (task) await updateTask(task.id, payload);
      else await createTask(payload);
      onSaved(task ? 'Task updated.' : 'Task created.');
    } catch (err) {
      setError(errMsg(err, 'Failed to save task.'));
    } finally { setSaving(false); }
  };

  const projectChanged = (e) => setForm({ ...form, projectId: e.target.value, assignedToId: '' });

  return (
    <Modal title={task ? 'Edit Task' : 'New Task'} onClose={onClose}>
      <form onSubmit={handleSave}>
        <div className="modal-body">
          {error && <div className="alert alert-error">⚠ {error}</div>}
          <div className="form-group">
            <label className="form-label">Task Title *</label>
            <input className="form-control" value={form.title} onChange={set('title')} maxLength={200} />
          </div>
          <div className="form-group">
            <label className="form-label">Description</label>
            <textarea className="form-control" value={form.description} onChange={set('description')} />
          </div>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Project *</label>
              <select className="form-control" value={form.projectId} onChange={projectChanged}>
                <option value="">— Select Project —</option>
                {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Assign To</label>
              <select className="form-control" value={form.assignedToId} onChange={set('assignedToId')} disabled={!form.projectId}>
                <option value="">— Unassigned —</option>
                {members.map(m => <option key={m.userId} value={m.userId}>{m.name}</option>)}
              </select>
            </div>
          </div>
          {form.projectId && members.length === 0 && (
            <p className="text-muted" style={{ marginBottom: 12 }}>This project has no team members yet. Add members on the Team page first.</p>
          )}
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Priority</label>
              <select className="form-control" value={form.priority} onChange={set('priority')}>
                {PRIORITIES.map(p => <option key={p} value={p}>{priorityLabel[p]}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Status</label>
              <select className="form-control" value={form.status} onChange={set('status')}>
                {STATUSES.map(s => <option key={s} value={s}>{statusLabel[s]}</option>)}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Due Date</label>
            <input type="date" className="form-control" value={form.deadline} onChange={set('deadline')} />
          </div>
        </div>
        <div className="modal-footer">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save Task'}</button>
        </div>
      </form>
    </Modal>
  );
}
