// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from './App.jsx'
import I18nProvider from '../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../shared/api/httpClient.js'

vi.mock('../shared/api/httpClient.js', async importOriginal => ({
  ...await importOriginal(), apiRequest: vi.fn(),
}))
vi.mock('../shared/api/coreApi.js', async importOriginal => ({
  ...await importOriginal(), coreEnabled: true,
}))
vi.mock('../features/admin/agentPlatform/platformApi.js', async importOriginal => ({
  ...await importOriginal(), agentPlatformEnabled: true,
}))

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  apiRequest.mockImplementation(async path => {
    if (path === '/v2/me') return { id: '5', role: 'ADMIN', displayName: 'Synthetic Admin', consents: [] }
    if (path === '/v2/auth/providers') return { google: false }
    if (path === '/v2/admin/knowledge-bases') return []
    if (path === '/v2/admin/agent-platform/options') return { models: [] }
    if (path === '/v2/admin/assessment-types') return []
    if (path === '/v1/guidance/dashboard') return {
      recentResults: [{ id: 'attempt-1', title: 'Synthetic completed task', score: 0 }],
      plan: { title: 'Previously saved plan', nextStep: 'Review one task' },
    }
    throw new Error(`Unexpected request: ${path}`)
  })
})
afterEach(() => { cleanup(); vi.restoreAllMocks() })

function page(path, locale = 'en') {
  return render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={[path]}><App /></MemoryRouter></I18nProvider>)
}

it.each([
  ['en', 'Knowledge Base Studio', 'Account', 'Create knowledge base'],
  ['vi', 'Không gian quản lý cơ sở tri thức', 'Tài khoản', 'Tạo cơ sở tri thức'],
])('keeps the actual UC-07 route and auth entry without mixed navigation or Syn (%s)', async (locale, title, account, create) => {
  const view = page('/admin/knowledge', locale)
  await screen.findByText(title)
  await waitFor(() => expect(screen.getAllByRole('button', { name: create })[0].disabled).toBe(false))
  expect(screen.getByRole('link', { name: account }).getAttribute('href')).toBe('/account')
  const links = [...view.container.querySelectorAll('a')].map(link => link.getAttribute('href'))
  expect(links).not.toContain('/assessment')
  for (const path of ['/admin/agents', '/admin/ai-usage', '/admin/assessments', '/agents', '/simulations', '/dashboard']) {
    expect(links).not.toContain(path)
  }
  expect(view.container.querySelector('.site-footer')).toBeNull()
  expect(view.container.querySelector('.institutional-bar__tagline')).toBeNull()
  expect(view.container.querySelector('.syn-launcher, .syn-widget')).toBeNull()
  expect(apiRequest.mock.calls.every(([path]) => !path.includes('/consents') && !path.includes('/guidance'))).toBe(true)
})

it('uses the existing Account route for sign-out and retains its pre-base consent UI', async () => {
  const view = page('/admin/knowledge')
  fireEvent.click(screen.getByRole('link', { name: 'Account' }))
  await screen.findByRole('button', { name: 'Sign out' })
  expect(screen.getByRole('heading', { name: 'Purpose-specific consent' })).toBeTruthy()
  expect(view.container.querySelector('.site-footer')).not.toBeNull()
  expect(view.container.querySelector('.syn-launcher, .syn-widget')).toBeNull()
})

it('keeps the pre-base Assessment Administration route and normal navigation available elsewhere', async () => {
  const view = page('/admin/assessments')
  await screen.findByRole('heading', { name: 'Assessment administration' })
  expect(within(view.container.querySelector('.site-header')).getByRole('link', { name: 'Career Assessments' })).toBeTruthy()
  expect(view.container.querySelector('.site-footer')).not.toBeNull()
  expect(view.container.querySelector('.syn-launcher, .syn-widget')).toBeNull()
})

it('preserves a denied KB response and does not enable writes or manufacture consent', async () => {
  apiRequest.mockImplementation(async path => {
    if (path === '/v2/me') return { id: '7', role: 'STUDENT' }
    if (path === '/v2/admin/knowledge-bases') throw new Error('Access or CSRF validation denied.')
    return { models: [] }
  })
  page('/admin/knowledge')
  await screen.findByText('Access or CSRF validation denied.')
  expect(screen.getByRole('button', { name: 'Create knowledge base' }).disabled).toBe(true)
  expect(apiRequest.mock.calls.every(([, options]) => !options?.method)).toBe(true)
})

it('retains legacy dashboard evidence and saved plans without dangling popup-launch actions', async () => {
  const view = page('/guidance')
  await screen.findByText('Synthetic completed task')
  expect(screen.getByText('Previously saved plan')).toBeTruthy()
  expect(view.container.querySelector('.simulation-history__score').textContent).toBe('0/100')
  expect(screen.queryByRole('button', { name: 'View with Syn' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Draft with Syn' })).toBeNull()
  expect(view.container.querySelector('.syn-launcher, .syn-widget')).toBeNull()
})
