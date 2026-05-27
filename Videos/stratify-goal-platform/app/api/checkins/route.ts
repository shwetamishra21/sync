// app/api/checkins/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import CheckIn from '@/models/CheckIn'
import Goal from '@/models/Goal'
import mongoose from 'mongoose'

export async function POST(req: NextRequest) {
  const session = await getServerSession(authOptions)
  if (!session?.user) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  await connectDB()
  const userId = (session.user as { id: string }).id

  const body = await req.json()
  const { goalId, progressValue, notes, _syncMeta } = body

  if (!goalId || progressValue == null) {
    return NextResponse.json({ error: 'goalId and progressValue are required' }, { status: 422 })
  }

  // Validate goal exists and belongs to user
  const goal = await Goal.findOne({ _id: goalId, userId })
  if (!goal) {
    return NextResponse.json({ error: 'Goal not found' }, { status: 404 })
  }

  // If goal was rejected/completed while offline, return 409 conflict
  if (goal.status === 'REJECTED') {
    return NextResponse.json(
      { error: 'Goal was rejected while you were offline', conflict: true },
      { status: 409 }
    )
  }

  const progressPercent = Math.min((progressValue / goal.targetValue) * 100, 100)
  const wasOfflineSync = _syncMeta?.isOfflineWrite === true

  // Use client timestamp if offline sync, else server time
  const submittedAt = _syncMeta?.clientTimestamp
    ? new Date(_syncMeta.clientTimestamp)
    : new Date()

  const checkIn = await CheckIn.create({
    goalId: new mongoose.Types.ObjectId(goalId),
    submittedBy: new mongoose.Types.ObjectId(userId),
    progressValue,
    progressPercent,
    notes,
    submittedAt,
    serverReceivedAt: new Date(),
    _syncMeta,
  })

  // Update goal's current value and recompute score
  goal.currentValue = progressValue
  const pct = Math.min((progressValue / goal.targetValue) * 100, 100)
  goal.achievementScore = pct * (goal.weightage / 100)
  await goal.save()

  return NextResponse.json(
    {
      checkIn,
      goal: {
        _id: goal._id,
        currentValue: goal.currentValue,
        achievementScore: goal.achievementScore,
      },
      wasOfflineSync,
    },
    { status: 201 }
  )
}

export async function GET(req: NextRequest) {
  const session = await getServerSession(authOptions)
  if (!session?.user) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  await connectDB()
  const { searchParams } = new URL(req.url)
  const goalId = searchParams.get('goalId')

  if (!goalId) return NextResponse.json({ error: 'goalId required' }, { status: 400 })

  const checkIns = await CheckIn.find({ goalId }).sort({ submittedAt: -1 }).lean()
  return NextResponse.json({ checkIns })
}