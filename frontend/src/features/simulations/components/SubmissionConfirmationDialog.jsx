// Confirms the irreversible simulation submission with accessible modal behavior.
import { useEffect, useRef } from 'react'

const focusableSelector = [
  'button:not([disabled])',
  '[href]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
].join(',')

function SubmissionConfirmationDialog({ open, onConfirm, onCancel }) {
  const dialogRef = useRef(null)
  const cancelButtonRef = useRef(null)
  const previousFocusRef = useRef(null)

  useEffect(() => {
    if (!open) {
      previousFocusRef.current?.focus()
      previousFocusRef.current = null
      return undefined
    }

    previousFocusRef.current = document.activeElement
    cancelButtonRef.current?.focus()

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onCancel()
        return
      }

      if (event.key !== 'Tab') return
      const focusableElements = dialogRef.current?.querySelectorAll(focusableSelector)
      if (!focusableElements?.length) return

      const firstElement = focusableElements[0]
      const lastElement = focusableElements[focusableElements.length - 1]
      if (event.shiftKey && document.activeElement === firstElement) {
        event.preventDefault()
        lastElement.focus()
      } else if (!event.shiftKey && document.activeElement === lastElement) {
        event.preventDefault()
        firstElement.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, onCancel])

  if (!open) return null

  return (
    <div
      className="dialog-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) onCancel()
      }}
    >
      <section
        ref={dialogRef}
        className="confirmation-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="submission-dialog-title"
      >
        <h2 id="submission-dialog-title">Submit simulation?</h2>
        <p>
          After submitting, you cannot change your answers. Your result will be shown immediately.
        </p>
        <div className="confirmation-dialog__actions">
          <button className="button button--secondary" type="button" ref={cancelButtonRef} onClick={onCancel}>
            Cancel
          </button>
          <button className="button" type="button" onClick={onConfirm}>
            Confirm submit
          </button>
        </div>
      </section>
    </div>
  )
}

export default SubmissionConfirmationDialog
