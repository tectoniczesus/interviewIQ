// Visual breakdown of a job match: matched-vs-missing bars per category,
// plus chips for the individual skills and keywords. Pure Tailwind, no chart lib.

function Chip({ children, tone }) {
  const tones = {
    good: 'bg-verdant/10 text-verdant border-verdant/20',
    bad: 'bg-brick/10 text-brick border-brick/20',
    warn: 'bg-ochre/10 text-ochre border-ochre/30',
  }
  return (
    <span
      className={`inline-block max-w-full break-words rounded-full border px-2.5 py-0.5 text-xs ${tones[tone]}`}
    >
      {children}
    </span>
  )
}

function CoverageBar({ label, matched, missing }) {
  const total = matched + missing
  if (total === 0) return null
  const pct = Math.round((matched / total) * 100)

  return (
    <div>
      <div className="flex items-baseline justify-between gap-3 text-sm">
        <span className="text-ink">{label}</span>
        <span className="font-mono text-xs tabular-nums text-ash">
          {matched} of {total} · {pct}%
        </span>
      </div>
      <div
        className="mt-1.5 flex h-2 overflow-hidden rounded-full bg-brick/20"
        role="img"
        aria-label={`${label}: ${matched} matched, ${missing} missing`}
      >
        <div
          className="bg-verdant transition-[width] duration-200 ease-out motion-reduce:transition-none"
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}

function ChipGroup({ title, items, tone }) {
  if (!items || items.length === 0) return null
  return (
    <div>
      <div className="eyebrow">{title}</div>
      <div className="mt-1.5 flex flex-wrap gap-1.5">
        {items.map((item, i) => (
          <Chip key={i} tone={tone}>
            {item}
          </Chip>
        ))}
      </div>
    </div>
  )
}

export default function MatchBreakdown({ match }) {
  const matchingSkills = match.matchingSkills || []
  const missingSkills = match.missingSkills || []
  const matchingExperience = match.matchingExperience || []
  const missingRequirements = match.missingRequirements || []
  const keywordGaps = match.keywordGaps || []

  const hasData =
    matchingSkills.length + missingSkills.length + matchingExperience.length + missingRequirements.length > 0
  if (!hasData && keywordGaps.length === 0) return null

  return (
    <div className="card space-y-5">
      <h2 className="font-display text-sm font-semibold text-ink">Match breakdown</h2>

      <div className="space-y-4">
        <CoverageBar label="Skills" matched={matchingSkills.length} missing={missingSkills.length} />
        <CoverageBar
          label="Experience & requirements"
          matched={matchingExperience.length}
          missing={missingRequirements.length}
        />
      </div>

      <div className="space-y-4">
        <ChipGroup title="Skills you have" items={matchingSkills} tone="good" />
        <ChipGroup title="Skills to add" items={missingSkills} tone="bad" />
        <ChipGroup title="Keywords to work in" items={keywordGaps} tone="warn" />
      </div>
    </div>
  )
}
