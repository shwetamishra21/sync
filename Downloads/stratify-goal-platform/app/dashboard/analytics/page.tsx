// app/dashboard/analytics/page.tsx
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import { redirect } from 'next/navigation'
import Link from 'next/link'

export default async function AnalyticsPage() {
  const session = await getServerSession(authOptions)
  if (!session?.user || (session.user as any).role === 'EMPLOYEE') {
    redirect('/dashboard')
  }

  return (
    <div className="max-w-6xl mx-auto space-y-5" style={{ gap: '20px' }}>
      <div className="flex items-center justify-between mb-5">
        <h1 className="h1-title">Analytics Dashboard</h1>
        <Link 
          href="/api/analytics/export" 
          target="_blank"
          className="btn-primary inline-flex items-center gap-2"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          Export Report
        </Link>
      </div>

      <div className="base-card flex flex-col items-center justify-center p-12 text-center" style={{ minHeight: '300px' }}>
        <svg className="w-16 h-16 mx-auto mb-4" style={{ color: 'var(--text-muted)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
        <p className="h2-title mb-2">Analytics modules are under construction.</p>
        <p className="text-[13px]" style={{ color: 'var(--text-secondary)' }}>You can export the current quarter report using the button above.</p>
      </div>
    </div>
  )
}
