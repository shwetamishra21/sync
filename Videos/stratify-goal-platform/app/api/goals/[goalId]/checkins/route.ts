// app/api/goals/[goalId]/checkins/route.ts
// Server-side check-in handler.
// Accepts both online and offline-replayed requests.
// Detects offline writes via X-Offline-Sync header and stamps _syncMeta.

import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import connectDB from '@/lib/mongodb';
import Goal from '@/models/Goal';
import AuditLog, { AuditAction } from '@/models/AuditLog';
import CheckIn from '@/models/CheckIn';
import Quarter from '@/models/Quarter';
import { computeRiskScore } from '@/lib/risk';
import mongoose from 'mongoose';
import { z } from 'zod';

const checkInSchema = z.object({
  progressValue: z.number().min(0),
  progressPercent: z.number().min(0).max(200), // allow >100% to track overachievement
  notes: z.string().max(500).optional(),
  submittedAt: z.string().datetime().optional(),
  _syncMeta: z.object({
    isOfflineWrite: z.boolean(),
    clientTimestamp: z.string(),
    deviceId: z.string(),
    outboxId: z.number().optional(),
  }).optional(),
});

export async function POST(
  req: NextRequest,
  { params }: { params: Promise<{ goalId: string }> }
) {
  const session = await getServerSession(authOptions);
  if (!session?.user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const { goalId } = await params;
  const userId = (session.user as { id?: string }).id;
  if (!userId) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const isOfflineSync = req.headers.get('X-Offline-Sync') === 'true';
  const clientTimestamp = req.headers.get('X-Client-Timestamp');
  const deviceId = req.headers.get('X-Device-Id');

  const body = await req.json().catch(() => null);
  if (!body) {
    return NextResponse.json({ error: 'Invalid body' }, { status: 400 });
  }

  const parsed = checkInSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: 'Validation failed', issues: parsed.error.issues },
      { status: 422 }
    );
  }

  await connectDB();

  const goal = await Goal.findById(goalId);
  if (!goal) {
    return NextResponse.json({ error: 'Goal not found' }, { status: 404 });
  }

  const quarter = await Quarter.findById(goal.quarterId);
  if (!quarter) {
    return NextResponse.json({ error: 'Quarter not found' }, { status: 404 });
  }

  if (quarter.status === 'CLOSED') {
    if (isOfflineSync) {
      const clientTimestampDate = clientTimestamp ? new Date(clientTimestamp) : new Date();
      const diffMs = clientTimestampDate.getTime() - quarter.endDate.getTime();
      if (diffMs > 5 * 60 * 1000) {
        return NextResponse.json({ conflict: true, reason: 'quarter_closed' }, { status: 409 });
      }
    } else {
      return NextResponse.json({ error: 'Quarter is closed for check-ins' }, { status: 400 });
    }
  }

  // ── Conflict detection ─────────────────────────────────────────────────────
  // If this is an offline replay, check if the goal's state still allows it.
  if (isOfflineSync) {
    if (['REJECTED', 'COMPLETED', 'LOCKED'].includes(goal.status)) {
      return NextResponse.json(
        {
          error: `Goal is ${goal.status} — offline check-in cannot be applied`,
          conflict: true,
          currentStatus: goal.status,
        },
        { status: 409 }
      );
    }
  }

  // ── Transactional write ────────────────────────────────────────────────────
  const dbSession = await mongoose.startSession();
  dbSession.startTransaction();

  try {
    const syncMeta = {
      isOfflineWrite: isOfflineSync,
      clientTimestamp: clientTimestamp ?? new Date().toISOString(),
      deviceId: deviceId ?? 'unknown',
      syncedAt: isOfflineSync ? new Date() : null,
    };

    // Create standalone check-in document
    const [checkIn] = await CheckIn.create(
      [
        {
          goalId: goal._id,
          progressValue: parsed.data.progressValue,
          progressPercent: parsed.data.progressPercent,
          notes: parsed.data.notes,
          submittedBy: new mongoose.Types.ObjectId(userId),
          submittedAt: parsed.data.submittedAt
            ? new Date(parsed.data.submittedAt)
            : new Date(),
          serverReceivedAt: new Date(),
          _syncMeta: syncMeta,
        },
      ],
      
    );

    // Update goal's currentValue and riskScore
    const newRiskScore = computeRiskScore(
      { ...goal.toObject(), currentValue: parsed.data.progressValue },
      quarter.startDate,
      quarter.endDate
    );

    await Goal.findByIdAndUpdate(
      goal._id,
      {
        currentValue: parsed.data.progressValue,
        riskScore: newRiskScore,
      },
      
    );

    await AuditLog.create([{
      userId: userId,
      action: isOfflineSync ? AuditAction.SYNC : AuditAction.UPDATE,
      entity: 'CheckIn',
      entityId: checkIn._id,
      payload: { progressValue: parsed.data.progressValue, notes: parsed.data.notes, isOfflineSync }
    }]);

    await dbSession.commitTransaction();

    return NextResponse.json(
      {
        success: true,
        checkInId: checkIn._id,
        isOfflineSync,
        message: isOfflineSync ? 'Offline check-in synced' : 'Check-in saved',
      },
      { status: 201 }
    );
  } catch (err) {
    await dbSession.abortTransaction();
    console.error('Check-in failed:', err);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  } finally {
    await dbSession.endSession();
  }
}
