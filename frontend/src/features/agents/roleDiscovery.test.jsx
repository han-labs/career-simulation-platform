// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../shared/api/httpClient.js'
import AssignedAgentsPage from './AssignedAgentsPage.jsx'
vi.mock('../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => vi.resetAllMocks())
afterEach(cleanup)
function page() { render(<I18nProvider initialLocale="en"><MemoryRouter><AssignedAgentsPage /></MemoryRouter></I18nProvider>) }
it('opens only the role-resolved published Student Agent without assignment requests', async () => {
  apiRequest.mockResolvedValue([{ id: 'role-active', name: 'Published role Agent', purpose: 'Safe purpose', runtimeAvailable: true }])
  page()
  expect((await screen.findByRole('link', { name: 'Published role Agent' })).getAttribute('href')).toBe('/agents/role-active')
  expect(apiRequest.mock.calls.map(([path]) => path)).toEqual(['/v2/agents'])
})
it('shows Enterprise role discovery honestly without linking to Student runtime', async () => {
  apiRequest.mockResolvedValue([{ id: 'enterprise-active', name: 'Enterprise Agent', purpose: 'Safe purpose', runtimeAvailable: false }])
  page()
  await screen.findByRole('heading', { name: 'Enterprise Agent' })
  expect(screen.queryByRole('link')).toBeNull()
  expect(screen.getByRole('status').textContent).toContain('does not grant Student-private tools')
})
