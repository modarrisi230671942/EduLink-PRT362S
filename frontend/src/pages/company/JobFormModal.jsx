import { useEffect, useState } from 'react';
import { Button, Col, Form, Modal, Row, Spinner } from 'react-bootstrap';
import { jobsApi } from '../../api/endpoints.js';
import { errorMessage, fieldErrors } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';
import { JOB_TYPES } from '../../utils/format.js';

const EMPTY = { title: '', jobType: 'GRADUATE', location: '', applicationDeadline: '', requirements: '', description: '' };
const todayIso = () => new Date().toISOString().slice(0, 10);

/** Create a new job, or edit an existing one when {@code job} is given. */
export default function JobFormModal({ show, job, onHide, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!show) return;
    setErrors({});
    setFormError(null);
    setForm(job ? {
      title: job.title,
      jobType: job.jobType,
      location: job.location ?? '',
      applicationDeadline: job.applicationDeadline ?? '',
      requirements: job.requirements ?? '',
      description: job.description ?? '',
    } : EMPTY);
  }, [show, job]);

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setErrors({});
    setFormError(null);
    try {
      const saved = job ? await jobsApi.update(job.jobId, form) : await jobsApi.create(form);
      toast.success(job ? 'Vacancy updated.' : 'Vacancy published! Students can now apply.');
      onSaved(saved);
      onHide();
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const bind = (name) => ({
    value: form[name],
    isInvalid: Boolean(errors[name]),
    onChange: (e) => setForm({ ...form, [name]: e.target.value }),
  });
  const feedback = (name) => <Form.Control.Feedback type="invalid">{errors[name]}</Form.Control.Feedback>;

  return (
    <Modal show={show} onHide={onHide} size="lg" centered>
      <Form onSubmit={submit} noValidate>
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">{job ? 'Edit vacancy' : 'Post a new vacancy'}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {formError && <div className="alert alert-danger py-2 small">{formError}</div>}
          <Row className="g-3">
            <Col md={8}>
              <Form.Group controlId="title">
                <Form.Label>Job title</Form.Label>
                <Form.Control placeholder="e.g. Junior Java Developer" maxLength={100} required {...bind('title')} />
                {feedback('title')}
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group controlId="jobType">
                <Form.Label>Job type</Form.Label>
                <Form.Select {...bind('jobType')}>
                  {JOB_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                </Form.Select>
                {feedback('jobType')}
              </Form.Group>
            </Col>
            <Col md={8}>
              <Form.Group controlId="location">
                <Form.Label>Location</Form.Label>
                <Form.Control placeholder="e.g. Cape Town or Remote" maxLength={100} required {...bind('location')} />
                {feedback('location')}
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group controlId="applicationDeadline">
                <Form.Label>Application deadline</Form.Label>
                <Form.Control type="date" min={todayIso()} required {...bind('applicationDeadline')} />
                {feedback('applicationDeadline')}
              </Form.Group>
            </Col>
            <Col xs={12}>
              <Form.Group controlId="requirements">
                <Form.Label>Required skills</Form.Label>
                <Form.Control placeholder="Java, Spring Boot, SQL" maxLength={1000} required {...bind('requirements')} />
                <Form.Text>Comma-separated. Students are matched against these, so list concrete skills.</Form.Text>
                {feedback('requirements')}
              </Form.Group>
            </Col>
            <Col xs={12}>
              <Form.Group controlId="description">
                <Form.Label>Description</Form.Label>
                <Form.Control as="textarea" rows={5} maxLength={5000} required
                              placeholder="Responsibilities, team, benefits…" {...bind('description')} />
                {feedback('description')}
              </Form.Group>
            </Col>
          </Row>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={onHide}>Cancel</Button>
          <Button type="submit" disabled={saving}>
            {saving && <Spinner size="sm" className="me-2" />}{job ? 'Save changes' : 'Publish vacancy'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
