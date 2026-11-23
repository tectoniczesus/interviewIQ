import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useUser } from '@clerk/clerk-react'
import { getDashboardSummary } from '../services/dashboardApi'
import { listApplications, updateApplication } from '../services/applicationApi'
import {
  CLOSED_STAGES,
  FOLLOW_UP_DAYS,
  OPEN_STAGES,
  STAGES,
  addDays,
  attentionFor,
  buildUpdate,
  jobLabel,
  todayIso,
} from '../lib/applications'
import ScoreCard, { tier } from '../components/ScoreCard'
import { Skeleton, SkeletonList } from '../components/Skeleton'
import {
  BriefcaseIcon,
  CheckIcon,
  ChevronRightIcon,
  DocumentIcon,
  MailIcon,
  MicIcon,
  PlusIcon,
  SparkleIcon,
} from '../components/icons'

// Theme: the app-wide Apple-style minimal tokens in index.css (#f5f5f7 ground, white cards,
// #1d1d1f ink, #0066cc links, system SF Pro type), chosen by the user over the tasteskill deals. Motion: the hero enters once on load, the stat
// figures count up once per session, linked cards lift 2px, buttons step and press.

const HERO_RING = 128
const COUNTED_KEY = 'iq:dashboard-counted'
const COUNT_MS = 1000

const prefersReducedMotion = () =>
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

// Counts from 0 to the value once, eased out. Screen readers get the final value only.
function CountUp({ value, animate }) {
  const [shown, setShown] = useState(animate ? 0 : value)
  useEffect(() => {
    if (!animate || typeof value !== 'number') {
      setShown(value)
      return undefined
    }
    let frame
    const start = performance.now()
    const tick = (now) => {
      const t = Math.min(1, (now - start) / COUNT_MS)
      setShown(Math.round(value * (1 - (1 - t) ** 3)))
      if (t < 1) frame = requestAnimationFrame(tick)
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [value, animate])
  return (
    <>
      <span aria-hidden="true">{shown}</span>
      <span className="sr-only">{value}</span>
    </>
  )
}

// Full class strings (not built dynamically) so Tailwind's JIT keeps them.
const PILL_BY_TIER = {
  'text-verdant': 'bg-verdant/10 text-verdant',
  'text-ochre': 'bg-ochre/10 text-ochre',
  'text-brick': 'bg-brick/10 text-brick',
}

const rtf = new Intl.RelativeTimeFormat('en', { numeric: 'auto' })
const UNITS = [
  ['year', 31536000],
  ['month', 2592000],
  ['week', 604800],
  ['day', 86400],
  ['hour', 3600],
  ['minute', 60],
]

function timeAgo(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return ''
  const seconds = Math.round((date.getTime() - Date.now()) / 1000)
  for (const [unit, size] of UNITS) {
    if (Math.abs(seconds) >= size) return rtf.format(Math.round(seconds / size), unit)
  }
  return 'just now'
}

function greeting() {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
}

const todayLabel = () =>
  new Date().toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' })

const DEFAULT_SUBTITLE = 'Here’s where your job search stands today.'

// A subtitle that reports something true about the pipeline, in priority order.
function subtitleFor(apps, attentionCount) {
  if (apps.length === 0) return DEFAULT_SUBTITLE
  if (attentionCount > 0) {
    return attentionCount === 1 ? '1 application needs you today.' : `${attentionCount} applications need you today.`
  }
  const offers = apps.filter((a) => a.status === 'OFFER').length
  if (offers > 0) return offers === 1 ? 'You have an offer to weigh.' : `You have ${offers} offers to weigh.`
  const interviewing = apps.filter((a) => a.status === 'INTERVIEWING').length
  if (interviewing > 0) {
    return interviewing === 1 ? '1 application is in interviews.' : `${interviewing} applications are in interviews.`
  }
  return DEFAULT_SUBTITLE
}

function IconChip({ icon: IconCmp }) {
  return (
    <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-paper text-ash transition-colors duration-150 group-hover:text-accent">
      <IconCmp size={18} />
    </span>
  )
}

function StatTile({ label, value, icon: IconCmp, to, loading, count }) {
  return (
    <Link
      to={to}
      className="group flex flex-col justify-between gap-6 p-4 transition-colors duration-150 hover:bg-paper/[0.04] sm:p-5"
    >
      <div className="flex items-center justify-between text-paper/60">
        <IconCmp size={18} />
        <ChevronRightIcon size={16} className="dash-arrow text-paper/40 group-hover:text-paper" />
      </div>
      <div>
        {loading ? (
          <Skeleton className="h-10 w-14 !bg-paper/10" />
        ) : (
          <div className="font-display text-4xl font-semibold leading-none tracking-tight tabular-nums text-paper sm:text-5xl">
            {typeof value === 'number' ? <CountUp value={value} animate={count} /> : '—'}
          </div>
        )}
        <div className="mt-2 text-xs font-medium uppercase tracking-wide text-paper/75">{label}</div>
      </div>
    </Link>
  )
}

function ScorePill({ score, prefix = '', suffix = '' }) {
  if (score == null) {
    return (
      <span className="shrink-0 rounded-full bg-line/60 px-2 py-0.5 text-xs font-medium text-ash">Not analyzed</span>
    )
  }
  return (
    <span
      className={`shrink-0 rounded-full px-2 py-0.5 font-mono text-xs font-medium tabular-nums ${PILL_BY_TIER[tier(score).text]}`}
    >
      {prefix}
      {score}
      {suffix}
    </span>
  )
}

function RecentRow({ to, icon, title, createdAt, pill }) {
  return (
    <Link to={to} className="card-link group flex items-center gap-3 px-4 py-3 text-sm">
      <IconChip icon={icon} />
      <div className="min-w-0 flex-1">
        <div className="truncate font-medium text-ink">{title}</div>
        {createdAt && <div className="mt-0.5 text-[13px] text-ash">{timeAgo(createdAt)}</div>}
      </div>
      {pill}
      <ChevronRightIcon size={16} className="dash-arrow shrink-0 text-ash/60 group-hover:text-accent" />
    </Link>
  )
}

function EmptyState({ icon: IconCmp, text, cta, to }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-line bg-surface/50 px-4 py-8 text-center">
      <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-paper text-ash">
        <IconCmp size={20} />
      </span>
      <p className="text-sm text-ash">{text}</p>
      <Link to={to} className="text-sm font-medium text-ink underline decoration-accent/50 underline-offset-4 transition-colors hover:decoration-accent">
        {cta}
      </Link>
    </div>
  )
}

function SectionHeader({ title, to, id }) {
  return (
    <div className="flex items-baseline justify-between gap-3">
      <h2 id={id} className="font-display text-lg text-ink">{title}</h2>
      <Link to={to} className="group inline-flex min-h-10 items-center gap-1 text-sm font-medium text-ash transition-colors hover:text-ink">
        View all<span className="sr-only"> {title.toLowerCase()}</span>
        <ChevronRightIcon size={16} className="dash-arrow" />
      </Link>
    </div>
  )
}

function GettingStarted({ summary }) {
  const steps = [
    { label: 'Upload a resume', done: summary.resumeCount > 0, to: '/resumes' },
    { label: 'Run an ATS analysis', done: summary.resumeScore != null, to: '/resumes' },
    { label: 'Match it against a job', done: summary.jobMatchCount > 0, to: '/jobs' },
  ]
  const completed = steps.filter((s) => s.done).length

  return (
    <section className="card mt-8" aria-labelledby="getting-started">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="getting-started" className="font-display text-lg text-ink">
          Get started
        </h2>
        <span className="font-mono text-xs text-ash">
          {completed}/{steps.length} done
        </span>
      </div>
      <ol className="mt-4 grid gap-2 sm:grid-cols-3">
        {steps.map((step, i) => (
          <li key={step.label}>
            {step.done ? (
              <div className="flex items-center gap-3 rounded-lg bg-verdant/10 px-3 py-2.5 text-sm text-ash">
                <span className="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-verdant text-paper">
                  <CheckIcon size={14} />
                </span>
                <span className="line-through decoration-ash/40">{step.label}</span>
              </div>
            ) : (
              <Link
                to={step.to}
                className="card-link group flex items-center gap-3 !rounded-lg px-3 py-2.5 text-sm text-ink"
              >
                <span className="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-line font-mono text-xs text-ash">
                  {i + 1}
                </span>
                <span className="flex-1">{step.label}</span>
                <ChevronRightIcon size={16} className="dash-arrow text-ash/60 group-hover:text-accent" />
              </Link>
            )}
          </li>
        ))}
      </ol>
    </section>
  )
}

function AttentionList({ attention, onSnooze, snoozingId, snoozeError }) {
  return (
    <section className="mt-8" aria-labelledby="attention-heading">
      <h2 id="attention-heading" className="eyebrow">
        Needs attention · <span className="font-mono tabular-nums">{attention.length}</span>
      </h2>
      <ul className="mt-2 space-y-2">
        {attention.map(({ app, reason }) => (
          <li key={app.jobId} className="card flex flex-wrap items-center gap-3 !border-l-[3px] !border-l-brick !py-3">
            <div className="min-w-[12rem] flex-1">
              <div className="truncate text-sm font-medium text-ink">{jobLabel(app)}</div>
              <div className="mt-0.5 text-sm text-brick">{reason.label}</div>
              {snoozeError?.jobId === app.jobId && (
                <div role="alert" className="mt-1 text-xs text-ash">
                  {snoozeError.message}
                </div>
              )}
            </div>
            <div className="flex w-full gap-2 sm:w-auto">
              <button
                type="button"
                className="btn-secondary flex-1 text-xs sm:flex-none"
                onClick={() => onSnooze(app)}
                disabled={snoozingId === app.jobId}
              >
                {snoozingId === app.jobId ? 'Snoozing…' : `Snooze ${FOLLOW_UP_DAYS} days`}
              </button>
              <Link to={`/applications?job=${app.jobId}`} className="btn-primary flex-1 text-xs sm:flex-none">
                Open application
              </Link>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

function Pipeline({ apps }) {
  const counts = Object.fromEntries(OPEN_STAGES.map((s) => [s, 0]))
  let closed = 0
  apps.forEach((a) => {
    if (CLOSED_STAGES.includes(a.status)) closed += 1
    else counts[a.status] += 1
  })
  const open = OPEN_STAGES.reduce((n, s) => n + counts[s], 0)
  const summary = OPEN_STAGES.map((s) => `${counts[s]} ${STAGES[s].label.toLowerCase()}`).join(', ')

  return (
    <section className="mt-8" aria-labelledby="pipeline-heading">
      <SectionHeader title="Application pipeline" to="/applications" id="pipeline-heading" />
      <div className="card mt-3">
        <div
          className="bar-reveal flex h-3 gap-0.5 overflow-hidden rounded-full bg-line/60"
          role="img"
          aria-label={`${open} open applications: ${summary}`}
        >
          {OPEN_STAGES.filter((s) => counts[s] > 0).map((s) => (
            <div key={s} className={STAGES[s].dot} style={{ flex: `${counts[s]} 1 0%` }} />
          ))}
        </div>

        <ul className="mt-4 grid grid-cols-2 gap-x-2 gap-y-4 sm:grid-cols-5 sm:gap-x-0 sm:divide-x sm:divide-line">
          {OPEN_STAGES.map((s) => (
            <li key={s} className="sm:px-4 sm:first:pl-0">
              <Link to="/applications" className="group block min-h-10 rounded-md">
                <span className="flex items-center gap-2 text-sm text-ash transition-colors group-hover:text-ink">
                  <span className={`h-2 w-2 rounded-full ${STAGES[s].dot}`} aria-hidden="true" />
                  {STAGES[s].label}
                </span>
                <span
                  className={`mt-1 block font-display text-2xl font-semibold leading-none tracking-tight tabular-nums ${
                    s === 'OFFER' && counts[s] > 0 ? 'text-verdant' : 'text-ink'
                  }`}
                >
                  {counts[s]}
                </span>
              </Link>
            </li>
          ))}
          <li className="col-span-2 sm:col-span-1 sm:px-4 sm:last:pr-0">
            <Link to="/applications" className="group block min-h-10 rounded-md">
              <span className="flex items-center gap-2 text-sm text-ash transition-colors group-hover:text-ink">Closed</span>
              <span className="mt-1 block font-display text-2xl font-semibold leading-none tracking-tight tabular-nums text-ash">
                {closed}
              </span>
            </Link>
          </li>
        </ul>
      </div>
    </section>
  )
}

// Holds the pipeline's place while applications load, so the page is close to its final height
// from the first paint and doesn't present a false bottom that grows once the data arrives.
function PipelineSkeleton() {
  return (
    <section className="mt-8" aria-hidden="true">
      <Skeleton className="h-6 w-48" />
      <div className="card mt-3">
        <Skeleton className="h-3 w-full !rounded-full" />
        <div className="mt-4 grid grid-cols-2 gap-x-2 gap-y-4 sm:grid-cols-5">
          {Array.from({ length: 5 }, (_, i) => (
            <div key={i} className="space-y-2">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-6 w-8" />
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

const quickActions = [
  { label: 'Upload resume', description: 'Add a PDF and get an ATS score', to: '/resumes', icon: DocumentIcon },
  { label: 'Analyze a job', description: 'See how you match a posting', to: '/jobs', icon: BriefcaseIcon },
  { label: 'Write a cover letter', description: 'Draft one tailored to a role', to: '/cover-letters', icon: MailIcon },
  { label: 'Practice interviews', description: 'Rehearse the questions a role is likely to ask', to: '/interview-prep', icon: MicIcon },
]

export default function Dashboard() {
  const { user } = useUser()
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [apps, setApps] = useState([])
  const [appsState, setAppsState] = useState('loading') // loading | ready | failed
  const [snoozingId, setSnoozingId] = useState(null)
  const [snoozeError, setSnoozeError] = useState(null) // { jobId, message }
  const requestId = useRef(0)
  const summaryRequestId = useRef(0)
  // The stat figures count up on the first visit of a session only; later visits show them at rest.
  const [countOnce] = useState(() => {
    try {
      return !sessionStorage.getItem(COUNTED_KEY) && !prefersReducedMotion()
    } catch {
      return false
    }
  })
  useEffect(() => {
    try {
      sessionStorage.setItem(COUNTED_KEY, '1')
    } catch {
      /* storage unavailable: figures simply rest */
    }
  }, [])

  // The pipeline is a bonus panel: if it fails to load, the rest of the dashboard still works.
  // A newer request supersedes an older one, and nothing is applied after unmount.
  const loadApps = useCallback(() => {
    const id = ++requestId.current
    setAppsState('loading')
    return listApplications()
      .then((data) => {
        if (id !== requestId.current) return
        setApps(Array.isArray(data) ? data : [])
        setAppsState('ready')
      })
      .catch(() => {
        if (id === requestId.current) setAppsState('failed')
      })
  }, [])

  const loadSummary = useCallback(() => {
    const id = ++summaryRequestId.current
    setLoading(true)
    setError('')
    return getDashboardSummary()
      .then((data) => {
        if (id === summaryRequestId.current) setSummary(data)
      })
      .catch((err) => {
        if (id === summaryRequestId.current) setError(err.message || 'Couldn’t load your dashboard.')
      })
      .finally(() => {
        if (id === summaryRequestId.current) setLoading(false)
      })
  }, [])

  useEffect(() => {
    loadApps()
    loadSummary()
    return () => {
      requestId.current += 1
      summaryRequestId.current += 1
    }
  }, [loadApps, loadSummary])

  const attentionAll = useMemo(
    () => apps.map((app) => ({ app, reason: attentionFor(app) })).filter((x) => x.reason),
    [apps],
  )
  const attention = attentionAll.slice(0, 5)
  const openCount = useMemo(() => apps.filter((a) => !CLOSED_STAGES.includes(a.status)).length, [apps])

  async function snooze(app) {
    setSnoozingId(app.jobId)
    setSnoozeError(null)
    try {
      const updated = await updateApplication(app.jobId, buildUpdate(app, { followUpOn: addDays(todayIso(), FOLLOW_UP_DAYS) }))
      setApps((prev) => prev.map((a) => (a.jobId === updated.jobId ? updated : a)))
    } catch (err) {
      setSnoozeError({ jobId: app.jobId, message: err.message || 'Couldn’t snooze. Try again.' })
    } finally {
      setSnoozingId(null)
    }
  }

  const firstName = user?.firstName
  const showGettingStarted =
    !loading && summary && (summary.resumeCount === 0 || summary.jobMatchCount === 0)

  return (
    <div className="mx-auto max-w-6xl pb-12">
      <section aria-label="Overview" className="overflow-hidden rounded-2xl bg-ink text-paper">
        <div className="grid md:grid-cols-[1fr_auto]">
          <div className="flex flex-col justify-between gap-8 p-6 sm:p-8">
            <div>
              <div className="dash-enter font-code text-xs uppercase tracking-wide text-accent-on-ink" style={{ '--i': 0 }}>
                {todayLabel()}
              </div>
              <h1 className="dash-enter mt-3 font-display text-3xl leading-tight sm:text-[2.75rem] sm:leading-[1.1]" style={{ '--i': 1 }}>
                {firstName ? `${greeting()}, ${firstName}` : 'Welcome back'}
              </h1>
              <p className="dash-enter mt-3 max-w-md text-base text-paper/75" style={{ '--i': 2 }}>
                {appsState === 'ready' ? subtitleFor(apps, attentionAll.length) : DEFAULT_SUBTITLE}
              </p>
            </div>
            {!showGettingStarted && (
              <Link
                to="/resumes"
                className="btn-primary dash-enter gap-2 self-start !bg-paper !text-ink hover:!bg-paper/90"
                style={{ '--i': 3 }}
              >
                <PlusIcon size={16} />
                Upload resume
              </Link>
            )}
          </div>

          <div
            className="dash-enter flex items-center justify-center border-t border-paper/10 p-6 sm:p-8 md:min-w-[240px] md:border-l md:border-t-0 lg:min-w-[260px]"
            style={{ '--i': 4 }}
          >
            {loading ? (
              <div className="flex flex-col items-center gap-2 py-2" aria-hidden="true">
                <div className="animate-pulse rounded-full bg-paper/10" style={{ width: HERO_RING, height: HERO_RING }} />
                <Skeleton className="h-3 w-16 !bg-paper/10" />
              </div>
            ) : summary?.resumeScore != null ? (
              <ScoreCard label="Latest resume score" score={summary.resumeScore} size={HERO_RING} onDark />
            ) : summary ? (
              <div className="flex flex-col items-center gap-2 py-2">
                <div
                  className="flex items-center justify-center rounded-full border-2 border-dashed border-paper/30 text-paper/50"
                  style={{ width: HERO_RING, height: HERO_RING }}
                  aria-hidden="true"
                >
                  <SparkleIcon size={28} />
                </div>
                <div className="text-xs font-medium uppercase tracking-wide text-paper/75">Latest resume score</div>
                <Link to="/resumes" className="inline-flex min-h-10 items-center text-sm font-medium text-paper underline decoration-accent-on-ink/60 underline-offset-4 transition-colors hover:decoration-accent-on-ink">
                  Analyze a resume
                </Link>
              </div>
            ) : (
              <div className="flex flex-col items-center gap-2 py-2 text-center">
                <div
                  className="flex items-center justify-center rounded-full border-2 border-dashed border-paper/30 font-mono text-2xl text-paper/50"
                  style={{ width: HERO_RING, height: HERO_RING }}
                  aria-hidden="true"
                >
                  —
                </div>
                <div className="text-xs font-medium uppercase tracking-wide text-paper/75">Latest resume score</div>
                <div className="text-xs text-paper/75">Unavailable right now</div>
              </div>
            )}
          </div>
        </div>

        <div className="grid grid-cols-2 border-t border-paper/10 sm:grid-cols-4 [&>*]:border-paper/10 [&>*:nth-child(odd)]:border-r [&>*:nth-child(-n+2)]:border-b sm:[&>*]:border-b-0 sm:[&>*]:border-r sm:[&>*:last-child]:border-r-0">
          <StatTile count={countOnce} label="Resumes" value={summary?.resumeCount} icon={DocumentIcon} to="/resumes" loading={loading} />
          <StatTile count={countOnce} label="Job matches" value={summary?.jobMatchCount} icon={BriefcaseIcon} to="/jobs" loading={loading} />
          <StatTile count={countOnce} label="Open applications" value={appsState === 'ready' ? openCount : undefined} icon={CheckIcon} to="/applications" loading={appsState === 'loading'} />
          <StatTile count={countOnce} label="Interview sessions" value={summary?.interviewSessionCount} icon={MicIcon} to="/interview-prep" loading={loading} />
        </div>
      </section>

      {error && (
        <div
          role="alert"
          className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-brick/30 bg-brick/10 px-4 py-3 text-sm text-brick"
        >
          <span>{error}</span>
          <button type="button" className="btn-secondary text-xs" onClick={loadSummary} disabled={loading}>
            {loading ? 'Retrying…' : 'Retry'}
          </button>
        </div>
      )}

      {appsState === 'ready' && apps.length > 0 && attentionAll.length === 0 && (
        <p className="mt-8 flex items-center gap-2 text-sm text-ash">
          <span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-verdant text-paper" aria-hidden="true">
            <CheckIcon size={12} />
          </span>
          You’re caught up. No follow-ups are due.
        </p>
      )}

      {attention.length > 0 && <AttentionList attention={attention} onSnooze={snooze} snoozingId={snoozingId} snoozeError={snoozeError} />}

      {showGettingStarted && <GettingStarted summary={summary} />}

      {appsState === 'loading' && <PipelineSkeleton />}

      {apps.length > 0 && <Pipeline apps={apps} />}

      {appsState === 'failed' && (
        <div role="status" className="mt-8 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-surface px-4 py-3 text-sm text-ash">
          <span>Couldn’t load your application pipeline.</span>
          <button type="button" className="btn-secondary text-xs" onClick={loadApps}>
            Retry
          </button>
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 gap-8 lg:grid-cols-2 lg:gap-6">
        <section>
          <SectionHeader title="My resumes" to="/resumes" />
          <div className="mt-3 space-y-2">
            {loading && <SkeletonList rich />}

            {!loading && summary && (summary.recentResumes ?? []).length === 0 && (
              <EmptyState icon={DocumentIcon} text="No resumes yet." cta="Upload your first resume" to="/resumes" />
            )}

            {(summary?.recentResumes ?? []).map((r) => (
              <RecentRow
                key={r.id}
                to={`/resumes/${r.id}/analysis`}
                icon={DocumentIcon}
                title={r.fileName || 'Untitled resume'}
                createdAt={r.createdAt}
                pill={<ScorePill score={r.atsScore} prefix="ATS " />}
              />
            ))}
          </div>
        </section>

        <section>
          <SectionHeader title="Recent job matches" to="/jobs" />
          <div className="mt-3 space-y-2">
            {loading && <SkeletonList rich />}

            {!loading && summary && (summary.recentJobMatches ?? []).length === 0 && (
              <EmptyState icon={BriefcaseIcon} text="No job matches yet." cta="Match a resume to a job" to="/jobs" />
            )}

            {(summary?.recentJobMatches ?? []).map((m) => (
              <RecentRow
                key={m.jobMatchId}
                to={`/jobs/${m.jobId}/match`}
                icon={BriefcaseIcon}
                title={`${m.jobTitle || 'Untitled role'}${m.company ? ` at ${m.company}` : ''}`}
                createdAt={m.createdAt}
                pill={<ScorePill score={m.matchScore} suffix="%" />}
              />
            ))}
          </div>
        </section>
      </div>

      <section className="mt-8">
        <h2 className="font-display text-lg text-ink">Quick actions</h2>
        <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-4">
          {quickActions.map((action) => (
            <Link key={action.label} to={action.to} className="card-link group flex items-start gap-3 p-3.5">
              <IconChip icon={action.icon} />
              <div className="min-w-0">
                <div className="text-sm font-medium text-ink">{action.label}</div>
                <div className="mt-0.5 text-[13px] leading-snug text-ash">{action.description}</div>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </div>
  )
}
