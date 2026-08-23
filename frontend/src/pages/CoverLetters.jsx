import { useEffect, useState } from 'react'
import { listResumes } from '../services/resumeApi'
import { listJobs } from '../services/jobApi'
import {
  createCoverLetter,
  deleteCoverLetter,
  listCoverLetters,
  regenerateCoverLetter,
} from '../services/coverLetterApi'

export default function CoverLetters() {
  const [resumes, setResumes] = useState([])
  const [jobs, setJobs] = useState([])
  const [coverLetters, setCoverLetters] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [selectedResumeId, setSelectedResumeId] = useState('')
  const [selectedJobId, setSelectedJobId] = useState('')
  const [candidateName, setCandidateName] = useState('')
  const [generating, setGenerating] = useState(false)

  const [expandedId, setExpandedId] = useState(null)
  const [regeneratingId, setRegeneratingId] = useState(null)
  const [copiedId, setCopiedId] = useState(null)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [resumeData, jobData, letterData] = await Promise.all([
        listResumes(),
        listJobs(),
        listCoverLetters(),
      ])
      setResumes(resumeData)
      setJobs(jobData)
      setCoverLetters(letterData)

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

  async function handleGenerate(e) {
    e.preventDefault()
    if (!selectedResumeId || !selectedJobId) return

    setGenerating(true)
    setError('')
    try {
      const letter = await createCoverLetter(selectedResumeId, selectedJobId, candidateName || undefined)
      setCoverLetters((prev) => [letter, ...prev])
      setExpandedId(letter.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setGenerating(false)
    }
  }

  async function handleRegenerate(id) {
    setRegeneratingId(id)
    setError('')
    try {
      const updated = await regenerateCoverLetter(id)
      setCoverLetters((prev) => prev.map((c) => (c.id === id ? updated : c)))
      setExpandedId(id)
    } catch (err) {
      setError(err.message)
    } finally {
      setRegeneratingId(null)
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this cover letter?')) return
    try {
      await deleteCoverLetter(id)
      setCoverLetters((prev) => prev.filter((c) => c.id !== id))
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleCopy(id, content) {
    try {
      await navigator.clipboard.writeText(content)
      setCopiedId(id)
      setTimeout(() => setCopiedId(null), 1500)
    } catch {
      setError('Could not copy to clipboard')
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

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="font-display text-2xl font-semibold text-ink">Cover Letters</h1>
      <p className="mt-1 text-sm text-ash">
        Generate a personalized cover letter from one of your resumes and a saved job.
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
              value={candidateName}
              onChange={(e) => setCandidateName(e.target.value)}
              placeholder="Your name (optional - used for the signature)"
              className="field mt-3"
            />

            <button
              type="submit"
              disabled={generating || !selectedResumeId || !selectedJobId}
              className="btn-primary mt-3"
            >
              {generating ? 'Generating…' : 'Generate Cover Letter'}
            </button>
          </>
        )}
      </form>

      {error && (
        <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>
      )}

      <div className="mt-6 space-y-3">
        {!loading && coverLetters.length === 0 && (
          <div className="card border-dashed text-center text-sm text-ash">No cover letters yet.</div>
        )}

        {coverLetters.map((letter) => {
          const isExpanded = expandedId === letter.id
          return (
            <div key={letter.id} className="card">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <div className="text-sm font-medium text-ink">{jobLabel(letter.jobId)}</div>
                  <div className="mt-1 text-xs text-ash">
                    {resumeName(letter.resumeId)} · {new Date(letter.updatedAt).toLocaleString()}
                  </div>
                </div>
                <button
                  onClick={() => setExpandedId(isExpanded ? null : letter.id)}
                  className="shrink-0 text-xs font-medium text-ochre hover:text-ochre/80"
                >
                  {isExpanded ? 'Hide' : 'View'}
                </button>
              </div>

              {isExpanded && (
                <>
                  <pre className="mt-3 whitespace-pre-wrap rounded-md bg-paper p-3 text-sm text-ink/80">
                    {letter.content}
                  </pre>
                  <div className="mt-3 flex gap-2">
                    <button onClick={() => handleCopy(letter.id, letter.content)} className="btn-secondary">
                      {copiedId === letter.id ? 'Copied!' : 'Copy'}
                    </button>
                    <button
                      onClick={() => handleRegenerate(letter.id)}
                      disabled={regeneratingId === letter.id}
                      className="btn-secondary"
                    >
                      {regeneratingId === letter.id ? 'Regenerating…' : 'Regenerate'}
                    </button>
                    <button onClick={() => handleDelete(letter.id)} className="btn-ghost-danger">
                      Delete
                    </button>
                  </div>
                </>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}