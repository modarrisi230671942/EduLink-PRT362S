import { useState } from 'react';
import { studentApi } from '../api/endpoints.js';
import { errorMessage } from '../api/client.js';
import { useToast } from '../context/ToastContext.jsx';

/**
 * Bookmark toggle for a job. Only rendered for students (job.saved is null for everyone else).
 * @param onChange called with (jobId, saved) after the server confirms
 */
export default function SaveJobButton({ job, onChange, withLabel = false }) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  if (job.saved === null || job.saved === undefined) return null;

  const toggle = async (event) => {
    event.stopPropagation();
    setBusy(true);
    try {
      if (job.saved) {
        await studentApi.unsaveJob(job.jobId);
      } else {
        await studentApi.saveJob(job.jobId);
        toast.success('Saved. Find it later under "Saved".');
      }
      onChange?.(job.jobId, !job.saved);
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const label = job.saved ? 'Remove from saved jobs' : 'Save job';
  return withLabel ? (
    <button type="button" className="btn btn-light" onClick={toggle} disabled={busy}>
      <i className={`bi bi-bookmark${job.saved ? '-fill text-primary' : ''} me-1`} aria-hidden="true" />
      {job.saved ? 'Saved' : 'Save'}
    </button>
  ) : (
    <button type="button" className={`save-btn ${job.saved ? 'saved' : ''}`} onClick={toggle} disabled={busy}
            aria-label={label} aria-pressed={job.saved} title={label}>
      <i className={`bi bi-bookmark${job.saved ? '-fill' : ''}`} aria-hidden="true" />
    </button>
  );
}
