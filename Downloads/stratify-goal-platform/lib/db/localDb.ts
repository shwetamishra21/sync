// lib/db/localDb.ts
// Layer 1 — IndexedDB via Dexie.js
// This is the browser-side persistent store for offline support.
// It survives page refresh and browser close.

import Dexie, { type Table } from 'dexie';

// ─── Types ───────────────────────────────────────────────────────────────────

export type OutboxStatus = 'pending' | 'syncing' | 'synced' | 'failed' | 'conflict';

export interface OutboxItem {
  id?: number;            // auto-incremented PK
  status: OutboxStatus;
  endpoint: string;       // e.g. '/api/goals/check-in'
  method: 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  payload: Record<string, unknown>;
  clientTimestamp: string; // ISO string — real time of the action
  createdAt: number;       // Date.now() for ordering
  retryCount: number;
  errorMessage?: string;
  deviceId: string;
}

export interface CachedGoal {
  goalId: string;          // server _id
  userId: string;
  quarterId: string;
  title: string;
  description?: string;
  weightage: number;
  status: string;
  targetValue?: number;
  currentValue?: number;
  achievementScore?: number;
  riskScore?: number;
  updatedAt: number;       // timestamp for cache invalidation
  checkIns?: CachedCheckIn[];
}

export interface CachedCheckIn {
  id?: number;
  goalId: string;
  progressValue: number;
  progressPercent: number;
  notes?: string;
  submittedAt: string;     // ISO
  isOfflinePending?: boolean;
}

export interface CachedNotification {
  notifId: string;
  recipientId: string;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: number;
}

// ─── Database class ───────────────────────────────────────────────────────────

export class GoalTrackerDB extends Dexie {
  outbox!: Table<OutboxItem, number>;
  goalsCache!: Table<CachedGoal, string>;
  checkInsCache!: Table<CachedCheckIn, number>;
  notificationsCache!: Table<CachedNotification, string>;

  constructor() {
    super('GoalTrackerDB');

    this.version(1).stores({
      // outbox — queued mutations
      outbox: '++id, status, endpoint, createdAt',

      // goals read cache
      goalsCache: 'goalId, userId, quarterId, updatedAt',

      // check-ins cache (includes optimistic offline ones)
      checkInsCache: '++id, goalId, submittedAt',

      // notifications read cache
      notificationsCache: 'notifId, recipientId, isRead',
    });
  }
}

// ─── Singleton ────────────────────────────────────────────────────────────────

// Guard for SSR — Dexie must only run in browser
let _db: GoalTrackerDB | null = null;

export function getLocalDb(): GoalTrackerDB {
  if (typeof window === 'undefined') {
    throw new Error('getLocalDb() must only be called in the browser');
  }
  if (!_db) {
    _db = new GoalTrackerDB();
  }
  return _db;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Generate a stable device ID, stored in localStorage */
export function getDeviceId(): string {
  if (typeof window === 'undefined') return 'ssr';
  const key = 'goal_tracker_device_id';
  let id = localStorage.getItem(key);
  if (!id) {
    id = `dev_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
    localStorage.setItem(key, id);
  }
  return id;
}
