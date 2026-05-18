// components/offline/ConflictAlert.tsx
'use client'

import { useState, useEffect } from 'react'

interface ConflictData {
  id: string
  title: string
  reason: string
  goalId?: string
}

export function ConflictAlert() {
  const [conflicts, setConflicts] = useState<ConflictData[]>([])

  useEffect(() => {
    const handle = (e: Event) => {
      const detail = (e as CustomEvent).detail
      
      let title = 'Unknown goal'
      let reason = 'Data changed server-side'
      let goalId = undefined
      
      try {
        const payload = detail?.item?.payload
        if (payload?.title) title = payload.title
        
        if (detail?.serverResponse) {
          const parsed = JSON.parse(detail.serverResponse)
          if (parsed.error) reason = parsed.error
          if (parsed.goalId) goalId = parsed.goalId
        }
        
        if (!goalId && detail?.item?.endpoint) {
          const match = detail.item.endpoint.match(/\/api\/goals\/([^\/]+)/)
          if (match) goalId = match[1]
        }
      } catch (err) {
        // ignore
      }

      setConflicts(prev => {
        const newId = detail?.item?.id?.toString() ?? Math.random().toString()
        if (prev.some(c => c.id === newId)) return prev
        return [...prev, { id: newId, title, reason, goalId }]
      })
    }
    window.addEventListener('stratify:sync:conflict', handle)
    return () => window.removeEventListener('stratify:sync:conflict', handle)
  }, [])

  if (conflicts.length === 0) return null

  return (
    <div className="fixed bottom-4 right-4 z-50 w-80 space-y-2">
      {conflicts.map((conflict) => (
        <div
          key={conflict.id}
          className="bg-orange-50 border border-orange-200 rounded-xl p-4 shadow-lg slide-down"
        >
          <div className="flex items-start gap-3">
            <svg className="w-5 h-5 text-orange-500 mt-0.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-orange-800">Sync conflict</p>
              <p className="text-xs text-orange-600 mt-0.5 truncate">{conflict.title}</p>
              <p className="text-[11px] text-orange-500 mt-1 font-mono truncate">{conflict.reason}</p>
            </div>
            <button
              onClick={() => setConflicts(prev => prev.filter(c => c.id !== conflict.id))}
              className="text-orange-400 hover:text-orange-600 transition"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div className="mt-3 flex items-center gap-2">
            <button
              onClick={() => setConflicts(prev => prev.filter(c => c.id !== conflict.id))}
              className="flex-1 text-xs bg-orange-100 hover:bg-orange-200 text-orange-800 font-medium py-1.5 rounded-lg transition"
            >
              Keep server version
            </button>
            {conflict.goalId && (
              <button
                onClick={() => { window.location.href = `/dashboard/goals/${conflict.goalId}` }}
                className="flex-1 text-xs bg-orange-500 hover:bg-orange-600 text-white font-medium py-1.5 rounded-lg transition"
              >
                View goal
              </button>
            )}
          </div>
        </div>
      ))}
    </div>
  )
}