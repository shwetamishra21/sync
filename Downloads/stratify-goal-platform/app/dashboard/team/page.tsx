import { TeamGoalsPage } from '@/components/team/TeamGoalsPage'
import { redirect } from 'next/navigation'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'

export const metadata = {
  title: 'Team Goals | Stratify',
}

export default async function Page() {
  const session = await getServerSession(authOptions)
  const role = (session?.user as any)?.role

  if (role !== 'MANAGER' && role !== 'ADMIN') {
    redirect('/dashboard')
  }

  return <TeamGoalsPage />
}
