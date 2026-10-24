import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { STATUSES, statusLabel, priorityClass, priorityLabel, fmtDate } from '../utils/helpers';

/**
 * Kanban board. Drag a card to another column, or use the dropdown on the card.
 * Uses the browser's built-in drag-and-drop - no extra library needed.
 */
export default function KanbanBoard({ tasks, canMove, onMove }) {
  const navigate = useNavigate();
  const [dragId, setDragId] = useState(null);
  const [overCol, setOverCol] = useState(null);

  const handleDrop = (status) => {
    const task = tasks.find(t => t.id === dragId);
    setOverCol(null);
    setDragId(null);
    if (task && task.status !== status && canMove(task)) onMove(task, status);
  };

  return (
    <div className="kanban">
      {STATUSES.map(status => {
        const col = tasks.filter(t => t.status === status);
        return (
          <div
            key={status}
            className={`kanban-col ${overCol === status ? 'drag-over' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setOverCol(status); }}
            onDragLeave={() => setOverCol(null)}
            onDrop={() => handleDrop(status)}
          >
            <div className="kanban-col-header">
              <span>{statusLabel[status]}</span>
              <span className="chip">{col.length}</span>
            </div>
            <div className="kanban-cards">
              {col.length === 0 && <div className="kanban-empty">No tasks</div>}
              {col.map(t => {
                const movable = canMove(t);
                return (
                  <div
                    key={t.id}
                    className={`kanban-card ${movable ? 'draggable' : ''} ${dragId === t.id ? 'dragging' : ''}`}
                    draggable={movable}
                    onDragStart={() => setDragId(t.id)}
                    onDragEnd={() => { setDragId(null); setOverCol(null); }}
                  >
                    <div className="kanban-card-title" onClick={() => navigate(`/tasks/${t.id}`)}>{t.title}</div>
                    <div className="kanban-card-meta">
                      <span className={`badge ${priorityClass[t.priority]}`}>{priorityLabel[t.priority]}</span>
                      {t.deadline && <span className={`text-muted ${t.overdue ? 'text-danger' : ''}`}>📅 {fmtDate(t.deadline)}</span>}
                    </div>
                    <div className="kanban-card-footer">
                      <span className="text-muted">{t.assignedToName ? `👤 ${t.assignedToName}` : 'Unassigned'}</span>
                      {movable && (
                        <select className="status-select" value={t.status} onChange={e => onMove(t, e.target.value)}>
                          {STATUSES.map(s => <option key={s} value={s}>{statusLabel[s]}</option>)}
                        </select>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
