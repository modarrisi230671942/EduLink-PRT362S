import { useCallback, useEffect, useState } from 'react';
import { Button, Container, Modal, Nav } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { applicationsApi, studentApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import { useConfirm } from '../../context/ConfirmContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorState, Loading, MatchBadge, PageHeader, StatusBadge } from '../../components/ui.jsx';
import { StudentInterviewPanel } from '../../components/interviews.jsx';
import { useLiveRefresh } from '../../api/live.js';
import { APPLICATION_STATUSES, formatDate, jobTypeLabel } from '../../utils/format.js';

const STEPS = ['PENDING', 'REVIEWED', 'DECIDED'];

/** Visual progress: Submitted → Under review → Decision. */
function Progress({ status }) {
  const reached = status === 'PENDING' ? 0 : status === 'REVIEWED' ? 1 : 2;
  const decisionLabel = status === 'ACCEPTED' ? 'Accepted' : status === 'REJECTED' ? 'Unsuccessful' : 'Decision';
  const labels = ['Submitted', 'Under review', decisionLabel];
  return (
    <ol className={`app-progress status-${status.toLowerCase()}`} aria-label="Application progress">
      {STEPS.map((step, i) => (
        <li key={step} className={i <= reached ? 'reached' : ''}>{labels[i]}</li>
      ))}
    </ol>
  );
}

export default function MyApplicationsPage() {
  const toast = useToast();
  const confirm = useConfirm();
  const [applications, setApplications] = useState(null);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('ALL');
  const [letter, setLetter] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setApplications(await studentApi.applications());
    } catch (err) {
      setError(errorMessage(err));
    }
  }, []);

  useEffect(() => { load(); }, [load]);
  // Refresh the moment a company changes a status or sends an interview invitation
  useLiveRefresh(load, ['APPLICATION_STATUS_CHANGED', 'INTERVIEW_PROPOSED', 'INTERVIEW_CANCELLED']);

  const updateInterview = (applicationId, interview) => {
    setApplications((list) => list.map((a) => (a.applicationId === applicationId ? { ...a, interview } : a)));
  };

  const withdraw = async (app) => {
    const ok = await confirm({
      title: 'Withdraw application?',
      message: `Your application for ${app.jobTitle} at ${app.companyName} will be removed. You can apply again while the job is open.`,
      confirmLabel: 'Withdraw',
      variant: 'danger',
    });
    if (!ok) return;
    try {
      await applicationsApi.withdraw(app.applicationId);
      toast.success('Application withdrawn.');
      load();
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  if (error) return <Container className="page"><ErrorState message={error} onRetry={load} /></Container>;
  if (!applications) return <Container className="page"><Loading /></Container>;

  const visible = filter === 'ALL' ? applications : applications.filter((a) => a.status === filter);

  return (
    <Container className="page">
      <PageHeader title="My applications" subtitle="Track every application from submission to decision." />

      <Nav variant="pills" className="filter-pills mb-4" activeKey={filter} onSelect={(k) => setFilter(k)}>
        <Nav.Item><Nav.Link eventKey="ALL">All ({applications.length})</Nav.Link></Nav.Item>
        {APPLICATION_STATUSES.map((s) => (
          <Nav.Item key={s.value}>
            <Nav.Link eventKey={s.value}>{s.label} ({applications.filter((a) => a.status === s.value).length})</Nav.Link>
          </Nav.Item>
        ))}
      </Nav>

      {visible.length === 0 ? (
        <EmptyState icon="journal-x" title={applications.length === 0 ? 'No applications yet' : 'Nothing here'}
                    action={applications.length === 0 && <Link to="/jobs" className="btn btn-primary">Find jobs</Link>}>
          {applications.length === 0 ? 'When you apply for jobs, you can follow their progress here.' : 'No applications with this status.'}
        </EmptyState>
      ) : (
        <div className="d-flex flex-column gap-3">
          {visible.map((app) => (
            <div key={app.applicationId} className="panel application-row">
              <div className="flex-grow-1 min-w-0">
                <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                  <h2 className="h6 fw-bold mb-0">{app.jobTitle}</h2>
                  <StatusBadge status={app.status} />
                  <MatchBadge match={app.match} size="sm" />
                </div>
                <div className="small text-muted mb-3">
                  <i className="bi bi-building me-1" aria-hidden="true" />{app.companyName}
                  <span className="mx-2">·</span>{jobTypeLabel(app.jobType)}
                  <span className="mx-2">·</span>Applied {formatDate(app.appliedDate)}
                  {app.statusUpdatedAt && <><span className="mx-2">·</span>Updated {formatDate(app.statusUpdatedAt)}</>}
                </div>
                <Progress status={app.status} />
                <StudentInterviewPanel interview={app.interview} onChanged={(interview) => updateInterview(app.applicationId, interview)} />
              </div>
              <div className="d-flex flex-md-column gap-2 align-self-center">
                <Button size="sm" variant="light" onClick={() => setLetter(app)}>
                  <i className="bi bi-file-text me-1" aria-hidden="true" />Cover letter
                </Button>
                {app.status === 'PENDING' && (
                  <Button size="sm" variant="outline-danger" onClick={() => withdraw(app)}>
                    <i className="bi bi-x-lg me-1" aria-hidden="true" />Withdraw
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal show={Boolean(letter)} onHide={() => setLetter(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">Cover letter — {letter?.jobTitle}</Modal.Title>
        </Modal.Header>
        <Modal.Body className="text-pre-line">{letter?.coverLetter}</Modal.Body>
      </Modal>
    </Container>
  );
}
