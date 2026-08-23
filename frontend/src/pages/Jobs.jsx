import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createJob, createJobFromUrl, deleteJob, listJobs } from '../services/jobApi'

export default function Jobs() {
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [mode, setMode] = useState('paste')
  const [title, setTitle] = useState('')
  const [company, setCompany] = useState('')
  const [description, setDescription] = useState('')
  const [jobUrl, setJobUrl] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function refresh() {
    setLoading(true)
    try {
      setJobs(await listJobs())
      setError('')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refresh()
  }, [])

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      if (mode === 'paste') {
        await createJob({ title, company, description })
      } else {
        await createJobFromUrl({ title, company, jobUrl })
      }
      setTitle('')
      setCompany('')
      setDescription('')
      setJobUrl('')
      await refresh()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this job?')) return
    try {
      await deleteJob(id)
      setJobs((prev) => prev.filter((j) => j.id !== id))
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="font-display text-2xl font-semibold text-ink">Jobs</h1>
      <p className="mt-1 text-sm text-ash">
        Paste a job description or provide a posting URL to extract it.
      </p>

      <form onSubmit={handleSubmit} className="card mt-4">
        <div className="flex gap-1 rounded-md bg-paper p-1 text-sm">
          <button
            type="button"
            onClick={() => setMode('paste')}
            className={`flex-1 rounded px-3 py-1.5 font-medium transition-colors ${
              mode === 'paste' ? 'bg-white text-ink shadow-sm' : 'text-ash hover:text-ink'
            }`}
          >
            Paste Description
          </button>
          <button
            type="button"
            onClick={() => setMode('url')}
            className={`flex-1 rounded px-3 py-1.5 font-medium transition-colors ${
              mode === 'url' ? 'bg-white text-ink shadow-sm' : 'text-ash hover:text-ink'
            }`}
          >
            From URL
          </button>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-3">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Job title (optional)"
            className="field"
          />
          <input
            value={company}
            onChange={(e) => setCompany(e.target.value)}
            placeholder="Company (optional)"
            className="field"
          />
        </div>

        {mode === 'paste' ? (
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Paste the job description here…"
            rows={6}
            required
            className="field mt-3"
          />
        ) : (
          <input
            value={jobUrl}
            onChange={(e) => setJobUrl(e.target.value)}
            placeholder="https://company.com/careers/job-posting"
            required
            className="field mt-3"
          />
        )}

        <button type="submit" disabled={submitting} className="btn-primary mt-3">
          {submitting ? 'Saving…' : 'Save Job'}
        </button>
      </form>

      {error && (
        <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>
      )}

      <div className="mt-6 space-y-3">
        {loading && <div className="text-sm text-ash">Loading…</div>}

        {!loading && jobs.length === 0 && (
          <div className="card border-dashed text-center text-sm text-ash">No jobs yet.</div>
        )}

        {jobs.map((job) => (
          <div
            key={job.id}
            className="flex items-start justify-between gap-4 rounded-lg border border-line bg-white px-4 py-3"
          >
            <div className="min-w-0">
              <Link to={`/jobs/${job.id}/match`} className="text-sm font-medium text-ink hover:text-ochre">
                {job.title || 'Untitled role'}
                {job.company ? ` at ${job.company}` : ''}
              </Link>
              <div className="mt-1 text-xs text-ash">
                {job.source === 'URL' ? 'From URL' : 'Pasted'} · {new Date(job.createdAt).toLocaleString()}
              </div>
              <p className="mt-2 text-sm text-ink/70">{job.descriptionPreview}</p>
            </div>
            <button onClick={() => handleDelete(job.id)} className="btn-ghost-danger shrink-0">
              Delete
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}