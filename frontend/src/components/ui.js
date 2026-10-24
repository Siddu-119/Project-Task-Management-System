import React from 'react';

export const Loader = ({ text = 'Loading...' }) => (
  <div className="loader-wrap"><div className="spinner" /><span className="text-muted">{text}</span></div>
);

export const EmptyState = ({ icon = '📭', text }) => (
  <div className="empty-state"><div className="empty-icon">{icon}</div><p>{text}</p></div>
);

export const ErrorBox = ({ message, onRetry }) => (
  <div className="alert alert-error">
    ⚠ {message}
    {onRetry && <button className="btn btn-ghost btn-sm" style={{ marginLeft: 'auto' }} onClick={onRetry}>Retry</button>}
  </div>
);

export const ProgressBar = ({ value }) => (
  <div className="progress-bar"><div className="progress-fill" style={{ width: `${value}%` }} /></div>
);

export const Modal = ({ title, onClose, children }) => (
  <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
    <div className="modal">
      <div className="modal-header">
        <span className="modal-title">{title}</span>
        <button className="modal-close" onClick={onClose}>×</button>
      </div>
      {children}
    </div>
  </div>
);
