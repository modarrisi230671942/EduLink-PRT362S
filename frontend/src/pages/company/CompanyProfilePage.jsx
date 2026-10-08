import { useCallback, useEffect, useState } from 'react';
import { Button, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { companyApi } from '../../api/endpoints.js';
import { errorMessage, fieldErrors } from '../../api/client.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { ErrorState, Loading, PageHeader } from '../../components/ui.jsx';
import { formatDate } from '../../utils/format.js';

export default function CompanyProfilePage() {
  const { refreshUser } = useAuth();
  const toast = useToast();
  const [company, setCompany] = useState(null);
  const [form, setForm] = useState(null);
  const [errors, setErrors] = useState({});
  const [loadError, setLoadError] = useState(null);
  const [saving, setSaving] = useState(false);

  const apply = (c) => {
    setCompany(c);
    setForm({ companyName: c.companyName, industry: c.industry ?? '', location: c.location ?? '', website: c.website ?? '' });
  };

  const load = useCallback(async () => {
    setLoadError(null);
    try {
      apply(await companyApi.profile());
    } catch (err) {
      setLoadError(errorMessage(err));
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setErrors({});
    try {
      apply(await companyApi.update(form));
      await refreshUser();
      toast.success('Company profile saved.');
    } catch (err) {
      setErrors(fieldErrors(err));
      toast.error(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  if (loadError) return <Container className="page"><ErrorState message={loadError} onRetry={load} /></Container>;
  if (!form) return <Container className="page"><Loading /></Container>;

  const field = (name, label, props = {}) => (
    <Form.Group controlId={name}>
      <Form.Label>{label}</Form.Label>
      <Form.Control value={form[name]} isInvalid={Boolean(errors[name])}
                    onChange={(e) => setForm({ ...form, [name]: e.target.value })} {...props} />
      <Form.Control.Feedback type="invalid">{errors[name]}</Form.Control.Feedback>
    </Form.Group>
  );

  return (
    <Container className="page">
      <PageHeader title="Company profile" subtitle="This information is shown to students on every vacancy you post." />
      <Row className="g-4">
        <Col lg={8}>
          <Form className="panel" onSubmit={save} noValidate>
            <Row className="g-3">
              <Col md={6}>{field('companyName', 'Company name', { required: true, maxLength: 100 })}</Col>
              <Col md={6}>{field('industry', 'Industry', { maxLength: 50 })}</Col>
              <Col md={6}>{field('location', 'Location', { maxLength: 100 })}</Col>
              <Col md={6}>{field('website', 'Website', { type: 'url', placeholder: 'https://' })}</Col>
            </Row>
            <div className="text-end mt-4">
              <Button type="submit" disabled={saving}>{saving && <Spinner size="sm" className="me-2" />}Save changes</Button>
            </div>
          </Form>
        </Col>
        <Col lg={4}>
          <div className="panel">
            <h2 className="h6 fw-bold mb-3">Account</h2>
            <dl className="detail-list mb-0">
              <dt>Login email</dt><dd>{company.email}</dd>
              <dt>Member since</dt><dd>{formatDate(company.createdAt)}</dd>
              <dt>Verification</dt>
              <dd>
                {company.verified
                  ? <span className="text-success fw-semibold"><i className="bi bi-patch-check-fill me-1" aria-hidden="true" />Verified employer</span>
                  : <span className="text-warning fw-semibold"><i className="bi bi-hourglass-split me-1" aria-hidden="true" />Pending review</span>}
              </dd>
            </dl>
          </div>
        </Col>
      </Row>
    </Container>
  );
}
