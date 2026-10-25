export default function StatusBadge({ tone = 'neutral', children }) {
  return <span className={'cs-badge cs-badge--' + tone}>{children}</span>
}
