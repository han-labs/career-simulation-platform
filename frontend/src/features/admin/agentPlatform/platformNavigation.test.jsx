// @vitest-environment jsdom
import { afterEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import PlatformNav from './PlatformNav.jsx'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../../shared/api/httpClient.js'

vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
afterEach(() => { cleanup(); vi.resetAllMocks(); vi.unstubAllGlobals() })
function Location() { const location = useLocation(); return <output aria-label="Current selection">{location.pathname + location.search}</output> }
function page() {
  return render(<I18nProvider initialLocale="en"><MemoryRouter initialEntries={['/admin/knowledge?kb=kb-a']}>
    <PlatformNav /><Location />
  </MemoryRouter></I18nProvider>)
}
function viewport(mobile) {
  const listeners = new Set(), media = { matches: mobile, addEventListener: (_, listener) => listeners.add(listener), removeEventListener: (_, listener) => listeners.delete(listener) }
  vi.stubGlobal('matchMedia', () => media)
  return value => act(() => { media.matches = value; listeners.forEach(listener => listener()) })
}

it('starts expanded and keeps exactly the three product links accessible when collapsed', () => {
  viewport(false); page()
  const collapse = screen.getByRole('button', { name: 'Collapse Admin navigation' })
  expect(collapse.getAttribute('aria-expanded')).toBe('true')
  fireEvent.click(collapse)
  expect(screen.getByRole('button', { name: 'Expand Admin navigation' }).getAttribute('aria-expanded')).toBe('false')
  expect(screen.getAllByRole('link')).toHaveLength(3)
  expect(screen.getByRole('link', { name: 'Knowledge Bases' }).getAttribute('aria-current')).toBe('page')
  expect(screen.getByRole('link', { name: 'AI Agents' }).getAttribute('href')).toBe('/admin/agents')
  expect(screen.getByRole('link', { name: 'AI Usage' }).getAttribute('href')).toBe('/admin/usage')
  expect(screen.queryByRole('button', { name: 'Sign out' })).toBeNull()
  expect(screen.queryByRole('link', { name: 'Sign in' })).toBeNull()
  expect(screen.queryByRole('link', { name: 'Account' })).toBeNull()
})

it('closes the mobile drawer with Escape, restores focus and navigates without writes', () => {
  viewport(true); page()
  const trigger = screen.getByRole('button', { name: 'Open Admin navigation' })
  trigger.focus(); fireEvent.click(trigger)
  let dialog = screen.getByRole('dialog', { name: 'Admin Studio' })
  expect(dialog.contains(document.activeElement)).toBe(true)
  fireEvent.keyDown(dialog, { key: 'Escape' })
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(document.activeElement).toBe(trigger)
  fireEvent.click(trigger)
  dialog = screen.getByRole('dialog')
  fireEvent.click(within(dialog).getByRole('link', { name: 'AI Agents' }))
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(screen.getByLabelText('Current selection').textContent).toBe('/admin/agents')
  fireEvent.click(trigger)
  expect(within(screen.getByRole('dialog')).getByRole('link', { name: 'AI Agents' }).getAttribute('aria-current')).toBe('page')
  expect(apiRequest).not.toHaveBeenCalled()
})

it('releases the mobile modal when resized to desktop and does not reopen it on return', () => {
  const resize = viewport(true); page()
  fireEvent.click(screen.getByRole('button', { name: 'Open Admin navigation' }))
  expect(screen.getByRole('dialog')).toBeTruthy()
  resize(false)
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(screen.getByRole('button', { name: 'Collapse Admin navigation' })).toBeTruthy()
  resize(true)
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(screen.getByRole('button', { name: 'Open Admin navigation' }).getAttribute('aria-expanded')).toBe('false')
})

it('keeps authentication controls out of Admin navigation without making API calls', () => {
  viewport(false)
  render(<I18nProvider initialLocale="en"><MemoryRouter><PlatformNav /></MemoryRouter></I18nProvider>)
  expect(screen.getAllByRole('link')).toHaveLength(3)
  expect(screen.getByRole('link', { name: 'Knowledge Bases' })).toBeTruthy()
  expect(screen.queryByText(/Sign in|Sign out|Account/)).toBeNull()
  expect(apiRequest).not.toHaveBeenCalled()
})
