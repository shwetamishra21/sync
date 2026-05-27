// components/goals/ShareGoalModal.tsx
'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

interface User {
  _id: string
  name: string
  email: string
}

interface ShareGoalModalProps {
  goalId: string
  onClose: () => void
}

export function ShareGoalModal({ goalId, onClose }: ShareGoalModalProps) {
  const qc = useQueryClient()
  const [targetUserId, setTargetUserId] = useState('')
  const [weightage, setWeightage] = useState(10)

  const { data: users } = useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      const res = await fetch('/api/users')
      if (!res.ok) throw new Error('Failed to fetch')
      return res.json() as Promise<User[]>
    }
  })

  const shareMutation = useMutation({
    mutationFn: async () => {
      const res = await fetch(`/api/goals/${goalId}/share`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ targetUserId, weightage })
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Failed to share')
      return data
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['goal', goalId] })
      qc.invalidateQueries({ queryKey: ['goals'] })
      onClose()
    },
    onError: (err: any) => {
      alert(err.message)
    }
  })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md shadow-2xl slide-up">
        <h2 className="text-xl font-bold text-white mb-4">Share Goal</h2>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1">Team Member</label>
            <select
              value={targetUserId}
              onChange={e => setTargetUserId(e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-green-500"
            >
              <option value="">Select a member...</option>
              {users?.map(u => (
                <option key={u._id} value={u._id}>{u.name} ({u.email})</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1">Weightage for their goal (%)</label>
            <input
              type="number"
              min="1"
              max="100"
              value={weightage}
              onChange={e => setWeightage(Number(e.target.value))}
              className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-green-500"
            />
          </div>
          <div className="flex gap-3 pt-2">
            <button
              onClick={onClose}
              disabled={shareMutation.isPending}
              className="flex-1 px-4 py-2 rounded-lg font-semibold text-gray-400 hover:text-white hover:bg-gray-800 transition"
            >
              Cancel
            </button>
            <button
              onClick={() => shareMutation.mutate()}
              disabled={!targetUserId || shareMutation.isPending}
              className="flex-1 px-4 py-2 bg-green-600 hover:bg-green-500 disabled:opacity-50 text-white rounded-lg font-semibold transition"
            >
              {shareMutation.isPending ? 'Sharing...' : 'Share Goal'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
