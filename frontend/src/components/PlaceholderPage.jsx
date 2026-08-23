export default function PlaceholderPage({ title, description, phase }) {
  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="text-xl font-semibold text-slate-900">{title}</h1>
      <p className="mt-2 text-sm text-slate-500">{description}</p>
      <div className="mt-6 rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-400">
        This page is scaffolded but not yet wired to the backend.
        <br />
        Functionality lands in {phase} of the build plan.
      </div>
    </div>
  )
}
