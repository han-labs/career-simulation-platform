// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { useSyncExternalStore } from 'react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import I18nProvider from '../i18n/I18nProvider.jsx'
import AppShell from './AppShell.jsx'
import { apiRequest, getAccountGeneration, invalidateAccount, subscribeAccount } from '../api/httpClient.js'

vi.mock('../api/httpClient.js', async original => ({ ...await original(), apiRequest: vi.fn() }))
beforeEach(() => { vi.clearAllMocks(); localStorage.clear() })
afterEach(cleanup)
function Shell() {
  const generation = useSyncExternalStore(subscribeAccount, getAccountGeneration)
  return <AppShell key={generation} />
}
function Destination() { return <h1>{useLocation().pathname}</h1> }
function page(path = '/assessment') {
  return render(<I18nProvider><MemoryRouter initialEntries={[path]}><Routes><Route element={<Shell />}>
    {['/', '/assessment', '/simulations', '/simulations/demo', '/simulations/demo/results', '/guidance', '/enterprise', '/admin', '/account'].map(route => <Route key={route} path={route} element={<Destination />} />)}
  </Route></Routes></MemoryRouter></I18nProvider>)
}
it.each(['STUDENT', 'ENTERPRISE', 'ADMIN'])('uses the server role for navigation and offers shared logout for %s', async role => {
  apiRequest.mockResolvedValue({ id: '7', role })
  page()
  await screen.findByRole('button', { name: 'Sign out' })
  expect(Boolean(screen.queryByRole('link', { name: 'Admin Studio' }))).toBe(role === 'ADMIN')
  for (const name of ['Home', 'Career Assessments', 'IT Career Tracks', 'Guidance']) expect(within(document.querySelector('header')).getByRole('link', { name })).toBeTruthy()
  expect(document.querySelector('a[href="/admin/assessments"], .syn-widget, .syn-launcher')).toBeNull()
})
it('never exposes privileged navigation while anonymous or when session lookup fails', async () => {
  apiRequest.mockRejectedValue(Object.assign(new Error('Sign in is required.'), { status: 401 }))
  page()
  await waitFor(() => expect(apiRequest).toHaveBeenCalledTimes(1))
  expect(screen.queryByRole('link', { name: 'Admin Studio' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Sign out' })).toBeNull()
})
it.each(['/assessment', '/enterprise', '/admin'])('logs out through the existing API, isolates account state and returns to /account from %s', async path => {
  let signedIn = true
  apiRequest.mockImplementation(async (url, options) => {
    if (url === '/v2/auth/logout') {
      expect(options.method).toBe('POST')
      signedIn = false; invalidateAccount(); return { loggedOut: true }
    }
    if (!signedIn) throw Object.assign(new Error('Unauthorized'), { status: 401 })
    return { id: '7', role: path === '/admin' ? 'ADMIN' : path === '/enterprise' ? 'ENTERPRISE' : 'STUDENT' }
  })
  const before = getAccountGeneration()
  page(path)
  fireEvent.click(await screen.findByRole('button', { name: 'Sign out' }))
  await screen.findByRole('heading', { name: '/account' })
  expect(getAccountGeneration()).toBeGreaterThan(before)
  expect(screen.queryByRole('button', { name: 'Sign out' })).toBeNull()
  expect(apiRequest).toHaveBeenCalledWith('/v2/auth/logout', expect.objectContaining({ method: 'POST', body: '{}' }))
})
it('blocks duplicate logout and preserves the session on failure so retry remains possible', async () => {
  let fail
  apiRequest.mockImplementation(url => url === '/v2/auth/logout' ? new Promise((_, reject) => { fail = reject }) : Promise.resolve({ id: '7', role: 'STUDENT' }))
  page()
  const button = await screen.findByRole('button', { name: 'Sign out' })
  fireEvent.click(button); fireEvent.click(button)
  expect(apiRequest.mock.calls.filter(([url]) => url === '/v2/auth/logout')).toHaveLength(1)
  expect(button.disabled).toBe(true)
  fail(new Error('The platform is not reachable. Please try again shortly.'))
  await screen.findByRole('alert')
  expect(button.disabled).toBe(false)
  expect(screen.getByRole('heading', { name: '/assessment' })).toBeTruthy()
})
it('preserves simulation catalogue/detail/results navigation without privileged links', async () => {
  apiRequest.mockResolvedValue({ id: '7', role: 'STUDENT' })
  page('/simulations/demo/results')
  await screen.findByRole('button', { name: 'Sign out' })
  fireEvent.click(screen.getByRole('link', { name: 'IT Career Tracks' }))
  expect(await screen.findByRole('heading', { name: '/simulations' })).toBeTruthy()
  expect(screen.queryByRole('link', { name: 'Admin Studio' })).toBeNull()
})
