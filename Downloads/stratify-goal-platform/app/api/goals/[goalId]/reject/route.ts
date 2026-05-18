import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'
import AuditLog, { AuditAction } from '@/models/AuditLog'
import Notification, { NotificationType } from '@/models/Notification'
import mongoose from 'mongoose'

export async function POST(
  req: NextRequest,
  { params }: { params: Promise<{ goalId: string }> }
) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    const user = session.user as { id: string; role: string }
    if (user.role !== 'MANAGER' && user.role !== 'ADMIN') {
      return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
    }

    const body = await req.json().catch(() => ({}))
    const { reason } = body

    await connectDB()
    const { goalId } = await params

    
    try {
      const goal = await Goal.findById(goalId)
      if (!goal) {
        throw new Error('Goal not found')
      }

      if (goal.status !== 'PENDING_APPROVAL') {
        throw new Error('Invalid status for rejection')
      }

      goal.status = 'REJECTED'
      await goal.save()

      await AuditLog.create([{
        userId: user.id,
        action: AuditAction.UPDATE,
        entity: 'Goal',
        entityId: goal._id,
        payload: { status: 'REJECTED', reason }
      }])

      await Notification.create([{
        userId: goal.userId,
        title: 'Goal Rejected',
        message: `Your goal "${goal.title}" has been rejected. ${reason ? `Reason: ${reason}` : ''}`,
        type: NotificationType.WARNING,
        read: false
      }])

            return NextResponse.json({ goal }, { status: 200 })
    } catch (error: any) {
            return NextResponse.json({ error: error.message }, { status: 400 })
    } finally {
          }
  } catch (err) {
    console.error('[POST /api/goals/[goalId]/reject]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
