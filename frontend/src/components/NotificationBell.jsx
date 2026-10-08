import { useCallback, useEffect, useState } from 'react';
import { Dropdown } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { readStoredAuth } from '../api/client.js';
import { notificationsApi } from '../api/endpoints.js';
import { connectLive, LIVE_EVENT } from '../api/live.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { timeAgo } from '../utils/format.js';

/** Safety net in case the live connection is down (e.g. a proxy blocks WebSockets). */
const FALLBACK_POLL_MS = 60_000;

const ICONS = {
  APPLICATION_RECEIVED: 'envelope-plus',
  APPLICATION_STATUS_CHANGED: 'arrow-repeat',
  APPLICATION_WITHDRAWN: 'envelope-dash',
  COMPANY_VERIFIED: 'patch-check',
  COMPANY_VERIFICATION_REVOKED: 'patch-exclamation',
  ACCOUNT_ENABLED: 'person-check',
  INTERVIEW_PROPOSED: 'calendar-plus',
  INTERVIEW_CONFIRMED: 'calendar-check',
  INTERVIEW_CANCELLED: 'calendar-x',
  JOB_MATCH: 'stars',
};

/**
 * Bell icon with an unread badge. New notifications arrive instantly over WebSocket;
 * each one also shows a toast and is re-broadcast to the page (see useLiveRefresh).
 */
export default function NotificationBell() {
  const { refreshUser } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [data, setData] = useState({ unreadCount: 0, items: [] });
  const [live, setLive] = useState(false);

  const load = useCallback(async () => {
    try {
      setData(await notificationsApi.list());
    } catch {
      // Ignore: the bell keeps its last state if the server is briefly unreachable
    }
  }, []);

  useEffect(() => {
    load();
    const timer = setInterval(load, FALLBACK_POLL_MS);
    return () => clearInterval(timer);
  }, [load]);

  // Live connection
  useEffect(() => {
    const token = readStoredAuth()?.token;
    if (!token) return undefined;
    return connectLive(token, (notification) => {
      setData((current) => ({
        unreadCount: current.unreadCount + 1,
        items: [notification, ...current.items.filter((n) => n.notificationId !== notification.notificationId)].slice(0, 20),
      }));
      toast.info(notification.message);
      if (notification.type.startsWith('COMPANY_VERIFI') || notification.type === 'COMPANY_VERIFIED') {
        refreshUser().catch(() => {});
      }
      window.dispatchEvent(new CustomEvent(LIVE_EVENT, { detail: notification }));
    }, setLive);
  }, [toast, refreshUser]);

  const open = async (notification) => {
    if (!notification.read) {
      await notificationsApi.markRead(notification.notificationId).catch(() => {});
    }
    load();
    if (notification.link) navigate(notification.link);
  };

  const markAll = async () => {
    await notificationsApi.markAllRead().catch(() => {});
    load();
  };

  return (
    <Dropdown align="end" onToggle={(isOpen) => isOpen && load()}>
      <Dropdown.Toggle variant="light" className="bell-toggle" aria-label={`Notifications, ${data.unreadCount} unread`}>
        <i className="bi bi-bell" aria-hidden="true" />
        {data.unreadCount > 0 && <span className="bell-badge">{data.unreadCount > 9 ? '9+' : data.unreadCount}</span>}
      </Dropdown.Toggle>
      <Dropdown.Menu className="notification-menu shadow">
        <div className="d-flex justify-content-between align-items-center px-3 py-2">
          <span className="fw-semibold">
            Notifications
            <span className={`live-dot ${live ? 'on' : ''}`} title={live ? 'Live updates connected' : 'Reconnecting…'} />
          </span>
          {data.unreadCount > 0 && (
            <button type="button" className="btn btn-link btn-sm p-0 text-decoration-none" onClick={markAll}>
              Mark all read
            </button>
          )}
        </div>
        <Dropdown.Divider className="my-0" />
        {data.items.length === 0 ? (
          <div className="text-center text-muted small py-4 px-3">
            <i className="bi bi-bell-slash d-block fs-4 mb-1" aria-hidden="true" />
            You&apos;re all caught up.
          </div>
        ) : (
          data.items.map((n) => (
            <Dropdown.Item key={n.notificationId} onClick={() => open(n)} className={`notification-item ${n.read ? '' : 'unread'}`}>
              <i className={`bi bi-${ICONS[n.type] ?? 'bell'} notification-icon`} aria-hidden="true" />
              <div className="flex-grow-1 min-w-0">
                <div className="notification-text">{n.message}</div>
                <div className="small text-muted">{timeAgo(n.createdAt)}</div>
              </div>
              {!n.read && <span className="unread-dot" aria-label="unread" />}
            </Dropdown.Item>
          ))
        )}
      </Dropdown.Menu>
    </Dropdown>
  );
}
