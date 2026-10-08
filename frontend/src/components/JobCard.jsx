import { deadlineLabel, daysUntil, jobTypeLabel } from '../utils/format.js';
import SaveJobButton from './SaveJobButton.jsx';
import { MatchBadge } from './ui.jsx';

/**
 * Summary card for a job in search results and recommendations.
 * @param onSavedChange called with (jobId, saved) when a student bookmarks/unbookmarks it
 */
export default function JobCard({ job, onOpen, onSavedChange }) {
  const days = daysUntil(job.applicationDeadline);
  const closingSoon = days !== null && days >= 0 && days <= 7;

  return (
    <article className="job-card" onClick={() => onOpen(job)}>
      <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
        <span className="company-avatar" aria-hidden="true">{job.companyName?.[0] ?? '?'}</span>
        <div className="d-flex flex-column align-items-end gap-1">
          <div className="d-flex align-items-center gap-1">
            <span className="type-pill">{jobTypeLabel(job.jobType)}</span>
            <SaveJobButton job={job} onChange={onSavedChange} />
          </div>
          {job.applied && <span className="applied-pill"><i className="bi bi-check2" aria-hidden="true" /> Applied</span>}
        </div>
      </div>
      <h3 className="job-title">{job.title}</h3>
      <div className="job-company">{job.companyName}</div>
      <p className="job-description">{job.description}</p>
      <div className="d-flex flex-wrap gap-1 mb-3">
        {job.requirementList.slice(0, 4).map((r) => <span key={r} className="skill-tag">{r}</span>)}
        {job.requirementList.length > 4 && <span className="skill-tag skill-more">+{job.requirementList.length - 4}</span>}
      </div>
      <div className="job-footer">
        <span><i className="bi bi-geo-alt" aria-hidden="true" /> {job.location}</span>
        <span className={closingSoon ? 'text-danger fw-semibold' : ''}>
          <i className="bi bi-clock" aria-hidden="true" /> {deadlineLabel(job.applicationDeadline)}
        </span>
      </div>
      {job.match && (
        <div className="mt-3">
          <MatchBadge match={job.match} />
        </div>
      )}
      <button type="button" className="stretched-link-btn" aria-label={`View ${job.title} at ${job.companyName}`} onClick={(e) => { e.stopPropagation(); onOpen(job); }} />
    </article>
  );
}
