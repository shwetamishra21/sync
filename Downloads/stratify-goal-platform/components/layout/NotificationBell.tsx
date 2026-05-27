// components/layout/NotificationBell.tsx
'use client'

import { useState, useRef, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Bell } from 'lucide-react'

interface Notification {
  _id: string
  title: string
  message: string
  type: string
  createdAt: string
  read: boolean
}

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const qc = useQueryClient()

  const { data } = useQuery({
    queryKey: ['notifications'],
    queryFn: async () => {
      const res = await fetch('/api/notifications')
      if (!res.ok) throw new Error('Failed to fetch')
      return res.json()
    },
    refetchInterval: 30_000,
  })

  const markRead = useMutation({
    mutationFn: async (id: string) => {
      const res = await fetch(`/api/notifications/${id}/read`, { method: 'PATCH' })
      if (!res.ok) throw new Error('Failed to mark read')
      return res.json()
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] })
    }
  })

  const notifications: Notification[] = data?.notifications || []
  const unreadCount = notifications.length

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 text-gray-400 hover:text-white transition rounded-full hover:bg-gray-800"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-gray-900" />
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 bg-gray-900 border border-gray-800 rounded-2xl shadow-xl z-50 overflow-hidden">
          <div className="p-4 border-b border-gray-800 flex items-center justify-between">
            <h3 className="font-semibold text-white">Notifications</h3>
            {unreadCount > 0 && (
              <button
                onClick={() => markRead.mutate('all')}
                className="text-xs text-green-400 hover:text-green-300 font-medium"
              >
                Mark all read
              </button>
            )}
          </div>
          <div className="max-h-80 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="p-6 text-center text-gray-500 text-sm">
                No new notifications
              </div>
            ) : (
              <div className="divide-y divide-gray-800">
                {notifications.map((n) => (
                  <div key={n._id} className="p-4 hover:bg-gray-800/50 transition relative group cursor-pointer" onClick={() => markRead.mutate(n._id)}>
                    <div className="flex gap-3">
                      <div className={`w-2 h-2 rounded-full mt-1.5 flex-shrink-0 ${
                        n.type === 'ALERT' ? 'bg-red-500' :
                        n.type === 'WARNING' ? 'bg-amber-500' : 'bg-blue-500'
                      }`} />
                      <div>
                        <p className="text-sm font-medium text-gray-200">{n.title}</p>
                        <p className="text-xs text-gray-400 mt-0.5 line-clamp-2">{n.message}</p>
                        <p className="text-[10px] text-gray-500 mt-2">
                          {new Date(n.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
