import Link from 'next/link'

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-white text-gray-900 font-sans p-6">
      <div className="max-w-3xl w-full flex flex-col items-center text-center">
        <div className="inline-flex items-center justify-center w-20 h-20 rounded-[2rem] bg-green-700 mb-8 shadow-sm">
          <svg className="w-10 h-10 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        
        <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight mb-6 text-gray-900">
          Welcome to Stratify
        </h1>
        
        <p className="text-xl md:text-2xl text-gray-500 mb-12 max-w-2xl leading-relaxed">
          The ultimate enterprise platform for setting, tracking, and achieving strategic goals across your organization.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 w-full max-w-2xl">
          {[
            { role: 'Employee', id: 'EMPLOYEE', icon: '👤', desc: 'Manage personal OKRs' },
            { role: 'Manager', id: 'MANAGER', icon: '👥', desc: 'Review & approve goals' },
            { role: 'Admin', id: 'ADMIN', icon: '⚙️', desc: 'System configuration' },
          ].map((item) => (
            <Link
              key={item.id}
              href={`/login?role=${item.id}&mode=register`}
              className="flex flex-col items-center p-8 border border-gray-200 rounded-3xl bg-white hover:border-green-600 hover:ring-1 hover:ring-green-600 hover:shadow-lg hover:-translate-y-1 transition-all group"
            >
              <div className="text-4xl mb-4">{item.icon}</div>
              <div className="font-semibold text-gray-900 text-lg">{item.role}</div>
              <div className="text-xs text-gray-500 mt-2 text-center">{item.desc}</div>
              <div className="mt-4 text-xs font-medium text-green-700 bg-green-50 px-3 py-1 rounded-full opacity-0 group-hover:opacity-100 transition-opacity">
                Select Role &rarr;
              </div>
            </Link>
          ))}
        </div>
        
        <div className="mt-16 text-sm text-gray-500">
          Already have an account?{' '}
          <Link href="/login" className="text-green-700 font-medium hover:underline">
            Log in here
          </Link>
        </div>
      </div>
    </div>
  )
}
