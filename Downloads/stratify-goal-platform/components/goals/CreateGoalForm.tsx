// components/goals/CreateGoalForm.tsx
'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { mutateOfflineAware } from '@/lib/offline/mutateOfflineAware'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import { useSession } from 'next-auth/react'
import { z } from 'zod'
import { goalSchema } from '@/lib/validators/goal'

// Use the output type (after zod defaults are applied) for the form
type GoalFormValues = z.output<typeof goalSchema> & { employeeId?: string }

interface Props {
  onClose: () => void
  onSuccess: (wasOffline: boolean) => void
}

type SubmitState = 'idle' | 'submitting' | 'queued' | 'done' | 'error'

export function CreateGoalForm({ onClose, onSuccess }: Props) {
  const qc = useQueryClient()
  const { isOnline } = useOnlineStatus()
  const { data: session } = useSession()
  const userRole = (session?.user as any)?.role

  const [submitState, setSubmitState] = useState<SubmitState>('idle')
  const [errorMsg, setErrorMsg] = useState('')
  const [weightageDisplay, setWeightageDisplay] = useState(20)
  const [role, setRole] = useState('Employee')
  const [focusArea, setFocusArea] = useState('')

  const { data: employeesData } = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const res = await fetch('/api/users/employees')
      if (!res.ok) throw new Error('Failed to fetch employees')
      return res.json()
    },
    enabled: userRole === 'MANAGER' || userRole === 'ADMIN',
  })
  
  const employees = employeesData?.employees || []

  const { data: aiData, refetch: generateAI, isFetching: aiFetching } = useQuery({
    queryKey: ['ai-suggestions', role, focusArea],
    queryFn: async () => {
      if (!focusArea) throw new Error('Focus area is required')
      const res = await fetch('/api/ai/suggest-goals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role, focusArea })
      })
      if (!res.ok) throw new Error('Failed to generate suggestions')
      return res.json()
    },
    enabled: false, // only run on click
  })

  const { data: weightageData } = useQuery({
    queryKey: ['weightage-remaining', 'Q2-2025'],
    queryFn: async () => {
      const res = await fetch('/api/goals/weightage-remaining?quarterId=Q2-2025')
      if (!res.ok) throw new Error('Failed to fetch weightage')
      return res.json()
    },
  })

  const remainingWeightage = weightageData?.remaining ?? 100

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<GoalFormValues, unknown, GoalFormValues>({
    resolver: zodResolver(goalSchema) as import('react-hook-form').Resolver<GoalFormValues>,
    defaultValues: {
      title: '',
      description: '',
      weightage: 20,
      targetValue: 100,
      quarterId: 'Q2-2025',
    },
  })

  async function onSubmit(values: GoalFormValues) {
    setSubmitState('submitting')
    setErrorMsg('')

    // Optimistic update: immediately add a placeholder goal to the cache
    const optimisticGoal = {
      // eslint-disable-next-line react-hooks/purity
      _id: `temp-${Date.now()}`,
      title: values.title,
      description: values.description ?? '',
      weightage: values.weightage,
      targetValue: values.targetValue,
      quarterId: values.quarterId,
      status: 'DRAFT',
      currentValue: 0,
      achievementScore: 0,
      _isPending: true,
    }

    qc.setQueryData(['goals'], (old: { goals: unknown[] } | undefined) => ({
      goals: [optimisticGoal, ...(old?.goals ?? [])],
    }))

    try {
      const result = await mutateOfflineAware({
        endpoint: '/api/goals',
        method: 'POST',
        payload: {
          title: values.title,
          description: values.description ?? '',
          weightage: values.weightage,
          targetValue: values.targetValue,
          quarterId: values.quarterId ?? 'Q2-2025',
          ...((userRole === 'MANAGER' || userRole === 'ADMIN') && values.employeeId ? { employeeId: values.employeeId } : {}),
        },
        onSuccess: () => {
          // Server confirmed — refetch to replace optimistic entry with real data
          qc.invalidateQueries({ queryKey: ['goals'] })
        },
      })

      if (!result.online) {
        // Queued offline — show the queued state briefly then close
        setSubmitState('queued')
        setTimeout(() => onSuccess(true), 2500)
      } else {
        setSubmitState('done')
        setTimeout(() => onSuccess(false), 800)
      }
    } catch (err) {
      // Roll back optimistic update on hard error
      qc.invalidateQueries({ queryKey: ['goals'] })
      setSubmitState('error')
      setErrorMsg(err instanceof Error ? err.message : 'Something went wrong')
    }
  }

  // ── Queued state (offline success) ──────────────────────────────────────────
  if (submitState === 'queued') {
    return (
      <div className="fixed inset-0 z-40 flex items-center justify-center p-4" style={{ backgroundColor: 'var(--surface-overlay)', backdropFilter: 'blur(4px)' }}>
        <div className="base-card w-full max-w-sm p-8 text-center" style={{ borderColor: 'var(--border-subtle)' }}>
          <div className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4" style={{ backgroundColor: 'var(--warning-surface)', border: '2px solid rgba(245,158,11,0.4)' }}>
            <svg className="w-8 h-8 sync-pulse" style={{ color: 'var(--warning)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold mb-2" style={{ color: 'var(--text-primary)' }}>Queued for sync</h3>
          <p className="text-sm leading-relaxed" style={{ color: 'var(--text-secondary)' }}>
            You&apos;re offline. Your goal has been saved locally and will appear in your list. It will sync automatically when you reconnect.
          </p>
          <div className="mt-4 flex items-center justify-center gap-2">
            <span className="w-2 h-2 rounded-full inline-block sync-pulse" style={{ backgroundColor: 'var(--warning)' }} />
            <span className="text-xs font-medium" style={{ color: 'var(--warning)' }}>Pending sync</span>
          </div>
        </div>
      </div>
    )
  }

  // ── Done state (online success flash) ──────────────────────────────────────
  if (submitState === 'done') {
    return (
      <div className="fixed inset-0 z-40 flex items-center justify-center p-4" style={{ backgroundColor: 'var(--surface-overlay)', backdropFilter: 'blur(4px)' }}>
        <div className="base-card w-full max-w-sm p-8 text-center" style={{ borderColor: 'var(--border-subtle)' }}>
          <div className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4" style={{ backgroundColor: 'var(--success-surface)', border: '2px solid rgba(34,197,94,0.4)' }}>
            <svg className="w-8 h-8" style={{ color: 'var(--success)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold mb-1" style={{ color: 'var(--text-primary)' }}>Goal created!</h3>
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>Saved to MongoDB.</p>
        </div>
      </div>
    )
  }

  // ── Create form ─────────────────────────────────────────────────────────────
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center p-4 overflow-y-auto" style={{ backgroundColor: 'var(--surface-overlay)', backdropFilter: 'blur(4px)' }}>
      <div className="base-card !p-0 w-full max-w-4xl overflow-hidden flex flex-col md:flex-row my-auto" style={{ borderColor: 'var(--border-subtle)' }}>
        {/* Form Column */}
        <div className="w-full md:w-1/2 flex flex-col border-b md:border-b-0 md:border-r" style={{ borderColor: 'var(--border-subtle)' }}>
          {/* Header */}
          <div className="px-6 pt-6 pb-4 flex items-center justify-between" style={{ borderBottom: '1px solid var(--border-subtle)' }}>
          <div>
            <h2 className="font-semibold text-[16px]" style={{ color: 'var(--text-primary)' }}>Create New Goal</h2>
            <p className="text-[12px] mt-0.5" style={{ color: 'var(--text-secondary)' }}>Q2 2025</p>
          </div>
          <div className="flex items-center gap-3">
            {/* Online indicator */}
            <div className="flex items-center gap-1.5">
              <span className={`w-1.5 h-1.5 rounded-full inline-block ${isOnline ? 'bg-green-400' : 'bg-amber-400 sync-pulse'}`} />
              <span className={`text-[10px] font-medium ${isOnline ? 'text-green-400' : 'text-amber-400'}`}>
                {isOnline ? 'Online' : 'Offline'}
              </span>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1 rounded-lg transition"
              style={{ color: 'var(--text-secondary)' }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--surface-hover)'}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        {/* Offline notice */}
        {!isOnline && (
          <div className="mx-6 mt-4 flex items-center gap-2.5 rounded-xl px-4 py-3" style={{ backgroundColor: 'var(--warning-surface)', border: '1px solid rgba(245,158,11,0.3)' }}>
            <svg className="w-4 h-4 flex-shrink-0" style={{ color: 'var(--warning)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M18.364 5.636a9 9 0 010 12.728M15.536 8.464a5 5 0 010 7.072M12 12h.01M8.464 15.536a5 5 0 010-7.072M5.636 18.364a9 9 0 010-12.728" />
            </svg>
            <p className="text-xs font-medium" style={{ color: 'var(--warning)' }}>
              Offline — goal will be queued and synced when you reconnect
            </p>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
          {/* Title */}
          <div>
            <label className="label-caption block mb-1.5">
              Title <span style={{ color: 'var(--danger)' }}>*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Complete 50 customer discovery calls"
              autoFocus
              {...register('title')}
              className="input-field w-full"
            />
            {errors.title && (
              <p className="mt-1 text-xs" style={{ color: 'var(--danger)' }}>{errors.title.message}</p>
            )}
          </div>

          {/* Assign To (Manager Only) */}
          {(userRole === 'MANAGER' || userRole === 'ADMIN') && (
            <div>
              <label className="label-caption block mb-1.5">
                Assign To Employee <span style={{ color: 'var(--danger)' }}>*</span>
              </label>
              <select
                {...register('employeeId')}
                required
                className="input-field w-full appearance-none bg-no-repeat"
                style={{
                  backgroundImage: `url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e")`,
                  backgroundPosition: 'right 0.5rem center',
                  backgroundSize: '1.5em 1.5em',
                  paddingRight: '2.5rem'
                }}
              >
                <option value="">Select Employee</option>
                {employees.map((emp: any) => (
                  <option key={emp._id} value={emp._id}>{emp.name}</option>
                ))}
              </select>
            </div>
          )}

          {/* Description */}
          <div>
            <label className="label-caption block mb-1.5">
              Description <span style={{ color: 'var(--text-muted)' }}>(optional)</span>
            </label>
            <textarea
              rows={2}
              placeholder="What does success look like?"
              {...register('description')}
              className="input-field w-full resize-none"
            />
          </div>

          {/* Weightage + Target */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label-caption block mb-1.5">
                Weightage (%)
              </label>
              <input
                type="number"
                min={1}
                max={100}
                {...register('weightage', {
                  valueAsNumber: true,
                  onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
                    setWeightageDisplay(Number(e.target.value))
                  },
                })}
                className="input-field w-full"
              />
              {errors.weightage && (
                <p className="mt-1 text-xs" style={{ color: 'var(--danger)' }}>{errors.weightage.message}</p>
              )}
              {/* Weightage visual indicator */}
              <div className="mt-2 h-1 rounded-full overflow-hidden" style={{ backgroundColor: 'var(--surface-hover)' }}>
                <div
                  className="h-full rounded-full transition-all duration-300"
                  style={{ 
                    width: `${Math.min(weightageDisplay, 100)}%`,
                    backgroundColor: weightageDisplay > remainingWeightage ? 'var(--danger)' : 'var(--success)'
                  }}
                />
              </div>
              {weightageData && (
                <p className="mt-1.5 text-[11px] font-medium" style={{ color: weightageDisplay > remainingWeightage ? 'var(--danger)' : 'var(--text-muted)' }}>
                  {remainingWeightage}% weightage remaining this quarter
                </p>
              )}
            </div>
            <div>
              <label className="label-caption block mb-1.5">
                Target value
              </label>
              <input
                type="number"
                min={1}
                {...register('targetValue', { valueAsNumber: true })}
                className="input-field w-full"
              />
              {errors.targetValue && (
                <p className="mt-1 text-xs" style={{ color: 'var(--danger)' }}>{errors.targetValue.message}</p>
              )}
            </div>
          </div>

          {/* Error message */}
          {submitState === 'error' && errorMsg && (
            <div className="flex items-center gap-2.5 rounded-xl px-4 py-3" style={{ backgroundColor: 'var(--danger-surface)', border: '1px solid rgba(239,68,68,0.3)' }}>
              <svg className="w-4 h-4 flex-shrink-0" style={{ color: 'var(--danger)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <p className="text-sm" style={{ color: 'var(--danger)' }}>{errorMsg}</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="btn-secondary flex-1"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || submitState === 'submitting'}
              className="btn-primary flex-1 flex items-center justify-center gap-2"
            >
              {isSubmitting || submitState === 'submitting' ? (
                <>
                  <svg className="sync-pulse w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  {isOnline ? 'Saving…' : 'Queuing…'}
                </>
              ) : (
                isOnline ? 'Create Goal' : 'Queue Goal'
              )}
            </button>
          </div>
        </form>
        </div>

        {/* AI Column */}
        <div className="w-full md:w-1/2 p-6 flex flex-col" style={{ backgroundColor: 'var(--surface-hover)' }}>
          <div className="flex items-center gap-2 mb-4">
            <svg className="w-5 h-5" style={{ color: 'var(--brand)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            <h3 className="font-semibold" style={{ color: 'var(--text-primary)' }}>AI Goal Suggestions</h3>
          </div>
          
          <div className="space-y-4 mb-6">
            <div>
              <label className="label-caption block mb-1">Role</label>
              <select 
                value={role} 
                onChange={e => setRole(e.target.value)}
                className="input-field w-full appearance-none"
                style={{
                  backgroundImage: `url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e")`,
                  backgroundPosition: 'right 0.5rem center',
                  backgroundSize: '1.5em 1.5em',
                  paddingRight: '2.5rem'
                }}
              >
                <option value="Employee">Employee</option>
                <option value="Manager">Manager</option>
                <option value="Senior">Senior</option>
                <option value="Lead">Lead</option>
              </select>
            </div>
            <div>
              <label className="label-caption block mb-1">Focus Area</label>
              <input 
                type="text" 
                value={focusArea}
                onChange={e => setFocusArea(e.target.value)}
                placeholder="e.g. Q2 sales targets, system reliability"
                className="input-field w-full"
              />
            </div>
            <button 
              type="button"
              onClick={() => generateAI()}
              disabled={aiFetching || !focusArea}
              className="btn-primary w-full flex items-center justify-center"
              style={{ opacity: (aiFetching || !focusArea) ? 0.5 : 1 }}
            >
              {aiFetching ? 'Generating...' : 'Generate Suggestions'}
            </button>
          </div>

          <div className="flex-1 overflow-y-auto pr-2 space-y-3">
            {aiFetching && (
              <div className="space-y-3">
                {[1, 2, 3].map(i => (
                  <div key={i} className="animate-pulse rounded-xl p-4" style={{ backgroundColor: 'var(--surface-raised)', border: '1px solid var(--border-subtle)' }}>
                    <div className="h-4 rounded w-3/4 mb-2" style={{ backgroundColor: 'var(--surface-hover)' }}></div>
                    <div className="h-3 rounded w-full mb-1" style={{ backgroundColor: 'var(--surface-hover)' }}></div>
                    <div className="h-3 rounded w-5/6" style={{ backgroundColor: 'var(--surface-hover)' }}></div>
                  </div>
                ))}
              </div>
            )}
            
            {!aiFetching && aiData?.suggestions?.map((s: any, i: number) => (
              <div key={i} className="rounded-xl p-4 transition group cursor-pointer" style={{ backgroundColor: 'var(--surface-raised)', border: '1px solid var(--border-subtle)' }}
                onMouseEnter={(e) => e.currentTarget.style.borderColor = 'var(--brand)'}
                onMouseLeave={(e) => e.currentTarget.style.borderColor = 'var(--border-subtle)'}
                onClick={() => {
                  setValue('title', s.title)
                  setValue('description', s.description)
                }}
              >
                <h4 className="text-[14px] font-semibold mb-1" style={{ color: 'var(--text-primary)' }}>{s.title}</h4>
                <p className="text-[12px] mb-3" style={{ color: 'var(--text-secondary)' }}>{s.description}</p>
                <button 
                  type="button"
                  className="text-[12px] font-medium opacity-0 group-hover:opacity-100 transition"
                  style={{ color: 'var(--brand)' }}
                >
                  Use this goal &rarr;
                </button>
              </div>
            ))}
            
            {!aiFetching && !aiData && (
              <div className="h-full flex items-center justify-center text-center text-[13px] px-4" style={{ color: 'var(--text-muted)' }}>
                Describe your focus area and generate AI suggestions for quantified OKRs.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}