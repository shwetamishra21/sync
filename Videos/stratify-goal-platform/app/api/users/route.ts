// app/api/users/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import User from '@/models/User'

export async function GET(req: NextRequest) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    await connectDB()
    const users = await User.find({}).select('name email role').lean()
    
    return NextResponse.json(users)
  } catch (err) {
    console.error('[GET /api/users]', err)
    return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 })
  }
}
