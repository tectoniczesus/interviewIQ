import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { getApplication, updateApplication } from '../services/applicationApi'
import { STAGES, buildUpdate, isForwardMove, jobLabel } from '../lib/applications'

// One-click "move this application forward?" prompts. The app never changes a
// stage on its own: a page calls suggestStage() after an action that hints at
// progress (e.g. generating a cover letter), and only "Yes" moves the card.
const StageSuggestionContext = createContext({ suggestStage: () => {} })

export function useStageSuggestion() {
  return useContext(StageSuggestionContext)
}

export function StageSuggestionProvider({ children }) {
  const [suggestion, setSuggestion] = useState(null) // { app, target, reason }
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const confirmTimer = useRef(null)

  useEffect(() => () => clearTimeout(confirmTimer.current), [])

  const suggestStage = useCallback(async (jobId, target, reason) => {
    if (!jobId) return
    try {
      const app = await getApplication(jobId)
      if (!isForwardMove(app.status, target)) return
      setError('')
      setSuggestion({ app, target, reason })
    } catch {
      // Suggestions are a convenience; never surface a failure to fetch one.
    }
  }, [])

  async function accept() {
    if (!suggestion) return
    setBusy(true)
    setError('')
    try {
      const { app, target } = suggestion
      await updateApplication(app.jobId, buildUpdate(app, { status: target }))
      setSuggestion(null)
      setConfirmation(`Moved to ${STAGES[target].label}`)
      clearTimeout(confirmTimer.current)
      confirmTimer.current = setTimeout(() => setConfirmation(''), 3000)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <StageSuggestionContext.Provider value={{ suggestStage }}>
      {children}
      <div
        className="pointer-events-none fixed inset-x-0 bottom-0 z-50 flex justify-center px-4 pb-[max(1rem,env(safe-area-inset-bottom))] sm:justify-end sm:px-6"
        role="status"
        aria-live="polite"
      >
        {suggestion && (
          <div className="card pointer-events-auto w-full max-w-sm shadow-card-hover">
            <p className="text-sm text-ink">
              {suggestion.reason && <span className="text-ash">{suggestion.reason} </span>}
              Mark <span className="font-medium">{jobLabel(suggestion.app)}</span> as{' '}
              {STAGES[suggestion.target].label}?
            </p>
            {error && <p className="mt-2 text-xs text-brick">{error}</p>}
            <div className="mt-3 flex justify-end gap-2">
              <button type="button" className="btn-secondary" onClick={() => setSuggestion(null)} disabled={busy}>
                Not yet
              </button>
              <button type="button" className="btn-primary" onClick={accept} disabled={busy}>
                {busy ? 'Moving…' : `Yes, mark ${STAGES[suggestion.target].label}`}
              </button>
            </div>
          </div>
        )}
        {!suggestion && confirmation && (
          <div className="pointer-events-auto rounded-full bg-ink px-4 py-2 text-sm text-paper shadow-card-hover">
            {confirmation}
          </div>
        )}
      </div>
    </StageSuggestionContext.Provider>
  )
}
