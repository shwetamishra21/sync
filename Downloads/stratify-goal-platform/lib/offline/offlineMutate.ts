// lib/offlineMutate.ts
'use client'

import { addToOutbox } from './dexie'

interface MutateOptions {
  endpoint: string
  method?: string
  body: Record<string, unknown>
}

interface MutateResult {
  data?: unknown
  offline?: boolean
  outboxId?: number
}

/**
 * Tries the network first. If offline, queues in IndexedDB outbox.
 * Returns { data, offline: false } on success.
 * Returns { offline: true, outboxId } when queued.
 */
export async function mutateOfflineAware(options: MutateOptions): Promise<MutateResult> {
  const { endpoint, method = 'POST', body } = options

  const bodyStr = JSON.stringify({
    ...body,
    _syncMeta: {
      isOfflineWrite: !navigator.onLine,
      clientTimestamp: new Date().toISOString(),
      deviceId: getDeviceId(),
    },
  })

  // Try online first
  if (navigator.onLine) {
    try {
      const res = await fetch(endpoint, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: bodyStr,
      })

      if (res.ok) {
        const data = await res.json()
        return { data, offline: false }
      }

      // Server error - don't queue, propagate error
      const err = await res.json().catch(() => ({})) as { error?: string }
      throw new Error(err.error ?? `Request failed: ${res.status}`)
    } catch (networkError) {
      // Could be navigator.onLine lying (happens) - fall through to queue
      if (networkError instanceof TypeError && networkError.message.includes('fetch')) {
        // Genuine network failure - queue it
        const outboxId = await queueToOutbox(endpoint, method, bodyStr)
        return { offline: true, outboxId }
      }
      throw networkError
    }
  }

  // Definitely offline - queue
  const outboxId = await queueToOutbox(endpoint, method, bodyStr)
  return { offline: true, outboxId }
}

async function queueToOutbox(endpoint: string, method: string, body: string): Promise<number> {
  return addToOutbox({
    endpoint,
    method,
    body,
    status: 'pending',
    createdAt: new Date().toISOString(),
    retries: 0,
  })
}

function getDeviceId(): string {
  if (typeof window === 'undefined') return 'server'
  let id = localStorage.getItem('stratify_device_id')
  if (!id) {
    id = `device_${Math.random().toString(36).slice(2, 10)}`
    localStorage.setItem('stratify_device_id', id)
  }
  return id
}