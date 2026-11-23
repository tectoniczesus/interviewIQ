import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  deleteRoadmap,
  generateRoadmap,
  getRoadmap,
  setRoadmapTaskCompleted,
} from '../services/roadmapApi'
import { HOURS_OPTIONS, ProgressBar, jobLabel } from '../components/roadmap'
import { MapIcon } from '../components/icons'
import { AppIcon } from '../components/PageHeader'

const TYPE_STYLES = {
  LEARN: 'bg-ink/5 text-ink/70',
  PRACTICE: 'bg-ochre/10 text-ochre-deep',
  BUILD: 'bg-verdant/10 text-verdant',
}

// The AI never writes URLs (they'd be made up). We build free search links
// from the resource name + query instead, so every link actually resolves.
function webSearchUrl(task) {
  const q = [task.searchQuery, task.resourceName].filter(Boolean).join(' ')
  return `https://www.google.com/search?q=${encodeURIComponent(q)}`
}

function youtubeUrl(task) {
  return `https://www.youtube.com/results?search_query=${encodeURIComponent(task.searchQuery)}`
}

function TaskRow({ task, pending, onToggle }) {
  const isYoutube = /youtube/i.test(task.resourceName || '')
  return (
    <li className={`rounded-md border px-3 py-3 transition-colors ${task.completed ? 'border-verdant/30 bg-verdant/5' : 'border-line bg-white'}`}>
      <div className="flex items-start gap-3">
        <input
          id={`task-${task.id}`}
          type="checkbox"
          checked={task.completed}
          disabled={pending}
          onChange={() => onToggle(task)}
          className="mt-0.5 h-5 w-5 shrink-0 cursor-pointer rounded border-line accent-verdant disabled:cursor-wait"
        />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <label
              htmlFor={`task-${task.id}`}
              className={`cursor-pointer break-words text-sm font-medium ${task.completed ? 'text-ash line-through' : 'text-ink'}`}
            >
              {task.title}
            </label>
            <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold tracking-wide ${TYPE_STYLES[task.type] || TYPE_STYLES.LEARN}`}>
              {task.type}
            </span>
          </div>
          {task.description && (
            <p className="mt-1 break-words text-sm leading-relaxed text-ink/75">{task.description}</p>
          )}
          <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-[13px] font-medium">
            {!isYoutube && (
              <a href={webSearchUrl(task)} target="_blank" rel="noopener noreferrer" className="text-accent underline decoration-accent/40 underline-offset-2 hover:decoration-accent">
                Find on {task.resourceName || 'the web'} ↗
              </a>
            )}
            <a href={youtubeUrl(task)} target="_blank" rel="noopener noreferrer" className="text-accent underline decoration-accent/40 underline-offset-2 hover:decoration-accent">
              Watch on YouTube ↗
            </a>
          </div>
        </div>
      </div>
    </li>
  )
}

function WeekCard({ week, isLast, isCurrent, pendingIds, onToggle }) {
  const done = week.tasks.filter((t) => t.completed).length
  const complete = done === week.tasks.length
  return (
    <li className="relative pl-10 sm:pl-14">
      {/* timeline rail + node */}
      {!isLast && (
        <span
          className={`absolute bottom-0 left-[15px] top-9 w-0.5 sm:left-[23px] ${complete ? 'bg-hue-gold/50' : 'bg-line'}`}
          aria-hidden="true"
        />
      )}
      <span
        className={`absolute left-0 top-1 flex h-8 w-8 items-center justify-center rounded-full border-2 font-mono text-xs font-semibold sm:left-2 ${
          complete
            ? 'border-hue-gold bg-hue-gold text-white'
            : isCurrent
              ? 'border-hue-gold bg-white text-hue-gold ring-4 ring-hue-gold/15'
              : 'border-line bg-white text-ash'
        }`}
        aria-hidden="true"
      >
        {complete ? '✓' : week.week}
      </span>

      <div className={`card pb-5 ${isCurrent ? '!border-hue-gold/40' : ''}`}>
        <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
          <h2 className="font-display text-base font-semibold tracking-tight text-ink">
            <span className="text-ash">Week {week.week} · </span>{week.focus}
            {isCurrent && (
              <span className="ml-2 align-middle rounded-full bg-ochre/10 px-2 py-0.5 font-sans text-[11px] font-semibold uppercase tracking-wide text-ochre-deep">
                Current
              </span>
            )}
          </h2>
          <span className="shrink-0 font-mono text-xs text-ash">{done}/{week.tasks.length} done</span>
        </div>

        {week.skills?.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {week.skills.map((s) => (
              <span key={s} className="rounded-full border border-line bg-paper px-2 py-0.5 text-xs text-ink/70">{s}</span>
            ))}
          </div>
        )}

        <ul className="mt-3 space-y-2">
          {week.tasks.map((task) => (
            <TaskRow key={task.id} task={task} pending={pendingIds.has(task.id)} onToggle={onToggle} />
          ))}
        </ul>

        {week.milestone && (
          <div className="mt-3 flex gap-2 rounded-md bg-ochre/10 px-3 py-2 text-sm text-ink/80">
            <span aria-hidden="true">🏁</span>
            <span className="min-w-0 break-words"><span className="font-medium">Milestone:</span> {week.milestone}</span>
          </div>
        )}
      </div>
    </li>
  )
}

export default function LearningRoadmap() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [roadmap, setRoadmap] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [pendingIds, setPendingIds] = useState(() => new Set())
  const [hours, setHours] = useState(8)
  const [regenerating, setRegenerating] = useState(false)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    getRoadmap(id)
      .then((data) => {
        if (cancelled) return
        setRoadmap(data)
        setHours(data.hoursPerWeek)
        setError('')
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false))
    return () => {
      cancelled = true
    }
  }, [id])

  // Progress is derived from the task flags so optimistic toggles update it instantly.
  const progress = useMemo(() => {
    if (!roadmap) return { done: 0, total: 0, percent: 0 }
    const tasks = roadmap.weeks.flatMap((w) => w.tasks)
    const done = tasks.filter((t) => t.completed).length
    return { done, total: tasks.length, percent: tasks.length ? Math.round((done * 100) / tasks.length) : 0 }
  }, [roadmap])

  // The week the user is on: the first one that still has an open task.
  const currentIndex = roadmap ? roadmap.weeks.findIndex((w) => w.tasks.some((t) => !t.completed)) : -1

  function applyTask(taskId, completed) {
    setRoadmap((prev) => ({
      ...prev,
      weeks: prev.weeks.map((w) => ({
        ...w,
        tasks: w.tasks.map((t) => (t.id === taskId ? { ...t, completed } : t)),
      })),
    }))
  }

  async function handleToggle(task) {
    const next = !task.completed
    applyTask(task.id, next) // optimistic
    setPendingIds((prev) => new Set(prev).add(task.id))
    setError('')
    try {
      await setRoadmapTaskCompleted(roadmap.id, task.id, next)
    } catch (err) {
      applyTask(task.id, !next) // roll back
      setError(err.message)
    } finally {
      setPendingIds((prev) => {
        const copy = new Set(prev)
        copy.delete(task.id)
        return copy
      })
    }
  }

  async function handleRegenerate() {
    if (progress.done > 0 && !confirm('Regenerating creates a new plan and resets your progress. Continue?')) return
    setRegenerating(true)
    setError('')
    try {
      const fresh = await generateRoadmap(roadmap.jobMatchId, Number(hours))
      setRoadmap(fresh)
      if (fresh.id !== id) navigate(`/roadmaps/${fresh.id}`, { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setRegenerating(false)
    }
  }

  async function handleDelete() {
    if (!confirm('Delete this learning roadmap?')) return
    setDeleting(true)
    try {
      await deleteRoadmap(roadmap.id)
      navigate('/roadmaps')
    } catch (err) {
      setError(err.message)
      setDeleting(false)
    }
  }

  if (loading) return <div className="text-sm text-ash">Loading…</div>

  if (!roadmap) {
    return (
      <div className="mx-auto max-w-3xl">
        <Link to="/roadmaps" className="text-sm text-ash hover:text-accent">← Back to roadmaps</Link>
        <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error || 'Roadmap not found.'}</div>
      </div>
    )
  }

  const busy = regenerating || deleting

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/roadmaps" className="text-sm text-ash hover:text-accent">← Back to roadmaps</Link>

      <div className="mt-2 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
        <div className="flex min-w-0 items-start gap-4">
          <AppIcon icon={MapIcon} hue="gold" size={44} />
          <div className="min-w-0">
          <div className="eyebrow !text-hue-gold">Learning roadmap</div>
          <h1 className="mt-1.5 break-words font-display text-2xl font-semibold leading-tight tracking-tight text-ink sm:text-3xl">
            {jobLabel(roadmap)}
          </h1>
          <p className="mt-2 break-words text-[13px] text-ash">
            {roadmap.resumeFileName} · match score {roadmap.matchScore}% · {roadmap.weeks.length} weeks at {roadmap.hoursPerWeek} h/week
          </p>
          </div>
        </div>
        <div className="flex shrink-0 flex-wrap items-center gap-2">
          <label htmlFor="regen-hours" className="sr-only">Hours per week</label>
          <select
            id="regen-hours"
            value={hours}
            onChange={(e) => setHours(e.target.value)}
            disabled={busy}
            className="field w-auto py-1.5"
          >
            {HOURS_OPTIONS.map((h) => (
              <option key={h} value={h}>{h} h/week</option>
            ))}
          </select>
          <button onClick={handleRegenerate} disabled={busy} className="btn-secondary">
            {regenerating ? 'Regenerating…' : 'Regenerate'}
          </button>
          <button onClick={handleDelete} disabled={busy} className="btn-ghost-danger">
            {deleting ? 'Deleting…' : 'Delete'}
          </button>
        </div>
      </div>

      {error && <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>}

      <div className="card mt-4">
        <div className="flex items-end justify-between gap-4">
          <div>
            <div
              className={`font-mono text-3xl font-semibold leading-none tracking-tight tabular-nums sm:text-5xl ${
                progress.percent === 100 ? 'text-verdant' : 'text-ink'
              }`}
            >
              {progress.percent}%
            </div>
            <div className="eyebrow mt-2">{progress.done} of {progress.total} tasks complete</div>
          </div>
        </div>
        <ProgressBar percent={progress.percent} className="mt-3 h-2.5" />
        <p className="mt-4 max-w-prose break-words text-base leading-relaxed text-ink/80">{roadmap.summary}</p>
      </div>

      {progress.total > 0 && progress.done === progress.total && (
        <div className="mt-4 flex flex-col gap-3 rounded-lg border border-verdant/30 bg-verdant/10 px-4 py-3 text-sm text-verdant sm:flex-row sm:items-center sm:justify-between">
          <span><span aria-hidden="true">🎉 </span>Roadmap complete! Update your resume with what you built, then re-run the match to see your new score.</span>
          <Link to={`/jobs/${roadmap.jobId}/match`} className="btn-primary shrink-0 bg-verdant hover:bg-verdant/85">
            Re-run job match
          </Link>
        </div>
      )}

      <ol className="mt-6 space-y-4">
        {roadmap.weeks.map((week, i) => (
          <WeekCard
            key={week.week}
            week={week}
            isLast={i === roadmap.weeks.length - 1}
            isCurrent={i === currentIndex}
            pendingIds={pendingIds}
            onToggle={handleToggle}
          />
        ))}
      </ol>
    </div>
  )
}
