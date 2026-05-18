import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import User from '@/models/User'

export async function GET(req: NextRequest) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user || (session.user as any).role !== 'MANAGER') {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    await connectDB()
    const employees = await User.find({ managerId: (session.user as any).id }).select('_id name email').lean()
    return NextResponse.json({ employees })
  } catch (error) {
    console.error('[GET /api/users/employees]', error)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
