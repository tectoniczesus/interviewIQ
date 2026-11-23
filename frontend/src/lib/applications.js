// Shared application-tracker rules used by the board, the dashboard and the
// stage suggestions. Dates are the backend's ISO "YYYY-MM-DD" strings and are
// handled in the viewer's local time zone, so "today" means the user's today.

export const STAGES = {
  SAVED: { label: 'Saved', pill: 'bg-line/60 text-ash', dot: 'bg-ash/60' },
  APPLIED: { label: 'Applied', pill: 'bg-ink/[0.07] text-ink', dot: 'bg-ink' },
  INTERVIEWING: { label: 'Interviewing', pill: 'bg-ochre/10 text-ochre', dot: 'bg-ochre' },
  OFFER: { label: 'Offer', pill: 'bg-verdant/10 text-verdant', dot: 'bg-verdant' },
  REJECTED: { label: 'Rejected', pill: 'bg-brick/10 text-brick', dot: 'bg-brick' },
  WITHDRAWN: { label: 'Withdrawn', pill: 'bg-line/60 text-ash', dot: 'bg-ash/40' },
}

export const OPEN_STAGES = ['SAVED', 'APPLIED', 'INTERVIEWING', 'OFFER']
export const CLOSED_STAGES = ['REJECTED', 'WITHDRAWN']
export const ALL_STAGES = [...OPEN_STAGES, ...CLOSED_STAGES]

export const STALE_DAYS = 14
export const FOLLOW_UP_DAYS = 7

const STAGE_RANK = { SAVED: 0, APPLIED: 1, INTERVIEWING: 2, OFFER: 3 }

export const isClosed = (status) => CLOSED_STAGES.includes(status)

/** True when `target` is a step forward for an open application. */
export function isForwardMove(status, target) {
  return !isClosed(status) && STAGE_RANK[target] > (STAGE_RANK[status] ?? 0)
}

export function toIsoDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export const todayIso = () => toIsoDate(new Date())

export function parseIsoDate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function addDays(iso, days) {
  const date = parseIsoDate(iso)
  date.setDate(date.getDate() + days)
  return toIsoDate(date)
}

export function formatDate(iso) {
  if (!iso) return ''
  return parseIsoDate(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

export function daysSince(instant) {
  if (!instant) return 0
  const ms = Date.now() - new Date(instant).getTime()
  return Math.max(0, Math.floor(ms / 86_400_000))
}

export function jobLabel(app) {
  return `${app.title || 'Untitled role'}${app.company ? ` at ${app.company}` : ''}`
}

/**
 * Why an open application needs the user's attention, or null.
 * A due follow-up wins; otherwise an Applied/Interviewing card with no
 * follow-up set and no stage change for STALE_DAYS counts as stale.
 */
export function attentionFor(app, today = todayIso()) {
  if (isClosed(app.status)) return null

  if (app.followUpOn && app.followUpOn <= today) {
    return {
      kind: 'FOLLOW_UP_DUE',
      label: app.followUpOn === today ? 'Follow up today' : `Follow-up overdue since ${formatDate(app.followUpOn)}`,
    }
  }

  const days = daysSince(app.statusChangedAt)
  if ((app.status === 'APPLIED' || app.status === 'INTERVIEWING') && !app.followUpOn && days >= STALE_DAYS) {
    return { kind: 'STALE', label: `No update in ${days} days` }
  }
  return null
}

/**
 * Builds the full PUT payload for an application with `changes` applied.
 * Moving to Applied fills a missing applied date (today) and follow-up date
 * (applied + 7 days) using the user's local date; the server does the same
 * as a fallback.
 */
export function buildUpdate(app, changes = {}) {
  const next = {
    status: app.status,
    appliedOn: app.appliedOn ?? null,
    followUpOn: app.followUpOn ?? null,
    notes: app.notes ?? '',
    ...changes,
  }
  if (next.status === 'APPLIED' && app.status !== 'APPLIED') {
    if (!next.appliedOn) next.appliedOn = todayIso()
    if (!next.followUpOn) next.followUpOn = addDays(next.appliedOn, FOLLOW_UP_DAYS)
  }
  return next
}
