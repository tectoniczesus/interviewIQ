import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { listResumes } from '../services/resumeApi'
import { listJobs } from '../services/jobApi'
import { useStageSuggestion } from '../components/StageSuggestion'
import { Skeleton, SkeletonList } from '../components/Skeleton'
import { CheckIcon, ChevronRightIcon, MailIcon, PenIcon } from '../components/icons'
import PageHeader, { HUES, HueChip } from '../components/PageHeader'
import { timeAgo } from '../lib/time'
import {
  createCoverLetter,
  deleteCoverLetter,
  listCoverLetters,
  regenerateCoverLetter,
} from '../services/coverLetterApi'

const wordCount = (text) => (text || '').trim().split(/\s+/).filter(Boolean).length
const preview = (text) => (text || '').replace(/\s+/g, ' ').trim()

// A link that stands in for a missing composer input ("for [a saved job]").
function SlotLink({ to, children }) {
  return (
    <Link
      to={to}
      className="rounded-sm font-display text-accent-on-ink underline decoration-accent-on-ink/50 underline-offset-[6px] transition-colors hover:decoration-accent-on-ink"
    >
      {children}
    </Link>
  )
}

function LetterCard({
  letter,
  index,
  entered,
  open,
  reveal,
  title,
  subtitle,
  copied,
  regenerating,
  onToggle,
  onCopy,
  onRegenerate,
  onDelete,
}) {
  const bodyId = `letter-${letter.id}`
  return (
    <li
      style={{ '--i': Math.min(index, 6) }}
      className={`card !p-0 ${entered ? 'card-settle' : ''}`}
    >
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={open}
        aria-controls={bodyId}
        className="group flex w-full items-start gap-3 rounded-xl px-4 py-3 text-left sm:px-5 sm:py-4"
      >
        <HueChip icon={MailIcon} hue="pink" />
        <span className="min-w-0 flex-1">
          <span className="block break-words font-display text-base font-semibold leading-snug tracking-tight text-ink text-balance">
            {title}
          </span>
          <span className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-[13px] leading-5 text-ash">
            <span className="min-w-0 truncate">{subtitle}</span>
            <span className="inline-flex items-center gap-2 whitespace-nowrap">
              <span aria-hidden="true">·</span>
              <time dateTime={letter.updatedAt} className="tabular-nums">
                {timeAgo(letter.updatedAt)}
              </time>
            </span>
            <span className="inline-flex items-center gap-2 whitespace-nowrap">
              <span aria-hidden="true">·</span>
              <span className="font-mono tabular-nums">{wordCount(letter.content)} words</span>
            </span>
          </span>
          {!open && (
            <span className="mt-2 line-clamp-2 max-w-[65ch] text-sm leading-relaxed text-ink/70 text-pretty">
              {preview(letter.content)}
            </span>
          )}
        </span>
        <ChevronRightIcon
          size={18}
          className={`mt-1 shrink-0 text-ash/50 transition-[transform,color] duration-200 group-hover:text-ink motion-reduce:transition-none ${
            open ? 'rotate-90' : ''
          }`}
        />
      </button>

      <div id={bodyId} className="disclose" data-open={open}>
        <div>
          <div className="px-4 pb-4 sm:px-5 sm:pb-5">
            <article
              className={`rounded-lg bg-paper p-4 sm:p-6 ${reveal ? 'letter-reveal' : ''}`}
              aria-busy={regenerating}
              aria-label={`Cover letter for ${title}`}
            >
              <span aria-hidden="true" className="block h-0.5 w-10 rounded-full bg-hue-pink" />
              <pre
                className={`mt-4 max-w-[68ch] whitespace-pre-wrap break-words font-sans text-[15px] leading-7 transition-colors duration-200 ${
                  regenerating ? 'animate-pulse text-ink/40' : 'text-ink'
                }`}
              >
                {letter.content}
              </pre>
            </article>
            <div className="mt-3 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={onCopy}
                className={`btn-secondary gap-1.5 ${copied ? '!border-verdant/40 !text-verdant' : ''}`}
              >
                {copied ? (
                  <>
                    <CheckIcon size={14} />
                    Copied
                  </>
                ) : (
                  'Copy text'
                )}
              </button>
              <button type="button" onClick={onRegenerate} disabled={regenerating} className="btn-secondary">
                {regenerating ? 'Redrafting…' : 'Regenerate'}
              </button>
              <button type="button" onClick={onDelete} className="btn-ghost-danger">
                Delete
              </button>
            </div>
          </div>
        </div>
      </div>
    </li>
  )
}

export default function CoverLetters() {
  const { suggestStage } = useStageSuggestion()
  const [resumes, setResumes] = useState([])
  const [jobs, setJobs] = useState([])
  const [coverLetters, setCoverLetters] = useState([])
  const [loading, setLoading] = useState(true)
  const [entered, setEntered] = useState(false) // staggers the list once, on first load only
  const [error, setError] = useState('')

  const [selectedResumeId, setSelectedResumeId] = useState('')
  const [selectedJobId, setSelectedJobId] = useState('')
  const [candidateName, setCandidateName] = useState('')
  const [generating, setGenerating] = useState(false)

  const [expandedId, setExpandedId] = useState(null)
  const [revealId, setRevealId] = useState(null) // the letter that was just drafted or redrafted
  const [regeneratingId, setRegeneratingId] = useState(null)
  const [copiedId, setCopiedId] = useState(null)
  const copyTimer = useRef(null)

  useEffect(() => () => clearTimeout(copyTimer.current), [])

  useEffect(() => {
    let cancelled = false
    Promise.all([listResumes(), listJobs(), listCoverLetters()])
      .then(([resumeData, jobData, letterData]) => {
        if (cancelled) return
        setResumes(resumeData)
        setJobs(jobData)
        setCoverLetters(letterData)
        setEntered(true)
        const firstResume = resumeData.find((r) => r.processingStatus === 'COMPLETED')
        if (firstResume) setSelectedResumeId(firstResume.id)
        if (jobData.length > 0) setSelectedJobId(jobData[0].id)
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

  async function handleGenerate(e) {
    e.preventDefault()
    if (!selectedResumeId || !selectedJobId || generating) return

    setGenerating(true)
    setError('')
    try {
      const letter = await createCoverLetter(selectedResumeId, selectedJobId, candidateName || undefined)
      setCoverLetters((prev) => [letter, ...prev])
      setExpandedId(letter.id)
      setRevealId(letter.id)
      // A cover letter usually means you're about to apply; offer (never force) the move.
      suggestStage(selectedJobId, 'APPLIED', 'Cover letter ready.')
    } catch (err) {
      setError(err.message)
    } finally {
      setGenerating(false)
    }
  }

  async function handleRegenerate(id) {
    setRegeneratingId(id)
    setRevealId(null)
    setError('')
    try {
      const updated = await regenerateCoverLetter(id)
      setCoverLetters((prev) => prev.map((c) => (c.id === id ? updated : c)))
      setExpandedId(id)
      setRevealId(id)
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
      clearTimeout(copyTimer.current)
      copyTimer.current = setTimeout(() => setCopiedId(null), 1500)
    } catch {
      setError('Couldn’t copy to the clipboard. Select the letter text and copy it instead.')
    }
  }

  function resumeName(resumeId) {
    const name = resumes.find((r) => r.id === resumeId)?.fileName
    return name ? name.replace(/\.pdf$/i, '') : 'Resume'
  }

  function jobLabel(jobId) {
    const job = jobs.find((j) => j.id === jobId)
    if (!job) return 'Job'
    return job.title ? `${job.title}${job.company ? ` at ${job.company}` : ''}` : 'Untitled role'
  }

  const usableResumes = resumes.filter((r) => r.processingStatus === 'COMPLETED')
  const hasJobs = jobs.length > 0
  const hasResumes = usableResumes.length > 0
  const canDraft = hasJobs && hasResumes && selectedResumeId && selectedJobId

  let hint = 'You can regenerate any letter afterwards.'
  if (!hasJobs && !hasResumes) hint = 'Upload a resume and save a job first.'
  else if (!hasResumes) hint = 'Upload a resume first. Letters need its extracted text.'
  else if (!hasJobs) hint = 'Save a job first. Letters are written to its description.'

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        icon={PenIcon}
        hue="pink"
        title="Cover Letters"
        description="Each letter is drafted from one resume’s text and one saved job description, so it speaks to that role."
        meta={
          !loading &&
          coverLetters.length > 0 && (
            <span className="rounded-full bg-surface/80 px-3 py-1 text-sm font-medium tabular-nums text-hue-pink">
              {coverLetters.length} {coverLetters.length === 1 ? 'letter' : 'letters'}
            </span>
          )
        }
      >
      {/* The composer is one sentence. Each blank is a real, labelled control. */}
      <form
        onSubmit={handleGenerate}
        aria-labelledby="composer-heading"
        className="rounded-2xl bg-ink p-5 text-paper sm:p-6"
      >
        <h2 id="composer-heading" className="sr-only">
          Compose a cover letter
        </h2>

        {loading ? (
          <div className="space-y-4" aria-hidden="true">
            <div className="flex flex-wrap items-center gap-3">
              <Skeleton className="h-6 w-40 !bg-paper/15" />
              <Skeleton className="h-10 w-56 !bg-paper/15" />
              <Skeleton className="h-6 w-14 !bg-paper/15" />
              <Skeleton className="h-10 w-44 !bg-paper/15" />
            </div>
            <Skeleton className="h-10 w-32 !bg-paper/15" />
          </div>
        ) : (
          <>
            <p className="flex flex-wrap items-center gap-x-2.5 gap-y-3 font-display text-lg font-semibold leading-snug tracking-tight text-paper/85 sm:text-xl">
              <span>Write a cover letter for</span>

              {hasJobs ? (
                <>
                  <label htmlFor="cl-job" className="sr-only">
                    Job
                  </label>
                  <select
                    id="cl-job"
                    value={selectedJobId}
                    onChange={(e) => setSelectedJobId(e.target.value)}
                    className="field-on-ink sm:max-w-sm"
                  >
                    {jobs.map((j) => (
                      <option key={j.id} value={j.id}>
                        {j.title || 'Untitled role'}
                        {j.company ? ` at ${j.company}` : ''}
                      </option>
                    ))}
                  </select>
                </>
              ) : (
                <SlotLink to="/jobs">a saved job</SlotLink>
              )}

              <span>using</span>

              {hasResumes ? (
                <>
                  <label htmlFor="cl-resume" className="sr-only">
                    Resume
                  </label>
                  <select
                    id="cl-resume"
                    value={selectedResumeId}
                    onChange={(e) => setSelectedResumeId(e.target.value)}
                    className="field-on-ink sm:max-w-xs"
                  >
                    {usableResumes.map((r) => (
                      <option key={r.id} value={r.id}>
                        {(r.fileName || 'Resume').replace(/\.pdf$/i, '')}
                      </option>
                    ))}
                  </select>
                </>
              ) : (
                <SlotLink to="/resumes">an uploaded resume</SlotLink>
              )}

              <span>and sign it</span>

              <label htmlFor="cl-name" className="sr-only">
                Your name for the signature (optional)
              </label>
              <input
                id="cl-name"
                value={candidateName}
                onChange={(e) => setCandidateName(e.target.value)}
                placeholder="your name"
                autoComplete="name"
                className="field-on-ink w-[14ch] font-sans text-base font-medium"
              />
            </p>

            <div className="mt-5 flex flex-wrap items-center gap-x-4 gap-y-2">
              <button
                type="submit"
                disabled={!canDraft || generating}
                className={`btn-primary ${HUES.pink.button}`}
              >
                {generating ? 'Drafting…' : 'Draft the letter'}
              </button>
              <span className="text-sm text-paper/60">{hint}</span>
            </div>
          </>
        )}
      </form>
      </PageHeader>

      {error && (
        <div role="alert" className="mt-4 rounded-lg border border-brick/20 bg-brick/5 px-4 py-3 text-sm text-brick">
          {error}
        </div>
      )}

      <section className="mt-8" aria-labelledby="letters-heading">
        <div className="flex items-baseline justify-between gap-3">
          <h2 id="letters-heading" className="font-display text-base font-semibold tracking-tight text-ink">
            Your letters
          </h2>
        </div>

        {loading && (
          <div className="mt-3">
            <SkeletonList />
          </div>
        )}

        {!loading && (
          <ul className={`mt-3 space-y-3 ${entered ? 'stagger' : ''}`}>
            {generating && (
              <li className="card card-settle" role="status" aria-live="polite">
                <div className="text-sm text-ink">
                  Drafting a letter for <span className="font-medium">{jobLabel(selectedJobId)}</span> from{' '}
                  <span className="font-medium">{resumeName(selectedResumeId)}</span>…
                </div>
                <div className="mt-3 rounded-lg bg-paper p-4 sm:p-6" aria-hidden="true">
                  <span className="block h-0.5 w-10 rounded-full bg-hue-pink/40" />
                  <div className="mt-4 space-y-2.5">
                    <Skeleton className="h-3 w-5/6 !bg-ink/10" />
                    <Skeleton className="h-3 w-full !bg-ink/10" />
                    <Skeleton className="h-3 w-11/12 !bg-ink/10" />
                    <Skeleton className="h-3 w-2/3 !bg-ink/10" />
                  </div>
                </div>
              </li>
            )}

            {!generating && coverLetters.length === 0 && (
              <li className="rounded-xl border border-dashed border-hue-pink/30 bg-white/60 px-4 py-8 text-center text-sm text-ash">
                No letters yet. Your first draft will appear here.
              </li>
            )}

            {coverLetters.map((letter, i) => (
              <LetterCard
                key={letter.id}
                letter={letter}
                index={i}
                entered={entered}
                open={expandedId === letter.id}
                reveal={revealId === letter.id}
                title={jobLabel(letter.jobId)}
                subtitle={resumeName(letter.resumeId)}
                copied={copiedId === letter.id}
                regenerating={regeneratingId === letter.id}
                onToggle={() => setExpandedId(expandedId === letter.id ? null : letter.id)}
                onCopy={() => handleCopy(letter.id, letter.content)}
                onRegenerate={() => handleRegenerate(letter.id)}
                onDelete={() => handleDelete(letter.id)}
              />
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
