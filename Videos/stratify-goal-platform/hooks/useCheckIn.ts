// hooks/useCheckIn.ts
// The hook that powers offline check-ins — the "wow moment" in your demo.
// Wraps mutateOfflineAware() with React Query optimistic updates.

'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mutateOfflineAware } from '@/lib/offline/mutateOfflineAware';
import { useSyncStatus } from './useSyncStatus';

export interface CheckInPayload {
  goalId: string;
  progressValue: number;
  progressPercent: number;
  notes?: string;
}

export interface CheckInResult {
  isOffline: boolean;
  outboxId?: number;
}

export function useCheckIn() {
  const queryClient = useQueryClient();
  const { refreshCounts } = useSyncStatus();

  const mutation = useMutation({
    mutationFn: async (payload: CheckInPayload): Promise<CheckInResult> => {
      const result = await mutateOfflineAware({
        endpoint: `/api/goals/${payload.goalId}/check-in`,
        method: 'POST',
        payload: {
          progressValue: payload.progressValue,
          progressPercent: payload.progressPercent,
          notes: payload.notes,
          submittedAt: new Date().toISOString(),
        },
        onOptimisticUpdate: () => {
          // Immediately update the goal's progress in React Query cache
          queryClient.setQueryData(
            ['goal', payload.goalId],
            (old: Record<string, unknown> | undefined) => {
              if (!old) return old;
              return {
                ...old,
                currentValue: payload.progressValue,
                // Add pending check-in to the embedded list
                checkIns: [
                  {
                    progressValue: payload.progressValue,
                    progressPercent: payload.progressPercent,
                    notes: payload.notes,
                    submittedAt: new Date().toISOString(),
                    isOfflinePending: !navigator.onLine,
                  },
                  ...((old.checkIns as unknown[]) ?? []),
                ],
              };
            }
          );
          // Invalidate goals list so progress bars update
          queryClient.invalidateQueries({ queryKey: ['goals'] });
        },
        onSuccess: () => {
          // Server confirmed — refetch to get the real data
          queryClient.invalidateQueries({ queryKey: ['goal', payload.goalId] });
          queryClient.invalidateQueries({ queryKey: ['goals'] });
        },
      });

      // Update sync badge count
      await refreshCounts();

      return {
        isOffline: !result.online,
        outboxId: result.outboxId,
      };
    },
  });

  return mutation;
}
