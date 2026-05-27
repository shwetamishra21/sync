// models/Quarter.ts
import mongoose, { Document, Types } from 'mongoose'

export enum QuarterStatus {
  PLANNED = 'PLANNED',
  ACTIVE = 'ACTIVE',
  CLOSED = 'CLOSED',
}

export interface IQuarter extends Document {
  name: string // e.g., "Q1-2025"
  startDate: Date
  endDate: Date
  status: QuarterStatus
  createdAt: Date
  updatedAt: Date
}

export interface IQuarterModel extends mongoose.Model<IQuarter> {
  getActiveQuarter(): Promise<IQuarter | null>
}

const quarterSchema = new mongoose.Schema<IQuarter>(
  {
    name: { type: String, required: true, unique: true },
    startDate: { type: Date, required: true },
    endDate: { type: Date, required: true },
    status: { type: String, enum: Object.values(QuarterStatus), default: QuarterStatus.PLANNED },
  },
  { timestamps: true }
)

// Index for quick look‑up by name and date range
quarterSchema.index({ name: 1 })
quarterSchema.index({ startDate: 1, endDate: 1 })

quarterSchema.statics.getActiveQuarter = async function() {
  return this.findOne({ status: QuarterStatus.ACTIVE })
}

export default (mongoose.models.Quarter as IQuarterModel) || mongoose.model<IQuarter, IQuarterModel>('Quarter', quarterSchema)
