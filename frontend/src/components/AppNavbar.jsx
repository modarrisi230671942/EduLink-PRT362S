import { useState } from 'react';
import { Container, Dropdown, Nav, Navbar } from 'react-bootstrap';
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useTheme } from '../context/ThemeContext.jsx';
import NotificationBell from './NotificationBell.jsx';
import ChangePasswordModal from './ChangePasswordModal.jsx';

const LINKS = {
  STUDENT: [
    { to: '/student', label: 'Dashboard', icon: 'grid', end: true },
    { to: '/jobs', label: 'Find jobs', icon: 'search' },
    { to: '/student/saved', label: 'Saved', icon: 'bookmark' },
    { to: '/student/applications', label: 'Applications', icon: 'journal-check' },
    { to: '/student/profile', label: 'Profile & CV', icon: 'person-badge' },
  ],
  COMPANY: [
    { to: '/company/jobs', label: 'Vacancies', icon: 'briefcase' },
    { to: '/company/applications', label: 'Applicants', icon: 'people' },
    { to: '/company/interviews', label: 'Interviews', icon: 'calendar-event' },
    { to: '/company/profile', label: 'Profile', icon: 'building' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Analytics', icon: 'bar-chart', end: true },
    { to: '/admin/companies', label: 'Companies', icon: 'patch-check' },
    { to: '/admin/users', label: 'Users', icon: 'people' },
    { to: '/admin/activity', label: 'Activity log', icon: 'shield-check' },
  ],
  GUEST: [{ to: '/jobs', label: 'Browse jobs', icon: 'search' }],
};

export function Brand() {
  return (
    <span className="brand">
      <span className="brand-mark" aria-hidden="true">E</span>
      Edu<span className="brand-accent">Link</span>
    </span>
  );
}

export default function AppNavbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const { theme, toggle: toggleTheme } = useTheme();
  const [expanded, setExpanded] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const links = LINKS[user?.role ?? 'GUEST'];

  return (
    <>
      <Navbar expand="lg" sticky="top" className="navbar-edulink" expanded={expanded} onToggle={setExpanded}>
        <Container>
          <Navbar.Brand as={Link} to="/" onClick={() => setExpanded(false)}>
            <Brand />
          </Navbar.Brand>

          <div className="d-flex align-items-center order-lg-last ms-auto ms-lg-2 gap-1">
            <button type="button" className="btn icon-toggle" onClick={toggleTheme}
                    aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
                    title={theme === 'dark' ? 'Light mode' : 'Dark mode'}>
              <i className={`bi bi-${theme === 'dark' ? 'sun' : 'moon-stars'}`} aria-hidden="true" />
            </button>
            {isAuthenticated && <NotificationBell />}
          </div>

          <Navbar.Toggle aria-controls="main-nav" className="ms-2" />
          <Navbar.Collapse id="main-nav">
            <Nav className="me-auto ms-lg-4 gap-lg-1">
              {links.map((link) => (
                <Nav.Link key={link.to} as={NavLink} to={link.to} end={link.end} onClick={() => setExpanded(false)}>
                  <i className={`bi bi-${link.icon} me-1`} aria-hidden="true" />
                  {link.label}
                </Nav.Link>
              ))}
            </Nav>

            {isAuthenticated ? (
              <Dropdown align="end">
                <Dropdown.Toggle variant="light" className="user-toggle" id="user-menu">
                  <span className="avatar" aria-hidden="true">{user.displayName?.[0]?.toUpperCase() ?? '?'}</span>
                  <span className="d-none d-xl-inline text-truncate user-name">{user.displayName}</span>
                </Dropdown.Toggle>
                <Dropdown.Menu className="shadow-sm">
                  <Dropdown.Header>
                    <div className="fw-semibold text-body">{user.displayName}</div>
                    <div className="small">{user.email}</div>
                  </Dropdown.Header>
                  <Dropdown.Divider />
                  <Dropdown.Item onClick={() => setShowPassword(true)}>
                    <i className="bi bi-shield-lock me-2" aria-hidden="true" />Change password
                  </Dropdown.Item>
                  <Dropdown.Item onClick={logout} className="text-danger">
                    <i className="bi bi-box-arrow-right me-2" aria-hidden="true" />Log out
                  </Dropdown.Item>
                </Dropdown.Menu>
              </Dropdown>
            ) : (
              <div className="d-flex gap-2 mt-2 mt-lg-0">
                <Link to="/login" className="btn btn-light" onClick={() => setExpanded(false)}>Log in</Link>
                <Link to="/register" className="btn btn-primary" onClick={() => setExpanded(false)}>Get started</Link>
              </div>
            )}
          </Navbar.Collapse>
        </Container>
      </Navbar>
      <ChangePasswordModal show={showPassword} onHide={() => setShowPassword(false)} />
    </>
  );
}
