import { useCallback, useEffect, useState } from 'react';
import { Col, Container, Row } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { studentApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import { useAuth } from '../../context/AuthContext.jsx';
import JobCard from '../../components/JobCard.jsx';
import JobDetailModal from '../../components/JobDetailModal.jsx';
import { EmptyState, ErrorState, Loading, PageHeader, StatTile, StatusBadge } from '../../components/ui.jsx';
import { formatDate, formatDateTime } from '../../utils/format.js';
import { DateBadge } from '../../components/interviews.jsx';
import { useLiveRefresh } from '../../api/live.js';

export default function StudentDashboard() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [profile, applications, recommendations, interviews] = await Promise.all([
        studentApi.profile(), studentApi.applications(), studentApi.recommendations(6), studentApi.interviews(),
      ]);
      setData({ profile, applications, recommendations, interviews });
    } catch (err) {
      setError(errorMessage(err));
    }
  }, []);

  useEffect(() => { load(); }, [load]);
  useLiveRefresh(load, ['APPLICATION_STATUS_CHANGED', 'INTERVIEW_PROPOSED', 'INTERVIEW_CANCELLED', 'JOB_MATCH']);

  if (error) return <Container className="page"><ErrorState message={error} onRetry={load} /></Container>;
  if (!data) return <Container className="page"><Loading /></Container>;

  const { profile, applications, recommendations, interviews } = data;
  const onSavedChange = (jobId, saved) => {
    setData((d) => ({ ...d, recommendations: d.recommendations.map((j) => (j.jobId === jobId ? { ...j, saved } : j)) }));
    setSelected((s) => (s && s.jobId === jobId ? { ...s, saved } : s));
  };
  const count = (status) => applications.filter((a) => a.status === status).length;
  const checklist = [
    { done: profile.skillList.length >= 3, label: 'Add at least 3 skills', hint: 'Skills drive your match scores' },
    { done: profile.hasCv, label: 'Upload your CV', hint: 'Employers see it with every application' },
    { done: applications.length > 0, label: 'Send your first application', hint: 'Start with your best match below' },
  ];
  const completed = checklist.filter((c) => c.done).length;

  return (
    <Container className="page">
      <PageHeader
        title={`Hi, ${user.displayName.split(' ')[0]} 👋`}
        subtitle={`${profile.course} · ${profile.institution} · Class of ${profile.graduationYear}`}
        actions={<Link to="/jobs" className="btn btn-primary"><i className="bi bi-search me-2" aria-hidden="true" />Find jobs</Link>}
      />

      <Row className="g-3 mb-4">
        <Col xs={6} lg={3}><StatTile icon="send" label="Applications sent" value={applications.length} /></Col>
        <Col xs={6} lg={3}><StatTile icon="hourglass-split" label="Awaiting response" value={count('PENDING') + count('REVIEWED')} tone="warning" /></Col>
        <Col xs={6} lg={3}><StatTile icon="check-circle" label="Accepted" value={count('ACCEPTED')} tone="success" /></Col>
        <Col xs={6} lg={3}><StatTile icon="stars" label="Recommended jobs" value={recommendations.length} tone="info" /></Col>
      </Row>

      <Row className="g-4">
        <Col lg={8}>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2 className="h5 fw-bold mb-0">Recommended for you</h2>
            <span className="small text-muted">Ranked by skill match</span>
          </div>
          {recommendations.length === 0 ? (
            <EmptyState icon="stars" title="No recommendations yet"
                        action={<Link to="/student/profile" className="btn btn-outline-primary btn-sm">Add skills to your profile</Link>}>
              Add skills to your profile and we&apos;ll match you with open jobs.
            </EmptyState>
          ) : (
            <Row className="g-3">
              {recommendations.map((job) => (
                <Col md={6} key={job.jobId}><JobCard job={job} onOpen={setSelected} onSavedChange={onSavedChange} /></Col>
              ))}
            </Row>
          )}
        </Col>

        <Col lg={4}>
          {interviews.length > 0 && (
            <div className="panel mb-4">
              <h2 className="h6 fw-bold mb-3"><i className="bi bi-calendar-event me-2 text-primary" aria-hidden="true" />Upcoming interviews</h2>
              {interviews.map((i) => (
                <div key={i.interviewId} className="recent-row">
                  <div className="d-flex gap-2 align-items-center min-w-0">
                    {i.confirmedStart && <DateBadge value={i.confirmedStart} />}
                    <div className="min-w-0">
                      <div className="fw-semibold text-truncate">{i.jobTitle}</div>
                      <div className="small text-muted text-truncate">{i.companyName}</div>
                      <div className="small">
                        {i.status === 'CONFIRMED'
                          ? formatDateTime(i.confirmedStart)
                          : <Link to="/student/applications" className="fw-semibold">Choose a time →</Link>}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="panel mb-4">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <h2 className="h6 fw-bold mb-0">Profile strength</h2>
              <span className="small fw-semibold text-primary">{completed}/{checklist.length}</span>
            </div>
            <div className="progress mb-3" style={{ height: 6 }}>
              <div className="progress-bar" style={{ width: `${(completed / checklist.length) * 100}%` }} />
            </div>
            {checklist.map((c) => (
              <div key={c.label} className="checklist-item">
                <i className={`bi ${c.done ? 'bi-check-circle-fill text-success' : 'bi-circle text-muted'}`} aria-hidden="true" />
                <div>
                  <div className={c.done ? 'text-decoration-line-through text-muted' : 'fw-semibold'}>{c.label}</div>
                  {!c.done && <div className="small text-muted">{c.hint}</div>}
                </div>
              </div>
            ))}
            {completed < checklist.length && <Link to="/student/profile" className="btn btn-light btn-sm w-100 mt-2">Complete profile</Link>}
          </div>

          <div className="panel">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h2 className="h6 fw-bold mb-0">Recent applications</h2>
              <Link to="/student/applications" className="small">View all</Link>
            </div>
            {applications.length === 0 ? (
              <p className="small text-muted mb-0">You haven&apos;t applied for anything yet.</p>
            ) : (
              applications.slice(0, 4).map((a) => (
                <div key={a.applicationId} className="recent-row">
                  <div className="min-w-0">
                    <div className="fw-semibold text-truncate">{a.jobTitle}</div>
                    <div className="small text-muted">{a.companyName} · {formatDate(a.appliedDate)}</div>
                  </div>
                  <StatusBadge status={a.status} />
                </div>
              ))
            )}
          </div>
        </Col>
      </Row>

      <JobDetailModal job={selected} onHide={() => setSelected(null)} onApplied={load} onSavedChange={onSavedChange} />
    </Container>
  );
}
