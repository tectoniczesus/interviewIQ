import { useEffect, useState } from 'react'

export const tier = (score) => {
  if (score >= 80) return { text: 'text-verdant', verdict: 'Strong' }
  if (score >= 50) return { text: 'text-ochre', verdict: 'Needs work' }
  return { text: 'text-brick', verdict: 'Weak' }
}

// Lighter tier tones for dark surfaces (full class strings so Tailwind keeps them).
const TONE_ON_DARK = {
  'text-verdant': 'text-verdant-on-ink',
  'text-ochre': 'text-ochre-on-ink',
  'text-brick': 'text-brick-on-ink',
}

// Circular progress ring. The arc fills over 200ms (CSS transition on
// stroke-dashoffset) and is skipped entirely for reduced-motion users.
export default function ScoreCard({ label, score, size = 96, onDark = false }) {
  const SIZE = size
  const STROKE = size >= 120 ? 10 : 8
  const RADIUS = (SIZE - STROKE) / 2
  const CIRCUMFERENCE = 2 * Math.PI * RADIUS
  const value = Math.max(0, Math.min(100, Number(score) || 0))
  const [filled, setFilled] = useState(false)
  const { text: lightText, verdict } = tier(value)
  const text = onDark ? TONE_ON_DARK[lightText] : lightText

  useEffect(() => {
    const frame = requestAnimationFrame(() => setFilled(true))
    return () => cancelAnimationFrame(frame)
  }, [])

  const offset = CIRCUMFERENCE * (1 - (filled ? value : 0) / 100)

  return (
    <div className="flex flex-col items-center gap-2 py-2">
      <div
        className={`relative ${text}`}
        style={{ width: SIZE, height: SIZE }}
        role="img"
        aria-label={`${label}: ${value} out of 100, ${verdict}`}
      >
        <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`} className="-rotate-90">
          <circle
            cx={SIZE / 2}
            cy={SIZE / 2}
            r={RADIUS}
            fill="none"
            stroke="currentColor"
            strokeOpacity="0.15"
            strokeWidth={STROKE}
          />
          <circle
            cx={SIZE / 2}
            cy={SIZE / 2}
            r={RADIUS}
            fill="none"
            stroke="currentColor"
            strokeWidth={STROKE}
            strokeLinecap="round"
            strokeDasharray={CIRCUMFERENCE}
            strokeDashoffset={offset}
            className="transition-[stroke-dashoffset] duration-200 ease-out motion-reduce:transition-none"
          />
        </svg>
        <span className={`absolute inset-0 flex items-center justify-center font-mono ${size >= 120 ? 'text-4xl' : 'text-3xl'} font-semibold tabular-nums`}>
          {value}
        </span>
      </div>
      <div className={onDark ? 'text-xs font-medium uppercase tracking-wide text-paper/75' : 'eyebrow'}>{label}</div>
      <div className={`text-xs font-medium ${text}`}>{verdict}</div>
    </div>
  )
}
