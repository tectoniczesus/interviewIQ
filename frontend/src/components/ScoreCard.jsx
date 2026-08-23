const tiers = (score) => {
  if (score >= 80) return 'border-verdant text-verdant ring-verdant/10'
  if (score >= 50) return 'border-ochre text-ochre ring-ochre/10'
  return 'border-brick text-brick ring-brick/10'
}

export default function ScoreCard({ label, score }) {
  return (
    <div className="flex flex-col items-center gap-2 py-2">
      <div
        className={`flex h-24 w-24 -rotate-3 items-center justify-center rounded-full border-2 bg-white ring-8 ${tiers(score)}`}
      >
        <span className="font-mono text-3xl font-semibold tabular-nums">{score}</span>
      </div>
      <div className="eyebrow">{label}</div>
    </div>
  )
}