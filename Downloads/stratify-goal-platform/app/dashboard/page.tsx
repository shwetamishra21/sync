// app/dashboard/page.tsx
'use client'

import { useSession } from 'next-auth/react'
import { useQuery } from '@tanstack/react-query'
import Link from 'next/link'
import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts'

interface Goal {
  _id: string
  title: string
  weightage: number
  status: string
  targetValue: number
  currentValue: number
  achievementScore: number
}

function fetchGoals(): Promise<{ goals: Goal[] }> {
  return fetch('/api/goals').then(r => r.json())
}

const STATUS_CLASSES: Record<string, string> = {
  DRAFT: 'status-badge status-draft',
  PENDING_APPROVAL: 'status-badge status-pending',
  APPROVED: 'status-badge status-approved',
  LOCKED: 'status-badge status-locked',
  REJECTED: 'status-badge status-rejected',
  COMPLETED: 'status-badge status-completed',
}

export default function DashboardPage() {
  const { data: session } = useSession()
  const user = session?.user as { name?: string; role?: string }

  const { data, isLoading } = useQuery({
    queryKey: ['goals'],
    queryFn: fetchGoals,
    refetchInterval: 10_000,
  })

  const { data: overview } = useQuery({
    queryKey: ['dashboard-overview'],
    queryFn: () => fetch('/api/dashboard/overview').then(r => r.json()),
  })

  const goals = data?.goals ?? []
  const totalAchievement = goals
    .filter(g => ['APPROVED', 'LOCKED', 'COMPLETED'].includes(g.status))
    .reduce((sum, g) => sum + (g.achievementScore ?? 0), 0)

  const pending = goals.filter(g => g.status === 'PENDING_APPROVAL').length
  const approved = goals.filter(g => g.status === 'APPROVED').length

  const pieData = [
    { name: 'Achieved', value: Math.round(totalAchievement) },
    { name: 'Remaining', value: Math.max(0, 100 - Math.round(totalAchievement)) }
  ]

  return (
    <div className="space-y-5 max-w-4xl" style={{ gap: '20px' }}>
      {/* Welcome */}
      <div className="flex items-center justify-between mb-5">
        <div>
          <h1 className="h1-title">
            Good day, {user?.name?.split(' ')[0]} 
          </h1>
          <p className="text-[13px] mt-1" style={{ color: 'var(--text-secondary)' }}>Q2 2025 · Here&apos;s your goal overview</p>
        </div>
        {overview?.daysRemaining !== undefined && (
          <div className="px-4 py-2 rounded-lg text-sm font-medium" style={{ backgroundColor: 'var(--danger-surface)', color: 'var(--danger)', border: '1px solid rgba(239,68,68,0.2)' }}>
            {overview.daysRemaining} days remaining in quarter
          </div>
        )}
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-2 gap-5 sm:grid-cols-4">
        <KpiCard 
          label="Total Goals" 
          value={goals.length} 
          valueColor="var(--text-primary)"
          icon={
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"></circle><circle cx="12" cy="12" r="6"></circle><circle cx="12" cy="12" r="2"></circle>
            </svg>
          } 
        />
        <KpiCard
          label="Achievement"
          value={`${Math.round(totalAchievement)}%`}
          valueColor="var(--brand)"
          icon={
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline><polyline points="17 6 23 6 23 12"></polyline>
            </svg>
          }
        />
        <KpiCard 
          label="This Week's Check-ins" 
          value={overview?.recentCheckIns ?? 0} 
          valueColor="var(--text-primary)"
          icon={
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
            </svg>
          } 
        />
        <KpiCard 
          label="Pending Approval" 
          value={pending} 
          valueColor="var(--warning)"
          icon={
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline>
            </svg>
          } 
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {/* Quarter Progress Donut */}
        <div className="base-card flex flex-col items-center justify-center h-[340px]">
          <h3 className="h2-title w-full text-left mb-4">Quarter Progress</h3>
          <div className="h-40 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={70}
                  paddingAngle={2}
                  dataKey="value"
                  stroke="none"
                >
                  <Cell fill="var(--brand)" />
                  <Cell fill="var(--surface-hover)" />
                </Pie>
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-4 text-center">
            <span className="metric-value">{Math.round(totalAchievement)}%</span>
            <span className="text-[13px] block mt-1" style={{ color: 'var(--text-secondary)' }}>Overall Achievement</span>
          </div>
        </div>

        {/* Goals list */}
        <div className="base-card !p-0 overflow-hidden md:col-span-2 h-[340px] flex flex-col">
          <div className="flex items-center justify-between px-5 py-4" style={{ borderBottom: '1px solid var(--border-subtle)' }}>
            <h2 className="h2-title">My Goals — Q2 2025</h2>
            <Link
              href="/dashboard/goals"
              className="text-[13px] font-medium transition-colors"
              style={{ color: 'var(--brand)' }}
            >
              View all →
            </Link>
          </div>

          {isLoading ? (
            <div className="p-5 space-y-3">
              {[1, 2, 3].map(i => (
                <div key={i} className="h-[60px] rounded-md" style={{ backgroundColor: 'var(--surface-hover)', animation: 'pulse 1.5s cubic-bezier(0.4,0,0.6,1) infinite' }} />
              ))}
            </div>
          ) : goals.length === 0 ? (
            <div className="p-10 text-center text-[13px]" style={{ color: 'var(--text-muted)' }}>
              No goals yet.{' '}
              <Link href="/dashboard/goals" style={{ color: 'var(--brand)' }} className="hover:underline">
                Create your first goal
              </Link>
            </div>
          ) : (
            <ul className="flex-1 overflow-y-auto">
              {goals.slice(0, 5).map(goal => {
                const pct = goal.targetValue > 0
                  ? Math.min((goal.currentValue / goal.targetValue) * 100, 100)
                  : 0
                return (
                  <li key={goal._id} className="px-5 py-4 transition-colors" style={{ borderBottom: '1px solid var(--border-subtle)' }}
                      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--surface-hover)'}
                      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}>
                    <div className="flex items-center gap-4">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-2">
                          <p className="text-[13px] font-medium truncate" style={{ color: 'var(--text-primary)' }}>{goal.title}</p>
                          <span className={STATUS_CLASSES[goal.status] ?? STATUS_CLASSES['DRAFT']}>
                            {goal.status.replace('_', ' ')}
                          </span>
                        </div>
                        <div className="flex items-center gap-3">
                          <div className="flex-1 rounded-full overflow-hidden" style={{ height: '3px', backgroundColor: 'var(--surface-hover)' }}>
                            <div
                              className="h-full rounded-full progress-fill"
                              style={{ 
                                width: `${pct}%`, 
                                backgroundColor: pct < 30 ? 'var(--danger)' : pct < 60 ? 'var(--warning)' : pct < 80 ? 'var(--info)' : 'var(--success)'
                              }}
                            />
                          </div>
                          <span className="mono-id w-8 text-right">
                            {Math.round(pct)}%
                          </span>
                        </div>
                      </div>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </div>

      {/* Team Overview for Manager/Admin */}
      {overview?.teamStats && (
        <div className="base-card !p-0 overflow-hidden">
          <div className="px-5 py-4" style={{ borderBottom: '1px solid var(--border-subtle)' }}>
            <h2 className="h2-title">Team Overview</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full base-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Goals</th>
                  <th>Avg Achievement</th>
                  <th>At Risk</th>
                </tr>
              </thead>
              <tbody>
                {overview.teamStats.map((st: any, i: number) => (
                  <tr key={i}>
                    <td style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{st.name}</td>
                    <td>{st.goalCount}</td>
                    <td>
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full" style={{ backgroundColor: 'var(--brand-muted)', color: 'var(--brand)', fontSize: '11px', fontWeight: 600 }}>
                        {st.avgAchievement}%
                      </span>
                    </td>
                    <td>
                      {st.atRiskCount > 0 ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full" style={{ backgroundColor: 'var(--danger-surface)', color: 'var(--danger)', fontSize: '11px', fontWeight: 600 }}>
                          {st.atRiskCount}
                        </span>
                      ) : (
                        <span style={{ color: 'var(--text-muted)' }}>—</span>
                      )}
                    </td>
                  </tr>
                ))}
                {overview.teamStats.length === 0 && (
                  <tr>
                    <td colSpan={4} className="text-center py-8" style={{ color: 'var(--text-muted)' }}>
                      No team members found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

function KpiCard({
  label,
  value,
  valueColor,
  icon,
}: {
  label: string
  value: string | number
  valueColor: string
  icon: React.ReactNode
}) {
  return (
    <div className="metric-card">
      <div className="flex items-center gap-2">
        <span style={{ color: 'var(--text-muted)' }}>{icon}</span>
        <span className="label-caption">{label}</span>
      </div>
      <p className="metric-value mt-3" style={{ color: valueColor }}>{value}</p>
    </div>
  )
}