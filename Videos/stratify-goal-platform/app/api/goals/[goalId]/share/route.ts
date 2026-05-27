// app/api/goals/[goalId]/share/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'
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

    const { goalId } = await params
    const body = await req.json().catch(() => null)
    if (!body || !body.targetUserId || typeof body.weightage !== 'number') {
      return NextResponse.json({ error: 'targetUserId and weightage are required' }, { status: 400 })
    }

    await connectDB()

    
    try {
      const sourceGoal = await Goal.findById(goalId)
      if (!sourceGoal) {
                return NextResponse.json({ error: 'Source goal not found' }, { status: 404 })
      }

      // Check if target user has enough weightage remaining
      const targetUserGoals = await Goal.find({
        userId: body.targetUserId,
        quarterId: sourceGoal.quarterId,
        status: { $ne: 'REJECTED' }
      })

      const usedWeightage = targetUserGoals.reduce((sum, g) => sum + g.weightage, 0)
      if (usedWeightage + body.weightage > 100) {
                return NextResponse.json({ error: `Target user only has ${100 - usedWeightage}% weightage remaining` }, { status: 422 })
      }

      const [newGoal] = await Goal.create([{
        userId: new mongoose.Types.ObjectId(body.targetUserId as string),
        title: sourceGoal.title,
        description: sourceGoal.description,
        weightage: body.weightage,
        targetValue: sourceGoal.targetValue,
        quarterId: sourceGoal.quarterId,
        status: 'DRAFT',
        sharedGoalId: sourceGoal._id
      }])

      sourceGoal.sharedGoalId = newGoal._id
      await sourceGoal.save()

            return NextResponse.json({ success: true, sharedGoalId: newGoal._id })
    } catch (err) {
            throw err
    } finally {
      await     }
  } catch (err) {
    console.error('[POST /api/.../share]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
