// app/api/health/route.ts
// Tiny health endpoint used by useOnlineStatus() heartbeat.
// Must be as fast as possible — no DB calls.

import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json({ ok: true }, { status: 200 });
}

export async function HEAD() {
  return new NextResponse(null, { status: 200 });
}
