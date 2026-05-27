'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useSession } from 'next-auth/react'
import Link from 'next/link'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import { CheckInForm } from '@/components/goals/CheckInForm'
import { ShareGoalModal } from '@/components/goals/ShareGoalModal'

interface User {
  name: string
  email: string
}

interface CheckIn {
  _id: string
  progressValue: number
  progressPercent: number
  notes?: string
  managerComment?: string
  submittedAt: string
  _syncMeta?: any
}

interface Goal {
  _id: string
  title: string
  description?: string
  weightage: number
  status: string
  targetValue: number
  currentValue: number
  achievementScore: number
  riskScore: number
  userId: User
  quarterId?: string
}


export default function GoalDetailPage({ goalId }: { goalId: string }) {
  const qc = useQueryClient()
  const { isOnline } = useOnlineStatus()
  const { data: session } = useSession()
  const [showCheckIn, setShowCheckIn] = useState(false)
  const [showShareModal, setShowShareModal] = useState(false)
  const [editingCommentId, setEditingCommentId] = useState<string | null>(null)
  const [commentText, setCommentText] = useState('')

  const userRole = (session?.user as any)?.role
  const canComment = userRole === 'MANAGER' || userRole === 'ADMIN'

  const commentMutation = useMutation({
    mutationFn: async ({ checkInId, text }: { checkInId: string, text: string }) => {
      const res = await fetch(`/api/goals/${goalId}/checkins/${checkInId}/comment`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ managerComment: text })
      })
      if (!res.ok) throw new Error('Failed to add comment')
      return res.json()
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['goal', goalId] })
      setEditingCommentId(null)
    }
  })

  const { data, isLoading, isError } = useQuery({
    queryKey: ['goal', goalId],
    queryFn: async () => {
      const res = await fetch(`/api/goals/${goalId}`)
      if (!res.ok) throw new Error('Failed to fetch')
      return res.json()
    },
    refetchInterval: isOnline ? 15_000 : false,
  })

  const goal: Goal | undefined = data?.goal
  const checkIns: CheckIn[] = data?.checkIns || []

  const { data: goalsData } = useQuery({
    queryKey: ['goals'],
    queryFn: async () => {
      const res = await fetch('/api/goals')
      if (!res.ok) throw new Error('Failed to fetch goals')
      return res.json()
    },
    refetchInterval: isOnline ? 15_000 : false,
  })

  const allGoals: Goal[] = goalsData?.goals || []
  const quarterGoals = allGoals.filter(g => g.quarterId === goal?.quarterId && g.status !== 'REJECTED')
  const totalWeightage = quarterGoals.reduce((sum, g) => sum + (g.weightage || 0), 0)
  const totalAchievement = quarterGoals.reduce((sum, g) => sum + (g.achievementScore || 0), 0)
  const quarterPct = totalWeightage > 0 ? (totalAchievement / totalWeightage) * 100 : 0

  const submitMutation = useMutation({
    mutationFn: async () => {
      const res = await fetch(`/api/goals/${goalId}/submit`, { method: 'POST' })
      if (!res.ok) throw new Error('Failed to submit')
      return res.json()
    },
    onMutate: async () => {
      await qc.cancelQueries({ queryKey: ['goal', goalId] })
      const previousData = qc.getQueryData(['goal', goalId])
      qc.setQueryData(['goal', goalId], (old: any) => ({
        ...old,
        goal: { ...old?.goal, status: 'PENDING_APPROVAL' }
      }))
      return { previousData }
    },
    onError: (err, variables, context) => {
      qc.setQueryData(['goal', goalId], context?.previousData)
      alert('Error submitting goal')
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['goal', goalId] })
      qc.invalidateQueries({ queryKey: ['goals'] })
    }
  })

  const completeMutation = useMutation({
    mutationFn: async () => {
      const res = await fetch(`/api/goals/${goalId}/request-completion`, { method: 'POST' })
      if (!res.ok) throw new Error('Failed to request completion')
      return res.json()
    },
    onMutate: async () => {
      await qc.cancelQueries({ queryKey: ['goal', goalId] })
      const previousData = qc.getQueryData(['goal', goalId])
      qc.setQueryData(['goal', goalId], (old: any) => ({
        ...old,
        goal: { ...old?.goal, status: 'PENDING_COMPLETION' }
      }))
      return { previousData }
    },
    onError: (err, variables, context) => {
      qc.setQueryData(['goal', goalId], context?.previousData)
      alert('Error requesting completion')
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['goal', goalId] })
      qc.invalidateQueries({ queryKey: ['goals'] })
    }
  })

  if (isLoading) {
    return <div className="animate-pulse space-y-6 max-w-4xl">
      <div className="h-32 bg-gray-900 rounded-2xl" />
      <div className="grid grid-cols-2 gap-4">
        <div className="h-40 bg-gray-900 rounded-2xl" />
        <div className="h-40 bg-gray-900 rounded-2xl" />
      </div>
    </div>
  }

  if (isError || !goal) {
    return <div className="text-red-400">Failed to load goal</div>
  }

  const pct = goal.targetValue > 0 ? Math.min((goal.currentValue / goal.targetValue) * 100, 100) : 0

  return (
    <div className="max-w-4xl space-y-6">
      <div className="flex items-center gap-3 mb-6">
        <Link href="/dashboard/goals" className="text-gray-500 hover:text-gray-300 transition flex items-center gap-2">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          <span className="font-medium text-sm">Back to Goals</span>
        </Link>
      </div>

      {/* Hero Section */}
      <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 md:p-8 relative overflow-hidden">
        {goal.sharedGoalId && (
          <div className="bg-blue-500/10 border border-blue-500/20 text-blue-400 px-4 py-3 rounded-xl mb-6 flex items-center gap-3">
            <svg className="w-5 h-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            <span className="text-sm font-medium">This is a shared team goal</span>
          </div>
        )}

        <div className="absolute top-0 right-0 p-6 flex flex-wrap gap-3 justify-end w-2/3 md:w-auto">
          <span className="bg-gray-800 text-gray-300 px-3 py-1.5 rounded-lg text-xs font-bold uppercase whitespace-nowrap">Wt: {goal.weightage}%</span>
          <span className="bg-blue-500/20 text-blue-400 border border-blue-500/20 px-3 py-1.5 rounded-lg text-xs font-bold uppercase whitespace-nowrap">{goal.status}</span>
          {goal.riskScore > 50 && (
            <span className="bg-red-500/20 text-red-400 border border-red-500/20 px-3 py-1.5 rounded-lg text-xs font-bold uppercase flex items-center gap-1 whitespace-nowrap">
              <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              High Risk
            </span>
          )}
        </div>
        
        <h1 className="text-3xl font-bold text-white mb-3 pr-24 md:pr-64 leading-tight">{goal.title}</h1>
        <p className="text-gray-400 mb-8 max-w-2xl leading-relaxed">{goal.description || 'No description provided.'}</p>

        <div className="flex items-center justify-between mt-auto">
          <div className="flex gap-4">
            {canComment && !goal.sharedGoalId && (
              <button
                onClick={() => setShowShareModal(true)}
                className="bg-gray-800 hover:bg-gray-700 text-white px-5 py-2.5 rounded-xl font-semibold text-sm transition flex items-center gap-2"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                </svg>
                Share Goal
              </button>
            )}
            {(goal.status === 'DRAFT' || goal.status === 'REJECTED') && (
              <button
                onClick={() => submitMutation.mutate()}
                disabled={submitMutation.isPending || !isOnline}
                className="bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white px-5 py-2.5 rounded-xl font-semibold text-sm transition"
              >
                {submitMutation.isPending ? 'Submitting...' : 'Submit for Approval'}
              </button>
            )}
            {['APPROVED', 'LOCKED'].includes(goal.status) && (
              <button
                onClick={() => setShowCheckIn(true)}
                className="bg-green-600 hover:bg-green-500 text-white px-5 py-2.5 rounded-xl font-semibold text-sm transition flex items-center gap-2"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
                </svg>
                New Check-in
              </button>
            )}
            {goal.status === 'APPROVED' && goal.userId._id === (session?.user as any)?.id && (
              <button
                onClick={() => completeMutation.mutate()}
                disabled={completeMutation.isPending || !isOnline}
                className="bg-purple-600 hover:bg-purple-500 disabled:opacity-50 text-white px-5 py-2.5 rounded-xl font-semibold text-sm transition"
              >
                {completeMutation.isPending ? 'Requesting...' : 'Request Completion'}
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Progress Visualization */}
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6">
          <h3 className="text-lg font-bold text-white mb-4">Progress</h3>
          <div className="flex items-end gap-3 mb-2">
            <span className="text-4xl font-bold text-white">{goal.currentValue.toLocaleString()}</span>
            <span className="text-gray-500 mb-1">/ {goal.targetValue.toLocaleString()}</span>
          </div>
          <div className="flex justify-between text-sm text-gray-400 mb-3">
            <span>{Math.round(pct)}% Complete</span>
          </div>
          <div className="h-3 bg-gray-800 rounded-full overflow-hidden">
            <div
              className="h-full rounded-full transition-all duration-700"
              style={{ width: `${pct}%`, background: pct >= 80 ? '#22c55e' : pct >= 50 ? '#3b82f6' : pct >= 25 ? '#f59e0b' : '#ef4444' }}
            />
          </div>
        </div>

        {/* Achievement Formula Card */}
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 flex flex-col gap-6">
          <div>
            <h3 className="text-lg font-bold text-white mb-4">Achievement Score</h3>
            <div className="flex items-center gap-6">
              <div className="w-24 h-24 rounded-full border-4 border-gray-800 flex items-center justify-center relative flex-shrink-0">
                <span className="text-2xl font-bold text-white">{goal.achievementScore.toFixed(1)}%</span>
                <svg className="absolute inset-0 w-full h-full -rotate-90 scale-110" viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r="46" fill="none" stroke="#22c55e" strokeWidth="8" strokeDasharray={`${(goal.achievementScore / 100) * 289} 289`} strokeLinecap="round" />
                </svg>
              </div>
              <div className="flex-1 space-y-3">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-400 font-medium">Progress</span>
                  <span className="text-gray-200 font-semibold">{Math.round(pct)}%</span>
                </div>
                <div className="flex justify-between text-sm border-b border-gray-800 pb-3">
                  <span className="text-gray-400 font-medium">Weightage</span>
                  <span className="text-gray-200 font-semibold">× {goal.weightage}%</span>
                </div>
                <div className="flex justify-between text-sm pt-1">
                  <span className="text-gray-300 font-medium">Final Score</span>
                  <span className="text-green-400 font-bold text-base">{goal.achievementScore.toFixed(1)}%</span>
                </div>
              </div>
            </div>
          </div>
          
          <div className="pt-4 border-t border-gray-800">
            <h4 className="text-sm font-bold text-white mb-3">Quarter Summary</h4>
            <div className="space-y-3">
              <div className="flex justify-between text-xs">
                <span className="text-gray-400">Total Weightage</span>
                <span className="text-gray-200">{totalWeightage}%</span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-gray-400">Total Achievement</span>
                <span className="text-green-400 font-bold">{totalAchievement.toFixed(1)}%</span>
              </div>
              <div className="h-1.5 bg-gray-800 rounded-full overflow-hidden mt-1">
                <div
                  className="h-full bg-green-500 rounded-full transition-all duration-700"
                  style={{ width: `${quarterPct}%` }}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Check-in History */}
      <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6">
        <h3 className="text-lg font-bold text-white mb-6 flex items-center gap-2">
          <svg className="w-5 h-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          Check-in History
        </h3>
        {checkIns.length === 0 ? (
          <div className="text-gray-500 text-center py-10 bg-gray-800/30 rounded-xl border border-gray-800 border-dashed">
            No check-ins yet. Updates will appear here.
          </div>
        ) : (
          <div className="space-y-5">
            {checkIns.map((ci) => (
              <div key={ci._id} className="relative pl-6 pb-5 last:pb-0">
                {/* Timeline line */}
                <div className="absolute top-8 left-2 -ml-px h-full w-0.5 bg-gray-800 last:hidden" />
                
                <div className="flex items-start gap-4">
                  {/* Timeline dot */}
                  <div className="absolute top-2 left-0 w-4 h-4 rounded-full bg-gray-800 border-2 border-gray-900 ring-2 ring-gray-800" />
                  
                  <div className="flex-1 min-w-0 bg-gray-800/50 rounded-xl p-4 border border-gray-800">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-semibold text-white">Updated progress to <span className="text-green-400">{ci.progressValue}</span></span>
                      <span className="text-xs text-gray-500 font-medium">{new Date(ci.submittedAt).toLocaleDateString()}</span>
                    </div>
                    {ci.notes && <p className="text-sm text-gray-400 leading-relaxed bg-gray-900/50 p-3 rounded-lg border border-gray-800/50">{ci.notes}</p>}
                    
                    {ci.managerComment && (
                      <div className="mt-3 bg-blue-900/20 border border-blue-500/20 rounded-lg p-3">
                        <p className="text-xs font-semibold text-blue-400 mb-1">Manager Comment</p>
                        <p className="text-sm text-blue-200">{ci.managerComment}</p>
                      </div>
                    )}
                    
                    {canComment && !ci.managerComment && editingCommentId !== ci._id && (
                      <button
                        onClick={() => { setEditingCommentId(ci._id); setCommentText('') }}
                        className="mt-3 text-xs font-medium text-blue-400 hover:text-blue-300 transition"
                      >
                        + Add comment
                      </button>
                    )}
                    
                    {editingCommentId === ci._id && (
                      <div className="mt-3">
                        <textarea
                          autoFocus
                          value={commentText}
                          onChange={e => setCommentText(e.target.value)}
                          onBlur={() => {
                            if (commentText.trim()) {
                              commentMutation.mutate({ checkInId: ci._id, text: commentText })
                            } else {
                              setEditingCommentId(null)
                            }
                          }}
                          onKeyDown={e => {
                            if (e.key === 'Enter' && !e.shiftKey) {
                              e.preventDefault()
                              if (commentText.trim()) {
                                commentMutation.mutate({ checkInId: ci._id, text: commentText })
                              } else {
                                setEditingCommentId(null)
                              }
                            } else if (e.key === 'Escape') {
                              setEditingCommentId(null)
                            }
                          }}
                          className="w-full bg-gray-900 border border-blue-500/50 rounded-lg p-2 text-sm text-white focus:outline-none focus:border-blue-500 resize-none"
                          placeholder="Type comment and press Enter..."
                          rows={2}
                          disabled={commentMutation.isPending}
                        />
                      </div>
                    )}

                    {ci._syncMeta?.isOfflineWrite && !ci._syncMeta?.syncedAt && (
                      <span className="inline-flex mt-3 items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider bg-amber-500/10 text-amber-400 border border-amber-500/20 px-2.5 py-1 rounded-md">
                        <span className="w-1.5 h-1.5 bg-amber-400 rounded-full sync-pulse" />
                        Pending Sync
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {showCheckIn && (
        <CheckInForm
          goal={goal as any}
          onClose={() => setShowCheckIn(false)}
          onSuccess={() => {
            setShowCheckIn(false)
            qc.invalidateQueries({ queryKey: ['goal', goalId] })
            qc.invalidateQueries({ queryKey: ['goals'] })
          }}
        />
      )}

      {showShareModal && (
        <ShareGoalModal goalId={goalId} onClose={() => setShowShareModal(false)} />
      )}
    </div>
  )
}
