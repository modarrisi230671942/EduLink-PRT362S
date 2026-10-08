import { createContext, useCallback, useContext, useRef, useState } from 'react';
import { Button, Modal } from 'react-bootstrap';

const ConfirmContext = createContext(null);

/**
 * A styled replacement for window.confirm:
 *   const confirm = useConfirm();
 *   if (await confirm({ title: 'Delete job?', message: '...', confirmLabel: 'Delete', variant: 'danger' })) { ... }
 */
export function ConfirmProvider({ children }) {
  const [options, setOptions] = useState(null);
  const resolver = useRef(null);

  const confirm = useCallback((opts) => new Promise((resolve) => {
    resolver.current = resolve;
    setOptions(opts);
  }), []);

  const close = (answer) => {
    resolver.current?.(answer);
    resolver.current = null;
    setOptions(null);
  };

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <Modal show={Boolean(options)} onHide={() => close(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">{options?.title}</Modal.Title>
        </Modal.Header>
        <Modal.Body className="text-muted">{options?.message}</Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={() => close(false)}>Cancel</Button>
          <Button variant={options?.variant ?? 'primary'} onClick={() => close(true)}>
            {options?.confirmLabel ?? 'Confirm'}
          </Button>
        </Modal.Footer>
      </Modal>
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const context = useContext(ConfirmContext);
  if (!context) throw new Error('useConfirm must be used inside <ConfirmProvider>');
  return context;
}
