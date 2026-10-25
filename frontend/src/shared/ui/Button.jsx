export default function Button({ variant = 'secondary', busy = false, disabled, className = '', children, icon, type = 'button', ...props }) {
  return <button {...props} type={type} disabled={disabled || busy} aria-busy={busy || undefined} className={'cs-button cs-button--' + variant + ' ' + className}>
    {icon && <span aria-hidden="true" className="cs-button-icon">{icon}</span>}<span>{children}</span>
  </button>
}
