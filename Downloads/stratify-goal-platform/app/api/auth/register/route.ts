import { NextRequest, NextResponse } from 'next/server'
import bcrypt from 'bcryptjs'
import connectDB from '@/lib/mongodb'
import User from '@/models/User'

export async function POST(req: NextRequest) {
  try {
    const body = await req.json()
    const { name, email, password, role, managerId } = body

    if (!name || !email || !password) {
      return NextResponse.json({ error: 'Missing required fields' }, { status: 400 })
    }

    if (password.length < 6) {
      return NextResponse.json({ error: 'Password must be at least 6 characters' }, { status: 400 })
    }

    const validRoles = ['EMPLOYEE', 'MANAGER', 'ADMIN']
    const assignedRole = validRoles.includes(role) ? role : 'EMPLOYEE'

    await connectDB()

    const existingUser = await User.findOne({ email: email.toLowerCase() })
    if (existingUser) {
      return NextResponse.json({ error: 'Email is already registered' }, { status: 409 })
    }

    const passwordHash = await bcrypt.hash(password, 10)

    const userData: any = {
      name,
      email: email.toLowerCase(),
      passwordHash,
      role: assignedRole,
    }

    if (assignedRole === 'EMPLOYEE' && managerId) {
      userData.managerId = managerId
    }

    const user = await User.create(userData)

    return NextResponse.json(
      { message: 'User created successfully', user: { id: user._id, email: user.email, role: user.role } },
      { status: 201 }
    )
  } catch (error: any) {
    console.error('[POST /api/auth/register]', error)
    return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 })
  }
}
