import { useState } from 'react';
import { Button, Container, Form, Spinner } from 'react-bootstrap';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client.js';
import { HOME_BY_ROLE, useAuth } from '../context/AuthContext.jsx';
import { Brand } from '../components/AppNavbar.jsx';

/** Seeded demo accounts (see V3__demo_data.sql) — handy during the presentation. */
const DEMO_ACCOUNTS = [
  { label: 'Student', email: 'alice.student@test.com', password: 'password123', icon: 'mortarboard' },
  { label: 'Company', email: 'techcompany@test.com', password: 'password123', icon: 'building' },
  { label: 'Admin', email: 'admin@edulink.com', password: 'admin123', icon: 'shield-lock' },
];

export default function LoginPage() {
  const { login, user, sessionExpired } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    return <Navigate to={HOME_BY_ROLE[user.role]} replace />;
  }

  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const loggedIn = await login(email.trim(), password);
      const from = location.state?.from;
      navigate(from && from !== '/login' ? from : HOME_BY_ROLE[loggedIn.role], { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'Login failed.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Container className="auth-page">
      <div className="auth-card">
        <div className="text-center mb-4">
          <Brand />
          <h1 className="h4 fw-bold mt-3 mb-1">Welcome back</h1>
          <p className="text-muted small">Log in to continue to your dashboard.</p>
        </div>

        {sessionExpired && !error && (
          <div className="alert alert-warning py-2 small">Your session has ended. Please log in again.</div>
        )}
        {error && <div className="alert alert-danger py-2 small" role="alert">{error}</div>}

        <Form onSubmit={submit}>
          <Form.Group className="mb-3" controlId="email">
            <Form.Label>Email address</Form.Label>
            <Form.Control type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </Form.Group>
          <Form.Group className="mb-4" controlId="password">
            <Form.Label>Password</Form.Label>
            <Form.Control type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
          </Form.Group>
          <Button type="submit" className="w-100" size="lg" disabled={submitting}>
            {submitting && <Spinner size="sm" className="me-2" />}Log in
          </Button>
        </Form>

        <p className="text-center small text-muted mt-4 mb-0">
          New to EduLink? <Link to="/register">Create an account</Link>
        </p>

        <div className="demo-accounts">
          <div className="small text-muted text-center mb-2">Demo accounts — click to fill in</div>
          <div className="d-flex gap-2">
            {DEMO_ACCOUNTS.map((a) => (
              <button key={a.label} type="button" className="btn btn-light btn-sm flex-fill"
                      onClick={() => { setEmail(a.email); setPassword(a.password); }}>
                <i className={`bi bi-${a.icon} d-block`} aria-hidden="true" />{a.label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </Container>
  );
}
