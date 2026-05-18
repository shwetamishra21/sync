// app/login/page.tsx
'use client'

import { useState, useEffect, Suspense } from 'react'
import { signIn, useSession } from 'next-auth/react'
import { useRouter, useSearchParams } from 'next/navigation'

function LoginContent() {
  const { data: session, status } = useSession()
  const router = useRouter()
  const searchParams = useSearchParams()

  const defaultMode = searchParams.get('mode') === 'register' ? false : true
  const defaultRole = searchParams.get('role') || 'EMPLOYEE'

  const [isLogin, setIsLogin] = useState(defaultMode)
  const [isForgotPassword, setIsForgotPassword] = useState(false)
  const [forgotPasswordSuccess, setForgotPasswordSuccess] = useState(false)
  
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [role, setRole] = useState(defaultRole)
  const [managerId, setManagerId] = useState('')
  const [managers, setManagers] = useState<{_id: string, name: string}[]>([])
  
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (session) router.replace('/dashboard')
  }, [session, router])

  useEffect(() => {
    if (!isLogin && role === 'EMPLOYEE') {
      fetch('/api/users/managers')
        .then(res => res.json())
        .then(data => {
          if (data.managers) {
            setManagers(data.managers)
            if (data.managers.length > 0 && !managerId) {
              setManagerId(data.managers[0]._id)
            }
          }
        })
        .catch(err => console.error('Failed to fetch managers:', err))
    }
  }, [isLogin, role])

  async function handleForgotPassword(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError('')
    setForgotPasswordSuccess(false)
    
    // Mock forgot password API call
    setTimeout(() => {
      setLoading(false)
      if (!email) {
        setError('Please enter your email address')
        return
      }
      setForgotPasswordSuccess(true)
    }, 1000)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError('')

    if (isLogin) {
      const result = await signIn('credentials', {
        email,
        password,
        redirect: false,
      })

      setLoading(false)
      if (result?.error) {
        setError('Invalid email or password')
      } else {
        router.replace('/dashboard')
      }
    } else {
      // Registration flow
      try {
        const payload: any = { name, email, password, role }
        if (role === 'EMPLOYEE' && managerId) {
          payload.managerId = managerId
        }

        const res = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        })

        if (!res.ok) {
          const data = await res.json()
          throw new Error(data.error || 'Registration failed')
        }

        // Auto sign-in after registration
        const result = await signIn('credentials', {
          email,
          password,
          redirect: false,
        })

        setLoading(false)
        if (result?.error) {
          setError('Account created, but automatic sign-in failed. Please sign in.')
          setIsLogin(true)
        } else {
          router.replace('/dashboard')
        }
      } catch (err: any) {
        setLoading(false)
        setError(err.message)
      }
    }
  }

  if (status === 'loading') {
    return <div className="min-h-screen flex items-center justify-center">Loading…</div>
  }

  if (isForgotPassword) {
    return (
      <div className="w-full" style={{ maxWidth: '360px' }}>
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl mb-4" style={{ backgroundColor: 'var(--brand-muted)', color: 'var(--brand)' }}>
            <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
            </svg>
          </div>
          <h1 className="h1-title">Reset Password</h1>
          <p className="text-[13px] text-[var(--text-secondary)] mt-1">Enter your email to receive a reset link</p>
        </div>

        <div className="base-card">
          {forgotPasswordSuccess ? (
            <div className="text-center">
              <div className="p-3 mb-4 rounded-md" style={{ backgroundColor: 'var(--success-surface)', color: 'var(--success)', border: '1px solid rgba(34,197,94,0.2)' }}>
                <p className="text-[13px]">Password reset link sent! Check your email.</p>
              </div>
              <button 
                onClick={() => { setIsForgotPassword(false); setForgotPasswordSuccess(false); setError(''); }}
                className="btn-secondary w-full"
              >
                Back to Sign In
              </button>
            </div>
          ) : (
            <form onSubmit={handleForgotPassword} className="space-y-5">
              <div>
                <label className="input-label">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                  className="input-field w-full"
                  placeholder="sarah@example.com"
                />
              </div>

              {error && (
                <div className="p-3 rounded-md" style={{ backgroundColor: 'var(--danger-surface)', color: 'var(--danger)', fontSize: '13px' }}>
                  {error}
                </div>
              )}

              <div className="flex flex-col gap-3">
                <button
                  type="submit"
                  disabled={loading}
                  className="btn-primary w-full"
                >
                  {loading ? 'Sending...' : 'Send Reset Link'}
                </button>
                <button
                  type="button"
                  onClick={() => { setIsForgotPassword(false); setError(''); }}
                  className="text-[13px]"
                  style={{ color: 'var(--text-secondary)' }}
                >
                  Back to Sign In
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="w-full" style={{ maxWidth: '360px' }}>
      {/* Logo / Brand */}
      <div className="text-center mb-8">
        <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl mb-4" style={{ backgroundColor: 'var(--brand-muted)', color: 'var(--brand)' }}>
          <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h1 className="h1-title">Stratify</h1>
        <p className="text-[13px] text-[var(--text-secondary)] mt-1">Enterprise Goal Tracking</p>
      </div>

      <div className="base-card">
        <form onSubmit={handleSubmit} className="space-y-5">
          {!isLogin && (
            <div>
              <label className="input-label">Full Name</label>
              <input
                type="text"
                value={name}
                onChange={e => setName(e.target.value)}
                required={!isLogin}
                className="input-field w-full"
                placeholder="Sarah Smith"
              />
            </div>
          )}
          
          <div>
            <label className="input-label">Email</label>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
              className="input-field w-full"
              placeholder="sarah@example.com"
            />
          </div>

          <div>
            <div className="flex justify-between items-center mb-1.5">
              <label className="input-label !mb-0">Password</label>
              {isLogin && (
                <button 
                  type="button" 
                  onClick={() => { setIsForgotPassword(true); setError(''); }}
                  className="text-[11px] font-medium hover:underline"
                  style={{ color: 'var(--brand)' }}
                >
                  Forgot Password?
                </button>
              )}
            </div>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              className="input-field w-full"
              placeholder="••••••••"
              minLength={6}
            />
          </div>

          {!isLogin && (
            <div>
              <label className="input-label">Role</label>
              <select
                value={role}
                onChange={e => setRole(e.target.value)}
                className="input-field w-full"
                style={{ appearance: 'none', backgroundColor: 'var(--surface-inset)' }}
              >
                <option value="EMPLOYEE">Employee</option>
                <option value="MANAGER">Manager</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
          )}

          {!isLogin && role === 'EMPLOYEE' && (
            <div>
              <label className="input-label">Assign Manager</label>
              <select
                value={managerId}
                onChange={e => setManagerId(e.target.value)}
                className="input-field w-full"
                style={{ appearance: 'none', backgroundColor: 'var(--surface-inset)' }}
                required
              >
                {managers.length === 0 && <option value="">No managers available</option>}
                {managers.map(m => (
                  <option key={m._id} value={m._id}>{m.name}</option>
                ))}
              </select>
            </div>
          )}

          {error && (
            <div className="p-3 rounded-md" style={{ backgroundColor: 'var(--danger-surface)', color: 'var(--danger)', fontSize: '13px' }}>
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="btn-primary w-full"
          >
            {loading ? (isLogin ? 'Signing in…' : 'Creating account…') : (isLogin ? 'Sign in' : 'Create Account')}
          </button>
        </form>

        <div className="mt-6 text-center">
          {isLogin ? (
            <p className="text-[13px]" style={{ color: 'var(--text-secondary)' }}>
              Don't have an account?{' '}
              <button 
                onClick={() => { setIsLogin(false); setError(''); }} 
                className="font-medium hover:underline"
                style={{ color: 'var(--text-primary)' }}
              >
                Register now
              </button>
            </p>
          ) : (
            <p className="text-[13px]" style={{ color: 'var(--text-secondary)' }}>
              Already have an account?{' '}
              <button 
                onClick={() => { setIsLogin(true); setError(''); }} 
                className="font-medium hover:underline"
                style={{ color: 'var(--text-primary)' }}
              >
                Sign in
              </button>
            </p>
          )}
        </div>
      </div>
    </div>
  )
}

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: 'var(--surface-base)' }}>
      <Suspense fallback={<div className="min-h-screen flex items-center justify-center" style={{ color: 'var(--text-muted)' }}>Loading…</div>}>
        <LoginContent />
      </Suspense>
    </div>
  )
}