import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getUnreadCount } from '../services/api';

const baseNav = [
  { icon: '📊', label: 'Dashboard', path: '/dashboard' },
  { icon: '📁', label: 'Projects', path: '/projects' },
  { icon: '✅', label: 'Tasks', path: '/tasks' },
  { icon: '👥', label: 'Team', path: '/team' },
  { icon: '🔔', label: 'Notifications', path: '/notifications', badge: true },
  { icon: '📈', label: 'Reports', path: '/reports', adminOnly: true },
  { icon: '👤', label: 'Profile', path: '/profile' },
];

export default function Layout({ children }) {
  const { user, logoutUser } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [unread, setUnread] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);

  const navItems = baseNav.filter(n => !n.adminOnly || user?.role === 'ADMIN');

  // Notification count: load now, every 30s, and whenever a page says it changed
  useEffect(() => {
    const load = () => getUnreadCount().then(r => setUnread(r.data.count)).catch(() => {});
    load();
    const timer = setInterval(load, 30000);
    window.addEventListener('notifications-changed', load);
    return () => { clearInterval(timer); window.removeEventListener('notifications-changed', load); };
  }, []);

  useEffect(() => setMenuOpen(false), [location.pathname]);

  const handleLogout = () => { logoutUser(); navigate('/login'); };

  const current = navItems.find(n => location.pathname === n.path || location.pathname.startsWith(n.path + '/'));
  const pageTitle = current?.label || 'ProjectFlow';

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`}>
        <div className="sidebar-logo">
          <h2>ProjectFlow</h2>
          <span>{user?.role === 'ADMIN' ? 'Admin Panel' : 'Employee'} · v2.0</span>
        </div>

        <nav className="sidebar-nav">
          <div className="nav-section-label">Navigation</div>
          {navItems.map(item => (
            <button
              key={item.path}
              className={`nav-item ${current?.path === item.path ? 'active' : ''}`}
              onClick={() => navigate(item.path)}
            >
              <span className="icon">{item.icon}</span>
              <span>{item.label}</span>
              {item.badge && unread > 0 && <span className="nav-badge">{unread}</span>}
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user">
            <div className="avatar">{user?.name?.charAt(0).toUpperCase()}</div>
            <div className="sidebar-user-info">
              <div className="sidebar-user-name">{user?.name}</div>
              <div className="sidebar-user-role">{user?.role}</div>
            </div>
            <button className="logout-btn" onClick={handleLogout} title="Logout">⏻</button>
          </div>
        </div>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <button className="menu-btn" onClick={() => setMenuOpen(!menuOpen)}>☰</button>
          <div className="topbar-title">
            {pageTitle}
            <span>/ {user?.name}</span>
          </div>
          <button className="bell-btn" onClick={() => navigate('/notifications')} title="Notifications">
            🔔{unread > 0 && <span className="bell-count">{unread}</span>}
          </button>
        </header>
        <main className="page-content">{children}</main>
      </div>
    </div>
  );
}
