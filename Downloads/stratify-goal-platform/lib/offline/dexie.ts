// lib/dexie.ts
'use client'

import Dexie, { type Table } from 'dexie'
import type { OutboxItem, GoalCacheItem } from '@/types'

export class StratifyDB extends Dexie {
  outbox!: Table<OutboxItem, number>
  goalsCache!: Table<GoalCacheItem, string>

  constructor() {
    super('StratifyDB')

    this.version(1).stores({
      outbox: '++id, status, endpoint, createdAt',
      goalsCache: 'goalId, userId, updatedAt',
    })
  }
}

// Singleton - safe to import in client components
let _db: StratifyDB | null = null

export function getDB(): StratifyDB {
  if (!_db) {
    _db = new StratifyDB()
  }

  return _db
}

// Outbox helpers
export async function addToOutbox(
  item: Omit<OutboxItem, 'id'>
): Promise<number> {
  return getDB().outbox.add(item)
}

export async function getPendingOutbox(): Promise<OutboxItem[]> {
  return getDB()
    .outbox
    .where('status')
    .equals('pending')
    .toArray()
}

export async function markOutboxSynced(id: number): Promise<void> {
  await getDB().outbox.delete(id)
}

export async function markOutboxFailed(
  id: number,
  errorMessage: string
): Promise<void> {
  const db = getDB()

  const existing = await db.outbox.get(id)

  await db.outbox.update(id, {
    status: 'failed',
    errorMessage,
    retries: (existing?.retries ?? 0) + 1,
  })
}

export async function getPendingCount(): Promise<number> {
  return getDB()
    .outbox
    .where('status')
    .equals('pending')
    .count()
}

// Goals cache helpers
export async function cacheGoals(
  goals: GoalCacheItem[]
): Promise<void> {
  await getDB().goalsCache.bulkPut(goals)
}

export async function getCachedGoals(
  userId: string
): Promise<GoalCacheItem[]> {
  return getDB()
    .goalsCache
    .where('userId')
    .equals(userId)
    .toArray()
}