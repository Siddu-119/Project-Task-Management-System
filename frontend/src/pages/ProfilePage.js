import React, { useEffect, useState } from 'react';
import { getMe, changePassword } from '../services/api';
import { useToast } from '../components/Toast';
import { Loader } from '../components/ui';
import { errMsg } from '../utils/helpers';

export default function ProfilePage() {
  const toast = useToast();
  const [me, setMe] = useState(null);
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  useEffect(() => { getMe().then(r => setMe(r.data)).catch(e => toast.error(errMsg(e))); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!form.currentPassword) return setError('Enter your current password.');
    if (!/^(?=.*[A-Za-z])(?=.*\d).{6,}$/.test(form.newPassword))
      return setError('New password must be at least 6 characters and contain a letter and a number.');
    if (form.newPassword !== form.confirm) return setError('New password and confirmation do not match.');
    setSaving(true);
    try {
      await changePassword(form.currentPassword, form.newPassword);
      toast.success('Password updated.');
      setForm({ currentPassword: '', newPassword: '', confirm: '' });
    } catch (err) { setError(errMsg(err, 'Failed to update password.')); }
    finally { setSaving(false); }
  };

  if (!me) return <Loader />;

  return (
    <div className="grid-2">
      <div className="section">
        <div className="section-header"><span className="section-title">👤 My Profile</span></div>
        <div style={{ padding: 20 }}>
          <div className="avatar lg">{me.name.charAt(0).toUpperCase()}</div>
          <div className="detail-grid" style={{ marginTop: 16 }}>
            <div><span className="text-muted">Name</span><div className="fw-600">{me.name}</div></div>
            <div><span className="text-muted">Email</span><div>{me.email}</div></div>
            <div><span className="text-muted">Role</span><div><span className={`badge ${me.role === 'ADMIN' ? 'badge-admin' : 'badge-employee'}`}>{me.role}</span></div></div>
          </div>
        </div>
      </div>

      <div className="section">
        <div className="section-header"><span className="section-title">🔒 Change Password</span></div>
        <form onSubmit={handleSubmit} style={{ padding: 20 }}>
          {error && <div className="alert alert-error">⚠ {error}</div>}
          <div className="form-group"><label className="form-label">Current Password</label>
            <input type="password" className="form-control" value={form.currentPassword} onChange={set('currentPassword')} /></div>
          <div className="form-group"><label className="form-label">New Password</label>
            <input type="password" className="form-control" value={form.newPassword} onChange={set('newPassword')} /></div>
          <div className="form-group"><label className="form-label">Confirm New Password</label>
            <input type="password" className="form-control" value={form.confirm} onChange={set('confirm')} /></div>
          <button className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Update Password'}</button>
        </form>
      </div>
    </div>
  );
}
