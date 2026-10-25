import { useEffect, useId, useRef } from 'react'
import { X } from 'lucide-react'
import Button from './Button.jsx'
import './ui.css'

export default function Dialog({ title, description, children, footer, onRequestClose, closeLabel, initialFocusRef, restoreFocusRef, busy = false, wide = false, className = '' }) {
  const root = useRef(null), titleId = useId(), descriptionId = useId()
  useEffect(() => {
    const dialog = root.current, trigger = document.activeElement, fallback = restoreFocusRef?.current
    // Native modal supplies top-layer rendering and background inertness.
    if (typeof dialog.showModal === 'function') dialog.showModal()
    else dialog.setAttribute('open', '') // DOM-test / older-browser fallback.
    const first = initialFocusRef?.current ?? dialog.querySelector('[autofocus], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled])')
    ;(first ?? dialog).focus()
    return () => {
      if (dialog.open && typeof dialog.close === 'function') dialog.close()
      if (trigger?.isConnected && !trigger.disabled) trigger.focus()
      else if (fallback?.isConnected) fallback.focus()
    }
  }, [initialFocusRef, restoreFocusRef])
  useEffect(() => { if (busy) root.current?.focus() }, [busy])
  function close(event) { event.preventDefault(); if (!busy) onRequestClose('escape') }
  function keyboard(event) {
    if (event.key === 'Escape') { close(event); return }
    if (event.key !== 'Tab') return
    const controls = [...root.current.querySelectorAll('a[href], button, input, select, textarea, [tabindex]')]
      .filter(node => !node.disabled && node.tabIndex >= 0 && !node.closest('[hidden]') && node.type !== 'hidden')
    if (!controls.length) { event.preventDefault(); root.current.focus(); return }
    const first = controls[0], last = controls.at(-1), current = document.activeElement
    if (event.shiftKey && (current === first || current === root.current)) { event.preventDefault(); last.focus() }
    else if (!event.shiftKey && (current === last || current === root.current)) { event.preventDefault(); first.focus() }
  }
  return <dialog ref={root} tabIndex={-1} className={'cs-ui cs-dialog' + (wide ? ' cs-dialog--wide' : '') + (className ? ' ' + className : '')} aria-modal="true" aria-labelledby={titleId} aria-describedby={description ? descriptionId : undefined} aria-busy={busy || undefined} onCancel={close} onKeyDown={keyboard}>
    <header className="cs-dialog-header"><div><h2 id={titleId}>{title}</h2>{description && <p id={descriptionId}>{description}</p>}</div>
      <Button variant="quiet" disabled={busy} aria-label={closeLabel} onClick={() => onRequestClose('close')} icon={<X size={20} />} />
    </header>
    <div className="cs-dialog-body">{children}</div>
    {footer && <footer className="cs-dialog-footer">{footer}</footer>}
  </dialog>
}
