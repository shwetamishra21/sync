// app/api/analytics/export/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import connectDB from '@/lib/mongodb'
import Goal from '@/models/Goal'
import Quarter from '@/models/Quarter'

export async function GET(req: NextRequest) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user || (session.user as any).role === 'EMPLOYEE') {
      return new NextResponse('Unauthorized', { status: 401 })
    }

    await connectDB()
    const activeQuarter = await Quarter.getActiveQuarter()
    const quarterName = activeQuarter ? activeQuarter.name : 'Current Quarter'

    const goals = await Goal.find({ quarterId: activeQuarter?._id }).populate('userId').lean()
    
    let totalGoals = goals.length
    let atRisk = goals.filter(g => g.riskScore > 50).length

    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>Stratify Analytics Report - ${quarterName}</title>
        <style>
          body { font-family: sans-serif; padding: 40px; color: #333; }
          h1 { color: #111; border-bottom: 2px solid #22c55e; padding-bottom: 10px; }
          .stats { display: flex; gap: 20px; margin-bottom: 30px; }
          .stat-card { background: #f9fafb; border: 1px solid #e5e7eb; padding: 20px; border-radius: 8px; flex: 1; }
          .stat-card h3 { margin: 0 0 10px 0; font-size: 14px; color: #6b7280; text-transform: uppercase; }
          .stat-card p { margin: 0; font-size: 24px; font-weight: bold; color: #111; }
          table { width: 100%; border-collapse: collapse; margin-top: 20px; }
          th, td { padding: 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
          th { background: #f9fafb; font-weight: 600; }
          .at-risk { color: #ef4444; font-weight: bold; }
        </style>
      </head>
      <body>
        <h1>Stratify Analytics Report: ${quarterName}</h1>
        <p>Generated on: ${new Date().toLocaleDateString()}</p>
        
        <div class="stats">
          <div class="stat-card">
            <h3>Total Goals</h3>
            <p>${totalGoals}</p>
          </div>
          <div class="stat-card">
            <h3>At Risk Goals</h3>
            <p class="at-risk">${atRisk}</p>
          </div>
        </div>
        
        <h2>All Goals</h2>
        <table>
          <thead>
            <tr>
              <th>Employee</th>
              <th>Goal</th>
              <th>Progress</th>
              <th>Risk Score</th>
            </tr>
          </thead>
          <tbody>
            ${goals.map(g => `
              <tr>
                <td>${(g.userId as any)?.name || 'Unknown'}</td>
                <td>${g.title}</td>
                <td>${Math.round((g.currentValue / g.targetValue) * 100) || 0}%</td>
                <td class="${g.riskScore > 50 ? 'at-risk' : ''}">${g.riskScore}</td>
              </tr>
            `).join('')}
            ${totalGoals === 0 ? '<tr><td colspan="4">No goals found for this quarter.</td></tr>' : ''}
          </tbody>
        </table>
      </body>
      </html>
    `

    return new NextResponse(html, {
      headers: {
        'Content-Type': 'text/html',
        'Content-Disposition': 'attachment; filename=stratify-report.html'
      }
    })
  } catch (err) {
    console.error('[Export Analytics]', err)
    return new NextResponse('Internal Server Error', { status: 500 })
  }
}
