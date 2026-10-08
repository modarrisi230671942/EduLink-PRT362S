import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Toast, ToastContainer } from 'react-bootstrap';

const ToastContext = createContext(null);

const ICONS = { success: 'check-circle-fill', danger: 'exclamation-triangle-fill', info: 'info-circle-fill' };

/** Small pop-up messages in the bottom-right corner: toast.success('Saved!') / toast.error('...'). */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const remove = useCallback((id) => setToasts((list) => list.filter((t) => t.id !== id)), []);

  const show = useCallback((message, variant) => {
    const id = crypto.randomUUID();
    setToasts((list) => [...list.slice(-3), { id, message, variant }]);
  }, []);

  const api = useMemo(() => ({
    success: (message) => show(message, 'success'),
    error: (message) => show(message, 'danger'),
    info: (message) => show(message, 'info'),
  }), [show]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      <ToastContainer position="bottom-end" className="p-3 toast-stack">
        {toasts.map((t) => (
          <Toast key={t.id} onClose={() => remove(t.id)} delay={4500} autohide className={`toast-edulink toast-${t.variant}`}>
            <Toast.Body className="d-flex align-items-start gap-2">
              <i className={`bi bi-${ICONS[t.variant]} mt-1`} aria-hidden="true" />
              <span className="flex-grow-1">{t.message}</span>
              <button type="button" className="btn-close btn-sm" aria-label="Close" onClick={() => remove(t.id)} />
            </Toast.Body>
          </Toast>
        ))}
      </ToastContainer>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used inside <ToastProvider>');
  return context;
}
