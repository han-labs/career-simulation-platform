// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../shared/api/httpClient.js'
import AccountPage from './AccountPage.jsx'

vi.mock('../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(cleanup)
function page() { render(<I18nProvider><MemoryRouter><AccountPage /></MemoryRouter></I18nProvider>) }

it('checks the session before offering registration and lets an authenticated Student navigate to assessments', async () => {
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/auth/providers' ? { google: false } : { id: '7', role: 'STUDENT', displayName: 'Synthetic Student', consents: [] }))
  page()
  expect(screen.getByRole('status').textContent).toBe('Checking your session…')
  expect(screen.queryByRole('button', { name: 'Register' })).toBeNull()
  expect((await screen.findByRole('link', { name: 'Career Assessments' })).getAttribute('href')).toBe('/assessment')
  expect(screen.queryByRole('button', { name: 'Register' })).toBeNull()
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
