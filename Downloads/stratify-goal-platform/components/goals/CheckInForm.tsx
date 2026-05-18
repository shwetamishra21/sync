// components/goals/CheckInForm.tsx
'use client'

import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { mutateOfflineAware } from '@/lib/offline/mutateOfflineAware'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'

interface Goal {
  _id: string
  title: string
  targetValue: number
  currentValue: number
  weightage: number
}

interface Props {
  goal: Goal
  onClose: () => void
  onSuccess: () => void
}

type SubmitState = 'idle' | 'submitting' | 'queued' | 'synced' | 'error'

export function CheckInForm({ goal, onClose, onSuccess }: Props) {
  const qc = useQueryClient()
  const { isOnline } = useOnlineStatus()
  const [progressValue, setProgressValue] = useState(goal.currentValue)
  const [notes, setNotes] = useState('')
  const [submitState, setSubmitState] = useState<SubmitState>('idle')
  const [errorMsg, setErrorMsg] = useState('')

  const pct = goal.targetValue > 0
    ? Math.min((progressValue / goal.targetValue) * 100, 100)
    : 0

  const achievementContribution = (pct * goal.weightage / 100).toFixed(1)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitState('submitting')
    setErrorMsg('')

    try {
      const result = await mutateOfflineAware({
        endpoint: `/api/goals/${goal._id}/checkins`,
        method: 'POST',
        payload: {
          progressValue,
          progressPercent: Math.round(pct),
          notes: notes.trim() || undefined,
          submittedAt: new Date().toISOString(),
        },
        onOptimisticUpdate: () => {
          // Immediately update the goal's progress in React Query cache
          qc.setQueryData(['goals'], (old: { goals: { _id: string; currentValue: number }[] } | undefined) => {
            if (!old) return old
            return {
              ...old,
              goals: old.goals.map(g =>
                g._id === goal._id ? { ...g, currentValue: progressValue } : g
              ),
            }
          })
          
          qc.setQueryData(['goal', goal._id], (old: any) => {
            if (!old || !old.goal) return old
            
            // Add optimistic check-in to the top of the history
            const optimisticCheckIn = {
              _id: `temp-${Date.now()}`,
              progressValue,
              progressPercent: Math.round(pct),
              notes: notes.trim() || undefined,
              submittedAt: new Date().toISOString(),
              _syncMeta: {
                isOfflineWrite: !navigator.onLine
              }
            }
            
            return {
              ...old,
              goal: { ...old.goal, currentValue: progressValue },
              checkIns: [optimisticCheckIn, ...(old.checkIns || [])]
            }
          })
        },
        onSuccess: () => {
          qc.invalidateQueries({ queryKey: ['goals'] })
        },
      })

      if (!result.online) {
        setSubmitState('queued')
        setTimeout(() => onSuccess(), 2500)
      } else {
        setSubmitState('synced')
        setTimeout(() => onSuccess(), 1000)
      }
    } catch (err) {
      setSubmitState('error')
      setErrorMsg(err instanceof Error ? err.message : 'Something went wrong')
    }
  }

  // ── Queued state ─────────────────────────────────────────────────────────────
  if (submitState === 'queued') {
    return (
      <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
        <div className="bg-gray-900 border border-gray-700 rounded-2xl shadow-2xl w-full max-w-sm p-8 text-center">
          <div className="w-16 h-16 bg-amber-500/20 rounded-full flex items-center justify-center mx-auto mb-4 ring-2 ring-amber-500/40">
            <svg className="w-8 h-8 text-amber-400 sync-pulse" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-white mb-2">Queued for sync</h3>
          <p className="text-sm text-gray-400 leading-relaxed mb-4">
            You&apos;re offline. Your check-in has been saved locally and will sync automatically when you reconnect.
          </p>
          <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl px-4 py-3">
            <div className="flex items-center gap-2 justify-center mb-1">
              <span className="w-1.5 h-1.5 rounded-full bg-amber-400 inline-block sync-pulse" />
              <span className="text-xs font-semibold text-amber-400">Pending sync</span>
            </div>
            <p className="text-xs text-amber-500 text-center">
              Progress: {progressValue.toLocaleString()} / {goal.targetValue.toLocaleString()} ({Math.round(pct)}%)
            </p>
          </div>
        </div>
      </div>
    )
  }

  // ── Synced state ─────────────────────────────────────────────────────────────
  if (submitState === 'synced') {
    return (
      <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
        <div className="bg-gray-900 border border-gray-700 rounded-2xl shadow-2xl w-full max-w-sm p-8 text-center">
          <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-4 ring-2 ring-green-500/40">
            <svg className="w-8 h-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-white mb-1">Synced!</h3>
          <p className="text-sm text-gray-400">Check-in saved to MongoDB.</p>
        </div>
      </div>
    )
  }

  // ── Normal form ───────────────────────────────────────────────────────────────
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
      <div className="bg-gray-900 border border-gray-800 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        {/* Header */}
        <div className="px-6 pt-6 pb-4 border-b border-gray-800">
          <div className="flex items-start justify-between">
            <div className="flex-1 min-w-0 pr-4">
              <h2 className="font-semibold text-white">Update Progress</h2>
              <p className="text-xs text-gray-500 mt-0.5 truncate">{goal.title}</p>
            </div>
            <div className="flex items-center gap-2 flex-shrink-0">
              <div className="flex items-center gap-1.5">
                <span className={`w-1.5 h-1.5 rounded-full inline-block ${isOnline ? 'bg-green-400' : 'bg-amber-400 sync-pulse'}`} />
                <span className={`text-[10px] font-medium ${isOnline ? 'text-green-400' : 'text-amber-400'}`}>
                  {isOnline ? 'Online' : 'Offline'}
                </span>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="text-gray-500 hover:text-gray-300 transition p-1 rounded-lg hover:bg-gray-800"
              >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          {/* Offline indicator */}
          {!isOnline && (
            <div className="mt-3 flex items-center gap-2 bg-amber-500/10 border border-amber-500/30 rounded-xl px-3 py-2">
              <svg className="w-3.5 h-3.5 text-amber-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M18.364 5.636a9 9 0 010 12.728M15.536 8.464a5 5 0 010 7.072M12 12h.01M8.464 15.536a5 5 0 010-7.072M5.636 18.364a9 9 0 010-12.728" />
              </svg>
              <p className="text-xs text-amber-300 font-medium">
                Offline — check-in will be queued and synced on reconnect
              </p>
            </div>
          )}
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {/* Progress slider + input */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium text-gray-300">
                Progress value
                <span className="ml-1 text-gray-600 font-normal text-xs">(target: {goal.targetValue.toLocaleString()})</span>
              </label>
              <span className="text-xs text-gray-400 font-medium">{Math.round(pct)}%</span>
            </div>
            <input
              type="number"
              min={0}
              max={goal.targetValue * 2}
              value={progressValue}
              onChange={e => setProgressValue(Number(e.target.value))}
              className="w-full px-3.5 py-2.5 rounded-xl bg-gray-800 border border-gray-700 text-white text-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent transition"
            />

            {/* Live progress bar */}
            <div className="mt-3">
              <div className="flex justify-between text-xs text-gray-600 mb-1.5">
                <span>{Math.round(pct)}% complete</span>
                <span>Achievement contribution: {achievementContribution}%</span>
              </div>
              <div className="h-2 bg-gray-800 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-300"
                  style={{
                    width: `${pct}%`,
                    background: pct >= 80 ? '#22c55e' : pct >= 50 ? '#3b82f6' : '#f59e0b',
                  }}
                />
              </div>
            </div>
          </div>

          {/* Notes */}
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1.5">
              Notes <span className="text-gray-600 font-normal">(optional)</span>
            </label>
            <textarea
              value={notes}
              onChange={e => setNotes(e.target.value)}
              rows={2}
              maxLength={500}
              placeholder="What did you accomplish since the last check-in?"
              className="w-full px-3.5 py-2.5 rounded-xl bg-gray-800 border border-gray-700 text-white text-sm placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent transition resize-none"
            />
          </div>

          {/* Error */}
          {submitState === 'error' && errorMsg && (
            <div className="flex items-center gap-2.5 bg-red-500/10 border border-red-500/30 rounded-xl px-4 py-3">
              <svg className="w-4 h-4 text-red-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <p className="text-sm text-red-400">{errorMsg}</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2.5 rounded-xl border border-gray-700 text-sm text-gray-400 hover:bg-gray-800 hover:text-gray-200 transition font-medium"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitState === 'submitting'}
              className="flex-1 flex items-center justify-center gap-2 bg-green-600 hover:bg-green-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-2.5 rounded-xl text-sm transition"
            >
              {submitState === 'submitting' ? (
                <>
                  <svg className="sync-pulse w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  Saving…
                </>
              ) : (
                isOnline ? 'Submit Check-in' : 'Queue Check-in'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}