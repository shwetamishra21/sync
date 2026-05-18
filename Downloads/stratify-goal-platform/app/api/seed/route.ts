// app/api/seed/route.tsx
import { NextResponse } from 'next/server'
import connectDB from '@/lib/mongodb'
import User from '@/models/User'
import Goal from '@/models/Goal'
import CheckIn from '@/models/CheckIn'
import Department from '@/models/Department'
import AuditLog, { AuditAction } from '@/models/AuditLog'
import Notification, { NotificationType } from '@/models/Notification'
import Quarter, { QuarterStatus } from '@/models/Quarter'
import bcrypt from 'bcryptjs'

export async function POST() {
  await connectDB()

  const emails = ['sarah@demo.com', 'arun@demo.com', 'admin@demo.com', 'priya@demo.com', 'raj@demo.com']

  // Wipe existing seed data for these users to make it idempotent
  const existingUsers = await User.find({ email: { $in: emails } })
  const userIds = existingUsers.map((u) => u._id)

  await Promise.all([
    Goal.deleteMany({ userId: { $in: userIds } }),
    CheckIn.deleteMany({ userId: { $in: userIds } }),
    AuditLog.deleteMany({ userId: { $in: userIds } }),
    Notification.deleteMany({ userId: { $in: userIds } }),
    User.deleteMany({ email: { $in: emails } }),
    Department.deleteMany({ name: { $in: ['Engineering', 'Sales', 'Product', 'IT'] } }),
    Quarter.deleteMany({ name: 'Q2-2025' }),
  ])

  // Create Quarter
  const q2 = await Quarter.create({
    name: 'Q2-2025',
    startDate: new Date('2025-04-01'),
    endDate: new Date('2025-06-30'),
    status: QuarterStatus.ACTIVE,
  })

  // Create Departments
  const [engDept, salesDept, prodDept, itDept] = await Department.insertMany([
    { name: 'Engineering' },
    { name: 'Sales' },
    { name: 'Product' },
    { name: 'IT' },
  ])

  const hash = await bcrypt.hash('demo1234', 10)

  // Create Users
  const [arun, raj, admin] = await User.insertMany([
    { email: 'arun@demo.com', name: 'Arun Sharma', passwordHash: hash, role: 'MANAGER', departmentId: salesDept._id },
    { email: 'raj@demo.com', name: 'Raj Patel', passwordHash: hash, role: 'MANAGER', departmentId: engDept._id },
    { email: 'admin@demo.com', name: 'Admin User', passwordHash: hash, role: 'ADMIN', departmentId: itDept._id },
  ])

  const [sarah, priya] = await User.insertMany([
    { email: 'sarah@demo.com', name: 'Sarah Chen', passwordHash: hash, role: 'EMPLOYEE', departmentId: salesDept._id, managerId: arun._id },
    { email: 'priya@demo.com', name: 'Priya Singh', passwordHash: hash, role: 'EMPLOYEE', departmentId: engDept._id, managerId: raj._id },
  ])

  const quarterId = q2.name

  // Create Goals (15+)
  const goalsToInsert = [
    // Sarah's goals (Sales)
    {
      userId: sarah._id,
      title: 'Complete 50 customer discovery calls',
      description: 'Conduct structured discovery calls with enterprise prospects',
      weightage: 30,
      status: 'APPROVED',
      targetValue: 50,
      currentValue: 18,
      achievementScore: (18/50)*100 * 0.3,
      riskScore: 60, // > 50 risk
      quarterId,
    },
    {
      userId: sarah._id,
      title: 'Achieve $200K pipeline contribution',
      weightage: 40,
      status: 'APPROVED',
      targetValue: 200000,
      currentValue: 95000,
      achievementScore: (95000/200000)*100 * 0.4,
      riskScore: 20,
      quarterId,
    },
    {
      userId: sarah._id,
      title: 'Complete product certification',
      weightage: 20,
      status: 'PENDING_APPROVAL',
      targetValue: 100,
      currentValue: 0,
      achievementScore: 0,
      riskScore: 10,
      quarterId,
    },
    {
      userId: sarah._id,
      title: 'Submit 5 case studies',
      weightage: 10,
      status: 'DRAFT',
      targetValue: 5,
      currentValue: 0,
      achievementScore: 0,
      riskScore: 0,
      quarterId,
    },
    // Arun's goals (Sales Manager)
    {
      userId: arun._id,
      title: 'Team performance score above 80%',
      weightage: 50,
      status: 'APPROVED',
      targetValue: 100,
      currentValue: 62,
      achievementScore: 62 * 0.5,
      riskScore: 70, // > 50 risk
      quarterId,
    },
    {
      userId: arun._id,
      title: 'Complete 10 manager 1:1s',
      weightage: 50,
      status: 'LOCKED',
      targetValue: 10,
      currentValue: 10,
      achievementScore: 100 * 0.5,
      riskScore: 0,
      quarterId,
    },
    // Priya's goals (Engineering)
    {
      userId: priya._id,
      title: 'Reduce API latency by 30%',
      weightage: 40,
      status: 'APPROVED',
      targetValue: 30,
      currentValue: 15,
      achievementScore: (15/30)*100 * 0.4,
      riskScore: 40,
      quarterId,
    },
    {
      userId: priya._id,
      title: 'Ship offline mode feature',
      weightage: 40,
      status: 'APPROVED',
      targetValue: 100,
      currentValue: 80,
      achievementScore: 80 * 0.4,
      riskScore: 10,
      quarterId,
    },
    {
      userId: priya._id,
      title: 'Write 10 technical blog posts',
      weightage: 20,
      status: 'REJECTED',
      targetValue: 10,
      currentValue: 0,
      achievementScore: 0,
      riskScore: 0,
      quarterId,
    },
    // Raj's goals (Engineering Manager)
    {
      userId: raj._id,
      title: 'Hire 3 senior engineers',
      weightage: 60,
      status: 'APPROVED',
      targetValue: 3,
      currentValue: 1,
      achievementScore: (1/3)*100 * 0.6,
      riskScore: 30,
      quarterId,
    },
    {
      userId: raj._id,
      title: 'Maintain 99.99% uptime',
      weightage: 40,
      status: 'COMPLETED',
      targetValue: 100,
      currentValue: 100,
      achievementScore: 100 * 0.4,
      riskScore: 0,
      quarterId,
    },
    // Admin goals
    {
      userId: admin._id,
      title: 'Migrate to AWS',
      weightage: 50,
      status: 'APPROVED',
      targetValue: 100,
      currentValue: 50,
      achievementScore: 50 * 0.5,
      riskScore: 15,
      quarterId,
    },
    {
      userId: admin._id,
      title: 'Update SOC2 Compliance',
      weightage: 50,
      status: 'DRAFT',
      targetValue: 100,
      currentValue: 10,
      achievementScore: 10 * 0.5,
      riskScore: 5,
      quarterId,
    },
  ];

  const goals = await Goal.insertMany(goalsToInsert);

  // Check-ins (10+)
  const sarahGoal1 = goals[0];
  const sarahGoal2 = goals[1];
  const arunGoal1 = goals[4];
  const priyaGoal1 = goals[6];
  const priyaGoal2 = goals[7];

  const checkins = await CheckIn.insertMany([
    { goalId: sarahGoal1._id, submittedBy: sarah._id, progressValue: 8, progressPercent: 16, notes: 'Initial calls done', submittedAt: new Date('2025-04-10'), wasOfflineSync: false },
    { goalId: sarahGoal1._id, submittedBy: sarah._id, progressValue: 18, progressPercent: 36, notes: 'Getting into a rhythm', submittedAt: new Date('2025-04-24'), wasOfflineSync: true, _syncMeta: { isOfflineWrite: true } },
    { goalId: sarahGoal2._id, submittedBy: sarah._id, progressValue: 50000, progressPercent: 25, notes: 'Closed ACME corp', submittedAt: new Date('2025-04-15'), wasOfflineSync: false },
    { goalId: sarahGoal2._id, submittedBy: sarah._id, progressValue: 95000, progressPercent: 47.5, notes: 'Strong pipeline building up', submittedAt: new Date('2025-05-01'), wasOfflineSync: false },
    { goalId: arunGoal1._id, submittedBy: arun._id, progressValue: 40, progressPercent: 40, notes: 'Mid-quarter review', submittedAt: new Date('2025-05-05'), wasOfflineSync: false },
    { goalId: arunGoal1._id, submittedBy: arun._id, progressValue: 62, progressPercent: 62, notes: 'Pushed team to update goals', submittedAt: new Date('2025-05-15'), wasOfflineSync: true, _syncMeta: { isOfflineWrite: true } },
    { goalId: priyaGoal1._id, submittedBy: priya._id, progressValue: 5, progressPercent: 16.6, notes: 'Refactored database queries', submittedAt: new Date('2025-04-12'), wasOfflineSync: false },
    { goalId: priyaGoal1._id, submittedBy: priya._id, progressValue: 15, progressPercent: 50, notes: 'Added Redis caching', submittedAt: new Date('2025-05-02'), wasOfflineSync: true, _syncMeta: { isOfflineWrite: true } },
    { goalId: priyaGoal2._id, submittedBy: priya._id, progressValue: 50, progressPercent: 50, notes: 'IndexedDB integrated', submittedAt: new Date('2025-04-20'), wasOfflineSync: false },
    { goalId: priyaGoal2._id, submittedBy: priya._id, progressValue: 80, progressPercent: 80, notes: 'Outbox drain logic works', submittedAt: new Date('2025-05-10'), wasOfflineSync: false },
  ]);

  // Audit Logs (8+)
  await AuditLog.insertMany([
    { userId: sarah._id, action: AuditAction.CREATE, entity: 'Goal', entityId: sarahGoal1._id, payload: { title: sarahGoal1.title } },
    { userId: arun._id, action: AuditAction.UPDATE, entity: 'Goal', entityId: sarahGoal1._id, payload: { status: 'APPROVED' } },
    { userId: sarah._id, action: AuditAction.SYNC, entity: 'CheckIn', entityId: checkins[1]._id, payload: { wasOfflineSync: true } },
    { userId: priya._id, action: AuditAction.CREATE, entity: 'Goal', entityId: priyaGoal1._id, payload: { title: priyaGoal1.title } },
    { userId: raj._id, action: AuditAction.UPDATE, entity: 'Goal', entityId: priyaGoal1._id, payload: { status: 'APPROVED' } },
    { userId: priya._id, action: AuditAction.SYNC, entity: 'CheckIn', entityId: checkins[7]._id, payload: { wasOfflineSync: true } },
    { userId: sarah._id, action: AuditAction.UPDATE, entity: 'Goal', entityId: sarahGoal2._id, payload: { currentValue: 95000 } },
    { userId: priya._id, action: AuditAction.UPDATE, entity: 'Goal', entityId: priyaGoal2._id, payload: { currentValue: 80 } },
  ]);

  // Notifications (3 per employee)
  await Notification.insertMany([
    { userId: sarah._id, title: 'Goal Approved', message: `Your goal "${sarahGoal1.title}" was approved by Arun.`, type: NotificationType.INFO, read: false },
    { userId: sarah._id, title: 'Check-in Reminder', message: 'You have goals pending updates this week.', type: NotificationType.ALERT, read: false },
    { userId: sarah._id, title: 'High Risk Goal', message: `Your goal "${sarahGoal1.title}" is marked as high risk.`, type: NotificationType.WARNING, read: false },
    
    { userId: priya._id, title: 'Goal Approved', message: `Your goal "${priyaGoal1.title}" was approved by Raj.`, type: NotificationType.INFO, read: false },
    { userId: priya._id, title: 'Goal Rejected', message: `Your goal "Write 10 technical blog posts" needs revision.`, type: NotificationType.WARNING, read: false },
    { userId: priya._id, title: 'Check-in Reminder', message: 'You have goals pending updates this week.', type: NotificationType.ALERT, read: false },
  ]);

  return NextResponse.json({
    message: 'Seed complete',
    accounts: [
      { email: 'sarah@demo.com', password: 'demo1234', role: 'EMPLOYEE' },
      { email: 'arun@demo.com', password: 'demo1234', role: 'MANAGER' },
      { email: 'admin@demo.com', password: 'demo1234', role: 'ADMIN' },
      { email: 'priya@demo.com', password: 'demo1234', role: 'EMPLOYEE' },
      { email: 'raj@demo.com', password: 'demo1234', role: 'MANAGER' },
    ],
    stats: {
      users: 5,
      goals: goals.length,
      checkIns: checkins.length,
    },
  })
}