// models/Goal.ts
import mongoose, { Schema, Document, Model } from 'mongoose'

export interface IGoalDoc extends Document {
  userId: mongoose.Types.ObjectId
  title: string
  description?: string
  weightage: number
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'LOCKED' | 'REJECTED' | 'PENDING_COMPLETION' | 'COMPLETED'
  targetValue: number
  currentValue: number
  achievementScore: number
  riskScore: number
  quarterId?: string
  approvedBy?: mongoose.Types.ObjectId
  lockedAt?: Date
  sharedGoalId?: mongoose.Types.ObjectId
  createdAt: Date
  updatedAt: Date
}

const GoalSchema = new Schema<IGoalDoc>(
  {
    userId: { type: Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    title: { type: String, required: true },
    description: { type: String },
    weightage: { type: Number, required: true, min: 0, max: 100 },
    status: {
      type: String,
      enum: ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'LOCKED', 'REJECTED', 'PENDING_COMPLETION', 'COMPLETED'],
      default: 'DRAFT',
      index: true,
    },
    targetValue: { type: Number, default: 100 },
    currentValue: { type: Number, default: 0 },
    achievementScore: { type: Number, default: 0 },
    riskScore: { type: Number, default: 0 },
    quarterId: { type: String, index: true },
    approvedBy: { type: Schema.Types.ObjectId, ref: 'User' },
    lockedAt: { type: Date },
    sharedGoalId: { type: Schema.Types.ObjectId, ref: 'Goal' },
  },
  { timestamps: true }
)

// Compute achievement score before save
GoalSchema.pre('save', async function () {
  if (this.targetValue > 0) {
    const pct = Math.min((this.currentValue / this.targetValue) * 100, 100)
    this.achievementScore = pct * (this.weightage / 100)
  }
})

const Goal: Model<IGoalDoc> =
  mongoose.models.Goal ?? mongoose.model<IGoalDoc>('Goal', GoalSchema)

export default Goal