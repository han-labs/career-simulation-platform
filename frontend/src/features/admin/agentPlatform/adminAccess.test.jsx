// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../../shared/api/httpClient.js'
import AdminAccessGate from './AdminAccessGate.jsx'
vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => vi.resetAllMocks())
afterEach(cleanup)
function page() { render(<I18nProvider initialLocale="en"><MemoryRouter initialEntries={['/admin/agents']}><Routes><Route path="/admin/agents" element={<AdminAccessGate><h1>Protected Admin workspace</h1></AdminAccessGate>} /><Route path="/account" element={<h1>Real sign in</h1>} /></Routes></MemoryRouter></I18nProvider>) }
it('does not mount Admin content before the server verifies Administrator', async () => {
  apiRequest.mockResolvedValue({ role: 'ADMIN' }); page()
  expect(screen.queryByText('Protected Admin workspace')).toBeNull()
  expect(await screen.findByRole('heading', { name: 'Protected Admin workspace' })).toBeTruthy()
})
it('redirects anonymous back/refresh access to real sign in', async () => {
  apiRequest.mockRejectedValue(Object.assign(new Error('Unauthorized'), { status: 401 })); page()
  expect(await screen.findByRole('heading', { name: 'Real sign in' })).toBeTruthy()
  expect(screen.queryByText('Protected Admin workspace')).toBeNull()
})
it.each(['STUDENT','ENTERPRISE'])('denies %s without mounting Admin modules', async role => {
  apiRequest.mockResolvedValue({ role }); page()
  expect((await screen.findByRole('alert')).textContent).toBe('Administrator access is required.')
  expect(screen.queryByText('Protected Admin workspace')).toBeNull()
})
