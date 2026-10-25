// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { useRef, useState } from 'react'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import Button from './Button.jsx'
import Dialog from './Dialog.jsx'
import FormField from './FormField.jsx'
import Tabs from './Tabs.jsx'
import FeedbackPanel from './FeedbackPanel.jsx'

beforeEach(() => {
  // DOM double only; actual Chrome supplies top-layer modality and inertness.
  HTMLDialogElement.prototype.showModal = function () { this.setAttribute('open', '') }
  HTMLDialogElement.prototype.close = function () { this.removeAttribute('open') }
})
afterEach(cleanup)

it('prevents duplicate dispatch while busy/disabled and does not implicitly submit forms', () => {
  const click = vi.fn(), submit = vi.fn()
  const view = render(<form onSubmit={submit}><Button busy onClick={click}>Save changes</Button></form>)
  const button = screen.getByRole('button', { name: 'Save changes' })
  expect(button.disabled).toBe(true)
  expect(button.getAttribute('aria-busy')).toBe('true')
  fireEvent.click(button); expect(click).not.toHaveBeenCalled()
  view.rerender(<form onSubmit={submit}><Button onClick={click}>Save changes</Button></form>)
  fireEvent.click(button); expect(click).toHaveBeenCalledTimes(1)
  expect(submit).not.toHaveBeenCalled()
})

it('associates labels, existing descriptions, hint/error and required state using unique IDs', () => {
  render(<><p id="policy">Policy</p><FormField label="Name" required hint="180 characters" error="Enter a name"><input aria-describedby="policy" /></FormField><FormField label="Other name"><input /></FormField></>)
  const field = screen.getByLabelText('Name')
  expect(field.required).toBe(true)
  expect(field.getAttribute('aria-invalid')).toBe('true')
  const ids = field.getAttribute('aria-describedby').split(' ')
  expect(ids.map(id => document.getElementById(id)?.textContent)).toEqual(['Policy', '180 characters', 'Enter a name'])
  expect(screen.getByLabelText('Other name').id).not.toBe(field.id)
})

function ModalExample({ busy = false }) {
  const [open, setOpen] = useState(false), first = useRef(null)
  return <><Button onClick={() => setOpen(true)}>Edit</Button>{open && <Dialog title="Edit name" description="Closing retains the draft." closeLabel="Close dialog" initialFocusRef={first} busy={busy} onRequestClose={() => setOpen(false)} footer={<Button disabled={busy}>Last action</Button>}><FormField label="Name"><input ref={first} disabled={busy} /></FormField></Dialog>}</>
}
it('enters the labelled dialog, wraps keyboard focus, preserves it while busy and restores the trigger', () => {
  const view = render(<ModalExample />)
  const trigger = screen.getByRole('button', { name: 'Edit' })
  trigger.focus(); fireEvent.click(trigger)
  expect(document.activeElement).toBe(screen.getByLabelText('Name'))
  const dialog = screen.getByRole('dialog', { name: 'Edit name' })
  expect(dialog.getAttribute('aria-describedby')).toBeTruthy()
  const last = screen.getByRole('button', { name: 'Last action' })
  last.focus(); fireEvent.keyDown(last, { key: 'Tab' })
  expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Close dialog' }))
  fireEvent.keyDown(document.activeElement, { key: 'Tab', shiftKey: true })
  expect(document.activeElement).toBe(last)
  view.rerender(<ModalExample busy />)
  expect(document.activeElement).toBe(dialog)
  fireEvent.keyDown(dialog, { key: 'Escape' }); expect(screen.getByRole('dialog')).toBeTruthy()
  view.rerender(<ModalExample />)
  fireEvent.keyDown(dialog, { key: 'Escape' })
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(document.activeElement).toBe(trigger)
})

it('moves tab focus with arrows/Home/End but activates only on Enter/Space or click', () => {
  const change = vi.fn(), items = [{ value: 'a', label: 'Documents', content: 'Files' }, { value: 'b', label: 'Test KB', content: 'Console' }]
  const view = render(<Tabs label="Workspace" value="a" onChange={change} items={items} />)
  const first = screen.getByRole('tab', { name: 'Documents' }), second = screen.getByRole('tab', { name: 'Test KB' })
  first.focus(); fireEvent.keyDown(first, { key: 'ArrowRight' })
  expect(document.activeElement).toBe(second); expect(change).not.toHaveBeenCalled()
  fireEvent.keyDown(second, { key: 'Home' }); expect(document.activeElement).toBe(first)
  fireEvent.keyDown(first, { key: 'End' }); expect(document.activeElement).toBe(second)
  fireEvent.keyDown(second, { key: 'Enter' }); expect(change).toHaveBeenLastCalledWith('b')
  view.rerender(<Tabs label="Workspace" value="b" onChange={change} items={items} />)
  expect(second.getAttribute('aria-selected')).toBe('true')
  expect(screen.getByRole('tabpanel').getAttribute('aria-labelledby')).toBe(second.id)
})

it('keeps errors persistent and progress polite without announcing static metadata', () => {
  render(<><FeedbackPanel kind="error">Revision conflict</FeedbackPanel><FeedbackPanel kind="loading">Loading</FeedbackPanel><FeedbackPanel kind="error" live={false}>Stored error</FeedbackPanel></>)
  expect(screen.getByRole('alert').textContent).toContain('Revision conflict')
  expect(screen.getByRole('status').textContent).toContain('Loading')
  expect(screen.getByText('Stored error').closest('section').getAttribute('role')).toBeNull()
})

function RemovedTriggerExample() {
  const [open, setOpen] = useState(false), [used, setUsed] = useState(false), fallback = useRef(null)
  return <><h1 ref={fallback} tabIndex={-1}>Workspace</h1><Button disabled={used} onClick={() => { setUsed(true); setOpen(true) }}>Publish</Button>{open && <Dialog title="Confirm" closeLabel="Cancel" restoreFocusRef={fallback} onRequestClose={() => setOpen(false)}>Review target</Dialog>}</>
}
it('restores a logical fallback when the initiating action becomes disabled', () => {
  render(<RemovedTriggerExample />)
  const trigger = screen.getByRole('button', { name: 'Publish' })
  trigger.focus(); fireEvent.click(trigger)
  fireEvent.click(screen.getByRole('dialog'))
  expect(screen.getByRole('dialog')).toBeTruthy()
  fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })
  expect(document.activeElement).toBe(screen.getByRole('heading', { name: 'Workspace' }))
})
