import { z } from "zod";

export const checkInSchema = z.object({
  progress: z.number().min(0).max(100),
  notes: z.string().min(1),
});