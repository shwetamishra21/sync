import { NextResponse } from 'next/server'
import connectDB from '@/lib/mongodb'
import User from '@/models/User'

export async function GET() {
  try {
    await connectDB()
    const managers = await User.find({ role: 'MANAGER' }).select('_id name email').lean()
    return NextResponse.json({ managers })
  } catch (error) {
    console.error('[GET /api/users/managers]', error)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
