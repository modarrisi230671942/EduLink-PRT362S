import { Pagination as BsPagination, Spinner } from 'react-bootstrap';
import { matchTone, statusInfo } from '../utils/format.js';

/** Small presentational components shared by many pages. */

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header">
      <div>
        <h1 className="page-title">{title}</h1>
        {subtitle && <p className="page-subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="d-flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function Loading({ label = 'Loading…' }) {
  return (
    <div className="text-center text-muted py-5" role="status">
      <Spinner animation="border" size="sm" className="me-2" />
      {label}
    </div>
  );
}

export function EmptyState({ icon = 'inbox', title, children, action }) {
  return (
    <div className="empty-state">
      <i className={`bi bi-${icon}`} aria-hidden="true" />
      <h2 className="h6 fw-semibold mt-2 mb-1">{title}</h2>
      {children && <p className="text-muted small mb-3">{children}</p>}
      {action}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="alert alert-danger d-flex align-items-center justify-content-between gap-3">
      <span><i className="bi bi-exclamation-triangle me-2" aria-hidden="true" />{message}</span>
      {onRetry && <button type="button" className="btn btn-sm btn-outline-danger" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function StatusBadge({ status }) {
  const info = statusInfo(status);
  return (
    <span className={`status-badge status-${info.variant}`}>
      <i className={`bi bi-${info.icon}`} aria-hidden="true" />
      {info.label}
    </span>
  );
}

/** Colour-coded skill-match percentage. */
export function MatchBadge({ match, size = 'md' }) {
  if (!match) return null;
  return (
    <span className={`match-badge match-${matchTone(match.score)} match-${size}`} title={`${match.score}% of the requirements match your skills`}>
      <i className="bi bi-stars" aria-hidden="true" />
      {match.score}% match
    </span>
  );
}

/** Lists which requirements are matched and which are missing. */
export function MatchBreakdown({ match, perspective = 'student' }) {
  if (!match) return null;
  const missingLabel = perspective === 'student' ? 'Skills to develop' : 'Missing skills';
  return (
    <div className="match-breakdown">
      <div className="d-flex align-items-center gap-3 mb-2">
        <div className={`match-ring match-${matchTone(match.score)}`} style={{ '--score': match.score }}>
          <span>{match.score}%</span>
        </div>
        <div className="small text-muted">
          {perspective === 'student'
            ? 'How well your profile skills cover this job’s requirements.'
            : 'How well the applicant’s skills cover this job’s requirements.'}
        </div>
      </div>
      {match.matched.length > 0 && (
        <div className="mb-2">
          <div className="small fw-semibold mb-1">Matched</div>
          {match.matched.map((s) => <span key={s} className="skill-tag skill-matched"><i className="bi bi-check2" aria-hidden="true" />{s}</span>)}
        </div>
      )}
      {match.missing.length > 0 && (
        <div>
          <div className="small fw-semibold mb-1">{missingLabel}</div>
          {match.missing.map((s) => <span key={s} className="skill-tag skill-missing">{s}</span>)}
        </div>
      )}
    </div>
  );
}

export function SkillTags({ skills }) {
  if (!skills?.length) return <span className="text-muted small">No skills listed</span>;
  return skills.map((s) => <span key={s} className="skill-tag">{s}</span>);
}

export function Pager({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  const pages = [...Array(totalPages).keys()];
  return (
    <BsPagination className="justify-content-center mt-4">
      <BsPagination.Prev disabled={page === 0} onClick={() => onChange(page - 1)} />
      {pages.map((p) => (
        <BsPagination.Item key={p} active={p === page} onClick={() => onChange(p)}>{p + 1}</BsPagination.Item>
      ))}
      <BsPagination.Next disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)} />
    </BsPagination>
  );
}

export function StatTile({ icon, label, value, tone = 'primary', hint }) {
  return (
    <div className={`stat-tile stat-${tone}`}>
      <div className="stat-icon"><i className={`bi bi-${icon}`} aria-hidden="true" /></div>
      <div>
        <div className="stat-value">{value ?? '—'}</div>
        <div className="stat-label">{label}</div>
        {hint && <div className="stat-hint">{hint}</div>}
      </div>
    </div>
  );
}
