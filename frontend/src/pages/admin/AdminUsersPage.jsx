import { useCallback, useEffect, useState } from 'react';
import { Button, Col, Container, Form, InputGroup, Row } from 'react-bootstrap';
import { adminApi } from '../../api/endpoints.js';
import { errorMessage } from '../../api/client.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useConfirm } from '../../context/ConfirmContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { EmptyState, ErrorState, Loading, PageHeader, Pager } from '../../components/ui.jsx';
import { formatDate } from '../../utils/format.js';

const ROLE_LABELS = { STUDENT: 'Student', COMPANY: 'Company', ADMIN: 'Admin' };

export default function AdminUsersPage() {
  const { user: me } = useAuth();
  const toast = useToast();
  const confirm = useConfirm();
  const [role, setRole] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setResult(await adminApi.users({ role, search, page, size: 10 }));
    } catch (err) {
      setError(errorMessage(err));
    }
  }, [role, search, page]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    const timer = setTimeout(() => { setSearch(searchInput.trim()); setPage(0); }, 350);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const toggle = async (u) => {
    if (u.active) {
      const ok = await confirm({
        title: `Disable ${u.displayName}?`,
        message: 'They will be logged out immediately and will not be able to log in until the account is re-enabled.',
        confirmLabel: 'Disable account',
        variant: 'danger',
      });
      if (!ok) return;
    }
    try {
      await adminApi.setUserActive(u.userId, !u.active);
      toast.success(u.active ? 'Account disabled.' : 'Account re-enabled.');
      load();
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  return (
    <Container className="page">
      <PageHeader title="User accounts" subtitle="Search, enable and disable accounts. Disabled users lose access immediately." />

      <div className="filter-bar">
        <Row className="g-2">
          <Col md={8}>
            <InputGroup>
              <InputGroup.Text><i className="bi bi-search" aria-hidden="true" /></InputGroup.Text>
              <Form.Control placeholder="Search by email" aria-label="Search users by email" value={searchInput}
                            onChange={(e) => setSearchInput(e.target.value)} />
            </InputGroup>
          </Col>
          <Col md={4}>
            <Form.Select aria-label="Filter by role" value={role} onChange={(e) => { setRole(e.target.value); setPage(0); }}>
              <option value="">All roles</option>
              {Object.entries(ROLE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </Form.Select>
          </Col>
        </Row>
      </div>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !result && <Loading />}
      {result && (result.content.length === 0 ? (
        <EmptyState icon="people" title="No users found">Try a different search.</EmptyState>
      ) : (
        <>
          <div className="panel p-0 overflow-hidden">
            <div className="table-responsive">
              <table className="table table-edulink align-middle mb-0">
                <thead>
                  <tr><th>User</th><th>Role</th><th>Joined</th><th>Status</th><th className="text-end">Action</th></tr>
                </thead>
                <tbody>
                  {result.content.map((u) => (
                    <tr key={u.userId}>
                      <td>
                        <div className="fw-semibold">{u.displayName}</div>
                        <div className="small text-muted">{u.email}</div>
                      </td>
                      <td><span className={`role-pill role-${u.role.toLowerCase()}`}>{ROLE_LABELS[u.role]}</span></td>
                      <td className="small">{formatDate(u.createdAt)}</td>
                      <td>
                        {u.active
                          ? <span className="status-badge status-success"><i className="bi bi-check-circle" aria-hidden="true" />Active</span>
                          : <span className="status-badge status-danger"><i className="bi bi-slash-circle" aria-hidden="true" />Disabled</span>}
                      </td>
                      <td className="text-end">
                        {u.userId === me.userId ? (
                          <span className="small text-muted">You</span>
                        ) : (
                          <Button size="sm" variant={u.active ? 'outline-danger' : 'outline-success'} onClick={() => toggle(u)}>
                            {u.active ? 'Disable' : 'Enable'}
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <Pager page={result.page} totalPages={result.totalPages} onChange={setPage} />
        </>
      ))}
    </Container>
  );
}
