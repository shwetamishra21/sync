// models/Department.ts
import mongoose, { Schema, Document, Model } from 'mongoose'

export interface IDepartmentDoc extends Document {
  name: string
  createdAt: Date
  updatedAt: Date
}

const DepartmentSchema = new Schema<IDepartmentDoc>(
  {
    name: { type: String, required: true, unique: true },
  },
  { timestamps: true }
)

const Department: Model<IDepartmentDoc> =
  mongoose.models.Department ?? mongoose.model<IDepartmentDoc>('Department', DepartmentSchema)

export default Department
