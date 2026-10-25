// @vitest-environment jsdom
import { afterEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Link, useNavigate } from 'react-router-dom'
import NavigationScroll from './NavigationScroll.jsx'

afterEach(() => { cleanup(); vi.restoreAllMocks() })
function Fixture() {
  const navigate = useNavigate()
  return <><NavigationScroll /><main id="main-content" tabIndex={-1}>
    <Link to="/assessment">Assessment</Link><Link to="/simulations?track=web">Catalogue</Link>
    <Link to="/simulations?track=data">Filter</Link><Link to="/#details">Anchor</Link>
    <button onClick={() => navigate(-1)}>Back</button><button onClick={() => navigate(1)}>Forward</button>
    <input aria-label="Answer" /><h2 id="details">Details</h2>
  </main></>
}
it('resets committed routes/search/Back/Forward, preserves anchors and ordinary edits, restores browser policy', () => {
  const scroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  const anchor = vi.fn()
  Element.prototype.scrollIntoView = anchor
  window.history.scrollRestoration = 'auto'
  const mounted = render(<MemoryRouter><Fixture /></MemoryRouter>)
  expect(window.history.scrollRestoration).toBe('manual')
  expect(scroll).toHaveBeenLastCalledWith({ top: 0, left: 0, behavior: 'instant' })
  expect(document.activeElement.id).toBe('main-content')
  for (const name of ['Assessment', 'Catalogue', 'Filter', 'Back', 'Forward']) {
    const count = scroll.mock.calls.length
    fireEvent.click(screen.getByRole(name === 'Back' || name === 'Forward' ? 'button' : 'link', { name }))
    expect(scroll).toHaveBeenCalledTimes(count + 1)
  }
  const count = scroll.mock.calls.length
  fireEvent.change(screen.getByRole('textbox'), { target: { value: 'My answer' } })
  expect(scroll).toHaveBeenCalledTimes(count)
  fireEvent.click(screen.getByRole('link', { name: 'Anchor' }))
  expect(anchor).toHaveBeenCalledWith({ behavior: 'instant', block: 'start' })
  expect(scroll).toHaveBeenCalledTimes(count)
  mounted.unmount()
  expect(window.history.scrollRestoration).toBe('auto')
})
