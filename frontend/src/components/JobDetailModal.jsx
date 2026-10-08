import { useEffect, useState } from 'react';
import { Button, Form, Modal, Spinner } from 'react-bootstrap';
import { Link, useLocation } from 'react-router-dom';
import { applicationsApi } from '../api/endpoints.js';
import { errorMessage, fieldErrors } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { deadlineLabel, formatDate, jobTypeLabel, safeUrl } from '../utils/format.js';
import { MatchBreakdown } from './ui.jsx';
import SaveJobButton from './SaveJobButton.jsx';

const MIN_LETTER = 30;
const MAX_LETTER = 3000;

/**
 * Full job details. Students can apply from here with a cover letter.
 * @param onApplied called after a successful application so the list can refresh
 */
export default function JobDetailModal({ job, onHide, onApplied, onSavedChange }) {
  const { user } = useAuth();
  const toast = useToast();
  const location = useLocation();
  const [applying, setApplying] = useState(false);
  const [letter, setLetter] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setApplying(false);
    setLetter('');
    setError(null);
  }, [job?.jobId]);

  if (!job) return null;

  const isStudent = user?.role === 'STUDENT';
  const website = safeUrl(job.companyWebsite);

  const submit = async (event) => {
    event.preventDefault();
    if (letter.trim().length < MIN_LETTER) {
      setError(`Your cover letter must be at least ${MIN_LETTER} characters.`);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await applicationsApi.apply(job.jobId, letter.trim());
      toast.success(`Application sent to ${job.companyName}. Good luck!`);
      onApplied?.(job.jobId);
      onHide();
    } catch (err) {
      setError(fieldErrors(err).coverLetter || errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal show={Boolean(job)} onHide={onHide} size="lg" centered scrollable>
      <Modal.Header closeButton>
        <div>
          <Modal.Title className="h4 fw-bold mb-1">{job.title}</Modal.Title>
          <div className="text-primary fw-semibold">
            {job.companyName}
            {website && (
              <a href={website} target="_blank" rel="noopener noreferrer" className="ms-2 small">
                <i className="bi bi-box-arrow-up-right" aria-label="Company website" />
              </a>
            )}
          </div>
        </div>
      </Modal.Header>
      <Modal.Body>
        <div className="d-flex flex-wrap gap-2 mb-4">
          <span className="info-chip"><i className="bi bi-briefcase" aria-hidden="true" />{jobTypeLabel(job.jobType)}</span>
          <span className="info-chip"><i className="bi bi-geo-alt" aria-hidden="true" />{job.location}</span>
          <span className="info-chip"><i className="bi bi-calendar-event" aria-hidden="true" />Apply by {formatDate(job.applicationDeadline)} ({deadlineLabel(job.applicationDeadline)})</span>
          {job.companyIndustry && <span className="info-chip"><i className="bi bi-building" aria-hidden="true" />{job.companyIndustry}</span>}
        </div>

        <div className="row g-4">
          <div className={job.match ? 'col-md-7' : 'col-12'}>
            <h2 className="h6 fw-bold">About the role</h2>
            <p className="text-pre-line text-muted">{job.description}</p>
            <h2 className="h6 fw-bold mt-4">Requirements</h2>
            <div className="d-flex flex-wrap gap-1">
              {job.requirementList.map((r) => <span key={r} className="skill-tag">{r}</span>)}
            </div>
          </div>
          {job.match && (
            <div className="col-md-5">
              <div className="panel-soft">
                <h2 className="h6 fw-bold">Your skill match</h2>
                <MatchBreakdown match={job.match} />
                {job.match.missing.length > 0 && (
                  <p className="small text-muted mt-3 mb-0">
                    Have these skills? <Link to="/student/profile" onClick={onHide}>Add them to your profile</Link>.
                  </p>
                )}
              </div>
            </div>
          )}
        </div>

        {isStudent && applying && (
          <Form onSubmit={submit} className="mt-4" noValidate>
            <Form.Group controlId="coverLetter">
              <Form.Label className="fw-semibold">Cover letter</Form.Label>
              <Form.Control
                as="textarea"
                rows={6}
                value={letter}
                maxLength={MAX_LETTER}
                isInvalid={Boolean(error)}
                placeholder="Explain why you are a great fit for this role…"
                onChange={(e) => setLetter(e.target.value)}
                autoFocus
              />
              <div className="d-flex justify-content-between">
                <Form.Control.Feedback type="invalid" className="d-block">{error}</Form.Control.Feedback>
                <Form.Text className="ms-auto">{letter.length}/{MAX_LETTER}</Form.Text>
              </div>
            </Form.Group>
            <div className="d-flex justify-content-end gap-2 mt-3">
              <Button variant="light" onClick={() => setApplying(false)}>Cancel</Button>
              <Button type="submit" disabled={submitting}>
                {submitting && <Spinner size="sm" className="me-2" />}Submit application
              </Button>
            </div>
          </Form>
        )}
      </Modal.Body>

      {!applying && (
        <Modal.Footer>
          {!user && (
            <Link to="/login" state={{ from: location.pathname }} className="btn btn-primary" onClick={onHide}>
              Log in as a student to apply
            </Link>
          )}
          {isStudent && job.applied && (
            <span className="text-success fw-semibold"><i className="bi bi-check-circle-fill me-1" aria-hidden="true" />You have applied for this job</span>
          )}
          {isStudent && !job.applied && job.open && (
            <Button onClick={() => setApplying(true)}><i className="bi bi-send me-2" aria-hidden="true" />Apply now</Button>
          )}
          {isStudent && !job.applied && !job.open && <span className="text-muted">Applications for this job are closed.</span>}
          <SaveJobButton job={job} onChange={onSavedChange} withLabel />
          <Button variant="light" onClick={onHide}>Close</Button>
        </Modal.Footer>
      )}
    </Modal>
  );
}
