import { Client } from '@stomp/stompjs';
import { useEffect, useRef } from 'react';

/** Browser event fired for every notification pushed by the server (detail = the notification). */
export const LIVE_EVENT = 'edulink:live-notification';

/**
 * Opens a WebSocket (STOMP) connection to the backend and calls {@code onNotification} for every
 * notification pushed to this user. Reconnects automatically. Returns a function that disconnects.
 */
export function connectLive(token, onNotification, onStatus = () => {}) {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const client = new Client({
    brokerURL: `${protocol}://${window.location.host}/ws`,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    onConnect: () => {
      onStatus(true);
      client.subscribe('/user/queue/notifications', (message) => {
        try {
          onNotification(JSON.parse(message.body));
        } catch {
          // ignore malformed messages
        }
      });
    },
    onWebSocketClose: () => onStatus(false),
    onStompError: () => onStatus(false),
  });
  client.activate();
  return () => client.deactivate();
}

/**
 * Re-runs {@code refresh} whenever a live notification of one of the given types arrives,
 * so pages update the moment something changes — e.g. a student's application list refreshes
 * as soon as the company accepts them.
 */
export function useLiveRefresh(refresh, types) {
  const refreshRef = useRef(refresh);
  refreshRef.current = refresh;
  const typeKey = types.join(',');

  useEffect(() => {
    const wanted = new Set(typeKey.split(','));
    const handler = (event) => {
      if (wanted.has(event.detail?.type)) refreshRef.current();
    };
    window.addEventListener(LIVE_EVENT, handler);
    return () => window.removeEventListener(LIVE_EVENT, handler);
  }, [typeKey]);
}
