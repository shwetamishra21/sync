// models/Notification.ts
import mongoose, { Document, Types } from 'mongoose'

export enum NotificationType {
  INFO = 'INFO',
  WARNING = 'WARNING',
  ALERT = 'ALERT',
}

export interface INotification extends Document {
  userId: Types.ObjectId
  title: string
  message: string
  type: NotificationType
  read: boolean
  createdAt: Date
  updatedAt: Date
}

const notificationSchema = new mongoose.Schema<INotification>(
  {
    userId: { type: mongoose.Schema.Types.ObjectId, required: true, ref: 'User' },
    title: { type: String, required: true },
    message: { type: String, required: true },
    type: { type: String, enum: Object.values(NotificationType), default: NotificationType.INFO },
    read: { type: Boolean, default: false },
  },
  { timestamps: true }
)

notificationSchema.index({ userId: 1, read: 1 })
notificationSchema.index({ createdAt: -1 })

export default mongoose.models.Notification || mongoose.model<INotification>('Notification', notificationSchema)
