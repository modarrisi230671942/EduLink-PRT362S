/** Display helpers shared across pages. Pure functions — unit tested in format.test.js. */

export const JOB_TYPES = [
  { value: 'GRADUATE', label: 'Graduate programme' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'FULL_TIME', label: 'Full-time' },
];

export const APPLICATION_STATUSES = [
  { value: 'PENDING', label: 'Pending', variant: 'warning', icon: 'hourglass-split' },
  { value: 'REVIEWED', label: 'Under review', variant: 'info', icon: 'eye' },
  { value: 'ACCEPTED', label: 'Accepted', variant: 'success', icon: 'check-circle' },
  { value: 'REJECTED', label: 'Unsuccessful', variant: 'danger', icon: 'x-circle' },
];

export function jobTypeLabel(value) {
  return JOB_TYPES.find((t) => t.value === value)?.label ?? value;
}

export function statusInfo(value) {
  return APPLICATION_STATUSES.find((s) => s.value === value) ?? { value, label: value, variant: 'secondary', icon: 'circle' };
}

export function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? '—'
    : date.toLocaleDateString('en-ZA', { day: 'numeric', month: 'short', year: 'numeric' });
}

/** e.g. "Mon, 5 Oct 2026, 10:00" */
export function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? '—'
    : date.toLocaleString('en-ZA', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

/** A Date as the value of an <input type="datetime-local"> ("2026-10-05T10:00"), in local time. */
export function toDateTimeInput(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** Saves a file returned by axios (responseType: 'blob') to the user's downloads. */
export function downloadFile(response, fileName) {
  const url = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

/** Whole days from today until a yyyy-mm-dd date (negative when past). */
export function daysUntil(isoDate, today = new Date()) {
  if (!isoDate) return null;
  const [y, m, d] = isoDate.split('-').map(Number);
  const target = Date.UTC(y, m - 1, d);
  const start = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  return Math.round((target - start) / 86_400_000);
}

export function deadlineLabel(isoDate, today = new Date()) {
  const days = daysUntil(isoDate, today);
  if (days === null) return 'No deadline';
  if (days < 0) return 'Closed';
  if (days === 0) return 'Closes today';
  if (days === 1) return 'Closes tomorrow';
  return `${days} days left`;
}

export function timeAgo(value, now = new Date()) {
  if (!value) return '';
  const seconds = Math.max(0, Math.round((now - new Date(value)) / 1000));
  if (seconds < 60) return 'just now';
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  const days = Math.round(hours / 24);
  if (days < 30) return `${days} d ago`;
  return formatDate(value);
}

/** Tone for a skill-match score: strong ≥ 70, partial ≥ 40, otherwise low. */
export function matchTone(score) {
  if (score >= 70) return 'strong';
  if (score >= 40) return 'partial';
  return 'low';
}

/** Only allow http(s) links, so user-entered URLs like "javascript:..." can never run code. */
export function safeUrl(url) {
  if (!url) return null;
  try {
    const parsed = new URL(url);
    return parsed.protocol === 'http:' || parsed.protocol === 'https:' ? parsed.href : null;
  } catch {
    return null;
  }
}

/** Opens a PDF returned by axios (responseType: 'blob') in a new browser tab. */
export function openPdf(response) {
  const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
  window.open(url, '_blank');
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Mirrors the backend password rule so users get instant feedback. */
export function passwordProblem(password) {
  if (!password || password.length < 8) return 'Use at least 8 characters';
  if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) return 'Include at least one letter and one number';
  return null;
}
