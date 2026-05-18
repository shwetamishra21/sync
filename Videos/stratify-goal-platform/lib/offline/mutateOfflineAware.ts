// lib/offline/mutateOfflineAware.ts

import { enqueueMutation } from './outbox'

export interface MutateOptions {
  endpoint: string
  method: 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  payload: Record<string, unknown>

  // optimistic UI
  onOptimisticUpdate?: () => void

  // online success
  onSuccess?: (data: unknown) => void

  // online/server failure
  onError?: (error: Error) => void
}

export interface MutateResult {
  online: boolean
  outboxId?: number
  data?: unknown
}

/**
 * Every write operation in the app should go through this function.
 *
 * Online:
 *   → send directly to API
 *   → fallback to outbox if request fails
 *
 * Offline:
 *   → enqueue immediately
 *   → optimistic UI still updates
 */
export async function mutateOfflineAware(
  opts: MutateOptions
): Promise<MutateResult> {
  const {
    endpoint,
    method,
    payload,
    onOptimisticUpdate,
    onSuccess,
    onError,
  } = opts

  // Always optimistic first
  onOptimisticUpdate?.()

  const isOnline =
    typeof navigator !== 'undefined'
      ? navigator.onLine
      : true

  // ── OFFLINE PATH ─────────────────────────────────────────────
  if (!isOnline) {
    const outboxId = await enqueueMutation(
      endpoint,
      method,
      payload
    )

    return {
      online: false,
      outboxId,
    }
  }

  // ── ONLINE PATH ──────────────────────────────────────────────
  try {
    const response = await fetch(endpoint, {
      method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })

    // API/server error
    if (!response.ok) {
      const outboxId = await enqueueMutation(
        endpoint,
        method,
        payload
      )

      const error = new Error(
        `HTTP ${response.status}`
      )

      onError?.(error)

      return {
        online: false,
        outboxId,
      }
    }

    // Safe JSON parsing
    let data: unknown = null

    const contentType =
      response.headers.get('content-type')

    if (contentType?.includes('application/json')) {
      data = await response.json()
    }

    onSuccess?.(data)

    return {
      online: true,
      data,
    }
  } catch (err) {
    // network failure mid-flight
    const outboxId = await enqueueMutation(
      endpoint,
      method,
      payload
    )

    if (err instanceof Error) {
      onError?.(err)
    }

    return {
      online: false,
      outboxId,
    }
  }
}