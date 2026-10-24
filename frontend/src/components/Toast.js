import React, { createContext, useContext, useState, useCallback } from 'react';

const ToastContext = createContext(null);

export const ToastProvider = ({ children }) => {
  const [toast, setToast] = useState(null);

  const show = useCallback((type, message) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 3500);
  }, []);

  const value = {
    success: (m) => show('success', m),
    error: (m) => show('error', m),
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      {toast && <div className={`toast toast-${toast.type}`}>{toast.type === 'success' ? '✔' : '⚠'} {toast.message}</div>}
    </ToastContext.Provider>
  );
};

export const useToast = () => useContext(ToastContext);
