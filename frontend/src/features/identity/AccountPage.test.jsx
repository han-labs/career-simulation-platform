// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import * as coreApi from '../../shared/api/coreApi.js'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../shared/api/httpClient.js'
import AccountPage from './AccountPage.jsx'

vi.mock('../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(cleanup)
function LocationEvidence() { return <span aria-label="Current route">{useLocation().pathname}</span> }
function page(url = '/account') { render(<I18nProvider><MemoryRouter initialEntries={[url]}><Routes><Route path="/account" element={<AccountPage />} />
  <Route path="/assessment" element={<h1>Student workspace</h1>} /><Route path="/enterprise" element={<h1>Enterprise workspace landing</h1>} /><Route path="/admin" element={<h1>Admin workspace landing</h1>} />
</Routes><LocationEvidence /></MemoryRouter></I18nProvider>) }

it('checks the session before offering registration and redirects an authenticated Student to assessments', async () => {
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/auth/providers' ? { google: false } : { id: '7', role: 'STUDENT', displayName: 'Synthetic Student', consents: [] }))
  page()
  expect(screen.getByRole('status').textContent).toBe('Checking your session…')
  expect(screen.queryByRole('button', { name: 'Register' })).toBeNull()
  expect(await screen.findByRole('heading', { name: 'Student workspace' })).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Register' })).toBeNull()
})

it.each([
  ['cancelled', 'Google sign-in was cancelled.'],
  ['provider-unavailable', 'Sign-in is temporarily unavailable.'],
  ['account-disabled', 'You do not have permission'],
  ['link-required', 'explicitly link Google.'],
  ['untrusted-provider-details', 'Your sign-in could not be verified.'],
])('explains the safe OAuth return %s without creating a session', async (code, message) => {
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? Promise.resolve({ google: true }) : Promise.reject(Object.assign(new Error('Unauthorized'), { status: 401 })))
  page(`/account?oauthError=${code}`)
  expect(screen.getByRole('alert').textContent).toContain(message)
  expect(await screen.findByRole('link', { name: 'Continue with Google' })).toBeTruthy()
  expect(screen.queryByText('Student workspace')).toBeNull()
})

it.each(['STUDENT', 'ENTERPRISE', 'ADMIN'])('restores the real account before Google role landing for %s', async role => {
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/auth/providers' ? { google: true } : { id: '71', role, displayName: 'Synthetic', consents: [] }))
  page('/account?oauth=success&role=ADMIN')
  expect(screen.getByRole('status').textContent).toBe('Restoring your Google sign-in session…')
  if (role === 'STUDENT') expect(await screen.findByText('Student workspace')).toBeTruthy()
  else if (role === 'ADMIN' || coreApi.coreEnabled) expect(await screen.findByText(`${role === 'ENTERPRISE' ? 'Enterprise' : 'Admin'} workspace landing`)).toBeTruthy()
  else expect(await screen.findByText('Synthetic · ' + role)).toBeTruthy()
  expect(apiRequest).toHaveBeenCalledWith('/v2/me')
})

it('shows only authentication controls on the anonymous account entry', async () => {
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? Promise.resolve({ google: false }) : Promise.reject(Object.assign(new Error('Unauthorized'), { status: 401 })))
  page()
  expect(await screen.findByRole('button', { name: 'Register', exact: true })).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'Account access' })).toBeTruthy()
  for (const text of ['Account and privacy', 'Purpose-specific consent', 'Assessment administration', 'Knowledge Base Studio', 'Agent Builder', 'My assigned Agents']) expect(screen.queryByText(text)).toBeNull()
  expect(screen.queryByRole('checkbox')).toBeNull()
  expect(apiRequest.mock.calls.map(([path]) => path).sort()).toEqual(['/v2/auth/providers', '/v2/me'])
})

it.each(['STUDENT', 'ENTERPRISE', 'ADMIN'])('redirects password authentication by the server role %s and ignores query destinations', async role => {
  apiRequest.mockImplementation(path => {
    if (path === '/v2/auth/providers') return Promise.resolve({ google: false })
    if (path === '/v2/auth/login') return Promise.resolve({ id: '72', role, displayName: 'Synthetic' })
    return Promise.reject(Object.assign(new Error('Unauthorized'), { status: 401 }))
  })
  page('/account?role=ADMIN&redirect=https://untrusted.example&returnTo=/admin/assessments')
  await screen.findByRole('button', { name: 'Register', exact: true })
  fireEvent.change(screen.getByLabelText('Email *'), { target: { value: 'synthetic@example.test' } })
  fireEvent.change(screen.getByLabelText('Password *'), { target: { value: 'synthetic-password-only' } })
  fireEvent.submit(screen.getByLabelText('Email *').closest('form'))
  const expected = role === 'STUDENT' ? '/assessment' : role === 'ADMIN' ? '/admin' : coreApi.coreEnabled ? '/enterprise' : '/account'
  await waitFor(() => expect(screen.getByLabelText('Current route').textContent).toBe(expected))
  expect(apiRequest).toHaveBeenCalledWith('/v2/auth/login', expect.objectContaining({ method: 'POST', body: JSON.stringify({ email: 'synthetic@example.test', password: 'synthetic-password-only' }) }))
})

it('supports Enterprise role landing when the merged Core application is enabled', async () => {
  vi.spyOn(coreApi, 'coreEnabled', 'get').mockReturnValue(true)
  try {
    apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/auth/providers' ? { google: false } : { id: '73', role: 'ENTERPRISE' }))
    page('/account?oauth=success&role=ADMIN')
    expect(await screen.findByRole('heading', { name: 'Enterprise workspace landing' })).toBeTruthy()
    expect(screen.getByLabelText('Current route').textContent).toBe('/enterprise')
  } finally { vi.restoreAllMocks() }
})

it('does not grant a destination to an unsupported role and permits safe logout', async () => {
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/auth/providers' ? { google: false } : path === '/v2/auth/logout' ? {} : { id: '74', role: 'UNSUPPORTED' }))
  page('/account?oauth=success&role=ADMIN')
  expect((await screen.findByRole('alert')).textContent).toContain('You do not have permission')
  expect(screen.getByLabelText('Current route').textContent).toBe('/account')
  fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))
  expect(await screen.findByRole('button', { name: 'Register', exact: true })).toBeTruthy()
  expect(apiRequest).toHaveBeenCalledWith('/v2/auth/logout', expect.objectContaining({ method: 'POST' }))
})

it('explains an OAuth email collision without implicit linking, consent or mixed navigation', async () => {
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? Promise.resolve({ google: true }) : Promise.reject(Object.assign(new Error('Unauthorized'), { status: 401 })))
  page('/account?oauthError=link-required')
  expect(await screen.findByText('Sign in to your existing account, then explicitly link Google.')).toBeTruthy()
  expect(await screen.findByRole('link', { name: 'Continue with Google' })).toBeTruthy()
  expect(screen.getByLabelText('Email *')).toBeTruthy()
  expect(screen.queryByRole('checkbox')).toBeNull()
  expect(document.querySelector('a[href="/admin/assessments"], a[href="/admin/knowledge"], a[href="/admin/agents"], a[href="/agents"]')).toBeNull()
  expect(apiRequest.mock.calls.every(([, options]) => !options?.method)).toBe(true)
})

it('does not trust a successful callback without an authenticated session and lets the user sign in again', async () => {
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? Promise.resolve({ google: true }) : Promise.reject(Object.assign(new Error('Unauthorized'), { status: 401 })))
  page('/account?oauth=success')
  expect((await screen.findByRole('alert')).textContent).toContain('could not be verified')
  expect(screen.queryByText('Student workspace')).toBeNull()
  fireEvent.click(screen.getByRole('link', { name: 'Sign in', exact: true }))
  expect(await screen.findByRole('button', { name: 'Register' })).toBeTruthy()
})

it('offers provider availability retry instead of silently treating an outage as missing configuration', async () => {
  let providers = 0
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? (++providers === 1 ? Promise.reject(new Error('Offline')) : Promise.resolve({ google: true })) : Promise.reject(Object.assign(new Error('Unauthorized'), { status: 401 })))
  page()
  expect(await screen.findByText(/Google availability could not be checked/)).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Continue with Google' }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  expect(await screen.findByRole('link', { name: 'Continue with Google' })).toBeTruthy()
})

it('offers retry after a session lookup outage and then recovers to anonymous registration', async () => {
  let attempts = 0
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? Promise.resolve({ google: false }) : Promise.reject(++attempts === 1 ? new Error('Network unavailable') : Object.assign(new Error('Sign in is required.'), { status: 401 })))
  page()
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Register' })).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  expect(await screen.findByRole('button', { name: 'Register' })).toBeTruthy()
  expect(screen.queryByRole('alert')).toBeNull()
})
