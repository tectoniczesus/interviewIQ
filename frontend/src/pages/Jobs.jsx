import { useEffect, useState } from 'react'
import StagePill from '../components/StagePill'
import { Link } from 'react-router-dom'
import { createJob, createJobFromUrl, deleteJob, listJobs } from '../services/jobApi'
import { SkeletonList } from '../components/Skeleton'
import { BriefcaseIcon } from '../components/icons'
import PageHeader, { HUES, HueChip } from '../components/PageHeader'

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
      <PageHeader
        icon={BriefcaseIcon}
        hue="teal"
        title="Jobs"
        description="Paste a job description or provide a posting URL to extract it."
        meta={
          !loading &&
          jobs.length > 0 && (
            <span className="rounded-full bg-surface/80 px-3 py-1 text-sm font-medium tabular-nums text-hue-teal">
              {jobs.length} saved
            </span>
          )
        }
      >
      <form onSubmit={handleSubmit} className="card">
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

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
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

        <button type="submit" disabled={submitting} className={`btn-primary mt-3 w-full sm:w-auto ${HUES.teal.button}`}>
          {submitting ? 'Saving…' : 'Save Job'}
        </button>
      </form>
      </PageHeader>

      {error && (
        <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>
      )}

      <div className="mt-6 space-y-3">
        {loading && <SkeletonList />}

        {!loading && jobs.length === 0 && (
          <div className="card border-dashed text-center text-sm text-ash">No jobs yet.</div>
        )}

        {jobs.map((job) => (
          <div
            key={job.id}
            className="card flex items-start gap-3 !px-4 !py-3 sm:gap-4"
          >
            <HueChip icon={BriefcaseIcon} hue="teal" />
            <div className="min-w-0 flex-1">
              <Link
                to={`/jobs/${job.id}/match`}
                className="break-words font-display text-base font-semibold leading-snug tracking-tight text-ink text-balance hover:text-accent"
              >
                {job.title || 'Untitled role'}
                {job.company ? ` at ${job.company}` : ''}
              </Link>
              <div className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-[13px] leading-5 text-ash">
                <Link to={`/applications?job=${job.id}`} title="Open in Applications">
                  <StagePill status={job.applicationStatus} />
                </Link>
                <span>
                  {job.source === 'URL' ? 'From URL' : 'Pasted'} ·{' '}
                  <time dateTime={job.createdAt} className="tabular-nums">
                    {new Date(job.createdAt).toLocaleDateString(undefined, {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                    })}
                  </time>
                </span>
              </div>
              <p className="mt-2 line-clamp-3 max-w-[65ch] break-words text-sm leading-relaxed text-ink/75 text-pretty">{job.descriptionPreview}</p>
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