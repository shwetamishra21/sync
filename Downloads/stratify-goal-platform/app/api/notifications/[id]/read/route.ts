// app/api/notifications/[id]/read/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Notification from '@/models/Notification'

export async function PATCH(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    await connectDB()
    const userId = (session.user as { id: string }).id
    const resolvedParams = await params
    
    // To support "mark all as read" we can check if id is 'all'
    if (resolvedParams.id === 'all') {
      await Notification.updateMany({ userId, read: false }, { read: true })
      return NextResponse.json({ success: true })
    }

    const notification = await Notification.findOneAndUpdate(
      { _id: resolvedParams.id, userId },
      { read: true },
      { new: true }
    )

    if (!notification) {
      return NextResponse.json({ error: 'Notification not found' }, { status: 404 })
    }

    return NextResponse.json({ notification })
  } catch (err) {
    console.error('[PATCH /api/notifications/[id]/read]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
