import { useEffect, useState } from 'react'
import { listResumes } from '../services/resumeApi'
import { listJobs } from '../services/jobApi'
import {
  createInterviewSession,
  deleteInterviewSession,
  getInterviewQuestions,
  listInterviewSessions,
  regenerateInterviewQuestions,
} from '../services/interviewApi'

const CATEGORIES = [
  { key: 'TECHNICAL', label: 'Technical' },
  { key: 'HR', label: 'HR' },
  { key: 'RESUME_BASED', label: 'Resume Based' },
  { key: 'JOB_SPECIFIC', label: 'Job Specific' },
]

const DIFFICULTY_STYLES = {
  EASY: 'bg-verdant/10 text-verdant',
  MEDIUM: 'bg-ochre/10 text-ochre',
  HARD: 'bg-brick/10 text-brick',
}

function QuestionCard({ q }) {
  const [revealed, setRevealed] = useState(false)
  return (
    <div className="card">
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-ink">{q.question}</p>
        <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${DIFFICULTY_STYLES[q.difficulty] || ''}`}>
          {q.difficulty}
        </span>
      </div>

      <button
        onClick={() => setRevealed((r) => !r)}
        className="mt-3 text-xs font-medium text-ochre hover:text-ochre/80"
      >
        {revealed ? 'Hide suggested answer' : 'Show suggested answer'}
      </button>

      {revealed && (
        <div className="mt-3 space-y-3 border-t border-line pt-3">
          <p className="text-sm text-ink/80">{q.suggestedAnswer}</p>
          {q.keyPoints?.length > 0 && (
            <div>
              <div className="eyebrow">Key points</div>
              <ul className="mt-1 space-y-1 text-sm text-ink/80">
                {q.keyPoints.map((point, i) => (
                  <li key={i} className="flex gap-2">
                    <span className="text-ochre">·</span>
                    <span>{point}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default function InterviewPreparation() {
  const [resumes, setResumes] = useState([])
  const [jobs, setJobs] = useState([])
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [selectedResumeId, setSelectedResumeId] = useState('')
  const [selectedJobId, setSelectedJobId] = useState('')
  const [role, setRole] = useState('')
  const [generating, setGenerating] = useState(false)

  const [activeSession, setActiveSession] = useState(null)
  const [questions, setQuestions] = useState([])
  const [activeCategory, setActiveCategory] = useState('TECHNICAL')
  const [regenerating, setRegenerating] = useState(false)
  const [deleting, setDeleting] = useState(false)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [resumeData, jobData, sessionData] = await Promise.all([
        listResumes(),
        listJobs(),
        listInterviewSessions(),
      ])
      setResumes(resumeData)
      setJobs(jobData)
      setSessions(sessionData)

      const firstResume = resumeData.find((r) => r.processingStatus === 'COMPLETED')
      if (firstResume) setSelectedResumeId(firstResume.id)
      if (jobData.length > 0) setSelectedJobId(jobData[0].id)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function openSession(session) {
    setError('')
    try {
      const qs = await getInterviewQuestions(session.id)
      setQuestions(qs)
      setActiveSession(session)
      setActiveCategory('TECHNICAL')
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleGenerate(e) {
    e.preventDefault()
    if (!selectedResumeId || !selectedJobId || !role.trim()) return

    setGenerating(true)
    setError('')
    try {
      const session = await createInterviewSession(selectedResumeId, selectedJobId, role.trim())
      setSessions((prev) => [session, ...prev])
      await openSession(session)
    } catch (err) {
      setError(err.message)
    } finally {
      setGenerating(false)
    }
  }

  async function handleRegenerate() {
    if (!activeSession) return
    setRegenerating(true)
    setError('')
    try {
      const qs = await regenerateInterviewQuestions(activeSession.id)
      setQuestions(qs)
    } catch (err) {
      setError(err.message)
    } finally {
      setRegenerating(false)
    }
  }

  async function handleDelete(sessionId) {
    if (!confirm('Delete this interview prep session?')) return
    setDeleting(true)
    setError('')
    try {
      await deleteInterviewSession(sessionId)
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
      if (activeSession?.id === sessionId) setActiveSession(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setDeleting(false)
    }
  }

  function resumeName(resumeId) {
    return resumes.find((r) => r.id === resumeId)?.fileName || 'Resume'
  }

  function jobLabel(jobId) {
    const job = jobs.find((j) => j.id === jobId)
    if (!job) return 'Job'
    return job.title ? `${job.title}${job.company ? ` at ${job.company}` : ''}` : 'Untitled role'
  }

  const usableResumes = resumes.filter((r) => r.processingStatus === 'COMPLETED')

  // --- Detail view ---
  if (activeSession) {
    const questionsForCategory = questions.filter((q) => q.category === activeCategory)

    return (
      <div className="mx-auto max-w-3xl">
        <button onClick={() => setActiveSession(null)} className="text-xs text-ash hover:text-ochre">
          ← Back to sessions
        </button>

        <div className="mt-2 flex items-center justify-between gap-4">
          <div>
            <h1 className="font-display text-2xl font-semibold text-ink">{activeSession.role}</h1>
            <p className="mt-1 text-xs text-ash">
              {resumeName(activeSession.resumeId)} · {jobLabel(activeSession.jobId)}
            </p>
          </div>
          <div className="flex shrink-0 gap-2">
            <button onClick={handleRegenerate} disabled={regenerating || deleting} className="btn-secondary">
              {regenerating ? 'Regenerating…' : 'Regenerate'}
            </button>
            <button
              onClick={() => handleDelete(activeSession.id)}
              disabled={deleting || regenerating}
              className="btn-ghost-danger"
            >
              {deleting ? 'Deleting…' : 'Delete'}
            </button>
          </div>
        </div>

        {error && (
          <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>
        )}

        <div className="mt-4 flex gap-1 rounded-md bg-white p-1 text-sm">
          {CATEGORIES.map((c) => (
            <button
              key={c.key}
              onClick={() => setActiveCategory(c.key)}
              className={`flex-1 rounded px-3 py-2 font-medium transition-colors ${
                activeCategory === c.key ? 'bg-ink text-paper' : 'text-ash hover:text-ink'
              }`}
            >
              {c.label}
            </button>
          ))}
        </div>

        <div className="mt-4 space-y-3">
          {questionsForCategory.length === 0 && (
            <div className="card border-dashed text-center text-sm text-ash">
              No questions in this category.
            </div>
          )}
          {questionsForCategory.map((q) => (
            <QuestionCard key={q.id} q={q} />
          ))}
        </div>
      </div>
    )
  }

  // --- List + generate form ---
  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="font-display text-2xl font-semibold text-ink">Interview Preparation</h1>
      <p className="mt-1 text-sm text-ash">
        Generate technical, HR, resume-based, and job-specific questions for a target role.
      </p>

      <form onSubmit={handleGenerate} className="card mt-4">
        {loading ? (
          <div className="text-sm text-ash">Loading…</div>
        ) : (
          <>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <select
                value={selectedResumeId}
                onChange={(e) => setSelectedResumeId(e.target.value)}
                className="field"
              >
                {usableResumes.length === 0 && <option value="">No processed resumes yet</option>}
                {usableResumes.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.fileName}
                  </option>
                ))}
              </select>

              <select
                value={selectedJobId}
                onChange={(e) => setSelectedJobId(e.target.value)}
                className="field"
              >
                {jobs.length === 0 && <option value="">No saved jobs yet</option>}
                {jobs.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.title || 'Untitled role'}
                    {j.company ? ` at ${j.company}` : ''}
                  </option>
                ))}
              </select>
            </div>

            <input
              value={role}
              onChange={(e) => setRole(e.target.value)}
              placeholder="Target role (e.g. Backend Developer)"
              required
              className="field mt-3"
            />

            <button
              type="submit"
              disabled={generating || !selectedResumeId || !selectedJobId || !role.trim()}
              className="btn-primary mt-3"
            >
              {generating ? 'Generating…' : 'Generate Interview Prep'}
            </button>
          </>
        )}
      </form>

      {error && (
        <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>
      )}

      <div className="mt-6 space-y-3">
        {!loading && sessions.length === 0 && (
          <div className="card border-dashed text-center text-sm text-ash">
            No interview prep sessions yet.
          </div>
        )}

        {sessions.map((session) => (
          <div
            key={session.id}
            className="flex items-center justify-between gap-3 rounded-lg border border-line bg-white px-4 py-3 transition-colors hover:border-ochre/50"
          >
            <button onClick={() => openSession(session)} className="min-w-0 flex-1 text-left">
              <div className="truncate text-sm font-medium text-ink">{session.role}</div>
              <div className="mt-1 truncate text-xs text-ash">
                {resumeName(session.resumeId)} · {jobLabel(session.jobId)}
              </div>
            </button>
            <span className="shrink-0 font-mono text-xs text-ash">
              {new Date(session.createdAt).toLocaleDateString()}
            </span>
            <button onClick={() => handleDelete(session.id)} className="btn-ghost-danger shrink-0">
              Delete
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}