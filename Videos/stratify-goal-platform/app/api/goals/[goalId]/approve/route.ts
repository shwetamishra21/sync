import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'
import AuditLog, { AuditAction } from '@/models/AuditLog'
import Notification, { NotificationType } from '@/models/Notification'
import mongoose from 'mongoose'
import User from '@/models/User'

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

    await connectDB()
    const { goalId } = await params

    
    try {
      const goal = await Goal.findById(goalId)
      if (!goal) {
        throw new Error('Goal not found')
      }

      if (user.role === 'MANAGER') {
        const employee = await User.findById(goal.userId)
        if (!employee || employee.managerId?.toString() !== user.id) {
          throw new Error('Unauthorized: You are not the assigned manager for this employee')
        }
      }

      if (goal.status !== 'PENDING_APPROVAL' && goal.status !== 'PENDING_COMPLETION') {
        throw new Error('Invalid status for approval')
      }

      const isCompletion = goal.status === 'PENDING_COMPLETION'
      const newStatus = isCompletion ? 'COMPLETED' : 'APPROVED'

      goal.status = newStatus
      goal.approvedBy = new mongoose.Types.ObjectId(user.id)
      await goal.save()

      await AuditLog.create([{
        userId: user.id,
        action: AuditAction.UPDATE,
        entity: 'Goal',
        entityId: goal._id,
        payload: { status: newStatus, approvedBy: user.id }
      }])

      await Notification.create([{
        userId: goal.userId,
        title: isCompletion ? 'Goal Completion Approved' : 'Goal Approved',
        message: isCompletion 
          ? `Your goal "${goal.title}" has been officially marked as completed.`
          : `Your goal "${goal.title}" has been approved.`,
        type: NotificationType.INFO,
        read: false
      }])

            return NextResponse.json({ goal }, { status: 200 })
    } catch (error: any) {
            return NextResponse.json({ error: error.message }, { status: 400 })
    } finally {
          }
  } catch (err) {
    console.error('[POST /api/goals/[goalId]/approve]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
