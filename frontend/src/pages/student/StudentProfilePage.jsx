import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { studentApi } from '../../api/endpoints.js';
import { errorMessage, fieldErrors } from '../../api/client.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useConfirm } from '../../context/ConfirmContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { ErrorState, Loading, PageHeader, SkillTags } from '../../components/ui.jsx';
import { formatDate, openPdf } from '../../utils/format.js';

const MAX_CV_BYTES = 5 * 1024 * 1024;

export default function StudentProfilePage() {
  const { refreshUser } = useAuth();
  const toast = useToast();
  const confirm = useConfirm();
  const fileInput = useRef(null);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(null);
  const [errors, setErrors] = useState({});
  const [loadError, setLoadError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  const [suggestions, setSuggestions] = useState(null);
  const [scanning, setScanning] = useState(false);

  const applyProfile = (p) => {
    setProfile(p);
    setForm({ fullName: p.fullName, course: p.course, institution: p.institution, graduationYear: p.graduationYear ?? '', skills: p.skills ?? '', jobAlerts: p.jobAlerts });
  };

  /** Reads the uploaded CV on the server and suggests skills that are not on the profile yet. */
  const scanCv = async () => {
    setScanning(true);
    try {
      const result = await studentApi.cvSkills();
      setSuggestions(result);
      if (result.found.length === 0) toast.info('We could not find any recognised skills in your CV.');
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setScanning(false);
    }
  };

  const addSkills = (skills) => {
    setForm((f) => {
      const current = f.skills.split(',').map((s) => s.trim()).filter(Boolean);
      const lower = new Set(current.map((s) => s.toLowerCase()));
      const merged = [...current, ...skills.filter((s) => !lower.has(s.toLowerCase()))];
      return { ...f, skills: merged.join(', ') };
    });
    setSuggestions((s) => s && { ...s, newSkills: s.newSkills.filter((x) => !skills.includes(x)) });
  };

  const load = useCallback(async () => {
    setLoadError(null);
    try {
      applyProfile(await studentApi.profile());
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
      applyProfile(await studentApi.update({ ...form, graduationYear: Number(form.graduationYear) }));
      await refreshUser();
      toast.success('Profile saved. Your job matches have been updated.');
    } catch (err) {
      setErrors(fieldErrors(err));
      toast.error(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const upload = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      toast.error('Please choose a PDF file.');
      return;
    }
    if (file.size > MAX_CV_BYTES) {
      toast.error('The file is too large. The maximum size is 5 MB.');
      return;
    }
    setUploading(true);
    try {
      applyProfile(await studentApi.uploadCv(file));
      toast.success('CV uploaded. Employers will see it with your applications.');
      scanCv();
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  const viewCv = async () => {
    try {
      openPdf(await studentApi.downloadCv());
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  const removeCv = async () => {
    if (!await confirm({ title: 'Remove your CV?', message: 'Employers will no longer be able to view it.', confirmLabel: 'Remove', variant: 'danger' })) return;
    try {
      applyProfile(await studentApi.deleteCv());
      setSuggestions(null);
      toast.success('CV removed.');
    } catch (err) {
      toast.error(errorMessage(err));
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
      <PageHeader title="Profile & CV" subtitle="Keep your details current — they drive your job matches." />
      <Row className="g-4">
        <Col lg={8}>
          <Form className="panel" onSubmit={save} noValidate>
            <h2 className="h6 fw-bold mb-3">Personal details</h2>
            <Row className="g-3">
              <Col md={6}>{field('fullName', 'Full name', { required: true })}</Col>
              <Col md={6}>
                <Form.Group controlId="studentNumber">
                  <Form.Label>Student number</Form.Label>
                  <Form.Control value={profile.studentNumber} disabled readOnly />
                </Form.Group>
              </Col>
              <Col md={6}>{field('course', 'Course / qualification', { required: true })}</Col>
              <Col md={6}>{field('institution', 'Institution', { required: true })}</Col>
              <Col md={4}>{field('graduationYear', 'Graduation year', { type: 'number', min: 2000, max: 2100, required: true })}</Col>
              <Col md={8}>{field('skills', 'Skills (comma-separated)', { placeholder: 'Java, SQL, Python' })}</Col>
              <Col xs={12}>
                <div className="small text-muted mb-1">Preview</div>
                <SkillTags skills={form.skills.split(',').map((s) => s.trim()).filter(Boolean)} />
              </Col>
              {suggestions && suggestions.newSkills.length > 0 && (
                <Col xs={12}>
                  <div className="suggestion-box">
                    <div className="d-flex justify-content-between align-items-center mb-2 gap-2">
                      <div className="fw-semibold small">
                        <i className="bi bi-magic me-1" aria-hidden="true" />
                        We found {suggestions.newSkills.length} skill{suggestions.newSkills.length === 1 ? '' : 's'} in your CV that {suggestions.newSkills.length === 1 ? "isn't" : "aren't"} on your profile
                      </div>
                      <Button size="sm" variant="primary" onClick={() => addSkills(suggestions.newSkills)}>Add all</Button>
                    </div>
                    {suggestions.newSkills.map((s) => (
                      <button key={s} type="button" className="skill-tag" onClick={() => addSkills([s])}>
                        <i className="bi bi-plus" aria-hidden="true" />{s}
                      </button>
                    ))}
                    <div className="small text-muted mt-1">Click a skill to add it, then <strong>Save changes</strong>.</div>
                  </div>
                </Col>
              )}
              <Col xs={12}>
                <Form.Check
                  type="switch"
                  id="jobAlerts"
                  label="Notify me when a new job matches at least 60% of my skills"
                  checked={Boolean(form.jobAlerts)}
                  onChange={(e) => setForm({ ...form, jobAlerts: e.target.checked })}
                />
              </Col>
            </Row>
            <div className="text-end mt-4">
              <Button type="submit" disabled={saving}>{saving && <Spinner size="sm" className="me-2" />}Save changes</Button>
            </div>
          </Form>
        </Col>

        <Col lg={4}>
          <div className="panel">
            <h2 className="h6 fw-bold mb-3">Curriculum vitae</h2>
            {profile.hasCv ? (
              <div className="cv-box">
                <i className="bi bi-file-earmark-pdf-fill cv-icon" aria-hidden="true" />
                <div className="min-w-0">
                  <div className="fw-semibold text-truncate">{profile.cvFileName}</div>
                  <div className="small text-muted">Uploaded {formatDate(profile.cvUploadedAt)}</div>
                </div>
              </div>
            ) : (
              <div className="cv-box cv-empty">
                <i className="bi bi-cloud-arrow-up cv-icon" aria-hidden="true" />
                <div className="small text-muted">No CV uploaded yet. Students with a CV are far more likely to be shortlisted.</div>
              </div>
            )}

            <input ref={fileInput} type="file" accept="application/pdf,.pdf" hidden onChange={upload} />
            <div className="d-grid gap-2 mt-3">
              <Button onClick={() => fileInput.current?.click()} disabled={uploading}>
                {uploading ? <Spinner size="sm" className="me-2" /> : <i className="bi bi-upload me-2" aria-hidden="true" />}
                {profile.hasCv ? 'Replace CV' : 'Upload CV (PDF)'}
              </Button>
              {profile.hasCv && (
                <>
                  <div className="d-flex gap-2">
                    <Button variant="light" className="flex-fill" onClick={viewCv}><i className="bi bi-eye me-1" aria-hidden="true" />View</Button>
                    <Button variant="outline-danger" className="flex-fill" onClick={removeCv}><i className="bi bi-trash me-1" aria-hidden="true" />Remove</Button>
                  </div>
                  <Button variant="outline-primary" onClick={scanCv} disabled={scanning}>
                    {scanning ? <Spinner size="sm" className="me-2" /> : <i className="bi bi-magic me-2" aria-hidden="true" />}
                    Find skills in my CV
                  </Button>
                </>
              )}
            </div>
            <p className="small text-muted mt-3 mb-0">PDF only, up to 5 MB. Only employers you apply to can view it.</p>
          </div>
        </Col>
      </Row>
    </Container>
  );
}
