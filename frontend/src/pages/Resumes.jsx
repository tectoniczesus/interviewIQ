import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { deleteResume, listResumes, uploadResume } from '../services/resumeApi'
import StatusBadge from '../components/StatusBadge'

export default function Resumes() {
  const [resumes, setResumes] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const fileInputRef = useRef(null)

  async function refresh() {
    setLoading(true)
    try {
      setResumes(await listResumes())
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

  async function handleFileChange(e) {
    const file = e.target.files?.[0]
    if (!file) return

    setUploading(true)
    setError('')
    try {
      await uploadResume(file)
      await refresh()
    } catch (err) {
      setError(err.message)
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this resume?')) return
    try {
      await deleteResume(id)
      setResumes((prev) => prev.filter((r) => r.id !== id))
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">My Resumes</h1>
          <p className="mt-1 text-sm text-ash">Upload a PDF resume to get started.</p>
        </div>

        <label className="btn-primary cursor-pointer">
          {uploading ? 'Uploading…' : 'Upload Resume'}
          <input
            ref={fileInputRef}
            type="file"
            accept="application/pdf"
            className="hidden"
            disabled={uploading}
            onChange={handleFileChange}
          />
        </label>
      </div>

      {error && (
        <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>
      )}

      <div className="mt-6 space-y-3">
        {loading && <div className="text-sm text-ash">Loading…</div>}

        {!loading && resumes.length === 0 && (
          <div className="card border-dashed text-center text-sm text-ash">
            No resumes yet. Upload a PDF to get started.
          </div>
        )}

        {resumes.map((resume) => (
          <div key={resume.id} className="flex items-center justify-between rounded-lg border border-line bg-white px-4 py-3">
            <div>
              <Link
                to={`/resumes/${resume.id}/analysis`}
                className="text-sm font-medium text-ink hover:text-ochre"
              >
                {resume.fileName}
              </Link>
              <div className="mt-1 text-xs text-ash">
                Uploaded {new Date(resume.createdAt).toLocaleString()}
              </div>
            </div>

            <div className="flex items-center gap-3">
              <StatusBadge status={resume.processingStatus} />
              <button onClick={() => handleDelete(resume.id)} className="btn-ghost-danger">
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}