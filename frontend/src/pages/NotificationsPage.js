import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getNotifications, markNotificationRead, markAllNotificationsRead } from '../services/api';
import { useToast } from '../components/Toast';
import { Loader, EmptyState, ErrorBox } from '../components/ui';
import { errMsg, fmtDateTime } from '../utils/helpers';

const icons = { TASK_ASSIGNED: '📌', STATUS_CHANGED: '🔄', DUE_SOON: '⏰', TASK_COMPLETED: '🎉', ADDED_TO_PROJECT: '👥' };

export default function NotificationsPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');

  const load = () => {
    setError('');
    getNotifications().then(r => setItems(r.data)).catch(e => setError(errMsg(e, 'Could not load notifications.')));
    window.dispatchEvent(new Event('notifications-changed'));
  };
  useEffect(load, []);

  const open = async (n) => {
    try {
      if (!n.read) await markNotificationRead(n.id);
      window.dispatchEvent(new Event('notifications-changed'));
    } catch (e) { toast.error(errMsg(e)); }
    if (n.taskId) navigate(`/tasks/${n.taskId}`);
    else if (n.projectId) navigate(`/projects/${n.projectId}`);
    else load();
  };

  const markAll = async () => {
    try { await markAllNotificationsRead(); toast.success('All notifications marked as read.'); load(); }
    catch (e) { toast.error(errMsg(e)); }
  };

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!items) return <Loader />;
  const unread = items.filter(n => !n.read).length;

  return (
    <div className="section">
      <div className="section-header">
        <span className="section-title">🔔 Notifications {unread > 0 && <span className="chip">{unread} unread</span>}</span>
        {unread > 0 && <button className="btn btn-ghost btn-sm" onClick={markAll}>Mark all as read</button>}
      </div>
      {items.length === 0 ? <EmptyState icon="🔔" text="You're all caught up. No notifications yet." />
        : items.map(n => (
          <div key={n.id} className={`notif ${n.read ? '' : 'unread'}`} onClick={() => open(n)}>
            <span className="notif-icon">{icons[n.type] || '🔔'}</span>
            <div style={{ flex: 1 }}>
              <div>{n.message}</div>
              <span className="text-muted">{fmtDateTime(n.createdAt)}</span>
            </div>
            {!n.read && <span className="unread-dot" title="Unread" />}
          </div>
        ))}
    </div>
  );
}
