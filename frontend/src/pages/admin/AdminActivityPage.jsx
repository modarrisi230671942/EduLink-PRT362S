import { useCallback, useEffect, useState } from 'react';
import { Col, Container, Form, InputGroup, Row } from 'react-bootstrap';
import { adminApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorState, Loading, PageHeader, Pager } from '../../components/ui.jsx';
import { formatDateTime, timeAgo } from '../../utils/format.js';

/** How each audit action is shown. `kind` groups them: security events, admin actions, account info. */
const ACTIONS = {
  LOGIN_FAILED: { label: 'Failed login', icon: 'shield-exclamation', kind: 'security' },
  ACCOUNT_LOCKED: { label: 'Account locked', icon: 'lock', kind: 'security' },
  LOGIN_SUCCESS: { label: 'Logged in', icon: 'box-arrow-in-right', kind: 'info' },
  PASSWORD_CHANGED: { label: 'Password changed', icon: 'key', kind: 'info' },
  USER_REGISTERED: { label: 'Registered', icon: 'person-plus', kind: 'info' },
  COMPANY_VERIFIED: { label: 'Company verified', icon: 'patch-check', kind: 'admin' },
  COMPANY_VERIFICATION_REVOKED: { label: 'Verification revoked', icon: 'patch-minus', kind: 'admin' },
  USER_ENABLED: { label: 'Account enabled', icon: 'person-check', kind: 'admin' },
  USER_DISABLED: { label: 'Account disabled', icon: 'person-slash', kind: 'admin' },
  JOB_DELETED_BY_ADMIN: { label: 'Job removed by admin', icon: 'trash', kind: 'admin' },
};

export default function AdminActivityPage() {
  const [action, setAction] = useState('');
  const [actorInput, setActorInput] = useState('');
  const [actor, setActor] = useState('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setResult(await adminApi.activity({ action, actor, page, size: 20 }));
    } catch (err) {
      setError(errorMessage(err));
    }
  }, [action, actor, page]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    const timer = setTimeout(() => { setActor(actorInput.trim()); setPage(0); }, 350);
    return () => clearTimeout(timer);
  }, [actorInput]);

  return (
    <Container className="page">
      <PageHeader
        title="Activity log"
        subtitle="A record of sign-ins, failed logins, lockouts and every administrator action."
        actions={<button type="button" className="btn btn-light" onClick={load}><i className="bi bi-arrow-clockwise me-1" aria-hidden="true" />Refresh</button>}
      />

      <div className="filter-bar">
        <Row className="g-2">
          <Col md={7}>
            <InputGroup>
              <InputGroup.Text><i className="bi bi-search" aria-hidden="true" /></InputGroup.Text>
              <Form.Control placeholder="Filter by email" aria-label="Filter by email" value={actorInput}
                            onChange={(e) => setActorInput(e.target.value)} />
            </InputGroup>
          </Col>
          <Col md={5}>
            <Form.Select aria-label="Filter by event" value={action} onChange={(e) => { setAction(e.target.value); setPage(0); }}>
              <option value="">All events</option>
              <optgroup label="Security">
                {Object.entries(ACTIONS).filter(([, a]) => a.kind === 'security').map(([k, a]) => <option key={k} value={k}>{a.label}</option>)}
              </optgroup>
              <optgroup label="Administrator actions">
                {Object.entries(ACTIONS).filter(([, a]) => a.kind === 'admin').map(([k, a]) => <option key={k} value={k}>{a.label}</option>)}
              </optgroup>
              <optgroup label="Accounts">
                {Object.entries(ACTIONS).filter(([, a]) => a.kind === 'info').map(([k, a]) => <option key={k} value={k}>{a.label}</option>)}
              </optgroup>
            </Form.Select>
          </Col>
        </Row>
      </div>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !result && <Loading />}
      {result && (result.content.length === 0 ? (
        <EmptyState icon="shield-check" title="No matching activity">Try another filter.</EmptyState>
      ) : (
        <>
          <div className="panel p-0 overflow-hidden">
            <div className="table-responsive">
              <table className="table table-edulink align-middle mb-0">
                <thead>
                  <tr><th>Event</th><th>Who</th><th>Details</th><th>When</th><th>IP address</th></tr>
                </thead>
                <tbody>
                  {result.content.map((entry) => {
                    const info = ACTIONS[entry.action] ?? { label: entry.action, icon: 'dot', kind: 'info' };
                    return (
                      <tr key={entry.logId}>
                        <td>
                          <div className="d-flex align-items-center gap-2">
                            <span className={`activity-icon activity-${info.kind}`}><i className={`bi bi-${info.icon}`} aria-hidden="true" /></span>
                            <span className="fw-semibold">{info.label}</span>
                          </div>
                        </td>
                        <td className="small">{entry.actorEmail ?? '—'}</td>
                        <td className="small text-muted">{entry.details ?? ''}</td>
                        <td className="small" title={formatDateTime(entry.createdAt)}>{timeAgo(entry.createdAt)}</td>
                        <td className="small text-muted font-monospace">{entry.ipAddress ?? '—'}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
          <Pager page={result.page} totalPages={Math.min(result.totalPages, 20)} onChange={setPage} />
        </>
      ))}
    </Container>
  );
}
