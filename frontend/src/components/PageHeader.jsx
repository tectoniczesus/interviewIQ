// iOS-Settings-style colour for tool pages (the tint sits on white, so it stays clean on the grey ground): each page owns one hue, shown as a solid app-icon
// tile, a soft tint behind the page's header and main tool, and its primary action.
// Full class strings (not built dynamically) so Tailwind's JIT keeps them.
export const HUES = {
  blue: {
    panel: 'bg-surface bg-[linear-gradient(rgb(var(--c-hue-blue)/0.1),rgb(var(--c-hue-blue)/0.1))] ring-1 ring-inset ring-hue-blue/10',
    tile: 'bg-hue-blue',
    text: 'text-hue-blue',
    chip: 'bg-hue-blue/[0.12] text-hue-blue',
    button: '!bg-hue-blue !text-white hover:!bg-hue-blue/90',
    border: 'border-hue-blue',
  },
  teal: {
    panel: 'bg-surface bg-[linear-gradient(rgb(var(--c-hue-teal)/0.1),rgb(var(--c-hue-teal)/0.1))] ring-1 ring-inset ring-hue-teal/10',
    tile: 'bg-hue-teal',
    text: 'text-hue-teal',
    chip: 'bg-hue-teal/[0.12] text-hue-teal',
    button: '!bg-hue-teal !text-white hover:!bg-hue-teal/90',
    border: 'border-hue-teal',
  },
  orange: {
    panel: 'bg-surface bg-[linear-gradient(rgb(var(--c-hue-orange)/0.1),rgb(var(--c-hue-orange)/0.1))] ring-1 ring-inset ring-hue-orange/10',
    tile: 'bg-hue-orange',
    text: 'text-hue-orange',
    chip: 'bg-hue-orange/[0.12] text-hue-orange',
    button: '!bg-hue-orange !text-white hover:!bg-hue-orange/90',
    border: 'border-hue-orange',
  },
  gold: {
    panel: 'bg-surface bg-[linear-gradient(rgb(var(--c-hue-gold)/0.1),rgb(var(--c-hue-gold)/0.1))] ring-1 ring-inset ring-hue-gold/10',
    tile: 'bg-hue-gold',
    text: 'text-hue-gold',
    chip: 'bg-hue-gold/[0.12] text-hue-gold',
    button: '!bg-hue-gold !text-white hover:!bg-hue-gold/90',
    border: 'border-hue-gold',
  },
  pink: {
    panel: 'bg-surface bg-[linear-gradient(rgb(var(--c-hue-pink)/0.1),rgb(var(--c-hue-pink)/0.1))] ring-1 ring-inset ring-hue-pink/10',
    tile: 'bg-hue-pink',
    text: 'text-hue-pink',
    chip: 'bg-hue-pink/[0.12] text-hue-pink',
    button: '!bg-hue-pink !text-white hover:!bg-hue-pink/90',
    border: 'border-hue-pink',
  },
}

// A solid rounded-square tile with a white glyph, like an iOS app icon.
export function AppIcon({ icon: IconCmp, hue = 'blue', size = 48 }) {
  return (
    <span
      aria-hidden="true"
      className={`inline-flex shrink-0 items-center justify-center rounded-[28%] text-white shadow-[inset_0_1px_0_rgb(255_255_255/0.22),0_1px_2px_rgb(0_0_0/0.12)] ${HUES[hue].tile}`}
      style={{ width: size, height: size }}
    >
      <IconCmp size={Math.round(size * 0.5)} />
    </span>
  )
}

// A small tinted chip with the glyph in the page hue, for list rows.
export function HueChip({ icon: IconCmp, hue = 'blue' }) {
  return (
    <span aria-hidden="true" className={`inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-[10px] ${HUES[hue].chip}`}>
      <IconCmp size={18} />
    </span>
  )
}

// The page's header and main tool on one tinted panel: icon tile, title, a line of what the
// page does, an optional fact on the right, then the tool (form, drop zone) as a white card.
export default function PageHeader({ icon, hue = 'blue', title, description, meta, children }) {
  return (
    <section aria-labelledby="page-title" className={`rounded-2xl p-5 sm:p-6 ${HUES[hue].panel}`}>
      <div className="flex items-start gap-4">
        <AppIcon icon={icon} hue={hue} />
        <div className="min-w-0 flex-1">
          <h1 id="page-title" className="font-display text-2xl font-semibold leading-tight text-ink text-balance">
            {title}
          </h1>
          {description && <p className="mt-1 max-w-prose text-sm leading-relaxed text-ink/70 text-pretty">{description}</p>}
        </div>
        {meta && <div className="hidden shrink-0 self-center sm:block">{meta}</div>}
      </div>
      {children && <div className="mt-5">{children}</div>}
    </section>
  )
}
