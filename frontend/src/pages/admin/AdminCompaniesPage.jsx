import { useCallback, useEffect, useState } from 'react';
import { Button, Container, Nav } from 'react-bootstrap';
import { adminApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import { useConfirm } from '../../context/ConfirmContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorState, Loading, PageHeader } from '../../components/ui.jsx';
import { formatDate, safeUrl } from '../../utils/format.js';

const FILTERS = [
  { key: 'all', label: 'All', verified: undefined },
  { key: 'pending', label: 'Awaiting verification', verified: false },
  { key: 'verified', label: 'Verified', verified: true },
];

export default function AdminCompaniesPage() {
  const toast = useToast();
  const confirm = useConfirm();
  const [filter, setFilter] = useState('pending');
  const [companies, setCompanies] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setCompanies(await adminApi.companies(FILTERS.find((f) => f.key === filter).verified));
    } catch (err) {
      setError(errorMessage(err));
    }
  }, [filter]);

  useEffect(() => { load(); }, [load]);

  const setVerified = async (company, verified) => {
    if (!verified) {
      const ok = await confirm({
        title: 'Revoke verification?',
        message: `${company.companyName} will no longer be able to post vacancies, and its open vacancies will be hidden from students.`,
        confirmLabel: 'Revoke',
        variant: 'danger',
      });
      if (!ok) return;
    }
    try {
      await adminApi.setVerified(company.companyId, verified);
      toast.success(verified ? `${company.companyName} is now verified and has been notified.` : 'Verification revoked.');
      load();
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  return (
    <Container className="page">
      <PageHeader title="Employer verification" subtitle="Only verified employers can post vacancies — this protects students from fake job adverts." />

      <Nav variant="pills" className="filter-pills mb-4" activeKey={filter} onSelect={(k) => { setCompanies(null); setFilter(k); }}>
        {FILTERS.map((f) => <Nav.Item key={f.key}><Nav.Link eventKey={f.key}>{f.label}</Nav.Link></Nav.Item>)}
      </Nav>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !companies && <Loading />}
      {companies && (companies.length === 0 ? (
        <EmptyState icon="patch-check" title="Nothing to review">No companies match this filter.</EmptyState>
      ) : (
        <div className="panel p-0 overflow-hidden">
          <div className="table-responsive">
            <table className="table table-edulink align-middle mb-0">
              <thead>
                <tr><th>Company</th><th>Industry</th><th>Location</th><th>Registered</th><th>Status</th><th className="text-end">Action</th></tr>
              </thead>
              <tbody>
                {companies.map((c) => {
                  const website = safeUrl(c.website);
                  return (
                    <tr key={c.companyId}>
                      <td>
                        <div className="fw-semibold">{c.companyName}</div>
                        <div className="small text-muted">
                          {c.email}
                          {website && <> · <a href={website} target="_blank" rel="noopener noreferrer">website</a></>}
                        </div>
                      </td>
                      <td className="small">{c.industry ?? '—'}</td>
                      <td className="small">{c.location ?? '—'}</td>
                      <td className="small">{formatDate(c.createdAt)}</td>
                      <td>
                        {c.verified
                          ? <span className="status-badge status-success"><i className="bi bi-patch-check" aria-hidden="true" />Verified</span>
                          : <span className="status-badge status-warning"><i className="bi bi-hourglass-split" aria-hidden="true" />Pending</span>}
                      </td>
                      <td className="text-end">
                        {c.verified
                          ? <Button size="sm" variant="outline-danger" onClick={() => setVerified(c, false)}>Revoke</Button>
                          : <Button size="sm" variant="success" onClick={() => setVerified(c, true)}><i className="bi bi-patch-check me-1" aria-hidden="true" />Verify</Button>}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      ))}
    </Container>
  );
}
