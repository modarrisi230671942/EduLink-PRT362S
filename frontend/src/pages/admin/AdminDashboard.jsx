import { useCallback, useEffect, useState } from 'react';
import { Col, Container, Row } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import { ErrorState, Loading, PageHeader, StatTile } from '../../components/ui.jsx';
import ChartCard from './ChartCard.jsx';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setStats(await adminApi.stats());
    } catch (err) {
      setError(errorMessage(err));
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (error) return <Container className="page"><ErrorState message={error} onRetry={load} /></Container>;
  if (!stats) return <Container className="page"><Loading label="Loading analytics…" /></Container>;

  const t = stats.totals;

  return (
    <Container className="page">
      <PageHeader
        title="Platform analytics"
        subtitle="Live figures from the EduLink database."
        actions={<button type="button" className="btn btn-light" onClick={load}><i className="bi bi-arrow-clockwise me-1" aria-hidden="true" />Refresh</button>}
      />

      {t.pendingCompanies > 0 && (
        <div className="alert alert-warning d-flex justify-content-between align-items-center gap-3">
          <span>
            <i className="bi bi-patch-exclamation me-2" aria-hidden="true" />
            <strong>{t.pendingCompanies}</strong> {t.pendingCompanies === 1 ? 'company is' : 'companies are'} waiting for verification.
          </span>
          <Link to="/admin/companies" className="btn btn-sm btn-warning">Review now</Link>
        </div>
      )}

      <Row className="g-3 mb-4">
        <Col xs={6} lg={3}><StatTile icon="mortarboard" label="Students" value={t.students} /></Col>
        <Col xs={6} lg={3}><StatTile icon="building" label="Companies" value={t.companies} hint={`${t.verifiedCompanies} verified`} tone="info" /></Col>
        <Col xs={6} lg={3}><StatTile icon="briefcase" label="Open vacancies" value={t.openJobs} hint={`${t.jobs} posted in total`} tone="success" /></Col>
        <Col xs={6} lg={3}><StatTile icon="percent" label="Acceptance rate" value={`${t.acceptanceRate}%`} hint={t.decidedApplications ? `${t.acceptedApplications} accepted of ${t.decidedApplications} decided` : 'No decisions yet'} tone="warning" /></Col>
      </Row>

      <Row className="g-4">
        <Col lg={8}>
          <ChartCard title="Applications per month" subtitle="Last six months" type="line"
                     rows={stats.applicationsPerMonth} valueLabel="Applications" height={260} />
        </Col>
        <Col lg={4}>
          <ChartCard title="Applications by status" subtitle={`${t.applications} in total`} type="hbar"
                     rows={stats.applicationsByStatus} valueLabel="Applications" height={260} />
        </Col>
        <Col lg={6}>
          <ChartCard title="Vacancies by type" type="bar" rows={stats.jobsByType} valueLabel="Vacancies" />
        </Col>
        <Col lg={6}>
          <ChartCard title="Most popular employers" subtitle="By applications received" type="hbar"
                     rows={stats.topCompanies} valueLabel="Applications" />
        </Col>
      </Row>
    </Container>
  );
}
