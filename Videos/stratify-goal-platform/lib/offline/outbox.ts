// lib/offline/outbox.ts
// The outbox service — enqueues mutations, drains them when online.
// "Every write goes to IndexedDB first, then to the server."

import { getLocalDb, getDeviceId, type OutboxItem } from '@/lib/db/localDb';

// ─── Enqueue ─────────────────────────────────────────────────────────────────

/**
 * Add a mutation to the offline outbox.
 * Call this instead of fetch() for all write operations.
 * Returns the auto-generated outbox ID.
 */
export async function enqueueMutation(
  endpoint: string,
  method: OutboxItem['method'],
  payload: Record<string, unknown>
): Promise<number> {
  const db = getLocalDb();

  const item: OutboxItem = {
    status: 'pending',
    endpoint,
    method,
    payload,
    clientTimestamp: new Date().toISOString(),
    createdAt: Date.now(),
    retryCount: 0,
    deviceId: getDeviceId(),
  };

  const id = await db.outbox.add(item);
  return id as number;
}

// ─── Pending count ────────────────────────────────────────────────────────────

/** How many items are queued (pending or failed — not yet resolved) */
export async function getPendingCount(): Promise<number> {
  const db = getLocalDb();
  return db.outbox
    .where('status')
    .anyOf(['pending', 'syncing', 'failed'])
    .count();
}

// ─── Drain outbox ─────────────────────────────────────────────────────────────

export type DrainResult = {
  synced: number;
  failed: number;
  conflicts: number;
};

/**
 * Process every pending outbox item in chronological order.
 * Called when the user comes back online.
 *
 * Each item is replayed to the server. On success it is marked 'synced'.
 * On 409 it is marked 'conflict'. On other errors: retry up to 3 times.
 */
export async function drainOutbox(
  onProgress?: (synced: number, total: number) => void
): Promise<DrainResult> {
  const db = getLocalDb();
  const result: DrainResult = { synced: 0, failed: 0, conflicts: 0 };

  // Fetch pending items ordered by creation time
  const items = await db.outbox
    .where('status')
    .anyOf(['pending', 'failed'])
    .sortBy('createdAt');

  const total = items.length;
  if (total === 0) return result;

  for (let i = 0; i < items.length; i++) {
    const item = items[i];
    if (!item.id) continue;

    // Mark as syncing
    await db.outbox.update(item.id, { status: 'syncing' });

    try {
      const response = await fetch(item.endpoint, {
        method: item.method,
        headers: {
          'Content-Type': 'application/json',
          // Offline provenance header — server uses this for audit trail
          'X-Offline-Sync': 'true',
          'X-Client-Timestamp': item.clientTimestamp,
          'X-Device-Id': item.deviceId,
        },
        body: JSON.stringify({
          ...item.payload,
          _syncMeta: {
            isOfflineWrite: true,
            clientTimestamp: item.clientTimestamp,
            deviceId: item.deviceId,
            outboxId: item.id,
          },
        }),
      });

      if (response.status === 409) {
        // Conflict — goal state changed server-side while offline
        const errorText = await response.text();
        await db.outbox.update(item.id, {
          status: 'conflict',
          errorMessage: errorText,
        });
        result.conflicts++;
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('stratify:sync:conflict', {
            detail: { item, serverResponse: errorText }
          }));
        }
      } else if (response.ok) {
        await db.outbox.update(item.id, { status: 'synced' });
        result.synced++;
      } else {
        // Non-409 server error
        const retry = (item.retryCount ?? 0) + 1;
        await db.outbox.update(item.id, {
          status: retry >= 3 ? 'failed' : 'pending',
          retryCount: retry,
          errorMessage: `HTTP ${response.status}`,
        });
        if (retry >= 3) result.failed++;
      }
    } catch {
      // Network error during drain — go back to pending, try next time
      await db.outbox.update(item.id, {
        status: 'pending',
        retryCount: (item.retryCount ?? 0) + 1,
      });
    }

    onProgress?.(i + 1, total);
  }

  // Clean up old synced records (keep last 100 for debugging)
  const syncedIds = await db.outbox
    .where('status')
    .equals('synced')
    .sortBy('createdAt');
  if (syncedIds.length > 100) {
    const toDelete = syncedIds.slice(0, syncedIds.length - 100).map((r) => r.id!);
    await db.outbox.bulkDelete(toDelete);
  }

  return result;
}

// ─── Get conflicts ─────────────────────────────────────────────────────────────

export async function getConflicts(): Promise<OutboxItem[]> {
  const db = getLocalDb();
  return db.outbox.where('status').equals('conflict').toArray();
}

/** Dismiss a conflict by marking it 'failed' (user acknowledged) */
export async function dismissConflict(id: number): Promise<void> {
  const db = getLocalDb();
  await db.outbox.update(id, { status: 'failed' });
}
