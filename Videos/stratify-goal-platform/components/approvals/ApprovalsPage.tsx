'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'

interface User {
  name: string
  email: string
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
  userId: User
}

async function fetchPendingGoals(): Promise<{ goals: Goal[] }> {
  const res = await fetch('/api/goals?status=PENDING_APPROVAL')
  if (!res.ok) throw new Error(`Failed to fetch goals (${res.status})`)
  return res.json()
}

export default function ApprovalsPage() {
  const qc = useQueryClient()
  const { isOnline } = useOnlineStatus()
  
  const [selectedGoalId, setSelectedGoalId] = useState<string | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [showRejectForm, setShowRejectForm] = useState(false)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['goals', 'pending_approval'],
    queryFn: fetchPendingGoals,
  })

  const goals = data?.goals ?? []
  const selectedGoal = goals.find((g: Goal) => g._id === selectedGoalId)

  if (selectedGoalId && !selectedGoal && !isLoading) {
    setSelectedGoalId(null)
  }

  const approveMutation = useMutation({
    mutationFn: async (goalId: string) => {
      const res = await fetch(`/api/goals/${goalId}/approve`, {
        method: 'POST',
      })
      if (!res.ok) throw new Error('Failed to approve')
      return res.json()
    },
    onMutate: async (goalId) => {
      await qc.cancelQueries({ queryKey: ['goals', 'pending_approval'] })
      const previousGoals = qc.getQueryData(['goals', 'pending_approval']) as any
      qc.setQueryData(['goals', 'pending_approval'], (old: any) => ({
        ...old,
        goals: old?.goals.filter((g: Goal) => g._id !== goalId) ?? []
      }))
      return { previousGoals }
    },
    onError: (err, newGoal, context) => {
      qc.setQueryData(['goals', 'pending_approval'], context?.previousGoals)
      alert('Error approving goal.')
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['goals', 'pending_approval'] })
    }
  })

  const rejectMutation = useMutation({
    mutationFn: async ({ goalId, reason }: { goalId: string, reason: string }) => {
      const res = await fetch(`/api/goals/${goalId}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason })
      })
      if (!res.ok) throw new Error('Failed to reject')
      return res.json()
    },
    onMutate: async ({ goalId }) => {
      await qc.cancelQueries({ queryKey: ['goals', 'pending_approval'] })
      const previousGoals = qc.getQueryData(['goals', 'pending_approval']) as any
      qc.setQueryData(['goals', 'pending_approval'], (old: any) => ({
        ...old,
        goals: old?.goals.filter((g: Goal) => g._id !== goalId) ?? []
      }))
      return { previousGoals }
    },
    onError: (err, newGoal, context) => {
      qc.setQueryData(['goals', 'pending_approval'], context?.previousGoals)
      alert('Error rejecting goal.')
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['goals', 'pending_approval'] })
    }
  })

  const handleApprove = () => {
    if (!selectedGoalId || !isOnline) return
    approveMutation.mutate(selectedGoalId)
  }

  const handleReject = () => {
    if (!selectedGoalId || !isOnline) return
    rejectMutation.mutate({ goalId: selectedGoalId, reason: rejectReason })
    setRejectReason('')
    setShowRejectForm(false)
  }

  return (
    <div className="flex flex-col md:flex-row gap-6 h-[calc(100vh-8rem)]">
      {/* Left List Panel */}
      <div className="w-full md:w-1/3 flex flex-col gap-4 overflow-y-auto" style={{ borderRight: '1px solid var(--border-subtle)', paddingRight: '24px' }}>
        <h2 className="h1-title tracking-tight">Pending Approvals</h2>
        
        {isLoading ? (
          <div className="space-y-4">
            {[1,2,3].map(i => <div key={i} className="h-[80px] rounded-md" style={{ backgroundColor: 'var(--surface-hover)', animation: 'pulse 1.5s cubic-bezier(0.4,0,0.6,1) infinite' }} />)}
          </div>
        ) : isError ? (
          <div className="text-[13px]" style={{ color: 'var(--danger)' }}>Failed to load goals.</div>
        ) : goals.length === 0 ? (
          <div className="text-center py-10 rounded-lg" style={{ color: 'var(--text-muted)', border: '1px solid var(--border-subtle)' }}>
            No goals pending approval.
          </div>
        ) : (
          <div className="space-y-2">
            {goals.map((goal: Goal) => {
              const isSelected = selectedGoalId === goal._id
              return (
                <button
                  key={goal._id}
                  onClick={() => {
                    setSelectedGoalId(goal._id)
                    setShowRejectForm(false)
                    setRejectReason('')
                  }}
                  className="w-full text-left p-4 rounded-md transition-all"
                  style={{
                    backgroundColor: isSelected ? 'var(--brand-muted)' : 'transparent',
                    borderLeft: isSelected ? '3px solid var(--brand)' : '3px solid transparent',
                    borderTop: '1px solid transparent',
                    borderRight: '1px solid transparent',
                    borderBottom: '1px solid transparent'
                  }}
                  onMouseEnter={(e) => {
                    if (!isSelected) e.currentTarget.style.backgroundColor = 'var(--surface-hover)'
                  }}
                  onMouseLeave={(e) => {
                    if (!isSelected) e.currentTarget.style.backgroundColor = 'transparent'
                  }}
                >
                  <div className="flex justify-between items-start mb-1">
                    <h3 className="h2-title truncate">{goal.title}</h3>
                  </div>
                  <p className="text-[13px] truncate mb-3" style={{ color: 'var(--text-secondary)' }}>{goal.userId?.name || 'Unknown User'}</p>
                  <div className="flex gap-2">
                    <span className="label-caption" style={{ padding: '2px 6px', backgroundColor: 'var(--surface-inset)', borderRadius: 'var(--radius-sm)' }}>Wt: {goal.weightage}%</span>
                    <span className="status-badge status-pending">Pending</span>
                  </div>
                </button>
              )
            })}
          </div>
        )}
      </div>

      {/* Right Detail Panel */}
      <div className="w-full md:w-2/3 flex flex-col overflow-y-auto pb-6 md:pb-0 px-6">
        {selectedGoal ? (
          <div className="flex-1 flex flex-col">
            <div className="mb-6 pb-6" style={{ borderBottom: '1px solid var(--border-subtle)' }}>
              <h1 className="h1-title mb-2">{selectedGoal.title}</h1>
              <div className="text-[13px] flex items-center gap-2" style={{ color: 'var(--text-secondary)' }}>
                <span>Submitted by <span className="font-semibold" style={{ color: 'var(--text-primary)' }}>{selectedGoal.userId?.name}</span></span>
                <span>•</span>
                <span>{selectedGoal.userId?.email}</span>
              </div>
            </div>

            <div className="space-y-6 mb-8 flex-1">
              <div>
                <h4 className="label-caption mb-2">Description</h4>
                <p className="text-[13px] whitespace-pre-wrap leading-relaxed" style={{ color: 'var(--text-secondary)' }}>{selectedGoal.description || 'No description provided.'}</p>
              </div>

              <div className="flex gap-10">
                <div>
                  <h4 className="label-caption mb-1">Target Value</h4>
                  <p className="metric-value">{selectedGoal.targetValue}</p>
                </div>
                <div>
                  <h4 className="label-caption mb-1">Weightage</h4>
                  <p className="metric-value">{selectedGoal.weightage}%</p>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="pt-6 mt-auto">
              {!isOnline && (
                <div className="mb-4 text-[13px] p-3 rounded-md flex items-center gap-2" style={{ color: 'var(--warning)', backgroundColor: 'var(--warning-surface)' }}>
                  <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  Requires connection to approve or reject
                </div>
              )}

              {showRejectForm ? (
                <div className="space-y-4 p-5 rounded-lg" style={{ backgroundColor: 'var(--danger-surface)' }}>
                  <label className="input-label">Reason for rejection</label>
                  <textarea
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                    className="input-field w-full"
                    rows={3}
                    placeholder="Explain why this goal needs revision..."
                  />
                  <div className="flex flex-col gap-3 w-full">
                    <button
                      onClick={handleReject}
                      disabled={!rejectReason.trim() || rejectMutation.isPending || !isOnline}
                      className="btn-destructive w-full"
                    >
                      {rejectMutation.isPending ? 'Rejecting...' : 'Confirm Reject'}
                    </button>
                    <button
                      onClick={() => setShowRejectForm(false)}
                      disabled={rejectMutation.isPending}
                      className="btn-secondary w-full"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col gap-3 w-full">
                  <button
                    onClick={handleApprove}
                    disabled={approveMutation.isPending || !isOnline}
                    className="btn-primary w-full flex justify-center items-center gap-2"
                  >
                    {approveMutation.isPending ? 'Approving...' : (
                      <>
                        <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                        </svg>
                        Approve Goal
                      </>
                    )}
                  </button>
                  <button
                    onClick={() => setShowRejectForm(true)}
                    disabled={approveMutation.isPending || !isOnline}
                    className="btn-secondary w-full flex justify-center items-center gap-2"
                  >
                    Reject Goal
                  </button>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-center p-8 rounded-lg" style={{ border: '1px dashed var(--border-subtle)' }}>
            <div className="w-16 h-16 rounded-md flex items-center justify-center mb-4" style={{ backgroundColor: 'var(--surface-hover)' }}>
              <svg className="w-8 h-8" style={{ color: 'var(--text-muted)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <h3 className="h2-title mb-1">No Goal Selected</h3>
            <p className="text-[13px] max-w-sm" style={{ color: 'var(--text-secondary)' }}>Select a goal from the queue to review its details and approve or reject it.</p>
          </div>
        )}
      </div>
    </div>
  )
}
