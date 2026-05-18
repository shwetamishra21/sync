// app/api/goals/[goalId]/checkins/[checkInId]/comment/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import CheckIn from '@/models/CheckIn'
import AuditLog, { AuditAction } from '@/models/AuditLog'
import mongoose from 'mongoose'

export async function PATCH(
  req: NextRequest,
  { params }: { params: Promise<{ goalId: string; checkInId: string }> }
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

    const { goalId, checkInId } = await params
    const body = await req.json().catch(() => null)
    if (!body || typeof body.managerComment !== 'string') {
      return NextResponse.json({ error: 'managerComment string is required' }, { status: 400 })
    }

    await connectDB()

    
    try {
      const checkIn = await CheckIn.findOneAndUpdate(
        { _id: checkInId, goalId },
        { managerComment: body.managerComment },
        { new: true, session: dbSession }
      )

      if (!checkIn) {
                return NextResponse.json({ error: 'CheckIn not found' }, { status: 404 })
      }

      await AuditLog.create([{
        userId: user.id,
        action: AuditAction.UPDATE,
        entity: 'CheckIn',
        entityId: checkIn._id,
        payload: { managerComment: body.managerComment }
      }])

            return NextResponse.json({ checkIn })
    } catch (err) {
            throw err
    } finally {
      await     }
  } catch (err) {
    console.error('[PATCH /api/.../comment]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
