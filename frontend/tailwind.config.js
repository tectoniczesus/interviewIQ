/** @type {import('tailwindcss').Config} */

// Colours and faces read CSS variables so a page can re-theme its own ground
// (`.theme-dash` on the Dashboard). Defaults live in `:root` in src/index.css.
const token = (name) => `rgb(var(--c-${name}) / <alpha-value>)`

export default {
  content: [
    './index.html',
    './src/**/*.{js,jsx}',
  ],
  theme: {
    extend: {
      colors: {
        paper: token('paper'),
        surface: token('surface'),
        ink: token('ink'),
        ash: token('ash'),
        line: token('line'),
        accent: token('accent'),
        ochre: token('ochre'),
        'ochre-deep': token('ochre-deep'),
        verdant: token('verdant'),
        brick: token('brick'),
        // Tones for text sitting on an ink slab.
        'accent-on-ink': token('accent-on-ink'),
        'verdant-on-ink': token('verdant-on-ink'),
        'ochre-on-ink': token('ochre-on-ink'),
        'brick-on-ink': token('brick-on-ink'),
        // Page hues (index.css): one per tool page.
        'hue-blue': token('hue-blue'),
        'hue-teal': token('hue-teal'),
        'hue-orange': token('hue-orange'),
        'hue-gold': token('hue-gold'),
        'hue-pink': token('hue-pink'),
        // Sidebar ground and text (ink and paper by default).
        chrome: token('chrome'),
        'chrome-ink': token('chrome-ink'),
        'chrome-muted': token('chrome-muted'),
      },
      boxShadow: {
        card: '0 1px 2px rgb(28 36 48 / 0.04), 0 1px 3px rgb(28 36 48 / 0.06)',
        'card-hover': '0 2px 4px rgb(28 36 48 / 0.05), 0 6px 16px rgb(28 36 48 / 0.08)',
      },
      fontFamily: {
        display: ['var(--font-display)', '"Space Grotesk"', 'system-ui', 'sans-serif'],
        sans: ['var(--font-text)', '"IBM Plex Sans"', 'system-ui', 'sans-serif'],
        mono: ['var(--font-mono)', '"IBM Plex Mono"', 'ui-monospace', 'monospace'],
      },
    },
  },
  plugins: [],
}
