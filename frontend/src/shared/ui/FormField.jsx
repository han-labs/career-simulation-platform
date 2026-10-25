import { cloneElement, useId } from 'react'

export default function FormField({ label, hint, error, required, children, className = '' }) {
  const generated = useId(), id = children.props.id ?? generated
  const description = [children.props['aria-describedby'], hint && id + '-hint', error && id + '-error'].filter(Boolean).join(' ') || undefined
  return <div className={'cs-field ' + className}>
    <div className="cs-field-label"><label htmlFor={id}>{label}</label>{required && <span aria-hidden="true"> *</span>}</div>
    {hint && <p id={id + '-hint'} className="cs-field-hint">{hint}</p>}
    {cloneElement(children, { id, required: required ?? children.props.required, 'aria-describedby': description, 'aria-invalid': error ? true : children.props['aria-invalid'], className: ['cs-control', children.props.className].filter(Boolean).join(' ') })}
    {error && <p id={id + '-error'} className="cs-field-error">{error}</p>}
  </div>
}
