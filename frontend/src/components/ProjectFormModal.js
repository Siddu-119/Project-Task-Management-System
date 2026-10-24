import React, { useState } from 'react';
import { createProject, updateProject } from '../services/api';
import { Modal } from './ui';
import { PROJECT_STATUSES, projectStatusLabel, errMsg } from '../utils/helpers';

export default function ProjectFormModal({ project, onClose, onSaved }) {
  const [form, setForm] = useState({
    name: project?.name || '',
    description: project?.description || '',
    startDate: project?.startDate || '',
    endDate: project?.endDate || '',
    status: project?.status || 'PLANNING',
  });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const handleSave = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) return setError('Project name is required.');
    if (form.startDate && form.endDate && form.endDate < form.startDate)
      return setError('End date cannot be before the start date.');
    setSaving(true); setError('');
    try {
      const payload = { ...form, startDate: form.startDate || null, endDate: form.endDate || null };
      if (project) await updateProject(project.id, payload);
      else await createProject(payload);
      onSaved(project ? 'Project updated.' : 'Project created.');
    } catch (err) {
      setError(errMsg(err, 'Failed to save project.'));
    } finally { setSaving(false); }
  };

  return (
    <Modal title={project ? 'Edit Project' : 'New Project'} onClose={onClose}>
      <form onSubmit={handleSave}>
        <div className="modal-body">
          {error && <div className="alert alert-error">⚠ {error}</div>}
          <div className="form-group">
            <label className="form-label">Project Name *</label>
            <input className="form-control" value={form.name} onChange={set('name')} maxLength={200} />
          </div>
          <div className="form-group">
            <label className="form-label">Description</label>
            <textarea className="form-control" value={form.description} onChange={set('description')} />
          </div>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Start Date</label>
              <input type="date" className="form-control" value={form.startDate} onChange={set('startDate')} />
            </div>
            <div className="form-group">
              <label className="form-label">End Date</label>
              <input type="date" className="form-control" value={form.endDate} onChange={set('endDate')} />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Status</label>
            <select className="form-control" value={form.status} onChange={set('status')}>
              {PROJECT_STATUSES.map(s => <option key={s} value={s}>{projectStatusLabel[s]}</option>)}
            </select>
          </div>
          <p className="text-muted">Progress is calculated automatically from the project's tasks.</p>
        </div>
        <div className="modal-footer">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save Project'}</button>
        </div>
      </form>
    </Modal>
  );
}
