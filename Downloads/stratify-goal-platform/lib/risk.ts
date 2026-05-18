// lib/risk.ts
import { IGoalDoc } from '@/models/Goal'

export function computeRiskScore(goal: IGoalDoc, quarterStartDate: Date, quarterEndDate: Date): number {
  if (!goal.targetValue || goal.targetValue <= 0) return 0
  if (goal.status === 'COMPLETED' || goal.status === 'REJECTED') return 0

  const now = new Date()
  const totalDays = Math.max(1, (quarterEndDate.getTime() - quarterStartDate.getTime()) / (1000 * 60 * 60 * 24))
  const daysRemaining = Math.max(0, (quarterEndDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))

  const progressPercent = Math.min((goal.currentValue / goal.targetValue) * 100, 100)

  // Math.max(0, Math.min(100, ((daysRemaining/totalDays) - (progressPercent/100)) * 100 + 50))
  const riskScoreRaw = ((daysRemaining / totalDays) - (progressPercent / 100)) * 100 + 50
  
  return Math.round(Math.max(0, Math.min(100, riskScoreRaw)))
}
