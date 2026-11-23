import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { listRoadmaps } from '../services/roadmapApi'
import { ProgressBar, jobLabel } from '../components/roadmap'
import { Skeleton } from '../components/Skeleton'
import { ChevronRightIcon, MapIcon } from '../components/icons'
import PageHeader from '../components/PageHeader'

function OverallPanel({ roadmaps }) {
  const totalTasks = roadmaps.reduce((n, r) => n + (r.totalTasks || 0), 0)
  const doneTasks = roadmaps.reduce((n, r) => n + (r.completedTasks || 0), 0)
  const finished = roadmaps.filter((r) => r.progressPercent === 100).length
  const overall = totalTasks > 0 ? Math.round((doneTasks / totalTasks) * 100) : 0

  return (
    <section aria-label="Overall progress" className="card">
      <div className="flex flex-wrap items-end justify-between gap-x-8 gap-y-4">
        <div>
          <div
            className={`font-mono text-5xl font-semibold leading-none tracking-tight tabular-nums ${
              overall === 100 ? 'text-verdant' : 'text-ink'
            }`}
          >
            {overall}%
          </div>
          <div className="eyebrow mt-2">Overall progress</div>
        </div>
        <dl className="flex gap-8">
          <div>
            <dd className="font-mono text-2xl font-semibold leading-none tabular-nums text-ink">
              {doneTasks}
              <span className="text-ash">/{totalTasks}</span>
            </dd>
            <dt className="eyebrow mt-2">Tasks done</dt>
          </div>
          <div>
            <dd className="font-mono text-2xl font-semibold leading-none tabular-nums text-ink">
              {finished}
              <span className="text-ash">/{roadmaps.length}</span>
            </dd>
            <dt className="eyebrow mt-2">Roadmaps finished</dt>
          </div>
        </dl>
      </div>
      <ProgressBar percent={overall} className="mt-5 h-2" />
    </section>
  )
}

function RoadmapCard({ r }) {
  const done = r.progressPercent === 100
  return (
    <Link to={`/roadmaps/${r.id}`} className="card-link group flex items-center gap-4 px-4 py-4 sm:gap-5 sm:px-5">
      <div
        className={`w-16 shrink-0 text-right font-mono text-3xl font-semibold leading-none tracking-tight tabular-nums ${
          done ? 'text-verdant' : 'text-ink'
        }`}
      >
        {r.progressPercent}
        <span className="text-base text-ash">%</span>
      </div>
      <div className="min-w-0 flex-1">
        <div className="break-words text-sm font-medium text-ink">{jobLabel(r)}</div>
        <div className="mt-0.5 break-words text-[13px] text-ash">
          {r.resumeFileName} · {r.weekCount} weeks · match {r.matchScore}%
        </div>
        <ProgressBar percent={r.progressPercent} className="mt-3 h-1.5" />
        <div className="mt-2 text-[13px] text-ash">
          {r.completedTasks}/{r.totalTasks} tasks · updated {new Date(r.updatedAt).toLocaleDateString()}
        </div>
      </div>
      <ChevronRightIcon
        size={16}
        className="shrink-0 text-ash/40 transition-[color,transform] group-hover:translate-x-0.5 group-hover:text-hue-gold motion-reduce:transform-none"
      />
    </Link>
  )
}

function LoadingList() {
  return (
    <div role="status" aria-label="Loading" className="space-y-3">
      <Skeleton className="h-28 w-full !rounded-xl" />
      {[0, 1].map((i) => (
        <Skeleton key={i} className="h-24 w-full !rounded-xl" />
      ))}
    </div>
  )
}

export default function Roadmaps() {
  const [roadmaps, setRoadmaps] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const requestId = useRef(0)

  // A newer request supersedes an older one, and nothing is applied after unmount.
  const load = useCallback(() => {
    const id = ++requestId.current
    setLoading(true)
    setError('')
    return listRoadmaps()
      .then((data) => {
        if (id === requestId.current) setRoadmaps(Array.isArray(data) ? data : [])
      })
      .catch((err) => {
        if (id === requestId.current) setError(err.message || 'Couldn’t load your roadmaps.')
      })
      .finally(() => {
        if (id === requestId.current) setLoading(false)
      })
  }, [])

  useEffect(() => {
    load()
    return () => {
      requestId.current += 1
    }
  }, [load])

  const hasProgress = useMemo(() => roadmaps.some((r) => (r.totalTasks || 0) > 0), [roadmaps])

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        icon={MapIcon}
        hue="gold"
        title="Learning Roadmaps"
        description="Week-by-week plans that close the skill gaps found in your job matches, using only free resources."
        meta={
          !loading &&
          roadmaps.length > 0 && (
            <span className="rounded-full bg-surface/80 px-3 py-1 text-sm font-medium tabular-nums text-hue-gold">
              {roadmaps.length} {roadmaps.length === 1 ? 'roadmap' : 'roadmaps'}
            </span>
          )
        }
      >
        {!loading && !error && roadmaps.length > 0 && hasProgress && <OverallPanel roadmaps={roadmaps} />}
      </PageHeader>

      {error && (
        <div
          role="alert"
          className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-brick/20 bg-brick/5 px-4 py-3 text-sm text-brick"
        >
          <span>{error}</span>
          <button type="button" className="btn-secondary text-xs" onClick={load} disabled={loading}>
            {loading ? 'Retrying…' : 'Retry'}
          </button>
        </div>
      )}

      <div className="mt-6 space-y-3">
        {loading && <LoadingList />}

        {!loading && !error && roadmaps.length === 0 && (
          <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-hue-gold/30 bg-white/60 px-4 py-10 text-center">
            <p className="text-sm text-ash">No roadmaps yet.</p>
            <p className="max-w-sm text-sm text-ash">
              Open a job, run a match against your resume, then click{' '}
              <span className="font-medium text-ink">Build learning roadmap</span>.
            </p>
            <Link to="/jobs" className="btn-secondary">
              Go to Jobs
            </Link>
          </div>
        )}

        {!loading && roadmaps.map((r) => <RoadmapCard key={r.id} r={r} />)}
      </div>
    </div>
  )
}
