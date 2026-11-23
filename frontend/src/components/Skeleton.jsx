// Placeholder blocks shown while data loads. Pure CSS (Tailwind animate-pulse),
// so they render on the first paint with no JS or network cost.
export function Skeleton({ className = '' }) {
  return <div aria-hidden="true" className={`animate-pulse rounded-md bg-line/70 ${className}`} />
}

export function SkeletonRow({ rich = false }) {
  if (rich) {
    // Matches the dashboard's icon + two-line + pill rows.
    return (
      <div className="flex items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3 shadow-card">
        <Skeleton className="h-9 w-9 shrink-0 !rounded-lg" />
        <div className="min-w-0 flex-1 space-y-1.5">
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="h-3 w-1/4" />
        </div>
        <Skeleton className="h-5 w-14 !rounded-full" />
      </div>
    )
  }
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-line bg-surface px-4 py-3">
      <Skeleton className="h-4 w-2/3" />
      <Skeleton className="h-3 w-12" />
    </div>
  )
}

export function SkeletonList({ rows = 3, rich = false }) {
  return (
    <div role="status" aria-label="Loading" className="space-y-2">
      {Array.from({ length: rows }, (_, i) => (
        <SkeletonRow key={i} rich={rich} />
      ))}
    </div>
  )
}

export function SkeletonRing({ size = 96 }) {
  return (
    <div className="flex flex-col items-center gap-2 py-2" aria-hidden="true">
      <div className="animate-pulse rounded-full bg-line/70" style={{ width: size, height: size }} />
      <Skeleton className="h-3 w-16" />
    </div>
  )
}

export function SkeletonCard({ lines = 3 }) {
  return (
    <div className="card space-y-2" aria-hidden="true">
      <Skeleton className="h-4 w-1/3" />
      {Array.from({ length: lines }, (_, i) => (
        <Skeleton key={i} className={`h-3 ${i === lines - 1 ? 'w-2/3' : 'w-full'}`} />
      ))}
    </div>
  )
}
