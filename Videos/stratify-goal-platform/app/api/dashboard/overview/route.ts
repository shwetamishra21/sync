// app/api/dashboard/overview/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Quarter from '@/models/Quarter'
import CheckIn from '@/models/CheckIn'
import Goal from '@/models/Goal'

export async function GET(req: NextRequest) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

    await connectDB()
    const activeQuarter = await Quarter.getActiveQuarter()
    let daysRemaining = 0
    if (activeQuarter) {
      const now = new Date()
      daysRemaining = Math.max(0, Math.floor((activeQuarter.endDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)))
    }

    const userId = (session.user as any).id
    const userRole = (session.user as any).role

    const oneWeekAgo = new Date()
    oneWeekAgo.setDate(oneWeekAgo.getDate() - 7)

    const recentCheckIns = await CheckIn.countDocuments({
      submittedBy: userId,
      submittedAt: { $gte: oneWeekAgo }
    })

    let teamStats = null
    if (userRole === 'MANAGER' || userRole === 'ADMIN') {
      const teamGoals = await Goal.find({ quarterId: activeQuarter?._id }).populate('userId').lean()
      const employeeMap = new Map()
      
      for (const g of teamGoals) {
        if (!g.userId) continue
        const uid = (g.userId as any)._id.toString()
        if (!employeeMap.has(uid)) {
          employeeMap.set(uid, {
            name: (g.userId as any).name,
            goalCount: 0,
            achievementSum: 0,
            atRiskCount: 0
          })
        }
        const st = employeeMap.get(uid)
        st.goalCount++
        st.achievementSum += (g.achievementScore || 0)
        if (g.riskScore > 50) st.atRiskCount++
      }
      
      teamStats = Array.from(employeeMap.values()).map(st => ({
        name: st.name,
        goalCount: st.goalCount,
        avgAchievement: st.goalCount > 0 ? Math.round(st.achievementSum / st.goalCount) : 0,
        atRiskCount: st.atRiskCount
      }))
    }

    return NextResponse.json({
      daysRemaining,
      recentCheckIns,
      teamStats
    })

  } catch (err) {
    return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 })
  }
}
