import type { Metadata } from 'next'
import GoalDetailPage from '@/components/goals/GoalDetailPage'

export const metadata: Metadata = {
  title: 'Goal Details — Stratify',
}

export default async function Page({ params }: { params: Promise<{ goalId: string }> }) {
  const { goalId } = await params
  return <GoalDetailPage goalId={goalId} />
}
