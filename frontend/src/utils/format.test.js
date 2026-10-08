import { describe, expect, it } from 'vitest';
import { daysUntil, deadlineLabel, jobTypeLabel, matchTone, passwordProblem, safeUrl, statusInfo, toDateTimeInput } from './format.js';

describe('safeUrl', () => {
  it('allows http and https links', () => {
    expect(safeUrl('https://techcorp.example.com')).toBe('https://techcorp.example.com/');
    expect(safeUrl('http://example.com/jobs')).toBe('http://example.com/jobs');
  });

  it('blocks javascript: and other dangerous or invalid URLs (XSS protection)', () => {
    expect(safeUrl('javascript:alert(1)')).toBeNull();
    expect(safeUrl('data:text/html,<script>alert(1)</script>')).toBeNull();
    expect(safeUrl('not a url')).toBeNull();
    expect(safeUrl(null)).toBeNull();
  });
});

describe('deadlines', () => {
  const today = new Date(2026, 8, 28); // 28 Sep 2026

  it('counts days until a deadline', () => {
    expect(daysUntil('2026-10-08', today)).toBe(10);
    expect(daysUntil('2026-09-27', today)).toBe(-1);
    expect(daysUntil(null, today)).toBeNull();
  });

  it('describes a deadline in words', () => {
    expect(deadlineLabel('2026-09-28', today)).toBe('Closes today');
    expect(deadlineLabel('2026-09-29', today)).toBe('Closes tomorrow');
    expect(deadlineLabel('2026-10-05', today)).toBe('7 days left');
    expect(deadlineLabel('2026-09-01', today)).toBe('Closed');
  });
});

describe('labels', () => {
  it('maps enum values to readable labels', () => {
    expect(jobTypeLabel('FULL_TIME')).toBe('Full-time');
    expect(statusInfo('REJECTED').label).toBe('Unsuccessful');
    expect(statusInfo('ACCEPTED').variant).toBe('success');
  });

  it('classifies match scores', () => {
    expect(matchTone(100)).toBe('strong');
    expect(matchTone(50)).toBe('partial');
    expect(matchTone(10)).toBe('low');
  });
});

describe('toDateTimeInput', () => {
  it('formats a local date for a datetime-local input', () => {
    expect(toDateTimeInput(new Date(2026, 9, 5, 9, 7))).toBe('2026-10-05T09:07');
  });
});

describe('passwordProblem', () => {
  it('matches the backend password policy', () => {
    expect(passwordProblem('short1')).toMatch(/8 characters/);
    expect(passwordProblem('onlyletters')).toMatch(/letter and one number/);
    expect(passwordProblem('password123')).toBeNull();
  });
});
