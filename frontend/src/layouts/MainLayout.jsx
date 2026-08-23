import { NavLink, Outlet } from 'react-router-dom'
import { UserButton } from '@clerk/clerk-react'

const navItems = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/resumes', label: 'Resumes' },
  { to: '/jobs', label: 'Jobs' },
  { to: '/cover-letters', label: 'Cover Letters' },
  { to: '/interview-prep', label: 'Interview Prep' },
]

export default function MainLayout() {
  return (
    <div className="flex min-h-screen bg-paper">
      <aside className="flex w-60 shrink-0 flex-col bg-ink px-4 py-6">
        <div className="px-2 font-display text-lg font-semibold tracking-tight text-paper">
          InterviewIQ
        </div>

        <nav className="mt-8 space-y-0.5">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `relative block rounded-md px-3 py-2 text-sm transition-colors ${
                  isActive
                    ? 'bg-white/10 font-medium text-paper'
                    : 'text-paper/60 hover:bg-white/5 hover:text-paper/90'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  {isActive && (
                    <span className="absolute -left-4 top-1/2 h-4 w-1 -translate-y-1/2 rounded-r bg-ochre" />
                  )}
                  {item.label}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="mt-auto px-2 pt-6 text-xs leading-relaxed text-paper/35">
          Resume analysis, job matching, and interview prep in one place.
        </div>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex items-center justify-end border-b border-line px-6 py-3">
          <UserButton afterSignOutUrl="/login" />
        </header>
        <main className="flex-1 px-6 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}