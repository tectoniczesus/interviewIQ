import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { analyzeResume, getResume, getResumeAnalysis } from '../services/resumeApi'
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

export default function ResumeAnalysis() {
  const { id } = useParams()
  const [resume, setResume] = useState(null)
  const [analysis, setAnalysis] = useState(null)
  const [loading, setLoading] = useState(true)
  const [analyzing, setAnalyzing] = useState(false)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    setError('')

    try {
      setResume(await getResume(id))
    } catch (err) {
      setError(err.message)
      setLoading(false)
      return
    }

    try {
      setAnalysis(await getResumeAnalysis(id))
    } catch (err) {
      if (err.status === 404) {
        setAnalysis(null) // not analyzed yet - not an error state
      } else {
        setError(err.message)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function handleAnalyze() {
    setAnalyzing(true)
    setError('')
    try {
      setAnalysis(await analyzeResume(id))
    } catch (err) {
      setError(err.message)
    } finally {
      setAnalyzing(false)
    }
  }

  if (loading) {
    return <div className="text-sm text-ash">Loading…</div>
  }

  const canAnalyze = resume?.processingStatus === 'COMPLETED'

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/resumes" className="text-xs text-ash hover:text-ochre">
        ← Back to resumes
      </Link>

      <div className="mt-2 flex items-center justify-between gap-4">
        <h1 className="truncate font-display text-2xl font-semibold text-ink">{resume?.fileName}</h1>
        <button onClick={handleAnalyze} disabled={analyzing || !canAnalyze} className="btn-primary shrink-0">
          {analyzing ? 'Analyzing…' : analysis ? 'Re-analyze' : 'Analyze Resume'}
        </button>
      </div>

      {!canAnalyze && (
        <div className="mt-4 rounded-md bg-ochre/10 px-4 py-2 text-sm text-ochre">
          This resume's text hasn't finished processing yet (status: {resume?.processingStatus}), so it can't be analyzed.
        </div>
      )}

      {error && (
        <div className="mt-4 rounded-md bg-brick/10 px-4 py-2 text-sm text-brick">{error}</div>
      )}

      {!analysis && !error && canAnalyze && (
        <div className="card mt-6 border-dashed text-center text-sm text-ash">
          No analysis yet. Click "Analyze Resume" to get an ATS score and feedback.
        </div>
      )}

      {analysis && (
        <div className="mt-6 space-y-4">
          <div className="card flex items-center justify-center gap-10">
            <ScoreCard label="ATS Score" score={analysis.atsScore} />
            <ScoreCard label="Overall Score" score={analysis.overallScore} />
          </div>

          <Section title="Strengths" items={analysis.strengths} />
          <Section title="Weaknesses" items={analysis.weaknesses} />
          <Section title="Missing Skills" items={analysis.missingSkills} />
          <Section title="Formatting Issues" items={analysis.formattingIssues} />
          <Section title="Keyword Suggestions" items={analysis.keywordSuggestions} />
          <Section title="Improvement Suggestions" items={analysis.improvementSuggestions} />

          <div className="font-mono text-xs text-ash">
            Analyzed {new Date(analysis.createdAt).toLocaleString()}
          </div>
        </div>
      )}
    </div>
  )
}