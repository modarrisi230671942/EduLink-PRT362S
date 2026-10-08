import { useCallback, useEffect, useState } from 'react';
import { Button, Col, Container, Dropdown, Row } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { companyApi, jobsApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useConfirm } from '../../context/ConfirmContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorState, Loading, PageHeader, StatTile } from '../../components/ui.jsx';
import { daysUntil, deadlineLabel, formatDate, jobTypeLabel } from '../../utils/format.js';
import JobFormModal from './JobFormModal.jsx';
import { useLiveRefresh } from '../../api/live.js';

function jobState(job) {
  if (!job.active) return { label: 'Closed', className: 'state-closed' };
  if (daysUntil(job.applicationDeadline) < 0) return { label: 'Expired', className: 'state-expired' };
  return { label: 'Live', className: 'state-live' };
}

export default function CompanyJobsPage() {
  const { refreshUser } = useAuth();
  const toast = useToast();
  const confirm = useConfirm();
  const [company, setCompany] = useState(null);
  const [jobs, setJobs] = useState(null);
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(null); // null = closed, {} = new job, job = edit

  const load = useCallback(async () => {
    setError(null);
    try {
      const [profile, list] = await Promise.all([companyApi.profile(), companyApi.jobs()]);
      setCompany(profile);
      setJobs(list);
    } catch (err) {
      setError(errorMessage(err));
    }
  }, []);

  useEffect(() => {
    load();
    refreshUser().catch(() => {}); // pick up a verification that happened since login
  }, [load, refreshUser]);
  // Unlocks "Post a vacancy" the moment the admin verifies the company, and keeps applicant counts live
  useLiveRefresh(load, ['COMPANY_VERIFIED', 'COMPANY_VERIFICATION_REVOKED', 'APPLICATION_RECEIVED', 'APPLICATION_WITHDRAWN']);

  const toggle = async (job) => {
    try {
      await jobsApi.setActive(job.jobId, !job.active);
      toast.success(job.active ? 'Vacancy closed to new applications.' : 'Vacancy re-opened.');
      load();
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  const remove = async (job) => {
    const ok = await confirm({
      title: 'Delete this vacancy?',
      message: `"${job.title}" and its ${job.applicationCount} application(s) will be permanently deleted.`,
      confirmLabel: 'Delete',
      variant: 'danger',
    });
    if (!ok) return;
    try {
      await jobsApi.remove(job.jobId);
      toast.success('Vacancy deleted.');
      load();
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  if (error) return <Container className="page"><ErrorState message={error} onRetry={load} /></Container>;
  if (!jobs) return <Container className="page"><Loading /></Container>;

  const live = jobs.filter((j) => jobState(j).label === 'Live').length;
  const totalApplications = jobs.reduce((sum, j) => sum + (j.applicationCount ?? 0), 0);

  return (
    <Container className="page">
      <PageHeader
        title="My vacancies"
        subtitle={company.companyName}
        actions={(
          <Button onClick={() => setEditing({})} disabled={!company.verified}
                  title={company.verified ? undefined : 'Your company must be verified first'}>
            <i className="bi bi-plus-lg me-2" aria-hidden="true" />Post a vacancy
          </Button>
        )}
      />

      {!company.verified && (
        <div className="alert alert-warning d-flex gap-3 align-items-start">
          <i className="bi bi-hourglass-split fs-4" aria-hidden="true" />
          <div>
            <div className="fw-semibold">Awaiting verification</div>
            <div className="small">
              To protect students from fraudulent adverts, an administrator reviews every new employer.
              You&apos;ll get a notification as soon as you can start posting vacancies.
            </div>
          </div>
        </div>
      )}

      <Row className="g-3 mb-4">
        <Col xs={6} lg={3}><StatTile icon="briefcase" label="Total vacancies" value={jobs.length} /></Col>
        <Col xs={6} lg={3}><StatTile icon="broadcast" label="Live now" value={live} tone="success" /></Col>
        <Col xs={6} lg={3}><StatTile icon="people" label="Applications received" value={totalApplications} tone="info" /></Col>
        <Col xs={6} lg={3}><StatTile icon={company.verified ? 'patch-check' : 'patch-exclamation'} label="Verification" value={company.verified ? 'Verified' : 'Pending'} tone={company.verified ? 'success' : 'warning'} /></Col>
      </Row>

      {jobs.length === 0 ? (
        <EmptyState icon="briefcase" title="No vacancies yet"
                    action={company.verified && <Button onClick={() => setEditing({})}>Post your first vacancy</Button>}>
          Vacancies you post will appear here with their applicant counts.
        </EmptyState>
      ) : (
        <div className="panel p-0 overflow-hidden">
          <div className="table-responsive">
            <table className="table table-edulink align-middle mb-0">
              <thead>
                <tr>
                  <th>Vacancy</th>
                  <th>Status</th>
                  <th>Deadline</th>
                  <th className="text-center">Applicants</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((job) => {
                  const state = jobState(job);
                  return (
                    <tr key={job.jobId}>
                      <td>
                        <div className="fw-semibold">{job.title}</div>
                        <div className="small text-muted">{jobTypeLabel(job.jobType)} · {job.location} · Posted {formatDate(job.postedDate)}</div>
                      </td>
                      <td><span className={`state-pill ${state.className}`}>{state.label}</span></td>
                      <td className="small">
                        {formatDate(job.applicationDeadline)}
                        <div className="text-muted">{deadlineLabel(job.applicationDeadline)}</div>
                      </td>
                      <td className="text-center">
                        <Link to={`/company/applications?jobId=${job.jobId}`} className="applicant-count">{job.applicationCount}</Link>
                      </td>
                      <td className="text-end">
                        <Dropdown align="end">
                          <Dropdown.Toggle variant="light" size="sm" aria-label={`Actions for ${job.title}`}>
                            <i className="bi bi-three-dots" aria-hidden="true" />
                          </Dropdown.Toggle>
                          <Dropdown.Menu>
                            <Dropdown.Item as={Link} to={`/company/applications?jobId=${job.jobId}`}><i className="bi bi-people me-2" aria-hidden="true" />View applicants</Dropdown.Item>
                            <Dropdown.Item onClick={() => setEditing(job)}><i className="bi bi-pencil me-2" aria-hidden="true" />Edit</Dropdown.Item>
                            <Dropdown.Item onClick={() => toggle(job)}>
                              <i className={`bi bi-${job.active ? 'pause-circle' : 'play-circle'} me-2`} aria-hidden="true" />
                              {job.active ? 'Close to applications' : 'Re-open'}
                            </Dropdown.Item>
                            <Dropdown.Divider />
                            <Dropdown.Item className="text-danger" onClick={() => remove(job)}><i className="bi bi-trash me-2" aria-hidden="true" />Delete</Dropdown.Item>
                          </Dropdown.Menu>
                        </Dropdown>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <JobFormModal
        show={editing !== null}
        job={editing?.jobId ? editing : null}
        onHide={() => setEditing(null)}
        onSaved={load}
      />
    </Container>
  );
}
