import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { getJob } from '../services/jobApi'
import { listResumes } from '../services/resumeApi'
import { createJobMatch, listJobMatches } from '../services/jobMatchApi'
import ScoreCard from '../components/ScoreCard'
import MatchBreakdown from '../components/MatchBreakdown'
import { Skeleton, SkeletonCard } from '../components/Skeleton'
import { generateRoadmap, listRoadmaps } from '../services/roadmapApi'
import { HOURS_OPTIONS, ProgressBar } from '../components/roadmap'

function Section({ title, items }) {
  if (!items || items.length === 0) return null
  return (
    <div className="card">
      <h2 className="font-display text-sm font-semibold text-ink">{title}</h2>
      <ul className="mt-2 space-y-1.5 text-sm text-ink/80">
        {items.map((item, i) => (
          <li key={i} className="flex gap-2">
            <span className="text-ochre">·</span>
            <span className="min-w-0 break-words">{item}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function RoadmapCard({ match, roadmap }) {
  const navigate = useNavigate()
  const [hours, setHours] = useState(8)
  const [building, setBuilding] = useState(false)
  const [error, setError] = useState('')

  async function handleBuild() {
    setBuilding(true)
    setError('')
    try {
      const created = await generateRoadmap(match.id, Number(hours))
      navigate(`/roadmaps/${created.id}`)
    } catch (err) {
      setError(err.message)
      setBuilding(false)
    }
  }

  if (roadmap) {
    return (
      <div className="card !border-accent/40">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <h2 className="font-display text-sm font-semibold text-ink">Learning roadmap</h2>
            <p className="mt-1 text-sm text-ink/70">
              {roadmap.completedTasks} of {roadmap.totalTasks} tasks done across {roadmap.weekCount} weeks.
            </p>
          </div>
          <Link to={`/roadmaps/${roadmap.id}`} className="btn-primary w-full shrink-0 sm:w-auto">
            View roadmap ({roadmap.progressPercent}%)
          </Link>
        </div>
        <ProgressBar percent={roadmap.progressPercent} className="mt-3 h-1.5" />
      </div>
    )
  }

  return (
    <div className="card !border-accent/40 bg-accent/5">
      <h2 className="font-display text-sm font-semibold text-ink">Close the gaps</h2>
      <p className="mt-1 text-sm text-ink/70">
        Turn the missing skills below into a week-by-week learning plan with free resources and progress tracking.
      </p>
      <div className="mt-3 flex flex-col gap-2 sm:flex-row sm:items-center">
        <label htmlFor="roadmap-hours" className="shrink-0 text-sm text-ink/70">Study time:</label>
        <select
          id="roadmap-hours"
          value={hours}
          onChange={(e) => setHours(e.target.value)}
          disabled={building}
          className="field sm:w-auto"
        >
          {HOURS_OPTIONS.map((h) => (
            <option key={h} value={h}>{h} hours / week</option>
          ))}
        </select>
        <button onClick={handleBuild} disabled={building} className="btn-primary w-full shrink-0 sm:w-auto">
          {building ? 'Building your roadmap…' : 'Build learning roadmap'}
        </button>
      </div>
      {building && <p className="mt-2 text-xs text-ash">This usually takes 5 to 15 seconds.</p>}
      {error && <div className="mt-3 rounded-md bg-brick/10 px-3 py-2 text-sm text-brick">{error}</div>}
    </div>
  )
}

export default function JobMatch() {
  const { id: jobId } = useParams()
  const [job, setJob] = useState(null)
  const [resumes, setResumes] = useState([])
  const [matches, setMatches] = useState([])
  const [selectedResumeId, setSelectedResumeId] = useState('')
  const [currentMatch, setCurrentMatch] = useState(null)
  const [roadmaps, setRoadmaps] = useState([])
  const [loading, setLoading] = useState(true)
  const [matching, setMatching] = useState(false)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [jobData, resumeData, allMatches, roadmapData] = await Promise.all([
        getJob(jobId),
        listResumes(),
        listJobMatches(),
        // A roadmap failure shouldn't block the match page from loading.
        listRoadmaps().catch(() => []),
      ])
      setJob(jobData)
      setResumes(resumeData)
      setRoadmaps(roadmapData)

      const forThisJob = allMatches.filter((m) => m.jobId === jobId)
      setMatches(forThisJob)
      if (forThisJob.length > 0) setCurrentMatch(forThisJob[0])

      const firstUsable = resumeData.find((r) => r.processingStatus === 'COMPLETED')
      if (firstUsable) setSelectedResumeId(firstUsable.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobId])

  async function handleCompare() {
    if (!selectedResumeId) return
    setMatching(true)
    setError('')
    try {
      const result = await createJobMatch(selectedResumeId, jobId)
      setCurrentMatch(result)
      setMatches((prev) => [result, ...prev])
    } catch (err) {
      setError(err.message)
    } finally {
      setMatching(false)
    }
  }

  function resumeName(resumeId) {
    return resumes.find((r) => r.id === resumeId)?.fileName || 'Resume'
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-3xl" role="status" aria-label="Loading job match">
        <Skeleton className="h-3 w-24" />
        <Skeleton className="mt-3 h-7 w-1/2" />
        <div className="card mt-4">
          <Skeleton className="h-9 w-full" />
        </div>
        <div className="mt-4 space-y-4">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      </div>
    )
  }

  const usableResumes = resumes.filter((r) => r.processingStatus === 'COMPLETED')

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/jobs" className="text-xs text-ash hover:text-accent">
        ← Back to jobs
      </Link>

      <h1 className="mt-2 break-words font-display text-xl font-semibold text-ink sm:text-2xl">
        {job?.title || 'Untitled role'}
        {job?.company ? ` at ${job.company}` : ''}
      </h1>

      <div className="card mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <label htmlFor="compare-resume" className="shrink-0 text-sm text-ink/70">Compare against:</label>
        <select
          id="compare-resume"
          value={selectedResumeId}
          onChange={(e) => setSelectedResumeId(e.target.value)}
          className="field min-w-0 sm:flex-1"
        >
          {usableResumes.length === 0 && <option value="">No processed resumes yet</option>}
          {usableResumes.map((r) => (
            <option key={r.id} value={r.id}>
              {r.fileName}
            </option>
          ))}
        </select>
        <button
          onClick={handleCompare}
          disabled={matching || !selectedResumeId}
          className="btn-primary w-full shrink-0 sm:w-auto"
        >
          {matching ? 'Comparing…' : 'Compare'}
        </button>
      </div>

      {usableResumes.length === 0 && (
        <div className="mt-3 rounded-md bg-ochre/10 px-4 py-2 text-sm text-ochre">
          Upload and process a resume first before running a match.
        </div>
      )}

      {error && (
        <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>
      )}

      {currentMatch && (
        <div className="mt-6 space-y-4">
          <div className="card flex items-center justify-center">
            <ScoreCard label="Match Score" score={currentMatch.matchScore} />
          </div>

          <RoadmapCard
            key={currentMatch.id}
            match={currentMatch}
            roadmap={roadmaps.find((r) => r.jobMatchId === currentMatch.id)}
          />

          <MatchBreakdown match={currentMatch} />

          <Section title="Matching Experience" items={currentMatch.matchingExperience} />
          <Section title="Missing Requirements" items={currentMatch.missingRequirements} />
          <Section title="Recommendations" items={currentMatch.recommendations} />

          <div className="font-mono text-xs text-ash">
            Matched against {resumeName(currentMatch.resumeId)} ·{' '}
            {new Date(currentMatch.createdAt).toLocaleString()}
          </div>
        </div>
      )}

      {matches.length > 1 && (
        <div className="mt-8">
          <h2 className="font-display text-sm font-semibold text-ink">Previous matches</h2>
          <div className="mt-2 space-y-2">
            {matches.map((m) => (
              <button
                key={m.id}
                onClick={() => setCurrentMatch(m)}
                className="flex w-full items-center justify-between gap-3 rounded-md border border-line bg-white px-3 py-2 text-left text-sm transition-colors hover:border-accent/50"
              >
                <span className="min-w-0 truncate">{resumeName(m.resumeId)}</span>
                <span className="shrink-0 font-mono text-xs text-ash sm:text-sm">
                  {m.matchScore}% · {new Date(m.createdAt).toLocaleDateString()}
                </span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}