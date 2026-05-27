// models/CheckIn.ts

import mongoose, { Schema, Document, Model } from 'mongoose';

export interface ICheckIn extends Document {
  goalId: mongoose.Types.ObjectId;
  progressValue: number;
  progressPercent: number;
  notes?: string;
  managerComment?: string;
  submittedBy: mongoose.Types.ObjectId;
  submittedAt: Date;
  serverReceivedAt: Date;
  _syncMeta: {
    isOfflineWrite: boolean;
    clientTimestamp: string;
    deviceId: string;
    syncedAt: Date | null;
  };
}

const CheckInSchema = new Schema<ICheckIn>(
  {
    goalId: { type: Schema.Types.ObjectId, ref: 'Goal', required: true, index: true },
    progressValue: { type: Number, required: true, min: 0 },
    progressPercent: { type: Number, required: true, min: 0 },
    notes: { type: String, maxlength: 500 },
    managerComment: { type: String, maxlength: 500 },
    submittedBy: { type: Schema.Types.ObjectId, ref: 'User', required: true },
    submittedAt: { type: Date, default: Date.now },
    serverReceivedAt: { type: Date, default: Date.now },
    _syncMeta: {
      isOfflineWrite: { type: Boolean, default: false },
      clientTimestamp: { type: String },
      deviceId: { type: String },
      syncedAt: { type: Date, default: null },
    },
  },
  { timestamps: true }
);

// Compound index for check-in history queries
CheckInSchema.index({ goalId: 1, submittedAt: -1 });

const CheckIn: Model<ICheckIn> =
  mongoose.models.CheckIn ?? mongoose.model<ICheckIn>('CheckIn', CheckInSchema);

export default CheckIn;
