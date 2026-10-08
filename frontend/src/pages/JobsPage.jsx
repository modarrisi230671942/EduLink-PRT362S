import { useCallback, useEffect, useState } from 'react';
import { Col, Container, Form, InputGroup, Row } from 'react-bootstrap';
import { useSearchParams } from 'react-router-dom';
import { jobsApi } from '../api/endpoints.js';
import { errorMessage } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import JobCard from '../components/JobCard.jsx';
import JobDetailModal from '../components/JobDetailModal.jsx';
import { EmptyState, ErrorState, Loading, PageHeader, Pager } from '../components/ui.jsx';
import { JOB_TYPES } from '../utils/format.js';

const PAGE_SIZE = 9;

/** Public job search. Filters live in the URL so a search can be bookmarked or shared. */
export default function JobsPage() {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const type = params.get('type') ?? '';
  const sort = params.get('sort') ?? 'newest';
  const page = Number(params.get('page') ?? 0);

  const [search, setSearch] = useState(q);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);

  const update = (changes) => {
    const next = { q, type, sort, page: 0, ...changes };
    setParams(Object.fromEntries(Object.entries(next).filter(([, v]) => v !== '' && v !== 0 && v !== 'newest')));
  };

  const load = useCallback(async () => {
    setError(null);
    try {
      setResult(await jobsApi.search({ q, type, sort, page, size: PAGE_SIZE }));
    } catch (err) {
      setError(errorMessage(err));
    }
  }, [q, type, sort, page]);

  useEffect(() => { load(); }, [load]);

  // Debounce typing in the search box
  useEffect(() => {
    if (search === q) return undefined;
    const timer = setTimeout(() => update({ q: search.trim() }), 350);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  const patchJob = (jobId, changes) => {
    setResult((r) => r && { ...r, content: r.content.map((j) => (j.jobId === jobId ? { ...j, ...changes } : j)) });
    setSelected((s) => (s && s.jobId === jobId ? { ...s, ...changes } : s));
  };
  const onApplied = (jobId) => patchJob(jobId, { applied: true });
  const onSavedChange = (jobId, saved) => patchJob(jobId, { saved });

  return (
    <Container className="page">
      <PageHeader
        title="Find opportunities"
        subtitle={user?.role === 'STUDENT'
          ? 'Each job shows how well it matches the skills on your profile.'
          : 'Graduate programmes, internships and full-time roles from verified employers.'}
      />

      <div className="filter-bar">
        <Row className="g-2">
          <Col md={6}>
            <InputGroup>
              <InputGroup.Text><i className="bi bi-search" aria-hidden="true" /></InputGroup.Text>
              <Form.Control
                placeholder="Search by title, skill, company or location"
                aria-label="Search jobs"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </InputGroup>
          </Col>
          <Col xs={6} md={3}>
            <Form.Select aria-label="Job type" value={type} onChange={(e) => update({ type: e.target.value })}>
              <option value="">All job types</option>
              {JOB_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </Form.Select>
          </Col>
          <Col xs={6} md={3}>
            <Form.Select aria-label="Sort" value={sort} onChange={(e) => update({ sort: e.target.value })}>
              <option value="newest">Newest first</option>
              <option value="deadline">Closing soonest</option>
            </Form.Select>
          </Col>
        </Row>
      </div>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !result && <Loading label="Finding jobs…" />}
      {result && (
        <>
          <p className="text-muted small mb-3">
            {result.totalElements} {result.totalElements === 1 ? 'opportunity' : 'opportunities'} found
          </p>
          {result.content.length === 0 ? (
            <EmptyState icon="search" title="No jobs match your search">Try different keywords or remove a filter.</EmptyState>
          ) : (
            <Row className="g-4">
              {result.content.map((job) => (
                <Col md={6} lg={4} key={job.jobId}><JobCard job={job} onOpen={setSelected} onSavedChange={onSavedChange} /></Col>
              ))}
            </Row>
          )}
          <Pager page={result.page} totalPages={result.totalPages} onChange={(p) => update({ page: p })} />
        </>
      )}

      <JobDetailModal job={selected} onHide={() => setSelected(null)} onApplied={onApplied} onSavedChange={onSavedChange} />
    </Container>
  );
}
