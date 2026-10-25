export default function FeedbackPanel({ kind = 'info', title, children, action, live = true }) {
  const role = live ? kind === 'error' ? 'alert' : ['loading', 'success'].includes(kind) ? 'status' : undefined : undefined
  return <section className={'cs-feedback cs-feedback--' + kind} role={role}>
    {title && <h3>{title}</h3>}{children && <div>{children}</div>}{action && <div className="cs-actions">{action}</div>}
  </section>
}
