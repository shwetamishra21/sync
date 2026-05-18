// lib/env.ts
import { z } from 'zod'

const envSchema = z.object({
  MONGODB_URI: z.string().min(1, 'MONGODB_URI is required'),
  NEXTAUTH_SECRET: z.string().min(1, 'NEXTAUTH_SECRET is required'),
  NEXTAUTH_URL: z.string().url().optional(),
  CRON_SECRET: z.string().optional(),
  ANTHROPIC_API_KEY: z.string().optional(),
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
})

export const env = envSchema.parse({
  MONGODB_URI: process.env.MONGODB_URI,
  NEXTAUTH_SECRET: process.env.NEXTAUTH_SECRET,
  NEXTAUTH_URL: process.env.NEXTAUTH_URL,
  CRON_SECRET: process.env.CRON_SECRET,
  ANTHROPIC_API_KEY: process.env.ANTHROPIC_API_KEY,
  NODE_ENV: process.env.NODE_ENV,
})
