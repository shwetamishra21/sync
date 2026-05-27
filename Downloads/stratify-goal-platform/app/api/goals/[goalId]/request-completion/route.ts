import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'
import AuditLog, { AuditAction } from '@/models/AuditLog'

export async function POST(
  req: NextRequest,
  { params }: { params: Promise<{ goalId: string }> }
) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    await connectDB()
    const user = session.user as { id: string; role: string }
    const { goalId } = await params

    const goal = await Goal.findById(goalId)
    if (!goal) {
      return NextResponse.json({ error: 'Goal not found' }, { status: 404 })
    }

    if (goal.userId.toString() !== user.id) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 403 })
    }

    if (goal.status !== 'APPROVED') {
      return NextResponse.json({ error: 'Only APPROVED goals can be submitted for completion.' }, { status: 400 })
    }

    goal.status = 'PENDING_COMPLETION'
    await goal.save()

    await AuditLog.create({
      userId: user.id,
      action: AuditAction.UPDATE,
      entity: 'Goal',
      entityId: goal._id,
      payload: { status: 'PENDING_COMPLETION' }
    })

    return NextResponse.json({ goal }, { status: 200 })
  } catch (err) {
    console.error('[POST /api/goals/[goalId]/request-completion]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
