'use client'

import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import Link from 'next/link'

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
  userId: { _id: string; name: string; email: string }
}

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

async function fetchTeamGoals(): Promise<{ goals: Goal[] }> {
  const res = await fetch('/api/goals?team=true')
  if (!res.ok) throw new Error('Failed to fetch team goals')
  return res.json()
}

function TeamGoalCard({ goal }: { goal: Goal }) {
  const pct = goal.targetValue > 0
    ? Math.min((goal.currentValue / goal.targetValue) * 100, 100)
    : 0

  const badgeClass = STATUS_CLASSES[goal.status] ?? STATUS_CLASSES['DRAFT']
  const label = STATUS_LABELS[goal.status] ?? goal.status

  return (
    <div className="base-card p-4">
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
            <span className="inline-flex items-center gap-1.5 text-[11px] font-medium px-2 py-0.5 rounded-full" style={{ backgroundColor: 'var(--brand-muted)', color: 'var(--brand)' }}>
              <svg width="10" height="10" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              {goal.userId?.name || 'Unknown User'}
            </span>
            {goal.riskScore > 50 && (
              <span className="inline-flex items-center text-[10px] font-semibold px-2 py-0.5 rounded-full gap-1" style={{ backgroundColor: 'var(--danger-surface)', color: 'var(--danger)', border: '1px solid rgba(239,68,68,0.2)' }}>
                High Risk
              </span>
            )}
            <span className="label-caption">Weight: {goal.weightage}%</span>
          </div>
        </div>
      </div>

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

      <div className="flex items-center justify-between mt-2">
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
  )
}

function TeamStats({ goals }: { goals: Goal[] }) {
  const totalGoals = goals.length
  const totalAchievement = totalGoals > 0
    ? goals.reduce((sum, g) => sum + (g.achievementScore ?? 0), 0) / totalGoals
    : 0
  const highRisk = goals.filter(g => g.riskScore > 50).length
  const completed = goals.filter(g => g.status === 'COMPLETED').length

  return (
    <div className="grid grid-cols-2 gap-5 sm:grid-cols-4 mb-5">
      {[
        { 
          label: 'Team Goals',   
          value: totalGoals,                  
          icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 00-3-3.87"></path><path d="M16 3.13a4 4 0 010 7.75"></path></svg>, 
          color: 'var(--text-primary)' 
        },
        { 
          label: 'Avg Achievement',   
          value: `${Math.round(totalAchievement)}%`, 
          icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline><polyline points="17 6 23 6 23 12"></polyline></svg>, 
          color: 'var(--brand)' 
        },
        { 
          label: 'Completed',        
          value: completed,                           
          icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>, 
          color: 'var(--success)' 
        },
        { 
          label: 'High Risk',       
          value: highRisk,                            
          icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>, 
          color: 'var(--danger)' 
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

export function TeamGoalsPage() {
  const { isOnline } = useOnlineStatus()
  const qc = useQueryClient()

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['team-goals'],
    queryFn: fetchTeamGoals,
    refetchInterval: isOnline ? 30_000 : false,
    retry: isOnline ? 2 : 0,
  })

  const goals = data?.goals ?? []

  // Group goals by user
  const groupedGoals = goals.reduce((acc, goal) => {
    const userId = goal.userId?._id || 'unknown'
    if (!acc[userId]) {
      acc[userId] = {
        user: goal.userId,
        goals: []
      }
    }
    acc[userId].goals.push(goal)
    return acc
  }, {} as Record<string, { user: { name: string; email: string }, goals: Goal[] }>)

  return (
    <div className="max-w-4xl space-y-5" style={{ gap: '20px' }}>
      <div className="flex items-center justify-between gap-4 mb-5">
        <div>
          <h1 className="h1-title tracking-tight">Team Goals</h1>
          <div className="flex items-center gap-2 mt-1">
            <span className="text-[13px]" style={{ color: 'var(--text-secondary)' }}>Track direct reports&apos; OKRs</span>
          </div>
        </div>
      </div>

      {!isLoading && goals.length > 0 && <TeamStats goals={goals} />}

      {isLoading ? (
        <div className="space-y-3">
          <div className="base-card p-4 animate-pulse h-24"></div>
          <div className="base-card p-4 animate-pulse h-24"></div>
        </div>
      ) : isError ? (
        <div className="base-card p-10 text-center" style={{ borderColor: 'rgba(239,68,68,0.3)' }}>
          <div className="w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-3" style={{ backgroundColor: 'var(--danger-surface)' }}>
            <svg className="w-6 h-6" style={{ color: 'var(--danger)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <p className="font-medium text-[14px] mb-1" style={{ color: 'var(--text-primary)' }}>Failed to load team goals</p>
          {isOnline && (
            <button
              onClick={() => refetch()}
              className="btn-secondary text-[12px] mt-4"
            >
              Retry
            </button>
          )}
        </div>
      ) : goals.length === 0 ? (
        <div className="base-card p-14 text-center">
          <p className="font-semibold text-[14px] mb-1" style={{ color: 'var(--text-primary)' }}>No team goals yet</p>
          <p className="text-[12px]" style={{ color: 'var(--text-secondary)' }}>Your team members have not created any goals, or you haven&apos;t assigned any yet.</p>
        </div>
      ) : (
        <div className="space-y-8">
          {Object.values(groupedGoals).map(({ user, goals }) => (
            <div key={user?.name || 'Unknown'}>
              <h3 className="text-[14px] font-semibold mb-3 flex items-center gap-2" style={{ color: 'var(--text-primary)' }}>
                <div className="w-6 h-6 rounded-full flex items-center justify-center font-medium text-[10px]" style={{ backgroundColor: 'var(--brand-muted)', color: 'var(--brand)' }}>
                  {user?.name?.[0]?.toUpperCase() ?? 'U'}
                </div>
                {user?.name || 'Unknown User'}
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {goals.map(goal => (
                  <TeamGoalCard key={goal._id} goal={goal} />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
