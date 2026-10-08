import { useState } from 'react';
import { Button, Form, Modal, Spinner } from 'react-bootstrap';
import { authApi } from '../api/endpoints.js';
import { errorMessage, fieldErrors } from '../api/client.js';
import { useToast } from '../context/ToastContext.jsx';
import { passwordProblem } from '../utils/format.js';

const EMPTY = { currentPassword: '', newPassword: '', confirmPassword: '' };

export default function ChangePasswordModal({ show, onHide }) {
  const toast = useToast();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const close = () => {
    setForm(EMPTY);
    setErrors({});
    onHide();
  };

  const submit = async (event) => {
    event.preventDefault();
    const problems = {};
    const weak = passwordProblem(form.newPassword);
    if (weak) problems.newPassword = weak;
    if (form.newPassword !== form.confirmPassword) problems.confirmPassword = 'Passwords do not match';
    setErrors(problems);
    if (Object.keys(problems).length) return;

    setSaving(true);
    try {
      await authApi.changePassword(form.currentPassword, form.newPassword);
      toast.success('Your password has been changed.');
      close();
    } catch (error) {
      setErrors({ ...fieldErrors(error), form: errorMessage(error) });
    } finally {
      setSaving(false);
    }
  };

  const bind = (name) => ({
    value: form[name],
    isInvalid: Boolean(errors[name]),
    onChange: (e) => setForm({ ...form, [name]: e.target.value }),
  });

  return (
    <Modal show={show} onHide={close} centered>
      <Form onSubmit={submit} noValidate>
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">Change password</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {errors.form && <div className="alert alert-danger py-2">{errors.form}</div>}
          <Form.Group className="mb-3" controlId="currentPassword">
            <Form.Label>Current password</Form.Label>
            <Form.Control type="password" autoComplete="current-password" required {...bind('currentPassword')} />
          </Form.Group>
          <Form.Group className="mb-3" controlId="newPassword">
            <Form.Label>New password</Form.Label>
            <Form.Control type="password" autoComplete="new-password" required {...bind('newPassword')} />
            <Form.Control.Feedback type="invalid">{errors.newPassword}</Form.Control.Feedback>
            <Form.Text>At least 8 characters, with a letter and a number.</Form.Text>
          </Form.Group>
          <Form.Group controlId="confirmPassword">
            <Form.Label>Confirm new password</Form.Label>
            <Form.Control type="password" autoComplete="new-password" required {...bind('confirmPassword')} />
            <Form.Control.Feedback type="invalid">{errors.confirmPassword}</Form.Control.Feedback>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={close}>Cancel</Button>
          <Button type="submit" disabled={saving}>
            {saving && <Spinner size="sm" className="me-2" />}Update password
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
