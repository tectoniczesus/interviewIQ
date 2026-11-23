import { STAGES } from '../lib/applications'

export default function StagePill({ status, className = '' }) {
  const stage = STAGES[status] || STAGES.SAVED
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium ${stage.pill} ${className}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${stage.dot}`} aria-hidden="true" />
      {stage.label}
    </span>
  )
}
