import { Suspense, useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { UserButton } from '@clerk/clerk-react'
import { StageSuggestionProvider } from '../components/StageSuggestion'
import { Skeleton, SkeletonCard } from '../components/Skeleton'

const SIDEBAR_KEY = 'interviewiq.sidebar'
const DESKTOP_QUERY = '(min-width: 1024px)'

function readSidebarOpen() {
  try {
    return localStorage.getItem(SIDEBAR_KEY) !== 'collapsed'
  } catch {
    return true
  }
}

function useIsDesktop() {
  const [isDesktop, setIsDesktop] = useState(() => window.matchMedia(DESKTOP_QUERY).matches)
  useEffect(() => {
    const mq = window.matchMedia(DESKTOP_QUERY)
    const onChange = (e) => setIsDesktop(e.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])
  return isDesktop
}

const navItems = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/resumes', label: 'Resumes' },
  { to: '/jobs', label: 'Jobs' },
  { to: '/applications', label: 'Applications' },
  { to: '/roadmaps', label: 'Roadmaps' },
  { to: '/cover-letters', label: 'Cover Letters' },
  { to: '/interview-prep', label: 'Interview Prep' },
]

function NavLinks({ onNavigate }) {
  return (
    <nav className="mt-8 space-y-0.5">
      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          onClick={onNavigate}
          className={({ isActive }) =>
            `relative block rounded-md px-3 py-2.5 text-sm transition-colors lg:py-2 ${
              isActive
                ? 'bg-chrome-ink/10 font-medium text-chrome-ink'
                : 'text-chrome-ink/60 hover:bg-chrome-ink/5 hover:text-chrome-ink/90'
            }`
          }
        >
          {({ isActive }) => (
            <>
              {isActive && (
                <span className="absolute -left-4 top-1/2 h-4 w-1 -translate-y-1/2 rounded-r bg-accent" />
              )}
              {item.label}
            </>
          )}
        </NavLink>
      ))}
    </nav>
  )
}

function SidebarContent({ onNavigate }) {
  return (
    <>
      <div className="px-2 font-display text-lg font-semibold tracking-tight text-chrome-ink">InterviewIQ</div>
      <NavLinks onNavigate={onNavigate} />
      <div className="mt-auto px-2 pt-6 text-xs leading-relaxed text-chrome-muted">
        Resume analysis, job matching, and interview prep in one place.
      </div>
    </>
  )
}

// Shown only while a page's code chunk loads; the sidebar and header stay in place.
function PageFallback() {
  return (
    <div role="status" aria-label="Loading" className="mx-auto max-w-6xl space-y-4">
      <Skeleton className="h-8 w-56" />
      <SkeletonCard lines={3} />
      <SkeletonCard lines={2} />
    </div>
  )
}

export default function MainLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  // Desktop: the hamburger shows or hides the sidebar, and the choice is remembered.
  // Below 1024px the same button opens the drawer instead.
  const [sidebarOpen, setSidebarOpen] = useState(readSidebarOpen)
  const isDesktop = useIsDesktop()
  const location = useLocation()

  useEffect(() => {
    try {
      localStorage.setItem(SIDEBAR_KEY, sidebarOpen ? 'pinned' : 'collapsed')
    } catch {
      // Storage unavailable (private mode); the choice just won't persist.
    }
  }, [sidebarOpen])

  const toggleNav = () => (isDesktop ? setSidebarOpen((open) => !open) : setMenuOpen(true))
  const navExpanded = isDesktop ? sidebarOpen : menuOpen
  const showWordmark = !isDesktop || !sidebarOpen

  // Close the mobile drawer whenever the route changes.
  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  // Escape closes the drawer; lock body scroll while it is open.
  useEffect(() => {
    if (!menuOpen) return undefined
    const onKey = (e) => e.key === 'Escape' && setMenuOpen(false)
    document.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [menuOpen])

  return (
    <div className="flex min-h-screen bg-paper">
      {/* Desktop / laptop sidebar */}
      <aside
        id="desktop-nav"
        className={`sticky top-0 hidden h-screen w-60 shrink-0 flex-col border-r border-line bg-chrome px-4 py-6 ${
          sidebarOpen ? 'lg:flex' : ''
        }`}
      >
        <SidebarContent />
      </aside>

      {/* Mobile / tablet drawer */}
      <div
        className={`fixed inset-0 z-40 bg-ink/40 transition-opacity lg:hidden ${
          menuOpen ? 'opacity-100' : 'pointer-events-none opacity-0'
        }`}
        onClick={() => setMenuOpen(false)}
        aria-hidden="true"
      />
      <aside
        id="mobile-nav"
        className={`fixed inset-y-0 left-0 z-50 flex w-64 max-w-[80vw] flex-col bg-chrome px-4 py-6 transition-transform duration-200 lg:hidden ${
          menuOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
        aria-hidden={!menuOpen}
      >
        <SidebarContent onNavigate={() => setMenuOpen(false)} />
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex items-center justify-between gap-3 border-b border-line bg-paper/95 px-4 py-3 backdrop-blur sm:px-6">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={toggleNav}
              className="inline-flex h-10 w-10 items-center justify-center rounded-md text-ink transition-colors hover:bg-ink/5"
              aria-label={isDesktop ? (sidebarOpen ? 'Hide navigation' : 'Show navigation') : 'Open navigation menu'}
              aria-expanded={navExpanded}
              aria-controls={isDesktop ? 'desktop-nav' : 'mobile-nav'}
            >
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <path d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
            {showWordmark && <span className="font-display text-base font-semibold tracking-tight text-ink">InterviewIQ</span>}
          </div>
          <UserButton afterSignOutUrl="/login" />
        </header>
        <main className="min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          <StageSuggestionProvider>
            <Suspense fallback={<PageFallback />}>
              <Outlet />
            </Suspense>
          </StageSuggestionProvider>
        </main>
      </div>
    </div>
  )
}
