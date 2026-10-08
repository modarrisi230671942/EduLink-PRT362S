import { useEffect, useState } from 'react';
import { Button, Col, Form, Modal, Row, Spinner } from 'react-bootstrap';
import { interviewsApi } from '../../api/endpoints.js';
import { errorMessage, fieldErrors } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';
import { toDateTimeInput } from '../../utils/format.js';

const MODES = [
  { value: 'ONLINE', label: 'Online (video call)', locationLabel: 'Meeting link', placeholder: 'https://meet.google.com/…' },
  { value: 'IN_PERSON', label: 'In person', locationLabel: 'Address', placeholder: 'e.g. 12 Long Street, Cape Town — 3rd floor' },
  { value: 'PHONE', label: 'Phone call', locationLabel: 'Phone number (optional)', placeholder: 'We will call the number on your CV' },
];

/** Default proposal: the next three weekdays at 10:00. */
function defaultSlots() {
  const slots = [];
  const day = new Date();
  day.setHours(10, 0, 0, 0);
  while (slots.length < 3) {
    day.setDate(day.getDate() + 1);
    if (day.getDay() !== 0 && day.getDay() !== 6) slots.push(toDateTimeInput(day));
  }
  return slots;
}

/** The company proposes up to three interview times; the student picks one. */
export default function InterviewFormModal({ application, show, onHide, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState(null);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!show || !application) return;
    const existing = application.interview?.status !== 'CANCELLED' ? application.interview : null;
    setErrors({});
    setFormError(null);
    setForm({
      mode: existing?.mode ?? 'ONLINE',
      location: existing?.location ?? '',
      durationMinutes: existing?.durationMinutes ?? 45,
      notes: existing?.notes ?? '',
      slots: defaultSlots(),
    });
  }, [show, application]);

  if (!form) return null;
  const mode = MODES.find((m) => m.value === form.mode);
  // Errors for individual slots arrive as "slots[0]", "slots[1]"…
  const slotsError = Object.entries(errors).find(([key]) => key.startsWith('slots'))?.[1];
  const rescheduling = application?.interview && application.interview.status !== 'CANCELLED';

  const setSlot = (index, value) => setForm({ ...form, slots: form.slots.map((s, i) => (i === index ? value : s)) });

  const submit = async (event) => {
    event.preventDefault();
    const slots = form.slots.filter(Boolean);
    if (slots.length === 0) {
      setFormError('Propose at least one time slot.');
      return;
    }
    setSaving(true);
    setErrors({});
    setFormError(null);
    try {
      const interview = await interviewsApi.propose(application.applicationId, {
        mode: form.mode,
        location: form.location,
        durationMinutes: Number(form.durationMinutes),
        notes: form.notes,
        slots,
      });
      toast.success(`${application.studentName} has been invited and will pick a time.`);
      onSaved(interview);
      onHide();
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered size="lg">
      <Form onSubmit={submit} noValidate>
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">
            {rescheduling ? 'Reschedule interview' : 'Invite to interview'} — {application?.studentName}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {formError && <div className="alert alert-danger py-2 small">{formError}</div>}
          <Row className="g-3">
            <Col md={5}>
              <Form.Group controlId="mode">
                <Form.Label>Interview type</Form.Label>
                <Form.Select value={form.mode} onChange={(e) => setForm({ ...form, mode: e.target.value })}>
                  {MODES.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
                </Form.Select>
              </Form.Group>
            </Col>
            <Col md={3}>
              <Form.Group controlId="duration">
                <Form.Label>Duration</Form.Label>
                <Form.Select value={form.durationMinutes} onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })}>
                  {[15, 30, 45, 60, 90, 120].map((m) => <option key={m} value={m}>{m} minutes</option>)}
                </Form.Select>
              </Form.Group>
            </Col>
            <Col xs={12}>
              <Form.Group controlId="location">
                <Form.Label>{mode.locationLabel}</Form.Label>
                <Form.Control value={form.location} placeholder={mode.placeholder} maxLength={255}
                              isInvalid={Boolean(errors.location)}
                              onChange={(e) => setForm({ ...form, location: e.target.value })} />
                <Form.Control.Feedback type="invalid">{errors.location}</Form.Control.Feedback>
              </Form.Group>
            </Col>
            <Col xs={12}>
              <Form.Label className="mb-1">Proposed times</Form.Label>
              <div className="small text-muted mb-2">Offer up to three options — the candidate chooses one. Leave a slot empty to offer fewer.</div>
              <Row className="g-2">
                {form.slots.map((slot, i) => (
                  <Col md={4} key={i}>
                    <Form.Control type="datetime-local" value={slot} min={toDateTimeInput(new Date())}
                                  aria-label={`Option ${i + 1}`} isInvalid={Boolean(slotsError)}
                                  onChange={(e) => setSlot(i, e.target.value)} />
                  </Col>
                ))}
              </Row>
              {slotsError && <div className="invalid-feedback d-block">{slotsError}</div>}
            </Col>
            <Col xs={12}>
              <Form.Group controlId="notes">
                <Form.Label>Notes for the candidate (optional)</Form.Label>
                <Form.Control as="textarea" rows={3} maxLength={1000} value={form.notes}
                              placeholder="Who they will meet, what to prepare, parking…"
                              onChange={(e) => setForm({ ...form, notes: e.target.value })} />
              </Form.Group>
            </Col>
          </Row>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={onHide}>Cancel</Button>
          <Button type="submit" disabled={saving}>
            {saving && <Spinner size="sm" className="me-2" />}
            <i className="bi bi-send me-1" aria-hidden="true" />{rescheduling ? 'Send new times' : 'Send invitation'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
