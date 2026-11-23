const styles = {
  UPLOADED: 'bg-line/60 text-ash',
  PROCESSING: 'bg-ochre/10 text-ochre-deep',
  COMPLETED: 'bg-verdant/10 text-verdant',
  FAILED: 'bg-brick/10 text-brick',
}

export default function StatusBadge({ status }) {
  const style = styles[status] || 'bg-line/60 text-ash'
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${style}`}>
      {status}
    </span>
  )
}