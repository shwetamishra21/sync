// lib/syncEngine.ts
'use client'

import {
  getPendingOutbox,
  markOutboxSynced,
  markOutboxFailed,
} from './dexie'

let isDraining = false

export async function drainOutbox(): Promise<{
  synced: number
  failed: number
  conflicts: number
}> {
  if (isDraining) return { synced: 0, failed: 0, conflicts: 0 }
  isDraining = true

  window.dispatchEvent(new Event('stratify:sync:start'))

  const results = { synced: 0, failed: 0, conflicts: 0 }

  try {
    const pending = await getPendingOutbox()
    if (pending.length === 0) {
      window.dispatchEvent(new Event('stratify:sync:end'))
      isDraining = false
      return results
    }

    for (const item of pending) {
      try {
        const response = await fetch(item.endpoint, {
          method: item.method,
          headers: { 'Content-Type': 'application/json' },
          body: item.body,
        })

        if (response.ok) {
          await markOutboxSynced(item.id!)
          results.synced++
        } else if (response.status === 409) {
          // Conflict: server state changed while offline
          await markOutboxFailed(item.id!, 'Conflict: server state changed')
          results.conflicts++
          // Dispatch conflict alert event
          window.dispatchEvent(
            new CustomEvent('stratify:sync:conflict', {
              detail: { item, status: response.status },
            })
          )
        } else {
          await markOutboxFailed(item.id!, `HTTP ${response.status}`)
          results.failed++
        }
      } catch (networkError) {
        // Still offline or network error - stop trying
        console.warn('Sync network error, stopping drain:', networkError)
        break
      }
    }
  } finally {
    isDraining = false
    window.dispatchEvent(new Event('stratify:sync:end'))
  }

  return results
}

// Call this on app mount and on online events
export function initSyncEngine(): () => void {
  const handleOnline = () => {
    console.log('[Stratify] Online - draining outbox...')
    drainOutbox()
  }

  window.addEventListener('online', handleOnline)

  // Also drain immediately if online at mount
  if (navigator.onLine) {
    drainOutbox()
  }

  return () => window.removeEventListener('online', handleOnline)
}