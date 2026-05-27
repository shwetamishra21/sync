// components/goals/page.tsx  (rendered at /dashboard/goals)
'use client'

import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import Link from 'next/link'
import { CheckInForm } from '@/components/goals/CheckInForm'
import { CreateGoalForm } from '@/components/goals/CreateGoalForm'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import { useSyncStatus } from '@/hooks/useSyncStatus'

// ── Types ─────────────────────────────────────────────────────────────────────

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
  quarterId?: string
  /** Injected locally for optimistic pending goals */
  _isPending?: boolean
}

// ── Constants ─────────────────────────────────────────────────────────────────

const STATUS_CLASSES: Record<string, string> = {
  DRAFT:            'status-badge status-draft',
  PENDING_APPROVAL: 'status-badge status-pending',
  APPROVED:         'status-badge status-approved',
  LOCKED:           'status-badge status-locked',
  REJECTED:         'status-badge status-rejected',
  COMPLETED:        'status-badge status-completed',
}

const STATUS_LABELS: Record<string, string> = {
  DRAFT:            'Draft',
  PENDING_APPROVAL: 'Pending',
  APPROVED:         'Approved',
  LOCKED:           'Locked',
  REJECTED:         'Rejected',
  COMPLETED:        'Completed',
}

// ── Fetcher ───────────────────────────────────────────────────────────────────

async function fetchGoals(): Promise<{ goals: Goal[] }> {
  const res = await fetch('/api/goals')
  if (!res.ok) throw new Error(`Failed to fetch goals (${res.status})`)
  return res.json()
}

// ── Skeleton card ─────────────────────────────────────────────────────────────

function GoalSkeleton() {
  return (
    <div className="base-card p-4 animate-pulse">
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1 space-y-2">
          <div className="h-4 rounded w-2/3" style={{ backgroundColor: 'var(--surface-hover)' }} />
          <div className="h-3 rounded w-1/2" style={{ backgroundColor: 'var(--surface-hover)' }} />
        </div>
        <div className="h-5 w-16 rounded-full" style={{ backgroundColor: 'var(--surface-hover)' }} />
      </div>
      <div className="h-2 rounded-full mb-4" style={{ backgroundColor: 'var(--surface-hover)' }} />
      <div className="flex justify-between">
        <div className="h-3 rounded w-24" style={{ backgroundColor: 'var(--surface-hover)' }} />
        <div className="h-7 w-28 rounded-lg" style={{ backgroundColor: 'var(--surface-hover)' }} />
      </div>
    </div>
  )
}

// ── Goal card ─────────────────────────────────────────────────────────────────

function GoalCard({
  goal,
  onCheckIn,
  onSubmitForApproval,
}: {
  goal: Goal
  onCheckIn: (goal: Goal) => void
  onSubmitForApproval?: (goalId: string) => void
}) {
  const pct = goal.targetValue > 0
    ? Math.min((goal.currentValue / goal.targetValue) * 100, 100)
    : 0

  const canCheckIn = ['APPROVED', 'LOCKED'].includes(goal.status) && !goal._isPending
  const badgeClass = STATUS_CLASSES[goal.status] ?? STATUS_CLASSES['DRAFT']
  const label = STATUS_LABELS[goal.status] ?? goal.status

  return (
    <div className="base-card p-4 relative group" style={{
      borderColor: goal._isPending ? 'var(--warning)' : undefined
    }}>
      {/* Title row */}
      <div className="flex items-start justify-between gap-3 mb-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-1">
            <Link href={`/dashboard/goals/${goal._id}`} className="text-[14px] font-medium leading-snug truncate hover:underline transition" style={{ color: 'var(--text-primary)' }}>
              {goal.title}
            </Link>
            <span className={badgeClass}>
              {label}
            </span>
          </div>
          <div className="flex items-center gap-2 mb-2">
            {goal.riskScore > 50 && (
              <span className="inline-flex items-center text-[10px] font-semibold px-2 py-0.5 rounded-full whitespace-nowrap gap-1" style={{ backgroundColor: 'var(--danger-surface)', color: 'var(--danger)', border: '1px solid rgba(239,68,68,0.2)' }}>
                High Risk
              </span>
            )}
            <span className="label-caption">Weight: {goal.weightage}%</span>
          </div>
          {goal.description && (
            <p className="text-[13px] line-clamp-2 leading-relaxed" style={{ color: 'var(--text-secondary)' }}>
              {goal.description}
            </p>
          )}
        </div>
      </div>

      {/* Pending sync banner */}
      {goal._isPending && (
        <div className="mb-3 px-3 py-1 flex items-center" style={{ borderLeft: '1px solid var(--warning)' }}>
          <span className="text-[11px]" style={{ color: 'var(--text-muted)' }}>Queued — will sync when online</span>
        </div>
      )}

      {/* Progress bar */}
      <div className="mb-4 mt-2">
        <div className="flex items-center justify-between mb-1.5">
          <span className="text-[12px]" style={{ color: 'var(--text-secondary)' }}>
            {goal.currentValue.toLocaleString()} / {goal.targetValue.toLocaleString()}
          </span>
          <span className="text-[12px] font-semibold" style={{ color: 'var(--text-primary)' }}>{Math.round(pct)}%</span>
        </div>
        <div className="rounded-full overflow-hidden" style={{ height: '3px', backgroundColor: 'var(--surface-hover)' }}>
          <div
            className="h-full rounded-full transition-all duration-700 progress-fill"
            style={{
              width: `${pct}%`,
              backgroundColor: pct < 30 ? 'var(--danger)' : pct < 60 ? 'var(--warning)' : pct < 80 ? 'var(--info)' : 'var(--success)'
            }}
          />
        </div>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <svg className="w-3.5 h-3.5" style={{ color: 'var(--text-muted)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
          </svg>
          <span className="text-[12px]" style={{ color: 'var(--text-secondary)' }}>
            Score:{' '}
            <span className="font-medium" style={{ color: 'var(--text-primary)' }}>
              {(goal.achievementScore ?? 0).toFixed(1)}%
            </span>
          </span>
        </div>

        <div className="flex items-center gap-1">
          {canCheckIn && (
            <button
              onClick={() => onCheckIn(goal)}
              className="w-8 h-8 rounded-md flex items-center justify-center transition-colors hover:bg-[var(--surface-hover)]"
              title="Update Progress"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--brand)" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 4v16m8-8H4" />
              </svg>
            </button>
          )}

          {(goal.status === 'DRAFT' || goal.status === 'REJECTED') && !goal._isPending && onSubmitForApproval && (
            <button
              onClick={() => onSubmitForApproval(goal._id)}
              className="w-8 h-8 rounded-md flex items-center justify-center transition-colors hover:bg-[var(--surface-hover)]"
              title="Submit for Approval"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--info)" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
              </svg>
            </button>
          )}
          
          <Link
            href={`/dashboard/goals/${goal._id}`}
            className="w-8 h-8 rounded-md flex items-center justify-center transition-colors hover:bg-[var(--surface-hover)]"
            title="Details"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--text-secondary)" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 18l6-6-6-6" />
            </svg>
          </Link>
        </div>
      </div>
    </div>
  )
}

// ── Stats summary ─────────────────────────────────────────────────────────────

function GoalStats({ goals }: { goals: Goal[] }) {
  const realGoals = goals.filter(g => !g._isPending)
  const totalAchievement = realGoals
    .filter(g => ['APPROVED', 'LOCKED', 'COMPLETED'].includes(g.status))
    .reduce((sum, g) => sum + (g.achievementScore ?? 0), 0)
  const approved = realGoals.filter(g => g.status === 'APPROVED').length
  const pending = realGoals.filter(g => g.status === 'PENDING_APPROVAL').length

  return (
    <div className="grid grid-cols-2 gap-5 sm:grid-cols-4">
      {[
        { 
          label: 'Total Goals',   
          value: realGoals.length,                  
          icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><circle cx="12" cy="12" r="6"></circle><circle cx="12" cy="12" r="2"></circle></svg>, 
          color: 'var(--text-primary)' 
        },
        { 
          label: 'Achievement',   
          value: `${Math.round(totalAchievement)}%`, 
          icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline><polyline points="17 6 23 6 23 12"></polyline></svg>, 
          color: 'var(--brand)' 
        },
        { 
          label: 'Active',        
          value: approved,                           
          icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>, 
          color: 'var(--success)' 
        },
        { 
          label: 'Pending',       
          value: pending,                            
          icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>, 
          color: 'var(--warning)' 
        },
      ].map(stat => (
        <div key={stat.label} className="metric-card">
          <div className="flex items-center gap-2">
            <span style={{ color: 'var(--text-muted)' }}>{stat.icon}</span>
            <span className="label-caption">{stat.label}</span>
          </div>
          <div className="metric-value mt-3" style={{ color: stat.color }}>{stat.value}</div>
        </div>
      ))}
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function GoalsPage() {
  const qc = useQueryClient()
  const { isOnline } = useOnlineStatus()
  const { pendingCount } = useSyncStatus()

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['goals'],
    queryFn: fetchGoals,
    refetchInterval: isOnline ? 15_000 : false,
    retry: isOnline ? 2 : 0,
  })

  const goals = data?.goals ?? []
  const [checkInGoal, setCheckInGoal] = useState<Goal | null>(null)
  const [showCreate, setShowCreate] = useState(false)

  function handleCheckInSuccess() {
    setCheckInGoal(null)
    qc.invalidateQueries({ queryKey: ['goals'] })
  }

  function handleCreateSuccess(wasOffline: boolean) {
    setShowCreate(false)
    if (!wasOffline) {
      qc.invalidateQueries({ queryKey: ['goals'] })
    }
    // If offline, the optimistic cache update in CreateGoalForm already updated the list
  }

  return (
    <div className="max-w-4xl space-y-5" style={{ gap: '20px' }}>
      {/* ── Page header ──────────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-4 mb-5">
        <div>
          <h1 className="h1-title tracking-tight">My Goals</h1>
          <div className="flex items-center gap-2 mt-1">
            <span className="text-[13px]" style={{ color: 'var(--text-secondary)' }}>Q2 2025</span>
            {pendingCount > 0 && (
              <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold px-2 py-0.5 rounded-full" style={{ backgroundColor: 'var(--warning-surface)', color: 'var(--warning)', border: '1px solid rgba(245,158,11,0.2)' }}>
                <span className="w-1.5 h-1.5 rounded-full sync-pulse inline-block" style={{ backgroundColor: 'var(--warning)' }} />
                {pendingCount} pending sync
              </span>
            )}
          </div>
        </div>

        <button
          onClick={() => setShowCreate(true)}
          className="btn-primary inline-flex items-center gap-2"
        >
          <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
          </svg>
          New Goal
        </button>
      </div>

      {/* ── Stats summary ─────────────────────────────────────────────────────── */}
      {!isLoading && goals.length > 0 && <GoalStats goals={goals} />}

      {/* ── Goals list ───────────────────────────────────────────────────────── */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map(i => <GoalSkeleton key={i} />)}
        </div>
      ) : isError ? (
        <div className="base-card p-10 text-center" style={{ borderColor: 'rgba(239,68,68,0.3)' }}>
          <div className="w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-3" style={{ backgroundColor: 'var(--danger-surface)' }}>
            <svg className="w-6 h-6" style={{ color: 'var(--danger)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <p className="font-medium text-[14px] mb-1" style={{ color: 'var(--text-primary)' }}>Failed to load goals</p>
          <p className="text-[12px] mb-4" style={{ color: 'var(--text-secondary)' }}>{isOnline ? 'Server error — try again' : 'You are offline'}</p>
          {isOnline && (
            <button
              onClick={() => refetch()}
              className="btn-secondary text-[12px]"
            >
              Retry
            </button>
          )}
        </div>
      ) : goals.length === 0 ? (
        <div className="base-card p-14 text-center">
          <div className="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-4" style={{ backgroundColor: 'var(--surface-hover)' }}>
            <svg className="w-7 h-7" style={{ color: 'var(--text-muted)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <p className="font-semibold text-[14px] mb-1" style={{ color: 'var(--text-primary)' }}>No goals yet</p>
          <p className="text-[12px] mb-5" style={{ color: 'var(--text-secondary)' }}>Create your first goal to start tracking progress</p>
          <button
            onClick={() => setShowCreate(true)}
            className="btn-primary inline-flex items-center gap-2"
          >
            <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
            </svg>
            Create your first goal
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {goals.map(goal => (
            <GoalCard 
              key={goal._id} 
              goal={goal} 
              onCheckIn={setCheckInGoal} 
              onSubmitForApproval={(goalId) => {
                if (!isOnline) {
                  alert('You must be online to submit a goal for approval.')
                  return
                }
                fetch(`/api/goals/${goalId}/submit`, { method: 'POST' })
                  .then(res => {
                    if (res.ok) qc.invalidateQueries({ queryKey: ['goals'] })
                    else alert('Error submitting goal')
                  })
              }} 
            />
          ))}
        </div>
      )}

      {/* ── Modals ───────────────────────────────────────────────────────────── */}
      {showCreate && (
        <CreateGoalForm
          onClose={() => setShowCreate(false)}
          onSuccess={handleCreateSuccess}
        />
      )}

      {checkInGoal && (
        <CheckInForm
          goal={checkInGoal}
          onClose={() => setCheckInGoal(null)}
          onSuccess={handleCheckInSuccess}
        />
      )}
    </div>
  )
}