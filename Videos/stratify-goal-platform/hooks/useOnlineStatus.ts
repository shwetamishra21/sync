// hooks/useOnlineStatus.ts
// Monitors navigator.onLine AND does a real HTTP heartbeat ping.
// navigator.onLine can lie (e.g. connected to wifi with no internet).

'use client';

import { useState, useEffect, useCallback } from 'react';

const HEARTBEAT_INTERVAL = 15_000; // 15 seconds
const HEARTBEAT_ENDPOINT = '/api/health'; // a tiny endpoint that just returns 200

export interface OnlineStatus {
  isOnline: boolean;
  /** Just came back online (true for one tick after reconnection) */
  justReconnected: boolean;
}

export function useOnlineStatus(): OnlineStatus {
  const [isOnline, setIsOnline] = useState<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  const [justReconnected, setJustReconnected] = useState(false);

  const checkHeartbeat = useCallback(async () => {
    try {
      const res = await fetch(HEARTBEAT_ENDPOINT, {
        method: 'HEAD',
        cache: 'no-store',
        signal: AbortSignal.timeout(4000),
      });
      const alive = res.ok;
      setIsOnline((prev) => {
        if (!prev && alive) {
          // Just came back
          setJustReconnected(true);
          setTimeout(() => setJustReconnected(false), 3000);
        }
        return alive;
      });
    } catch {
      setIsOnline((prev) => {
        if (prev) {
          // Just went offline
        }
        return false;
      });
    }
  }, []);

  useEffect(() => {
    const handleOnline = () => {
      checkHeartbeat(); // verify with real ping
    };
    const handleOffline = () => {
      setIsOnline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Periodic heartbeat
    const interval = setInterval(checkHeartbeat, HEARTBEAT_INTERVAL);

    // Initial check
    checkHeartbeat();

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      clearInterval(interval);
    };
  }, [checkHeartbeat]);

  return { isOnline, justReconnected };
}
