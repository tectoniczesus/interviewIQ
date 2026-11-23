import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listApplications, listApplicationEvents, updateApplication } from '../services/applicationApi'
import {
  ALL_STAGES,
  CLOSED_STAGES,
  FOLLOW_UP_DAYS,
  OPEN_STAGES,
  STAGES,
  addDays,
  attentionFor,
  buildUpdate,
  daysSince,
  formatDate,
  jobLabel,
  todayIso,
} from '../lib/applications'
import { tier } from '../components/ScoreCard'
import StagePill from '../components/StagePill'
import { Skeleton } from '../components/Skeleton'
import { BriefcaseIcon, ChevronRightIcon } from '../components/icons'

const SCORE_PILL = {
  'text-verdant': 'bg-verdant/10 text-verdant',
  'text-ochre': 'bg-ochre/10 text-ochre',
  'text-brick': 'bg-brick/10 text-brick',
}

function StageSelect({ id, value, onChange, label, className = '' }) {
  return (
    <select
      id={id}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label={label}
      className={`field !w-auto !py-1 !pl-2 !pr-7 text-xs ${className}`}
    >
      {ALL_STAGES.map((s) => (
        <option key={s} value={s}>
          {STAGES[s].label}
        </option>
      ))}
    </select>
  )
}

function ApplicationCard({ app, onOpen, onMove, dragging, onDragStart, onDragEnd, animateIn }) {
  const attention = attentionFor(app)
  const days = daysSince(app.statusChangedAt)

  return (
    <article
      draggable
      onDragStart={(e) => {
        e.dataTransfer.setData('text/plain', app.jobId)
        e.dataTransfer.effectAllowed = 'move'
        onDragStart(app.jobId)
      }}
      onDragEnd={onDragEnd}
      className={`card-link cursor-grab p-3 active:cursor-grabbing ${animateIn ? 'card-settle' : ''} ${dragging ? 'opacity-40' : ''} ${
        attention ? '!border-l-[3px] !border-l-brick/70' : ''
      }`}
    >
      <button type="button" onClick={() => onOpen(app.jobId)} className="block w-full text-left">
        <div className="text-sm font-medium leading-snug text-ink">{app.title || 'Untitled role'}</div>
        {app.company && <div className="mt-0.5 truncate text-[13px] text-ash">{app.company}</div>}
      </button>

      <div className="mt-3 flex flex-wrap items-center gap-1.5">
        {app.latestMatchScore != null && (
          <span
            className={`rounded-full px-2 py-0.5 font-mono text-xs font-medium tabular-nums ${
              SCORE_PILL[tier(app.latestMatchScore).text]
            }`}
            title="Latest match score"
          >
            {app.latestMatchScore}%
          </span>
        )}
        <span className="font-mono text-xs text-ash">
          {days === 0 ? 'today' : `${days}d`} in stage
        </span>
      </div>

      {attention ? (
        <div className="mt-2 text-[13px] font-medium text-brick">{attention.label}</div>
      ) : (
        app.followUpOn && <div className="mt-2 text-[13px] text-ash">Follow up {formatDate(app.followUpOn)}</div>
      )}

      <div className="mt-3 border-t border-line pt-2">
        <StageSelect
          id={`stage-${app.jobId}`}
          value={app.status}
          onChange={(status) => onMove(app, status)}
          label={`Stage for ${jobLabel(app)}`}
        />
      </div>
    </article>
  )
}

function Column({ stage, apps, children, dropActive, onDrop, onDragOver, onDragLeave }) {
  return (
    <section
      aria-label={`${STAGES[stage].label} (${apps.length})`}
      onDragOver={onDragOver}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
      className={`flex min-h-[12rem] flex-col rounded-xl border p-2.5 transition-colors ${
        dropActive ? 'border-accent/60 bg-accent/5' : 'border-transparent bg-line/30'
      }`}
    >
      <header className="flex items-center justify-between px-1 pb-2.5">
        <h2 className="flex items-center gap-2 font-display text-sm font-semibold tracking-tight text-ink">
          <span className={`h-2 w-2 rounded-full ${STAGES[stage].dot}`} aria-hidden="true" />
          {STAGES[stage].label}
        </h2>
        <span className="font-mono text-xs tabular-nums text-ash">{apps.length}</span>
      </header>
      <div className="flex flex-1 flex-col gap-2">
        {children}
        {apps.length === 0 && (
          <div className="flex flex-1 items-center justify-center rounded-lg border border-dashed border-line px-3 py-6 text-center text-xs text-ash">
            Drop a card here
          </div>
        )}
      </div>
    </section>
  )
}

function Timeline({ app, events, loading }) {
  if (loading) return <Skeleton className="h-16 w-full" />
  const entries = [
    { key: 'created', label: 'Saved to InterviewIQ', at: app.createdAt },
    ...events.map((e) => ({
      key: e.id,
      label: `${STAGES[e.fromStatus]?.label ?? 'Saved'} → ${STAGES[e.toStatus].label}`,
      at: e.createdAt,
    })),
  ]
  return (
    <ol className="space-y-2.5 border-l border-line pl-4">
      {entries.map((entry) => (
        <li key={entry.key} className="relative text-sm">
          <span className="absolute -left-[1.3rem] top-1.5 h-2 w-2 rounded-full bg-ash/50" aria-hidden="true" />
          <div className="text-ink">{entry.label}</div>
          <div className="text-[13px] text-ash">
            {new Date(entry.at).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
          </div>
        </li>
      ))}
    </ol>
  )
}

function DetailDrawer({ app, onClose, onSaved }) {
  const panelRef = useRef(null)
  const [form, setForm] = useState(() => buildUpdate(app))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [events, setEvents] = useState([])
  const [eventsLoading, setEventsLoading] = useState(true)

  useEffect(() => {
    setForm(buildUpdate(app))
  }, [app])

  useEffect(() => {
    let cancelled = false
    setEventsLoading(true)
    listApplicationEvents(app.jobId)
      .then((data) => !cancelled && setEvents(data))
      .catch(() => !cancelled && setEvents([]))
      .finally(() => !cancelled && setEventsLoading(false))
    return () => {
      cancelled = true
    }
  }, [app.jobId, app.status])

  // Move focus into the dialog on open and give it back to whatever opened it on close.
  useEffect(() => {
    const opener = document.activeElement
    panelRef.current?.querySelector('[data-autofocus]')?.focus()
    return () => {
      if (opener instanceof HTMLElement && document.contains(opener)) opener.focus()
    }
  }, [])

  // Keep Tab inside the dialog while it is open.
  function trapTab(e) {
    if (e.key !== 'Tab' || !panelRef.current) return
    const nodes = panelRef.current.querySelectorAll(
      'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled])',
    )
    if (nodes.length === 0) return
    const first = nodes[0]
    const last = nodes[nodes.length - 1]
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault()
      last.focus()
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault()
      first.focus()
    }
  }

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [onClose])

  // Picking Applied in the drawer pre-fills the dates so the user sees them before saving.
  function setStatus(status) {
    setForm((prev) => buildUpdate({ ...app, ...prev, status: app.status }, { ...prev, status }))
  }

  async function handleSave(e) {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      const saved = await updateApplication(app.jobId, {
        ...form,
        appliedOn: form.appliedOn || null,
        followUpOn: form.followUpOn || null,
      })
      onSaved(saved)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const today = todayIso()

  return (
    <>
      <div className="scrim-in fixed inset-0 z-40 bg-ink/30" onClick={onClose} aria-hidden="true" />
      <aside
        ref={panelRef}
        onKeyDown={trapTab}
        role="dialog"
        aria-modal="true"
        aria-labelledby="application-drawer-title"
        className="drawer-in fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col overflow-y-auto bg-paper shadow-card-hover"
      >
        <div className="flex items-start justify-between gap-3 border-b border-line bg-white px-5 py-4">
          <div className="min-w-0">
            <h2 id="application-drawer-title" className="font-display text-xl font-semibold leading-snug tracking-tight text-ink">
              {app.title || 'Untitled role'}
            </h2>
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-ash">
              {app.company && <span>{app.company}</span>}
              <StagePill status={app.status} />
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            data-autofocus
            className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-ash hover:bg-ink/5 hover:text-ink"
            aria-label="Close"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSave} className="space-y-5 px-5 py-5">
          <div>
            <label htmlFor="drawer-stage" className="eyebrow">Stage</label>
            <select
              id="drawer-stage"
              value={form.status}
              onChange={(e) => setStatus(e.target.value)}
              className="field mt-1.5"
            >
              {ALL_STAGES.map((s) => (
                <option key={s} value={s}>{STAGES[s].label}</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="min-w-0">
              <label htmlFor="drawer-applied" className="eyebrow">Applied on</label>
              <input
                id="drawer-applied"
                type="date"
                value={form.appliedOn || ''}
                max={today}
                onChange={(e) => setForm((f) => ({ ...f, appliedOn: e.target.value || null }))}
                className="field mt-1.5"
              />
            </div>
            <div className="min-w-0">
              <label htmlFor="drawer-followup" className="eyebrow">Follow up on</label>
              <input
                id="drawer-followup"
                type="date"
                value={form.followUpOn || ''}
                onChange={(e) => setForm((f) => ({ ...f, followUpOn: e.target.value || null }))}
                className="field mt-1.5"
              />
            </div>
          </div>
          <div className="-mt-3 flex flex-wrap gap-2">
            <button
              type="button"
              className="text-xs font-medium text-ash hover:text-ink"
              onClick={() => setForm((f) => ({ ...f, followUpOn: addDays(today, FOLLOW_UP_DAYS) }))}
            >
              Follow up in {FOLLOW_UP_DAYS} days
            </button>
            {form.followUpOn && (
              <button
                type="button"
                className="text-xs font-medium text-ash hover:text-brick"
                onClick={() => setForm((f) => ({ ...f, followUpOn: null }))}
              >
                Clear follow-up
              </button>
            )}
          </div>

          <div>
            <label htmlFor="drawer-notes" className="eyebrow">Notes</label>
            <textarea
              id="drawer-notes"
              rows={4}
              maxLength={5000}
              value={form.notes || ''}
              onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
              placeholder="Recruiter name, referral, salary range…"
              className="field mt-1.5 resize-y"
            />
          </div>

          {error && <div className="rounded-lg border border-brick/20 bg-brick/5 px-3 py-2 text-sm text-brick">{error}</div>}

          <button type="submit" className="btn-primary w-full" disabled={saving}>
            {saving ? 'Saving…' : 'Save changes'}
          </button>
        </form>

        <div className="space-y-3 border-t border-line px-5 py-5">
          <h3 className="eyebrow">Next steps</h3>
          <div className="grid grid-cols-2 gap-2">
            <Link to={`/jobs/${app.jobId}/match`} className="btn-secondary">Run match</Link>
            <Link to="/cover-letters" className="btn-secondary">Cover letter</Link>
            <Link to="/interview-prep" className="btn-secondary">Interview prep</Link>
            {app.jobUrl ? (
              <a href={app.jobUrl} target="_blank" rel="noreferrer" className="btn-secondary">Job posting ↗</a>
            ) : (
              <Link to="/roadmaps" className="btn-secondary">Roadmaps</Link>
            )}
          </div>
        </div>

        <div className="space-y-3 border-t border-line px-5 py-5">
          <h3 className="eyebrow">Timeline</h3>
          <Timeline app={app} events={events} loading={eventsLoading} />
        </div>
      </aside>
    </>
  )
}

export default function Applications() {
  const [apps, setApps] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [loadFailed, setLoadFailed] = useState(false)
  const loadId = useRef(0)
  const [draggingId, setDraggingId] = useState(null)
  const [dropStage, setDropStage] = useState(null)
  const [showClosed, setShowClosed] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()
  const openId = searchParams.get('job')
  const boardSettled = useRef(false)

  // A newer request supersedes an older one, and nothing is applied after unmount.
  const load = useCallback(() => {
    const id = ++loadId.current
    setLoading(true)
    setLoadFailed(false)
    return listApplications()
      .then((data) => {
        if (id === loadId.current) setApps(Array.isArray(data) ? data : [])
      })
      .catch(() => {
        if (id === loadId.current) setLoadFailed(true)
      })
      .finally(() => {
        if (id === loadId.current) setLoading(false)
      })
  }, [])

  useEffect(() => {
    load()
    return () => {
      loadId.current += 1
    }
  }, [load])

  useEffect(() => {
    if (!loading) boardSettled.current = true
  }, [loading])

  const byStage = useMemo(() => {
    const groups = Object.fromEntries(ALL_STAGES.map((s) => [s, []]))
    apps.forEach((a) => groups[a.status]?.push(a))
    return groups
  }, [apps])

  const replaceApp = (updated) => setApps((prev) => prev.map((a) => (a.jobId === updated.jobId ? updated : a)))

  // Optimistic move: the card jumps immediately and snaps back if the save fails.
  const moveApp = useCallback(async (app, status) => {
    if (app.status === status) return
    const payload = buildUpdate(app, { status })
    setApps((prev) =>
      prev.map((a) => (a.jobId === app.jobId ? { ...a, ...payload, statusChangedAt: new Date().toISOString() } : a)),
    )
    setError('')
    try {
      replaceApp(await updateApplication(app.jobId, payload))
    } catch (err) {
      replaceApp(app)
      setError(`Couldn't move "${jobLabel(app)}": ${err.message}`)
    }
  }, [])

  function dropOn(stage) {
    return (e) => {
      e.preventDefault()
      const jobId = e.dataTransfer.getData('text/plain')
      const app = apps.find((a) => a.jobId === jobId)
      setDropStage(null)
      setDraggingId(null)
      if (app) moveApp(app, stage)
    }
  }

  const openApp = (jobId) => setSearchParams({ job: jobId })
  const closeDrawer = useCallback(() => setSearchParams({}), [setSearchParams])
  const openedApp = apps.find((a) => a.jobId === openId)
  const closedCount = CLOSED_STAGES.reduce((n, s) => n + byStage[s].length, 0)
  const attentionCount = apps.filter((a) => attentionFor(a)).length

  const cardProps = (app) => ({
    app,
    onOpen: openApp,
    onMove: moveApp,
    dragging: draggingId === app.jobId,
    animateIn: boardSettled.current,
    onDragStart: setDraggingId,
    onDragEnd: () => {
      setDraggingId(null)
      setDropStage(null)
    },
  })

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold leading-tight tracking-tight text-ink sm:text-3xl">Applications</h1>
          <p className="mt-1.5 text-base text-ash">
            Drag a card, or use its stage menu, to move it through your pipeline.
            {attentionCount > 0 && (
              <span className="text-brick"> {attentionCount} need{attentionCount === 1 ? 's' : ''} attention.</span>
            )}
          </p>
        </div>
        <Link to="/jobs" className="btn-secondary self-start sm:self-auto">Add a job</Link>
      </div>

      {error && (
        <div role="alert" className="mt-4 rounded-lg border border-brick/20 bg-brick/5 px-4 py-3 text-sm text-brick">
          {error}
        </div>
      )}

      {loading ? (
        <div className="mt-6 grid auto-cols-[minmax(15rem,1fr)] grid-flow-col gap-3 overflow-x-auto">
          {OPEN_STAGES.map((s) => (
            <div key={s} className="space-y-2 rounded-xl bg-line/30 p-2.5">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-28 w-full !rounded-xl" />
              <Skeleton className="h-28 w-full !rounded-xl" />
            </div>
          ))}
        </div>
      ) : loadFailed && apps.length === 0 ? (
        <div role="alert" className="mt-6 flex flex-col items-center gap-3 rounded-xl border border-line bg-white px-4 py-12 text-center">
          <p className="text-sm text-ash">Couldn’t load your applications.</p>
          <button type="button" className="btn-secondary" onClick={load}>
            Try again
          </button>
        </div>
      ) : apps.length === 0 ? (
        <div className="mt-6 flex flex-col items-center gap-3 rounded-xl border border-dashed border-line bg-white/60 px-4 py-12 text-center">
          <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-paper text-ash">
            <BriefcaseIcon size={20} />
          </span>
          <p className="text-sm text-ash">Save a job and it shows up here as an application.</p>
          <Link to="/jobs" className="text-sm font-medium text-ink underline decoration-accent/50 underline-offset-4 hover:decoration-accent">
            Add your first job
          </Link>
        </div>
      ) : (
        <>
          {/* Board scrolls sideways inside its own container on narrow screens. */}
          <div className="-mx-1 mt-6 overflow-x-auto px-1 pb-2">
            <div className="grid auto-cols-[minmax(15rem,1fr)] grid-flow-col gap-3">
              {OPEN_STAGES.map((stage) => (
                <Column
                  key={stage}
                  stage={stage}
                  apps={byStage[stage]}
                  dropActive={dropStage === stage}
                  onDragOver={(e) => {
                    e.preventDefault()
                    if (dropStage !== stage) setDropStage(stage)
                  }}
                  onDragLeave={(e) => {
                    if (!e.currentTarget.contains(e.relatedTarget)) setDropStage(null)
                  }}
                  onDrop={dropOn(stage)}
                >
                  {byStage[stage].map((app) => (
                    <ApplicationCard key={app.jobId} {...cardProps(app)} />
                  ))}
                </Column>
              ))}
            </div>
          </div>

          <section className="mt-6">
            <button
              type="button"
              onClick={() => setShowClosed((v) => !v)}
              aria-expanded={showClosed}
              className="flex items-center gap-2 font-display text-base font-semibold tracking-tight text-ink"
            >
              <ChevronRightIcon size={16} className={`transition-transform ${showClosed ? 'rotate-90' : ''}`} />
              Closed
              <span className="font-mono text-xs font-normal text-ash">{closedCount}</span>
            </button>
            {showClosed && (
              <div className="fade-in mt-3 space-y-2">
                {closedCount === 0 && <p className="text-sm text-ash">Rejected and withdrawn applications land here.</p>}
                {CLOSED_STAGES.flatMap((s) => byStage[s]).map((app) => (
                  <div key={app.jobId} className="card-link flex flex-wrap items-center gap-3 px-4 py-3">
                    <button type="button" onClick={() => openApp(app.jobId)} className="min-w-0 flex-1 text-left">
                      <div className="truncate text-sm font-medium text-ink">{jobLabel(app)}</div>
                      <div className="mt-0.5 text-[13px] text-ash">
                        {STAGES[app.status].label}{' '}
                        {new Date(app.statusChangedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                      </div>
                    </button>
                    <StageSelect
                      id={`stage-closed-${app.jobId}`}
                      value={app.status}
                      onChange={(status) => moveApp(app, status)}
                      label={`Stage for ${jobLabel(app)}`}
                    />
                  </div>
                ))}
              </div>
            )}
          </section>
        </>
      )}

      {openedApp && <DetailDrawer app={openedApp} onClose={closeDrawer} onSaved={replaceApp} />}
    </div>
  )
}
