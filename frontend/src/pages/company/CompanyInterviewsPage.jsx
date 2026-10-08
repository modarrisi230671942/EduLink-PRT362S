import { useCallback, useEffect, useState } from 'react';
import { Container } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { companyApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import { useLiveRefresh } from '../../api/live.js';
import { CalendarButton, CancelInterviewButton, DateBadge, InterviewWhere } from '../../components/interviews.jsx';
import { EmptyState, ErrorState, Loading, PageHeader } from '../../components/ui.jsx';
import { formatDateTime } from '../../utils/format.js';

/** Upcoming confirmed interviews and invitations still waiting for the candidate. */
export default function CompanyInterviewsPage() {
  const [interviews, setInterviews] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setInterviews(await companyApi.interviews());
    } catch (err) {
      setError(errorMessage(err));
    }
  }, []);

  useEffect(() => { load(); }, [load]);
  useLiveRefresh(load, ['INTERVIEW_CONFIRMED', 'INTERVIEW_CANCELLED']);

  if (error) return <Container className="page"><ErrorState message={error} onRetry={load} /></Container>;
  if (!interviews) return <Container className="page"><Loading /></Container>;

  const confirmed = interviews.filter((i) => i.status === 'CONFIRMED');
  const waiting = interviews.filter((i) => i.status === 'PROPOSED');

  return (
    <Container className="page">
      <PageHeader title="Interviews" subtitle="Invite candidates from the Applicants page; they choose a time and you're notified instantly." />

      {interviews.length === 0 ? (
        <EmptyState icon="calendar-event" title="No upcoming interviews"
                    action={<Link to="/company/applications" className="btn btn-primary">Review applicants</Link>}>
          Open an applicant and click “Invite to interview” to propose times.
        </EmptyState>
      ) : (
        <>
          <h2 className="h6 fw-bold mb-3">Confirmed ({confirmed.length})</h2>
          {confirmed.length === 0 && <p className="small text-muted">No confirmed interviews yet.</p>}
          <div className="d-flex flex-column gap-3 mb-4">
            {confirmed.map((i) => (
              <div key={i.interviewId} className="panel d-flex flex-wrap gap-3 align-items-center">
                <DateBadge value={i.confirmedStart} />
                <div className="flex-grow-1 min-w-0">
                  <div className="fw-semibold">{i.studentName} <span className="text-muted fw-normal">· {i.jobTitle}</span></div>
                  <div className="small">{formatDateTime(i.confirmedStart)} · {i.durationMinutes} min</div>
                  <div className="small text-muted"><InterviewWhere interview={i} /></div>
                </div>
                <div className="d-flex gap-2">
                  <CalendarButton interview={i} />
                  <CancelInterviewButton interview={i} onChanged={load} />
                </div>
              </div>
            ))}
          </div>

          <h2 className="h6 fw-bold mb-3">Waiting for the candidate ({waiting.length})</h2>
          {waiting.length === 0 && <p className="small text-muted">No pending invitations.</p>}
          <div className="d-flex flex-column gap-3">
            {waiting.map((i) => (
              <div key={i.interviewId} className="panel d-flex flex-wrap gap-3 align-items-center">
                <div className="flex-grow-1 min-w-0">
                  <div className="fw-semibold">{i.studentName} <span className="text-muted fw-normal">· {i.jobTitle}</span></div>
                  <div className="small text-muted">
                    Offered: {i.slots.map((s) => formatDateTime(s.startsAt)).join(' · ')}
                  </div>
                </div>
                <span className="status-badge status-warning"><i className="bi bi-hourglass-split" aria-hidden="true" />Awaiting reply</span>
                <CancelInterviewButton interview={i} onChanged={load} />
              </div>
            ))}
          </div>
        </>
      )}
    </Container>
  );
}
