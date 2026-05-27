// components/ui/DashboardShell.tsx
'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { signOut } from 'next-auth/react'
import type { Session } from 'next-auth'
import { OfflineBanner } from '@/components/offline/OfflineBanner'
import { SyncBadge } from '@/components/offline/SyncBadge'
import { ConflictAlert } from '@/components/offline/ConflictAlert'
import { NotificationBell } from '@/components/layout/NotificationBell'

const navItems = [
  {
    href: '/dashboard',
    label: 'Dashboard',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
        <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
      </svg>
    ),
  },
  {
    href: '/dashboard/goals',
    label: 'My Goals',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
        <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  },
  {
    href: '/dashboard/team',
    label: 'Team Goals',
    roles: ['MANAGER', 'ADMIN'],
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <path d="M23 21v-2a4 4 0 00-3-3.87" />
        <path d="M16 3.13a4 4 0 010 7.75" />
      </svg>
    ),
  },
  {
    href: '/dashboard/approvals',
    label: 'Approvals',
    roles: ['MANAGER', 'ADMIN'],
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
        <path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
      </svg>
    ),
  },
  {
    href: '/dashboard/analytics',
    label: 'Analytics',
    roles: ['MANAGER', 'ADMIN'],
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
        <path d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
      </svg>
    ),
  },
  {
    href: '/dashboard/admin',
    label: 'Admin',
    roles: ['ADMIN'],
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
        <path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
        <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
    ),
  },
]

interface Props {
  children: React.ReactNode
  session: Session
}

export function DashboardShell({ children, session }: Props) {
  const pathname = usePathname()
  const user = session.user as { name?: string; email?: string; role?: string }

  const visibleNavItems = navItems.filter(item => !item.roles || (user.role && item.roles.includes(user.role)))

  // Compute breadcrumb dynamically based on pathname (mock for simple paths)
  const pathSegments = pathname.split('/').filter(Boolean)
  const breadcrumb = pathSegments.map(segment => segment.charAt(0).toUpperCase() + segment.slice(1)).join(' / ')

  return (
    <div className="flex h-screen overflow-hidden" style={{ backgroundColor: 'var(--surface-base)' }}>
      <OfflineBanner />

      {/* Sidebar */}
      <aside className="flex-shrink-0 flex flex-col" style={{ width: '220px', backgroundColor: 'var(--surface-raised)', borderRight: '1px solid var(--border-subtle)' }}>
        {/* Brand */}
        <div className="h-[52px] flex items-center px-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-md flex items-center justify-center" style={{ backgroundColor: 'var(--brand-muted)', color: 'var(--brand)' }}>
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <span className="font-semibold" style={{ fontSize: '15px', color: 'var(--text-primary)' }}>Stratify</span>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 px-2 py-4 space-y-1 overflow-y-auto">
          {visibleNavItems.map(item => {
            const active = pathname === item.href
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 transition-colors`}
                style={{
                  height: '36px',
                  padding: '0 10px',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '13px',
                  fontWeight: 400,
                  backgroundColor: active ? 'var(--brand-muted)' : 'transparent',
                  color: active ? 'var(--brand)' : 'var(--text-muted)',
                  border: active ? '1px solid var(--brand-border)' : '1px solid transparent',
                  borderLeft: active ? '2px solid var(--brand)' : '1px solid transparent'
                }}
                onMouseEnter={(e) => {
                  if (!active) {
                    e.currentTarget.style.backgroundColor = 'var(--surface-hover)'
                  }
                }}
                onMouseLeave={(e) => {
                  if (!active) {
                    e.currentTarget.style.backgroundColor = 'transparent'
                  }
                }}
              >
                <span style={{ display: 'flex', alignItems: 'center' }}>{item.icon}</span>
                <span style={{ lineHeight: '16px' }}>{item.label}</span>
              </Link>
            )
          })}
        </nav>

        {/* User section */}
        <div className="p-3" style={{ borderTop: '1px solid var(--border-subtle)' }}>
          <div className="flex items-center gap-2 px-1 py-1">
            <div className="w-7 h-7 rounded-full flex items-center justify-center font-medium text-xs flex-shrink-0" style={{ backgroundColor: 'var(--brand-muted)', color: 'var(--brand)' }}>
              {user.name?.[0]?.toUpperCase() ?? 'U'}
            </div>
            <div className="flex-1 min-w-0">
              <p className="font-medium truncate" style={{ fontSize: '12px', color: 'var(--text-primary)' }}>{user.name}</p>
              <p className="truncate uppercase" style={{ fontSize: '10px', fontWeight: 500, color: 'var(--text-muted)', letterSpacing: '0.06em' }}>{user.role}</p>
            </div>
          </div>
          <button
            onClick={() => signOut({ callbackUrl: '/login' })}
            className="mt-2 w-full text-left px-2 py-1.5 transition-colors"
            style={{ fontSize: '11px', color: 'var(--text-muted)', borderRadius: 'var(--radius-sm)' }}
            onMouseEnter={(e) => {
              e.currentTarget.style.color = 'var(--text-primary)';
              e.currentTarget.style.backgroundColor = 'var(--surface-hover)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.color = 'var(--text-muted)';
              e.currentTarget.style.backgroundColor = 'transparent';
            }}
          >
            Sign out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top bar */}
        <header className="flex items-center justify-between px-6 flex-shrink-0 sticky top-0 z-40" style={{ height: '52px', backgroundColor: 'var(--surface-base)', borderBottom: '1px solid var(--border-subtle)' }}>
          <div className="text-[13px] font-medium" style={{ color: 'var(--text-secondary)' }}>
            {breadcrumb}
          </div>
          <div className="flex items-center gap-4">
            <SyncBadge />
            <NotificationBell />
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <div style={{ padding: '28px 32px', maxWidth: '1120px' }}>
            {children}
          </div>
        </main>
      </div>

      <ConflictAlert />
    </div>
  )
}