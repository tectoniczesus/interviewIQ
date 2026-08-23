import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getDashboardSummary } from '../services/dashboardApi'
import ScoreCard from '../components/ScoreCard'

function CountCard({ label, value }) {
  return (
    <div className="card flex flex-col justify-between">
      <div className="font-mono text-3xl font-semibold tabular-nums text-ink">{value ?? '—'}</div>
      <div className="eyebrow mt-2">{label}</div>
    </div>
  )
}

const quickActions = [
  { label: 'Upload Resume', to: '/resumes' },
  { label: 'Analyze a Job', to: '/jobs' },
  { label: 'Generate Cover Letter', to: '/cover-letters' },
  { label: 'Interview Prep', to: '/interview-prep' },
]

export default function Dashboard() {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    getDashboardSummary()
      .then((data) => {
        if (!cancelled) setSummary(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold text-ink">Dashboard</h1>
      <p className="mt-1 text-sm text-ash">
        Overview of your resumes, job matches, and recent activity.
      </p>

      {error && (
        <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>
      )}

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="card flex flex-col items-center justify-center">
          {summary?.resumeScore != null ? (
            <ScoreCard label="Resume Score" score={summary.resumeScore} />
          ) : (
            <div className="flex flex-col items-center gap-2 py-2">
              <div className="flex h-24 w-24 items-center justify-center rounded-full border-2 border-dashed border-line text-ash">
                <span className="font-mono text-sm">—</span>
              </div>
              <div className="eyebrow">Resume Score</div>
            </div>
          )}
        </div>
        <CountCard label="Job Matches" value={summary?.jobMatchCount} />
        <CountCard label="Cover Letters" value={summary?.coverLetterCount} />
        <CountCard label="Interview Sessions" value={summary?.interviewSessionCount} />
      </div>

      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div>
          <h2 className="font-display text-sm font-semibold text-ink">My Resumes</h2>
          <div className="mt-3 space-y-2">
            {loading && <div className="text-sm text-ash">Loading…</div>}

            {!loading && summary?.recentResumes.length === 0 && (
              <div className="card border-dashed text-center text-sm text-ash">No resumes yet.</div>
            )}

            {summary?.recentResumes.map((r) => (
              <Link
                key={r.id}
                to={`/resumes/${r.id}/analysis`}
                className="flex items-center justify-between rounded-lg border border-line bg-white px-4 py-3 text-sm transition-colors hover:border-ochre/50"
              >
                <span className="truncate text-ink">{r.fileName}</span>
                <span className="shrink-0 font-mono text-xs text-ash">
                  {r.atsScore != null ? `ATS ${r.atsScore}` : 'Not analyzed'}
                </span>
              </Link>
            ))}
          </div>
        </div>

        <div>
          <h2 className="font-display text-sm font-semibold text-ink">Recent Job Matches</h2>
          <div className="mt-3 space-y-2">
            {loading && <div className="text-sm text-ash">Loading…</div>}

            {!loading && summary?.recentJobMatches.length === 0 && (
              <div className="card border-dashed text-center text-sm text-ash">No job matches yet.</div>
            )}

            {summary?.recentJobMatches.map((m) => (
              <Link
                key={m.jobMatchId}
                to={`/jobs/${m.jobId}/match`}
                className="flex items-center justify-between rounded-lg border border-line bg-white px-4 py-3 text-sm transition-colors hover:border-ochre/50"
              >
                <span className="truncate text-ink">
                  {m.jobTitle || 'Untitled role'}
                  {m.company ? ` at ${m.company}` : ''}
                </span>
                <span className="shrink-0 font-mono text-xs text-ash">{m.matchScore}%</span>
              </Link>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-8">
        <h2 className="font-display text-sm font-semibold text-ink">Quick Actions</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          {quickActions.map((action) => (
            <Link key={action.to} to={action.to} className="btn-secondary hover:border-ochre/50">
              {action.label}
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}