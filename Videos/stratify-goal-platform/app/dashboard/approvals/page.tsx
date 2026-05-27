import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import { redirect } from 'next/navigation'
import type { Metadata } from 'next'
import ApprovalsPage from '@/components/approvals/ApprovalsPage'

export const metadata: Metadata = {
  title: 'Approvals — Stratify',
  description: 'Manage goal approvals',
}

export default async function Page() {
  const session = await getServerSession(authOptions)
  const user = session?.user as { role?: string }
  if (!user || (user.role !== 'MANAGER' && user.role !== 'ADMIN')) {
    redirect('/dashboard')
  }

  return <ApprovalsPage />
}
