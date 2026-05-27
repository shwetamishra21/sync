import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'
import AuditLog, { AuditAction } from '@/models/AuditLog'
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

    await connectDB()
    const user = session.user as { id: string; role: string }
    const { goalId } = await params

    
    try {
      const goal = await Goal.findById(goalId)
      if (!goal) {
        throw new Error('Goal not found')
      }

      if (goal.userId.toString() !== user.id) {
        throw new Error('Unauthorized')
      }

      if (goal.status !== 'DRAFT' && goal.status !== 'REJECTED') {
        throw new Error('Invalid status for submission')
      }

      goal.status = 'PENDING_APPROVAL'
      await goal.save()

      await AuditLog.create([{
        userId: user.id,
        action: AuditAction.UPDATE,
        entity: 'Goal',
        entityId: goal._id,
        payload: { status: 'PENDING_APPROVAL' }
      }])

            return NextResponse.json({ goal }, { status: 200 })
    } catch (error: any) {
            return NextResponse.json({ error: error.message }, { status: error.message === 'Unauthorized' ? 403 : 400 })
    } finally {
          }
  } catch (err) {
    console.error('[POST /api/goals/[goalId]/submit]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
