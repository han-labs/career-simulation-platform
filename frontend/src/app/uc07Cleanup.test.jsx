// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom'
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
    if (path === '/v2/auth/logout') return { loggedOut: true }
    if (path === '/v2/admin/knowledge-bases') return []
    if (path === '/v2/admin/agents') return []
    if (path.startsWith('/v2/admin/agent-usage?')) return { groups: [], nativeTools: [] }
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

function LocationEvidence() {
  const location = useLocation(), navigate = useNavigate()
  return <><output aria-label="Current route">{location.pathname + location.search + location.hash}</output><button onClick={() => navigate(-1)}>Browser back</button></>
}
function page(path, locale = 'en', previous = []) {
  return render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={[...previous, path]}><App /><LocationEvidence /></MemoryRouter></I18nProvider>)
}

it.each([
  ['en', 'Knowledge Base Studio', 'Create knowledge base'],
  ['vi', 'Không gian quản lý cơ sở tri thức', 'Tạo cơ sở tri thức'],
])('keeps UC-07 accessible to an authorized actor without authentication UI or mixed navigation (%s)', async (locale, title, create) => {
  const view = page('/admin/knowledge', locale)
  await screen.findByText(title)
  await waitFor(() => expect(screen.getAllByRole('button', { name: create })[0].disabled).toBe(false))
  expect(screen.queryByRole('button', { name: /^(Sign in|Sign out|Đăng nhập|Đăng xuất)$/ })).toBeNull()
  const links = [...view.container.querySelectorAll('a')].map(link => link.getAttribute('href'))
  expect(links).not.toContain('/assessment')
  expect(links).not.toContain('/account')
  for (const path of ['/admin/ai-usage', '/admin/assessments', '/agents', '/simulations', '/dashboard']) {
    expect(links).not.toContain(path)
  }
  expect(links).toContain('/admin/agents')
  expect(links).toContain('/admin/usage')
  expect(view.container.querySelector('.site-footer')).toBeNull()
  expect(view.container.querySelector('.institutional-bar__tagline')).toBeNull()
  expect(view.container.querySelector('.syn-launcher, .syn-widget')).toBeNull()
  expect(apiRequest.mock.calls.every(([path]) => !path.includes('/consents') && !path.includes('/guidance'))).toBe(true)
})

it.each([
  ['/admin', 'Knowledge bases', 'Knowledge Bases', '/admin/knowledge'],
  ['/admin/knowledge', 'Knowledge bases', 'Knowledge Bases', '/admin/knowledge'],
  ['/admin/agents', 'Agent Builder', 'AI Agents', '/admin/agents'],
  ['/admin/usage', 'AI Usage Dashboard', 'AI Usage', '/admin/usage'],
])('uses one focused Admin shell at %s', async (path, heading, active, canonical) => {
  const view = page(path)
  await screen.findByRole('heading', { name: heading })
  const nav = screen.getByRole('navigation', { name: 'Admin Studio navigation' })
  expect(within(nav).getAllByRole('link').map(link => link.getAttribute('href'))).toEqual(['/admin/knowledge', '/admin/agents', '/admin/usage'])
  expect(within(nav).getByRole('link', { name: active }).getAttribute('aria-current')).toBe('page')
  expect(screen.getByLabelText('Current route').textContent).toBe(canonical)
  expect(view.container.querySelectorAll('.admin-studio')).toHaveLength(1)
  expect(view.container.querySelector('.site-nav, .site-footer, .institutional-bar__tagline, .platform-nav, .syn-widget, .syn-launcher')).toBeNull()
  expect(screen.queryByText('Purpose-specific consent')).toBeNull()
  expect(view.container.querySelector('a[href="/account"], a[href="/assessment"], a[href="/admin/assessments"]')).toBeNull()
  expect(apiRequest.mock.calls.every(([, options]) => !options?.method)).toBe(true)
})

it('redirects the legacy Usage URL with query/hash preserved and replaces history', async () => {
  page('/admin/ai-usage?mode=KB_TEST#usage', 'en', ['/admin/knowledge'])
  await screen.findByRole('heading', { name: 'AI Usage Dashboard' })
  expect(screen.getByLabelText('Current route').textContent).toBe('/admin/usage?mode=KB_TEST#usage')
  fireEvent.click(screen.getByRole('button', { name: 'Browser back' }))
  await screen.findByRole('heading', { name: 'Knowledge bases' })
  expect(screen.getByLabelText('Current route').textContent).toBe('/admin/knowledge')
})

it('keeps Usage loading explicit and preserves its existing backend endpoint through the shared shell', async () => {
  page('/admin/usage')
  expect(apiRequest).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Load usage' }))
  await screen.findByRole('table', { name: 'Executions and known resource usage' })
  expect(apiRequest.mock.calls.filter(([path]) => path.startsWith('/v2/admin/agent-usage?'))).toHaveLength(1)
})

it.each(['/admin/assessments'])('returns not-found for removed product route %s without identity or authoring APIs', async path => {
  const view = page(path)
  await screen.findByRole('heading', { name: 'Page not found' })
  expect(screen.getByRole('link', { name: 'Open Admin Studio' }).getAttribute('href')).toBe('/admin')
  expect(screen.queryByRole('heading', { name: 'Purpose-specific consent' })).toBeNull()
  expect(screen.queryByRole('heading', { name: 'Assessment administration' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Sign out' })).toBeNull()
  expect(view.container.querySelector('.site-nav, .site-footer, .syn-widget, .syn-launcher')).toBeNull()
  expect(apiRequest).not.toHaveBeenCalled()
})

it('keeps the auth-only account route reachable without restoring the Account/Privacy product surface', async () => {
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? Promise.resolve({ google: false }) : Promise.reject(Object.assign(new Error('Sign in is required.'), { status: 401 })))
  const view = page('/account')
  await screen.findByRole('button', { name: 'Register', exact: true })
  expect(screen.getByRole('heading', { name: 'Account access' })).toBeTruthy()
  expect(screen.getByLabelText('Current route').textContent).toBe('/account')
  expect(screen.queryByText('Account and privacy')).toBeNull()
  expect(screen.queryByText('Purpose-specific consent')).toBeNull()
  expect(view.container.querySelector('a[href="/admin/assessments"], a[href="/admin/knowledge"], .syn-widget, .syn-launcher')).toBeNull()
  expect(apiRequest.mock.calls.every(([path]) => ['/v2/me', '/v2/auth/providers'].includes(path))).toBe(true)
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
