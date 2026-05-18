// models/User.ts
import mongoose, { Schema, Document, Model } from 'mongoose'

export interface IUserDoc extends Document {
  email: string
  name: string
  passwordHash: string
  role: 'EMPLOYEE' | 'MANAGER' | 'ADMIN'
  departmentId?: mongoose.Types.ObjectId
  managerId?: mongoose.Types.ObjectId
  isActive: boolean
  createdAt: Date
  updatedAt: Date
}

const UserSchema = new Schema<IUserDoc>(
  {
    email: { type: String, required: true, unique: true, lowercase: true, trim: true },
    name: { type: String, required: true },
    passwordHash: { type: String, required: true, select: false },
    role: { type: String, enum: ['EMPLOYEE', 'MANAGER', 'ADMIN'], default: 'EMPLOYEE' },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department' },
    managerId: { type: Schema.Types.ObjectId, ref: 'User' },
    isActive: { type: Boolean, default: true },
  },
  { timestamps: true }
)

const User: Model<IUserDoc> =
  mongoose.models.User ?? mongoose.model<IUserDoc>('User', UserSchema)

export default User