// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { coreEnabled } from '../../shared/api/coreApi.js'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../shared/api/httpClient.js'
import AccountPage from './AccountPage.jsx'

vi.mock('../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(cleanup)
function page(url = '/account') { render(<I18nProvider><MemoryRouter initialEntries={[url]}><Routes><Route path="/account" element={<AccountPage />} />
  <Route path="/assessment" element={<h1>Student workspace</h1>} /><Route path="/enterprise" element={<h1>Enterprise workspace landing</h1>} /><Route path="/admin/assessments" element={<h1>Admin workspace landing</h1>} />
</Routes></MemoryRouter></I18nProvider>) }

it('checks the session before offering registration and lets an authenticated Student navigate to assessments', async () => {
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/auth/providers' ? { google: false } : { id: '7', role: 'STUDENT', displayName: 'Synthetic Student', consents: [] }))
  page()
  expect(screen.getByRole('status').textContent).toBe('Checking your session…')
  expect(screen.queryByRole('button', { name: 'Register' })).toBeNull()
  expect((await screen.findByRole('link', { name: 'Career Assessments' })).getAttribute('href')).toBe('/assessment')
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
  else if (coreEnabled) expect(await screen.findByText(`${role === 'ENTERPRISE' ? 'Enterprise' : 'Admin'} workspace landing`)).toBeTruthy()
  else expect(await screen.findByText('Synthetic · ' + role)).toBeTruthy()
  expect(apiRequest).toHaveBeenCalledWith('/v2/me')
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
