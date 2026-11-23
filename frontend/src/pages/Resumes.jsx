import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { deleteResume, listResumes, uploadResume } from '../services/resumeApi'
import { SkeletonList } from '../components/Skeleton'
import { ChevronRightIcon, DocumentIcon } from '../components/icons'
import PageHeader from '../components/PageHeader'
import { timeAgo } from '../lib/time'

// What each processing state means for the user, in product language. Full class
// strings (not built dynamically) so Tailwind's JIT keeps them.
const STATUS = {
  COMPLETED: { label: 'Text extracted', dot: 'bg-verdant', text: 'text-verdant', lines: 'text-ink/60' },
  PROCESSING: { label: 'Reading text…', dot: 'bg-ochre', text: 'text-ochre-deep', lines: 'text-ochre/70' },
  FAILED: { label: 'Couldn’t read this PDF', dot: 'bg-brick', text: 'text-brick', lines: 'text-brick/40' },
  UPLOADED: { label: 'Uploaded', dot: 'bg-ash/60', text: 'text-ash', lines: 'text-line' },
}
const FALLBACK_STATUS = STATUS.UPLOADED

const isPdf = (file) => file.type === 'application/pdf' || /\.pdf$/i.test(file.name || '')
const displayName = (fileName) => (fileName || 'Untitled resume').replace(/\.pdf$/i, '')

// A drawn page: folded corner and four text lines. The lines take the current text
// colour, and light up one after another while `busy` (see .page-lines in index.css).
function PageThumb({ busy = false, lines = 'text-ink/60', className = '' }) {
  return (
    <span
      aria-hidden="true"
      data-busy={busy}
      className={`page-lines relative block h-12 w-9 shrink-0 rounded-[3px] border border-line bg-white shadow-card ${lines} ${className}`}
    >
      <span className="absolute right-0 top-0 h-2.5 w-2.5 bg-paper [clip-path:polygon(0_0,100%_100%,0_100%)]" />
      <span className="absolute right-0 top-0 h-2.5 w-2.5 bg-line/80 [clip-path:polygon(0_0,0_100%,100%_100%)]" />
      <span className="absolute inset-x-[7px] top-4 flex flex-col gap-[3px]">
        {[82, 100, 68, 90].map((w, i) => (
          <span
            key={i}
            style={{ '--i': i, width: `${w}%` }}
            className="page-line block h-[2px] rounded-full bg-current"
          />
        ))}
      </span>
    </span>
  )
}

export default function Resumes() {
  const [resumes, setResumes] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [error, setError] = useState('')
  const [newId, setNewId] = useState(null)
  const [entered, setEntered] = useState(false) // staggers the list once, on first load only
  const fileInputRef = useRef(null)
  const dragDepth = useRef(0)

  useEffect(() => {
    let cancelled = false
    listResumes()
      .then((data) => {
        if (cancelled) return
        setResumes(Array.isArray(data) ? data : [])
        setEntered(true)
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

  async function handleFile(file) {
    if (!file || uploading) return
    if (!isPdf(file)) {
      setError('Only PDF resumes are supported right now.')
      return
    }
    setUploading(true)
    setError('')
    try {
      const created = await uploadResume(file)
      setResumes((prev) => [created, ...prev.filter((r) => r.id !== created.id)])
      setNewId(created.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  // Drag state has to survive the enter/leave pairs that fire on child elements.
  const hasFiles = (e) => Array.from(e.dataTransfer?.types || []).includes('Files')
  function onDragEnter(e) {
    if (!hasFiles(e)) return
    e.preventDefault()
    dragDepth.current += 1
    setDragging(true)
  }
  function onDragOver(e) {
    if (!hasFiles(e)) return
    e.preventDefault()
    e.dataTransfer.dropEffect = uploading ? 'none' : 'copy'
  }
  function onDragLeave(e) {
    if (!hasFiles(e)) return
    dragDepth.current = Math.max(0, dragDepth.current - 1)
    if (dragDepth.current === 0) setDragging(false)
  }
  function onDrop(e) {
    if (!hasFiles(e)) return
    e.preventDefault()
    dragDepth.current = 0
    setDragging(false)
    handleFile(e.dataTransfer.files?.[0])
  }

  async function handleDelete(id) {
    if (!confirm('Delete this resume? Its analyses, job matches and cover letters go with it.')) return
    try {
      await deleteResume(id)
      setResumes((prev) => prev.filter((r) => r.id !== id))
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        icon={DocumentIcon}
        hue="blue"
        title="Resumes"
        description="Upload a PDF. InterviewIQ reads the text, then you can score it and match it against jobs."
        meta={
          !loading &&
          resumes.length > 0 && (
            <span className="rounded-full bg-surface/80 px-3 py-1 text-sm font-medium tabular-nums text-hue-blue">
              {resumes.length} on file
            </span>
          )
        }
      >
      {/* Drop zone. The input is visually hidden but keyboard-reachable; the label is the
          whole target, and the input's focus ring is drawn on the label via `peer`. */}
      <div
        className="relative"
        onDragEnter={onDragEnter}
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
      >
        <input
          ref={fileInputRef}
          id="resume-file"
          type="file"
          accept="application/pdf"
          className="peer sr-only"
          disabled={uploading}
          onChange={(e) => handleFile(e.target.files?.[0])}
        />
        <label
          htmlFor="resume-file"
          className={`flex cursor-pointer items-center gap-4 rounded-xl border-2 border-dashed px-4 py-4 transition-[border-color,background-color,transform] duration-150 peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent peer-disabled:cursor-progress sm:px-5 sm:py-5 ${
            dragging
              ? 'scale-[1.01] border-accent bg-accent/5 motion-reduce:scale-100'
              : uploading
                ? 'border-hue-blue/60 bg-white'
                : 'border-hue-blue/30 bg-white hover:border-hue-blue/60'
          }`}
        >
          <PageThumb busy={uploading} lines={uploading || dragging ? 'text-hue-blue' : 'text-hue-blue/40'} />
          <span className="min-w-0 flex-1">
            <span className="block font-display text-base font-semibold tracking-tight text-ink">
              {uploading ? 'Reading your PDF…' : dragging ? 'Release to upload' : 'Drop a PDF here'}
            </span>
            <span className="mt-0.5 block text-sm text-ash">
              {uploading ? (
                'Uploading the file and extracting its text.'
              ) : (
                <>
                  or <span className="font-medium text-ink underline decoration-accent/60 underline-offset-4">browse your files</span>. PDF only.
                </>
              )}
            </span>
          </span>
        </label>
      </div>
      </PageHeader>

      {error && (
        <div
          role="alert"
          className="mt-4 rounded-lg border border-brick/20 bg-brick/5 px-4 py-3 text-sm text-brick"
        >
          {error}
        </div>
      )}

      <div className="mt-6">
        {loading && <SkeletonList />}

        {!loading && resumes.length === 0 && (
          <p className="px-1 text-sm text-ash">Your first resume will appear here.</p>
        )}

        {resumes.length > 0 && (
          <ul aria-label="Your resumes" className={`space-y-3 ${entered ? 'stagger' : ''}`}>
            {resumes.map((resume, i) => {
              const s = STATUS[resume.processingStatus] || FALLBACK_STATUS
              const ready = resume.processingStatus === 'COMPLETED'
              const analysisHref = `/resumes/${resume.id}/analysis`
              return (
                <li
                  key={resume.id}
                  style={{ '--i': Math.min(i, 6) }}
                  className={`relative ${entered ? 'card-settle' : ''} ${newId === resume.id ? 'ring-fade' : ''}`}
                >
                  <div className="card-link flex flex-wrap items-center gap-x-4 gap-y-3 px-4 py-3 sm:flex-nowrap">
                    <PageThumb lines={s.lines} />
                    <div className="min-w-0 flex-1">
                      <Link
                        to={analysisHref}
                        className="block break-words font-display text-base font-semibold leading-snug tracking-tight text-ink hover:text-accent"
                      >
                        {displayName(resume.fileName)}
                      </Link>
                      <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-[13px] leading-5 text-ash">
                        <span className={`inline-flex items-center gap-1.5 font-medium ${s.text}`}>
                          <span className={`h-1.5 w-1.5 rounded-full ${s.dot}`} aria-hidden="true" />
                          {s.label}
                        </span>
                        <span className="inline-flex items-center gap-2 whitespace-nowrap">
                          <span aria-hidden="true">·</span>
                          <time dateTime={resume.createdAt} className="tabular-nums">
                            Uploaded {timeAgo(resume.createdAt)}
                          </time>
                        </span>
                      </div>
                    </div>
                    <div className="flex w-full shrink-0 items-center justify-end gap-1 sm:w-auto">
                      {ready && (
                        <Link to={analysisHref} className="btn-secondary group gap-1 !pl-3 !pr-2 text-xs">
                          Open analysis
                          <ChevronRightIcon size={14} className="text-ash transition-transform group-hover:translate-x-0.5 motion-reduce:transform-none" />
                        </Link>
                      )}
                      <button type="button" onClick={() => handleDelete(resume.id)} className="btn-ghost-danger">
                        Delete
                      </button>
                    </div>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </div>
  )
}
