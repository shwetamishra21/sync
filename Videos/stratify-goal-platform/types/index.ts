// types/index.ts

export type UserRole = 'EMPLOYEE' | 'MANAGER' | 'ADMIN'
export type GoalStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'LOCKED' | 'REJECTED' | 'COMPLETED'
export type SyncStatus = 'synced' | 'pending' | 'failed' | 'conflict'

export interface IUser {
  _id: string
  email: string
  name: string
  role: UserRole
  departmentId?: string
  managerId?: string
  createdAt: Date
}

export interface IGoal {
  _id: string
  userId: string
  title: string
  description?: string
  weightage: number
  status: GoalStatus
  quarterId?: string
  targetValue: number
  currentValue: number
  achievementScore: number
  riskScore?: number
  createdAt: Date
  updatedAt: Date
}

export interface ICheckIn {
  _id?: string
  goalId: string
  progressValue: number
  progressPercent: number
  notes?: string
  managerComment?: string
  submittedAt: Date
  serverReceivedAt?: Date
  wasOfflineSync?: boolean
  _syncMeta?: {
    isOfflineWrite: boolean
    clientTimestamp: string
    deviceId: string
  }
}

// Dexie outbox item
export interface OutboxItem {
  id?: number
  endpoint: string
  method: string
  body: string // JSON stringified
  status: 'pending' | 'syncing' | 'failed'
  createdAt: string
  retries: number
  errorMessage?: string
}

// Dexie goals cache
export interface GoalCacheItem {
  goalId: string
  data: string // JSON stringified IGoal
  userId: string
  updatedAt: string
}