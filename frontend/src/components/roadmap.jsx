// Small pieces shared by the roadmap list, detail page and the Job Match card.

export const HOURS_OPTIONS = [4, 8, 12, 20]

export function jobLabel(r) {
  const title = r.jobTitle || 'Untitled role'
  return r.company ? `${title} at ${r.company}` : title
}

// Roadmap progress: gold (the Roadmaps hue) while in progress, green when done.
export function ProgressBar({ percent, className = 'h-2' }) {
  const value = Math.max(0, Math.min(100, percent || 0))
  return (
    <div
      className={`w-full overflow-hidden rounded-full bg-line ${className}`}
      role="progressbar"
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <div
        className={`h-full rounded-full transition-all duration-500 ${value === 100 ? 'bg-verdant' : 'bg-hue-gold'}`}
        style={{ width: `${value}%` }}
      />
    </div>
  )
}
