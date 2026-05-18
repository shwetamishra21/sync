// hooks/useSyncStatus.ts
// Exposes outbox pending count, syncing state, and conflict alerts to the UI.
// Drives the header sync badge and offline banner.

'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { useOnlineStatus } from './useOnlineStatus';
import { getPendingCount, drainOutbox, getConflicts, type DrainResult } from '@/lib/offline/outbox';
import type { OutboxItem } from '@/lib/db/localDb';

export interface SyncStatus {
  pendingCount: number;
  isSyncing: boolean;
  lastSyncResult: DrainResult | null;
  conflicts: OutboxItem[];
  /** Call this to manually trigger a sync drain */
  triggerSync: () => Promise<void>;
  /** Call to refresh counts without draining */
  refreshCounts: () => Promise<void>;
}

export function useSyncStatus(): SyncStatus {
  const { isOnline, justReconnected } = useOnlineStatus();
  const [pendingCount, setPendingCount] = useState(0);
  const [isSyncing, setIsSyncing] = useState(false);
  const [lastSyncResult, setLastSyncResult] = useState<DrainResult | null>(null);
  const [conflicts, setConflicts] = useState<OutboxItem[]>([]);
  const syncingRef = useRef(false);

  const refreshCounts = useCallback(async () => {
    if (typeof window === 'undefined') return;
    try {
      const [count, conflictItems] = await Promise.all([
        getPendingCount(),
        getConflicts(),
      ]);
      setPendingCount(count);
      setConflicts(conflictItems);
    } catch {
      // IndexedDB may not be ready yet
    }
  }, []);

  const triggerSync = useCallback(async () => {
    if (syncingRef.current || !isOnline) return;
    syncingRef.current = true;
    setIsSyncing(true);

    try {
      const result = await drainOutbox();
      setLastSyncResult(result);
      await refreshCounts();
    } finally {
      syncingRef.current = false;
      setIsSyncing(false);
    }
  }, [isOnline, refreshCounts]);

  // Auto-drain when coming back online
  useEffect(() => {
    if (justReconnected && isOnline) {
      triggerSync();
    }
  }, [justReconnected, isOnline, triggerSync]);

  // Poll pending count every 5s so badge stays up to date
  useEffect(() => {
    refreshCounts();
    const interval = setInterval(refreshCounts, 5000);
    return () => clearInterval(interval);
  }, [refreshCounts]);

  return {
    pendingCount,
    isSyncing,
    lastSyncResult,
    conflicts,
    triggerSync,
    refreshCounts,
  };
}
