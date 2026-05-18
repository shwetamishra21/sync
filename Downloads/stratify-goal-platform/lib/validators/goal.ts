import { z } from 'zod'

export const goalSchema = z.object({
  title: z.string().min(3, 'Title must be at least 3 characters'),
  description: z.string().optional().default(''),
  weightage: z.number().min(1, 'Minimum 1%').max(100, 'Maximum 100%'),
  targetValue: z.number().min(1, 'Target must be at least 1').default(100),
  quarterId: z.string().optional().default('Q2-2025'),
})

export type GoalFormValues = z.infer<typeof goalSchema>