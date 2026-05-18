import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'
import CheckIn from '@/models/CheckIn'

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ goalId: string }> }
) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    await connectDB()
    const { goalId } = await params

    const goal = await Goal.findById(goalId).populate('userId', 'name email').lean()
    if (!goal) {
      return NextResponse.json({ error: 'Goal not found' }, { status: 404 })
    }

    const checkIns = await CheckIn.find({ goalId }).sort({ submittedAt: -1 }).lean()

    return NextResponse.json({ goal, checkIns })
  } catch (err) {
    console.error('[GET /api/goals/[goalId]]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
