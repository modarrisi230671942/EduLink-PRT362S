import { useCallback, useEffect, useState } from 'react';
import { Col, Container, Row } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { studentApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import JobCard from '../../components/JobCard.jsx';
import JobDetailModal from '../../components/JobDetailModal.jsx';
import { EmptyState, ErrorState, Loading, PageHeader } from '../../components/ui.jsx';

export default function SavedJobsPage() {
  const [jobs, setJobs] = useState(null);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setJobs(await studentApi.savedJobs());
    } catch (err) {
      setError(errorMessage(err));
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const onSavedChange = (jobId, saved) => {
    if (!saved) {
      setJobs((list) => list.filter((j) => j.jobId !== jobId));
      setSelected(null);
    }
  };

  if (error) return <Container className="page"><ErrorState message={error} onRetry={load} /></Container>;
  if (!jobs) return <Container className="page"><Loading /></Container>;

  return (
    <Container className="page">
      <PageHeader title="Saved jobs" subtitle="Jobs you bookmarked to come back to. Don't miss the deadlines!" />
      {jobs.length === 0 ? (
        <EmptyState icon="bookmark" title="No saved jobs yet"
                    action={<Link to="/jobs" className="btn btn-primary">Browse jobs</Link>}>
          Tap the bookmark icon on any job to save it here.
        </EmptyState>
      ) : (
        <Row className="g-4">
          {jobs.map((job) => (
            <Col md={6} lg={4} key={job.jobId}><JobCard job={job} onOpen={setSelected} onSavedChange={onSavedChange} /></Col>
          ))}
        </Row>
      )}
      <JobDetailModal job={selected} onHide={() => setSelected(null)} onApplied={load} onSavedChange={onSavedChange} />
    </Container>
  );
}
