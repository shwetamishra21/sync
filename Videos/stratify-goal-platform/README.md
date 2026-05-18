# Stratify Platform

Stratify is a modern, dark-themed enterprise goal-tracking platform built with Next.js 15 (App Router).

## Tech Stack
- **Framework:** Next.js 15 (App Router)
- **Language:** TypeScript
- **Database:** MongoDB + Mongoose
- **Auth:** NextAuth (Credentials)
- **Styling:** Tailwind CSS v4
- **State/Fetching:** TanStack Query
- **Forms:** React Hook Form + Zod
- **Charts:** Recharts
- **Offline Sync:** Dexie.js (IndexedDB) + Custom Outbox pattern
- **PWA:** next-pwa (Workbox)
- **AI:** Anthropic Claude API (Goal Suggestions)

## Key Features
- **Goal Management:** Create, submit, approve, and track quarterly goals.
- **Offline-First:** Create goals and check-ins offline, sync when reconnected with conflict resolution.
- **Real-Time Dashboard:** Donut charts, KPI cards, and team overviews.
- **AI Suggestions:** Integrated Anthropic API for goal generation based on roles and focus areas.
- **Manager Commentary:** Direct inline feedback on check-ins.
- **Risk Scoring:** Automated risk score computation based on days remaining and progress.
- **PDF Export:** Analytics reports for quarterly performance.

## Setup Instructions

1. **Install Dependencies:**
   \`\`\`bash
   npm install
   \`\`\`

2. **Environment Variables:**
   Create a \`.env.local\` file with the following variables:
   \`\`\`env
   MONGODB_URI=your_mongodb_uri
   NEXTAUTH_SECRET=your_nextauth_secret
   NEXTAUTH_URL=http://localhost:3000
   CRON_SECRET=optional_cron_secret
   ANTHROPIC_API_KEY=optional_anthropic_api_key
   \`\`\`

3. **Seed Database:**
   To populate demo data (Users, Departments, Quarters, Goals, CheckIns):
   \`\`\`bash
   # Make sure the app is running first
   curl -X POST http://localhost:3000/api/seed
   \`\`\`

4. **Run Development Server:**
   \`\`\`bash
   npm run dev
   \`\`\`

## Demo Accounts (Password: demo1234)
- **Admin:** admin@demo.com
- **Manager:** arun@demo.com
- **Employee:** sarah@demo.com
