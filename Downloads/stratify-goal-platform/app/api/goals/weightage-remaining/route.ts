// app/api/goals/weightage-remaining/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'

export async function GET(req: NextRequest) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    await connectDB()
    const userId = (session.user as { id: string }).id
    const { searchParams } = new URL(req.url)
    const quarterId = searchParams.get('quarterId')

    if (!quarterId) {
      return NextResponse.json({ error: 'quarterId is required' }, { status: 400 })
    }

    // Do not count REJECTED goals towards weightage
    const goals = await Goal.find({ userId, quarterId, status: { $ne: 'REJECTED' } }).lean()
    
    let used = 0
    for (const goal of goals) {
      used += goal.weightage || 0
    }

    const remaining = Math.max(0, 100 - used)

    return NextResponse.json({ used, remaining })
  } catch (err) {
    console.error('[GET /api/goals/weightage-remaining]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
