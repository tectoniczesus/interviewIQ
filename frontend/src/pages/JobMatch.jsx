import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getJob } from '../services/jobApi'
import { listResumes } from '../services/resumeApi'
import { createJobMatch, listJobMatches } from '../services/jobMatchApi'
import ScoreCard from '../components/ScoreCard'

function Section({ title, items }) {
  if (!items || items.length === 0) return null
  return (
    <div className="card">
      <h2 className="font-display text-sm font-semibold text-ink">{title}</h2>
      <ul className="mt-2 space-y-1.5 text-sm text-ink/80">
        {items.map((item, i) => (
          <li key={i} className="flex gap-2">
            <span className="text-ochre">·</span>
            <span>{item}</span>
          </li>
        ))}
      </ul>
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
  const [loading, setLoading] = useState(true)
  const [matching, setMatching] = useState(false)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [jobData, resumeData, allMatches] = await Promise.all([
        getJob(jobId),
        listResumes(),
        listJobMatches(),
      ])
      setJob(jobData)
      setResumes(resumeData)

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
    return <div className="text-sm text-ash">Loading…</div>
  }

  const usableResumes = resumes.filter((r) => r.processingStatus === 'COMPLETED')

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/jobs" className="text-xs text-ash hover:text-ochre">
        ← Back to jobs
      </Link>

      <h1 className="mt-2 font-display text-2xl font-semibold text-ink">
        {job?.title || 'Untitled role'}
        {job?.company ? ` at ${job.company}` : ''}
      </h1>

      <div className="card mt-4 flex flex-wrap items-center gap-3">
        <label className="text-sm text-ink/70">Compare against:</label>
        <select
          value={selectedResumeId}
          onChange={(e) => setSelectedResumeId(e.target.value)}
          className="field w-auto"
        >
          {usableResumes.length === 0 && <option value="">No processed resumes yet</option>}
          {usableResumes.map((r) => (
            <option key={r.id} value={r.id}>
              {r.fileName}
            </option>
          ))}
        </select>
        <button onClick={handleCompare} disabled={matching || !selectedResumeId} className="btn-primary">
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

          <Section title="Matching Skills" items={currentMatch.matchingSkills} />
          <Section title="Missing Skills" items={currentMatch.missingSkills} />
          <Section title="Matching Experience" items={currentMatch.matchingExperience} />
          <Section title="Missing Requirements" items={currentMatch.missingRequirements} />
          <Section title="Keyword Gaps" items={currentMatch.keywordGaps} />
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
                className="flex w-full items-center justify-between rounded-md border border-line bg-white px-3 py-2 text-left text-sm transition-colors hover:border-ochre/50"
              >
                <span>{resumeName(m.resumeId)}</span>
                <span className="font-mono text-ash">
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