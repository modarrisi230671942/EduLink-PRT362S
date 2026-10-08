import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Col, Container, Form, Modal, Row } from 'react-bootstrap';
import { useSearchParams } from 'react-router-dom';
import { applicationsApi, companyApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorState, Loading, MatchBadge, MatchBreakdown, PageHeader, SkillTags, StatusBadge } from '../../components/ui.jsx';
import { APPLICATION_STATUSES, formatDate, formatDateTime, openPdf } from '../../utils/format.js';
import { useLiveRefresh } from '../../api/live.js';
import { InterviewWhere } from '../../components/interviews.jsx';
import InterviewFormModal from './InterviewFormModal.jsx';

export default function ApplicantsPage() {
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const jobId = params.get('jobId') ?? '';
  const status = params.get('status') ?? '';
  const [sortBy, setSortBy] = useState('match');
  const [jobs, setJobs] = useState([]);
  const [applications, setApplications] = useState(null);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);
  const [scheduling, setScheduling] = useState(false);

  const setFilter = (name, value) => {
    const next = { jobId, status, [name]: value };
    setParams(Object.fromEntries(Object.entries(next).filter(([, v]) => v)));
  };

  const load = useCallback(async () => {
    setError(null);
    try {
      setApplications(await companyApi.applications({ jobId, status }));
    } catch (err) {
      setError(errorMessage(err));
    }
  }, [jobId, status]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { companyApi.jobs().then(setJobs).catch(() => {}); }, []);
  // New applications and interview replies appear without refreshing the page
  useLiveRefresh(load, ['APPLICATION_RECEIVED', 'APPLICATION_WITHDRAWN', 'INTERVIEW_CONFIRMED', 'INTERVIEW_CANCELLED']);

  const sorted = useMemo(() => {
    if (!applications) return [];
    const list = [...applications];
    if (sortBy === 'match') list.sort((a, b) => b.match.score - a.match.score);
    return list; // otherwise newest first, as returned by the API
  }, [applications, sortBy]);

  const patchApplication = (applicationId, changes) => {
    setApplications((list) => list.map((a) => (a.applicationId === applicationId ? { ...a, ...changes } : a)));
    setSelected((s) => (s && s.applicationId === applicationId ? { ...s, ...changes } : s));
  };

  const changeStatus = async (app, next) => {
    try {
      const updated = await applicationsApi.setStatus(app.applicationId, next);
      patchApplication(updated.applicationId, updated);
      toast.success(`${app.studentName} has been notified.`);
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  const viewCv = async (app) => {
    try {
      openPdf(await applicationsApi.downloadCv(app.applicationId));
    } catch (err) {
      toast.error(errorMessage(err, 'Could not open the CV.'));
    }
  };

  return (
    <Container className="page">
      <PageHeader title="Applicants" subtitle="Review candidates ranked by how well they match your requirements." />

      <div className="filter-bar">
        <Row className="g-2">
          <Col md={5}>
            <Form.Select aria-label="Filter by vacancy" value={jobId} onChange={(e) => setFilter('jobId', e.target.value)}>
              <option value="">All vacancies</option>
              {jobs.map((j) => <option key={j.jobId} value={j.jobId}>{j.title}</option>)}
            </Form.Select>
          </Col>
          <Col xs={6} md={4}>
            <Form.Select aria-label="Filter by status" value={status} onChange={(e) => setFilter('status', e.target.value)}>
              <option value="">All statuses</option>
              {APPLICATION_STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </Form.Select>
          </Col>
          <Col xs={6} md={3}>
            <Form.Select aria-label="Sort applicants" value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
              <option value="match">Best match first</option>
              <option value="newest">Newest first</option>
            </Form.Select>
          </Col>
        </Row>
      </div>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !applications && <Loading />}
      {applications && (sorted.length === 0 ? (
        <EmptyState icon="envelope-open" title="No applicants yet">
          Applications matching these filters will appear here.
        </EmptyState>
      ) : (
        <div className="panel p-0 overflow-hidden">
          <div className="table-responsive">
            <table className="table table-edulink table-hover align-middle mb-0">
              <thead>
                <tr><th>Applicant</th><th>Vacancy</th><th>Match</th><th>Applied</th><th>Status</th><th /></tr>
              </thead>
              <tbody>
                {sorted.map((app) => (
                  <tr key={app.applicationId} className="clickable" onClick={() => setSelected(app)}>
                    <td>
                      <div className="fw-semibold">{app.studentName}</div>
                      <div className="small text-muted">{app.course} · {app.institution}</div>
                    </td>
                    <td className="small">{app.jobTitle}</td>
                    <td><MatchBadge match={app.match} size="sm" /></td>
                    <td className="small">{formatDate(app.appliedDate)}</td>
                    <td><StatusBadge status={app.status} /></td>
                    <td className="text-end">
                      {app.hasCv && <i className="bi bi-file-earmark-pdf text-danger me-2" title="CV attached" aria-label="CV attached" />}
                      <i className="bi bi-chevron-right text-muted" aria-hidden="true" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ))}

      <Modal show={Boolean(selected) && !scheduling} onHide={() => setSelected(null)} size="lg" centered scrollable>
        {selected && (
          <>
            <Modal.Header closeButton>
              <div>
                <Modal.Title className="h5 fw-bold mb-1">{selected.studentName}</Modal.Title>
                <div className="small text-muted">Applied for <strong>{selected.jobTitle}</strong> on {formatDate(selected.appliedDate)}</div>
              </div>
            </Modal.Header>
            <Modal.Body>
              <Row className="g-4">
                <Col md={7}>
                  <dl className="detail-list">
                    <dt>Email</dt><dd><a href={`mailto:${selected.studentEmail}`}>{selected.studentEmail}</a></dd>
                    <dt>Student no.</dt><dd>{selected.studentNumber}</dd>
                    <dt>Qualification</dt><dd>{selected.course}</dd>
                    <dt>Institution</dt><dd>{selected.institution}</dd>
                    <dt>Graduates</dt><dd>{selected.graduationYear ?? '—'}</dd>
                  </dl>
                  <div className="small fw-semibold mb-1">Skills</div>
                  <div className="mb-3"><SkillTags skills={selected.skills} /></div>
                  <div className="small fw-semibold mb-1">Cover letter</div>
                  <div className="cover-letter text-pre-line">{selected.coverLetter}</div>
                </Col>
                <Col md={5}>
                  <div className="panel-soft mb-3">
                    <h2 className="h6 fw-bold">Requirement match</h2>
                    <MatchBreakdown match={selected.match} perspective="company" />
                  </div>
                  <Button variant={selected.hasCv ? 'outline-primary' : 'light'} className="w-100" disabled={!selected.hasCv} onClick={() => viewCv(selected)}>
                    <i className="bi bi-file-earmark-pdf me-2" aria-hidden="true" />{selected.hasCv ? 'View CV' : 'No CV uploaded'}
                  </Button>

                  <div className="panel-soft mt-3">
                    <h2 className="h6 fw-bold">Interview</h2>
                    <InterviewSummary interview={selected.interview} />
                    <Button className="w-100 mt-2" disabled={selected.status === 'REJECTED'} onClick={() => setScheduling(true)}
                            title={selected.status === 'REJECTED' ? 'Change the status before inviting a declined candidate' : undefined}>
                      <i className="bi bi-calendar-plus me-2" aria-hidden="true" />
                      {activeInterview(selected) ? 'Reschedule' : 'Invite to interview'}
                    </Button>
                  </div>
                </Col>
              </Row>
            </Modal.Body>
            <Modal.Footer className="justify-content-between">
              <div className="d-flex align-items-center gap-2">
                <span className="small text-muted">Current status:</span><StatusBadge status={selected.status} />
              </div>
              <div className="d-flex flex-wrap gap-2">
                {selected.status !== 'REVIEWED' && <Button variant="outline-info" onClick={() => changeStatus(selected, 'REVIEWED')}>Mark under review</Button>}
                {selected.status !== 'REJECTED' && <Button variant="outline-danger" onClick={() => changeStatus(selected, 'REJECTED')}>Decline</Button>}
                {selected.status !== 'ACCEPTED' && <Button variant="success" onClick={() => changeStatus(selected, 'ACCEPTED')}>Accept</Button>}
              </div>
            </Modal.Footer>
          </>
        )}
      </Modal>

      <InterviewFormModal
        application={selected}
        show={scheduling}
        onHide={() => setScheduling(false)}
        onSaved={(interview) => patchApplication(selected.applicationId, {
          interview,
          status: selected.status === 'PENDING' ? 'REVIEWED' : selected.status,
        })}
      />
    </Container>
  );
}

const activeInterview = (app) => app.interview && app.interview.status !== 'CANCELLED';

function InterviewSummary({ interview }) {
  if (!interview || interview.status === 'CANCELLED') {
    return <p className="small text-muted mb-0">{interview ? 'The last interview was cancelled.' : 'Not scheduled yet.'}</p>;
  }
  if (interview.status === 'CONFIRMED') {
    return (
      <div className="small">
        <div className="text-success fw-semibold"><i className="bi bi-calendar-check me-1" aria-hidden="true" />Confirmed</div>
        <div>{formatDateTime(interview.confirmedStart)}</div>
        <div className="text-muted"><InterviewWhere interview={interview} /></div>
      </div>
    );
  }
  return (
    <div className="small">
      <div className="fw-semibold text-warning-emphasis"><i className="bi bi-hourglass-split me-1" aria-hidden="true" />Waiting for the candidate to choose</div>
      {interview.slots.map((s) => <div key={s.slotId} className="text-muted">{formatDateTime(s.startsAt)}</div>)}
    </div>
  );
}
