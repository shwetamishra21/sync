// app/api/goals/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'
import { goalSchema } from '@/lib/validators/goal'

export async function GET(req: NextRequest) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    await connectDB()
    const user = session.user as { id: string; role: string }
    const { searchParams } = new URL(req.url)
    const status = searchParams.get('status')
    const team = searchParams.get('team') === 'true'

    let query: any = {}

    if (team && (user.role === 'MANAGER' || user.role === 'ADMIN')) {
      const User = (await import('@/models/User')).default
      const teamMembers = await User.find({ managerId: user.id }).select('_id').lean()
      const teamMemberIds = teamMembers.map((m: any) => m._id)
      query.userId = { $in: teamMemberIds }
      if (status) query.status = status
    } else {
      query.userId = user.id
      if (status) {
        if ((user.role === 'MANAGER' || user.role === 'ADMIN') && status === 'PENDING_APPROVAL') {
          query = { status: 'PENDING_APPROVAL' }
        } else {
          query.status = status
        }
      }
    }

    const goals = await Goal.find(query).populate('userId', 'name email').sort({ createdAt: -1 }).lean()
    return NextResponse.json({ goals })
  } catch (err) {
    console.error('[GET /api/goals]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}

export async function POST(req: NextRequest) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    const user = session.user as { id: string; role: string }
    let targetUserId = user.id
    let initialStatus = 'DRAFT'

    const body = await req.json().catch(() => null)
    if (!body) {
      return NextResponse.json({ error: 'Invalid request body' }, { status: 400 })
    }

    if (user.role === 'MANAGER' && body.employeeId) {
      // Validate that the employee is actually managed by this manager
      const User = (await import('@/models/User')).default
      const employee = await User.findById(body.employeeId)
      if (!employee || employee.managerId?.toString() !== user.id) {
        return NextResponse.json({ error: 'Unauthorized to assign goals to this user' }, { status: 403 })
      }
      targetUserId = body.employeeId
      initialStatus = 'APPROVED'
    }

    // Strip _syncMeta before validation (added by mutateOfflineAware)
    const { _syncMeta, ...rest } = body

    const parsed = goalSchema.safeParse({
      ...rest,
      weightage: Number(rest.weightage),
      targetValue: Number(rest.targetValue ?? 100),
    })

    if (!parsed.success) {
      return NextResponse.json(
        { error: 'Validation failed', issues: parsed.error.flatten().fieldErrors },
        { status: 422 }
      )
    }

    const goal = await Goal.create({
      userId: targetUserId,
      title: parsed.data.title,
      description: parsed.data.description ?? '',
      weightage: parsed.data.weightage,
      targetValue: parsed.data.targetValue,
      quarterId: parsed.data.quarterId ?? 'Q2-2025',
      status: initialStatus,
      currentValue: 0,
      achievementScore: 0,
      // Preserve offline sync metadata for audit trail
      ...((_syncMeta as object | undefined) ? { _syncMeta } : {}),
    })

    return NextResponse.json({ goal }, { status: 201 })
  } catch (err) {
    console.error('[POST /api/goals]', err)
    if (err instanceof Error && err.message.includes('Validation Error')) {
      return NextResponse.json({ error: err.message }, { status: 422 })
    }
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
