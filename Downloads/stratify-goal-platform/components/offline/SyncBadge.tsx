// components/offline/SyncBadge.tsx
'use client'

import { useEffect, useState } from 'react'
import { useSyncStatus } from '@/hooks/useSyncStatus'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'

export function SyncBadge() {
  const [mounted, setMounted] = useState(false)
  const { isOnline } = useOnlineStatus()
  const { pendingCount, isSyncing } = useSyncStatus()

  useEffect(() => {
    setMounted(true)
  }, [])

  if (!mounted) {
    return (
      <div className="flex items-center gap-1.5 text-xs text-gray-400">
        <span className="w-2 h-2 rounded-full bg-green-500 inline-block" />
        Synced
      </div>
    )
  }

  if (isOnline && pendingCount === 0 && !isSyncing) {
    // Green dot: fully synced
    return (
      <div className="flex items-center gap-1.5 text-xs text-gray-400">
        <span className="w-2 h-2 rounded-full bg-green-500 inline-block" />
        Synced
      </div>
    )
  }

  if (!isOnline) {
    return (
      <div className="flex items-center gap-1.5 text-xs text-amber-600 font-medium">
        <span className="w-2 h-2 rounded-full bg-amber-500 inline-block" />
        Offline
        {pendingCount > 0 && (
          <span className="bg-amber-100 text-amber-700 rounded-full px-1.5 py-0.5 text-[10px] font-bold">
            {pendingCount}
          </span>
        )}
      </div>
    )
  }

  if (isSyncing) {
    return (
      <div className="flex items-center gap-1.5 text-xs text-blue-600 font-medium">
        <span className="sync-pulse w-2 h-2 rounded-full bg-blue-500 inline-block" />
        Syncing…
      </div>
    )
  }

  return (
    <div className="flex items-center gap-1.5 text-xs text-amber-600 font-medium">
      <span className="w-2 h-2 rounded-full bg-amber-500 inline-block" />
      {pendingCount} pending
    </div>
  )
}