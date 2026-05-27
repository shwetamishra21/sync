// components/offline/OfflineBanner.tsx
'use client'

import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import { useSyncStatus } from '@/hooks/useSyncStatus'
import { useEffect, useRef, useState } from 'react'

export function OfflineBanner() {
  const { isOnline } = useOnlineStatus()
  const { pendingCount, isSyncing, lastSyncResult } = useSyncStatus()
  const [showSynced, setShowSynced] = useState(false)
  const [mounted, setMounted] = useState(false)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const prevSyncResult = useRef(lastSyncResult)

  // Standard hydration guard — this is the canonical Next.js pattern for hydration safety
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { setMounted(true) }, [])

  useEffect(() => {
    // Only trigger when lastSyncResult changes (not on first render)
    if (lastSyncResult !== prevSyncResult.current && lastSyncResult && pendingCount === 0) {
      prevSyncResult.current = lastSyncResult
      if (timerRef.current) clearTimeout(timerRef.current)
      setShowSynced(true)
      timerRef.current = setTimeout(() => setShowSynced(false), 3000)
    }
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    }
  }, [lastSyncResult, pendingCount])

  // Avoid hydration mismatch
  if (!mounted) return null

  // Online + no pending + just synced flash
  if (isOnline && pendingCount === 0 && showSynced) {
    return (
      <div className="slide-down fixed top-0 left-0 right-0 z-50 flex items-center justify-center gap-2 bg-green-600 text-white text-sm font-medium py-2 px-4">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
        All actions synced successfully
      </div>
    )
  }

  // Online + pending items syncing
  if (isOnline && isSyncing) {
    return (
      <div className="slide-down fixed top-0 left-0 right-0 z-50 flex items-center justify-center gap-2 bg-blue-600 text-white text-sm font-medium py-2 px-4">
        <svg className="sync-pulse w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        Syncing {pendingCount} action{pendingCount !== 1 ? 's' : ''}…
      </div>
    )
  }

  // Offline banner
  if (!isOnline) {
    return (
      <div className="slide-down fixed top-0 left-0 right-0 z-50 flex items-center justify-center gap-2 bg-amber-500 text-white text-sm font-medium py-2 px-4">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M18.364 5.636a9 9 0 010 12.728M15.536 8.464a5 5 0 010 7.072M12 12h.01M8.464 15.536a5 5 0 010-7.072M5.636 18.364a9 9 0 010-12.728" />
        </svg>
        You are offline
        {pendingCount > 0 && (
          <span className="ml-1 bg-white/20 rounded-full px-2 py-0.5 text-xs">
            {pendingCount} action{pendingCount !== 1 ? 's' : ''} queued
          </span>
        )}
      </div>
    )
  }

  return null
}