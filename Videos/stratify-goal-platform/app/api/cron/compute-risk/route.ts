// app/api/cron/compute-risk/route.ts
import { NextRequest, NextResponse } from 'next/server'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'
import Quarter from '@/models/Quarter'
import { computeRiskScore } from '@/lib/risk'

export async function GET(req: NextRequest) {
  const authHeader = req.headers.get('authorization')
  const cronSecret = process.env.CRON_SECRET

  if (cronSecret && authHeader !== `Bearer ${cronSecret}`) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  try {
    await connectDB()

    const activeQuarter = await Quarter.getActiveQuarter()
    if (!activeQuarter) {
      return NextResponse.json({ message: 'No active quarter found' })
    }

    const goals = await Goal.find({ 
      quarterId: activeQuarter._id,
      status: { $nin: ['COMPLETED', 'REJECTED'] }
    })

    let updatedCount = 0

    for (const goal of goals) {
      const newRiskScore = computeRiskScore(goal, activeQuarter.startDate, activeQuarter.endDate)
      if (newRiskScore !== goal.riskScore) {
        goal.riskScore = newRiskScore
        await goal.save()
        updatedCount++
      }
    }

    return NextResponse.json({ success: true, updatedCount, totalGoals: goals.length })
  } catch (err) {
    console.error('[Cron Risk Compute]', err)
    return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 })
  }
}
