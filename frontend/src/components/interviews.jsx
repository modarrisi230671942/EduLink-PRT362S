import { useState } from 'react';
import { Button, Spinner } from 'react-bootstrap';
import { interviewsApi } from '../api/endpoints.js';
import { errorMessage } from '../api/client.js';
import { useConfirm } from '../context/ConfirmContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { downloadFile, formatDateTime, safeUrl } from '../utils/format.js';

/** Shared interview UI used by both students and companies. */

export const MODE_LABELS = { ONLINE: 'Online', IN_PERSON: 'In person', PHONE: 'Phone' };
const MODE_ICONS = { ONLINE: 'camera-video', IN_PERSON: 'geo-alt', PHONE: 'telephone' };

export function DateBadge({ value }) {
  const date = new Date(value);
  return (
    <div className="date-badge" aria-hidden="true">
      <div className="month">{date.toLocaleDateString('en-ZA', { month: 'short' })}</div>
      <div className="day">{date.getDate()}</div>
    </div>
  );
}

/** "Online · https://meet… " / "In person · 12 Long Street" — links only for safe http(s) URLs. */
export function InterviewWhere({ interview }) {
  const link = safeUrl(interview.location);
  return (
    <span>
      <i className={`bi bi-${MODE_ICONS[interview.mode]} me-1`} aria-hidden="true" />
      {MODE_LABELS[interview.mode]}
      {interview.location && (
        <>
          {' · '}
          {link ? <a href={link} target="_blank" rel="noopener noreferrer">Join meeting</a> : interview.location}
        </>
      )}
    </span>
  );
}

export function CalendarButton({ interview, size = 'sm' }) {
  const toast = useToast();
  const download = async () => {
    try {
      downloadFile(await interviewsApi.calendar(interview.interviewId), `edulink-interview-${interview.interviewId}.ics`);
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };
  return (
    <Button size={size} variant="outline-primary" onClick={download}>
      <i className="bi bi-calendar-plus me-1" aria-hidden="true" />Add to calendar
    </Button>
  );
}

export function CancelInterviewButton({ interview, onChanged, size = 'sm' }) {
  const toast = useToast();
  const confirm = useConfirm();
  const cancel = async () => {
    const ok = await confirm({
      title: 'Cancel this interview?',
      message: 'The other party will be notified immediately.',
      confirmLabel: 'Cancel interview',
      variant: 'danger',
    });
    if (!ok) return;
    try {
      onChanged(await interviewsApi.cancel(interview.interviewId));
      toast.success('Interview cancelled.');
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };
  return <Button size={size} variant="outline-danger" onClick={cancel}>Cancel</Button>;
}

/**
 * The student's view of an interview: pick a slot when invited, or see the confirmed time.
 * @param onChanged called with the updated interview
 */
export function StudentInterviewPanel({ interview, onChanged }) {
  const toast = useToast();
  const [busySlot, setBusySlot] = useState(null);

  if (!interview || interview.status === 'CANCELLED') {
    return interview ? (
      <div className="small text-muted mt-3"><i className="bi bi-calendar-x me-1" aria-hidden="true" />Interview cancelled.</div>
    ) : null;
  }

  const choose = async (slot) => {
    setBusySlot(slot.slotId);
    try {
      onChanged(await interviewsApi.confirm(interview.interviewId, slot.slotId));
      toast.success(`Interview confirmed for ${formatDateTime(slot.startsAt)}. Good luck!`);
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusySlot(null);
    }
  };

  if (interview.status === 'CONFIRMED') {
    return (
      <div className="interview-box confirmed mt-3">
        <div className="d-flex gap-3 align-items-center flex-wrap">
          <DateBadge value={interview.confirmedStart} />
          <div className="flex-grow-1">
            <div className="fw-semibold"><i className="bi bi-calendar-check text-success me-1" aria-hidden="true" />Interview confirmed</div>
            <div className="small">{formatDateTime(interview.confirmedStart)} · {interview.durationMinutes} min</div>
            <div className="small text-muted"><InterviewWhere interview={interview} /></div>
          </div>
          <div className="d-flex gap-2">
            <CalendarButton interview={interview} />
            <CancelInterviewButton interview={interview} onChanged={onChanged} />
          </div>
        </div>
        {interview.notes && <div className="small text-muted mt-2 text-pre-line">{interview.notes}</div>}
      </div>
    );
  }

  const now = new Date();
  return (
    <div className="interview-box mt-3">
      <div className="fw-semibold mb-1"><i className="bi bi-calendar-plus me-1 text-primary" aria-hidden="true" />You&apos;re invited to an interview</div>
      <div className="small text-muted mb-2">
        <InterviewWhere interview={interview} /> · {interview.durationMinutes} min — choose the time that suits you:
      </div>
      {interview.slots.map((slot) => {
        const past = new Date(slot.startsAt) <= now;
        return (
          <div key={slot.slotId} className="slot-option">
            <span className={past ? 'text-muted text-decoration-line-through' : 'fw-semibold'}>{formatDateTime(slot.startsAt)}</span>
            <Button size="sm" disabled={past || busySlot !== null} onClick={() => choose(slot)}>
              {busySlot === slot.slotId && <Spinner size="sm" className="me-1" />}Accept this time
            </Button>
          </div>
        );
      })}
      {interview.notes && <div className="small text-muted mt-2 text-pre-line">{interview.notes}</div>}
    </div>
  );
}
