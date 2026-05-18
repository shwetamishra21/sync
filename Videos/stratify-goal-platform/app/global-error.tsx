// app/global-error.tsx
'use client'

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <html>
      <body>
        <div className="flex h-screen flex-col items-center justify-center space-y-4 px-4 text-center bg-gray-950">
          <div className="bg-red-500/10 p-4 rounded-full">
            <svg className="w-12 h-12 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-white">Critical Platform Error</h2>
          <p className="text-gray-400 max-w-md">{error.message || 'We encountered a critical error.'}</p>
          <button
            onClick={() => reset()}
            className="mt-4 px-6 py-2 bg-green-600 hover:bg-green-500 text-white rounded-lg font-medium transition"
          >
            Recover
          </button>
        </div>
      </body>
    </html>
  )
}
