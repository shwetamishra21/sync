// app/api/ai/suggest-goals/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'

export async function POST(req: NextRequest) {
  try {
    const session = await getServerSession(authOptions)
    if (!session?.user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }

    const { role, focusArea } = await req.json()
    if (!role || !focusArea) {
      return NextResponse.json({ error: 'Role and focusArea required' }, { status: 400 })
    }

    const apiKey = process.env.ANTHROPIC_API_KEY
    if (!apiKey) {
      // Graceful fallback mocking AI
      return NextResponse.json({
        suggestions: [
          { title: `Mocked: Increase ${focusArea} by 20%`, description: 'Generated because API key is missing' },
          { title: `Mocked: Implement new processes for ${role}`, description: 'Ensure all team members are trained.' },
          { title: `Mocked: Review quarterly targets`, description: 'Track progress across all major KPIs.' }
        ]
      })
    }

    const prompt = `You are an expert OKR coach. Suggest exactly 3 highly quantified, role-specific goals for a "${role}" focusing on "${focusArea}".
Respond in strict JSON format:
{ "suggestions": [ { "title": "string", "description": "string" } ] }
Do not output any other text.`

    const res = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'x-api-key': apiKey,
        'anthropic-version': '2023-06-01',
        'content-type': 'application/json'
      },
      body: JSON.stringify({
        model: 'claude-3-haiku-20240307',
        max_tokens: 500,
        messages: [{ role: 'user', content: prompt }]
      })
    })

    if (!res.ok) {
      const errText = await res.text()
      console.error('[Anthropic API Error]', errText)
      throw new Error('Failed to fetch from Anthropic')
    }

    const data = await res.json()
    const content = data.content[0].text
    
    // Parse the JSON out of Claude's response
    const jsonMatch = content.match(/\{[\s\S]*\}/)
    if (!jsonMatch) throw new Error('Failed to parse AI response as JSON')

    const parsed = JSON.parse(jsonMatch[0])
    return NextResponse.json({ suggestions: parsed.suggestions })
  } catch (err) {
    console.error('[POST /api/ai/suggest-goals]', err)
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 })
  }
}
