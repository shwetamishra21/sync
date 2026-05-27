// models/AuditLog.ts
import mongoose, { Document, Types } from 'mongoose'

export enum AuditAction {
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  SYNC = 'SYNC',
}

export interface IAuditLog extends Document {
  userId: Types.ObjectId
  action: AuditAction
  entity: string // e.g., 'Goal', 'Quarter', etc.
  entityId: Types.ObjectId
  payload: Record<string, any>
  createdAt: Date
  // Mongoose timestamps will add updatedAt if needed
}

const auditLogSchema = new mongoose.Schema<IAuditLog>(
  {
    userId: { type: mongoose.Schema.Types.ObjectId, required: true, ref: 'User' },
    action: { type: String, enum: Object.values(AuditAction), required: true },
    entity: { type: String, required: true },
    entityId: { type: mongoose.Schema.Types.ObjectId, required: true },
    payload: { type: mongoose.Schema.Types.Mixed, default: {} },
  },
  { timestamps: true }
)

// Indexes for fast look‑up by user and entity
auditLogSchema.index({ userId: 1 })
auditLogSchema.index({ entity: 1, entityId: 1 })

auditLogSchema.index({ createdAt: -1 })

export default mongoose.models.AuditLog || mongoose.model<IAuditLog>('AuditLog', auditLogSchema)
