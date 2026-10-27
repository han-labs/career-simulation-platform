// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../../shared/api/httpClient.js'
import SimulationCatalog from '../components/SimulationCatalog.jsx'
import CoreCatalogPage from './CoreCatalogPage.jsx'
import CoreSimulationPage from './CoreSimulationPage.jsx'
import AccountPage from '../../identity/AccountPage.jsx'
import { LegacySimulationRedirect } from '../../../app/App.jsx'

vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn(), API_BASE_URL: '/api', getAccountGeneration: () => 0, subscribeAccount: () => () => {} }))

beforeEach(() => { vi.resetAllMocks(); sessionStorage.clear() })
afterEach(cleanup)

function LocationProbe() {
  const location = useLocation()
  return <output aria-label="location">{location.pathname}{location.search}</output>
}

function BackButton() {
  const navigate = useNavigate()
  return <button type="button" onClick={() => navigate(-1)}>Back</button>
}

function renderLegacyRedirect(initialEntry) {
  return render(<MemoryRouter initialEntries={['/before', initialEntry]} initialIndex={1}><Routes>
    <Route path="/before" element={<p>Before alias</p>} />
    <Route path="/simulation" element={<LegacySimulationRedirect />} />
    <Route path="/simulation/:slug" element={<LegacySimulationRedirect detail />} />
    <Route path="/simulations" element={<><LocationProbe /><BackButton /></>} />
    <Route path="/simulations/:slug" element={<><LocationProbe /><BackButton /></>} />
  </Routes></MemoryRouter>)
}

function renderRoute(element, initialEntry = '/simulations') {
  return render(<I18nProvider initialLocale="en"><MemoryRouter initialEntries={[initialEntry]}><Routes><Route path="/simulations" element={<>{element}<LocationProbe /></>} /><Route path="/simulations/:slug" element={element} /><Route path="/account" element={<><p>Account destination</p><LocationProbe /></>} /></Routes></MemoryRouter></I18nProvider>)
}

function renderAccount() {
  return render(<I18nProvider initialLocale="en"><MemoryRouter initialEntries={['/account']}><Routes><Route path="/account" element={<AccountPage />} /><Route path="/simulations/:slug" element={<p>Returned simulation detail</p>} /><Route path="/" element={<p>Student workspace</p>} /><Route path="/assessment" element={<p>Assessment workspace</p>} /></Routes></MemoryRouter></I18nProvider>)
}

const published = {
  id: '10', slug: 'api-triage', scenarioVersionId: '20', enterpriseName: 'CareerSim Studio', industryCode: 'IT',
  metadata: { title: 'API Triage', targetRole: 'Backend developer', careerTrack: 'Backend Development', summary: 'Investigate a production API issue.', introduction: 'Practice a representative incident investigation.', difficulty: 'INTERMEDIATE', estimatedMinutes: 45, translations: {} },
  media: { cardImageUrl: 'https://cdn.theforage.com/card.png', heroImageUrl: 'https://cdn.theforage.com/hero.png', companyLogoUrl: 'https://cdn.theforage.com/logo.png', overviewVideoUrl: null },
  skills: ['API debugging', 'Incident triage'],
  taskOutline: [{ id: '31', position: 1, title: 'Inspect the incident', description: '<p>Review the incident context &amp; identify the affected API.</p>', taskType: 'PRACTICAL' }],
}

it('replaces the singular catalogue alias and preserves its query', async () => {
  renderLegacyRedirect('/simulation?q=api&sort=TITLE_ASC')
  expect(await screen.findByText('/simulations?q=api&sort=TITLE_ASC')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Back' }))
  expect(await screen.findByText('Before alias')).toBeTruthy()
})

it('replaces the singular detail alias and preserves slug and query', async () => {
  renderLegacyRedirect('/simulation/api-triage?source=legacy')
  expect(await screen.findByText('/simulations/api-triage?source=legacy')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Back' }))
  expect(await screen.findByText('Before alias')).toBeTruthy()
})

it('keeps catalogue criteria in the URL, sends the IT filter, and resets cursor after search', async () => {
  apiRequest.mockResolvedValue({ items: [published], nextCursor: 'next-token' })
  renderRoute(<CoreCatalogPage />, '/simulations?q=api&careerTrack=Backend&difficulty=INTERMEDIATE&sort=TITLE_ASC&cursor=old-token')
  await screen.findByRole('link', { name: /API Triage/ })
  expect(screen.getByRole('button', { name: 'Hide search and filters' }).getAttribute('aria-expanded')).toBe('true')
  expect(apiRequest.mock.calls[0][0]).toContain('industry=IT')
  expect(apiRequest.mock.calls[0][0]).toContain('cursor=old-token')
  fireEvent.change(screen.getByRole('searchbox', { name: 'Search simulations' }), { target: { value: 'security' } })
  fireEvent.click(screen.getByRole('button', { name: 'Search' }))
  await waitFor(() => expect(screen.getByLabelText('location').textContent).toContain('q=security'))
  expect(screen.getByLabelText('location').textContent).not.toContain('cursor=')
  expect(screen.getByLabelText('location').textContent).toContain('industry=IT')
})

it('renders one semantic link per Core card without a nested Start action', () => {
  render(<I18nProvider initialLocale="en"><MemoryRouter><SimulationCatalog variant="core" status="ready" simulations={[{ id: published.slug, ...published.metadata, description: published.metadata.summary, enterpriseName: published.enterpriseName, industryCode: 'IT', media: published.media }]} /></MemoryRouter></I18nProvider>)
  const card = screen.getByRole('link', { name: /API Triage/ })
  expect(card.getAttribute('href')).toBe('/simulations/api-triage')
  expect(card.querySelectorAll('a')).toHaveLength(0)
  expect(card.querySelector('img[src="https://cdn.theforage.com/card.png"]')).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Start' })).toBeNull()
  expect(screen.queryByText('View simulation')).toBeNull()
  expect(screen.queryByText('Role')).toBeNull()
  expect(screen.queryByText('Duration')).toBeNull()
  expect(screen.queryByText('Difficulty')).toBeNull()
  expect(screen.getByText('Backend developer')).toBeTruthy()
  expect(screen.getByText('45 minutes')).toBeTruthy()
  expect(screen.getByText('INTERMEDIATE')).toBeTruthy()
  expect(card.querySelectorAll('.catalog-meta svg')).toHaveLength(3)
  expect(card.querySelector('.catalog-card__summary')).toBeTruthy()
  const css = readFileSync(resolve('src/styles/index.css'), 'utf8')
  expect(css).toMatch(/\.catalog-card--link \.catalog-card__summary[^}]*-webkit-line-clamp:\s*2;/)
})

it('offers unique API career tracks independently of filtered results and submits the exact selected value', async () => {
  apiRequest.mockImplementation(path => Promise.resolve(path.includes('paged=true')
    ? { items: [], nextCursor: null }
    : [published, published, { ...published, metadata: { ...published.metadata, careerTrack: 'Cybersecurity' } }]))
  renderRoute(<CoreCatalogPage />, '/simulations?q=missing&cursor=old-token')
  await screen.findByRole('option', { name: 'Cybersecurity' })
  const track = screen.getByRole('combobox', { name: 'Career track' })
  expect(screen.getAllByRole('option', { name: 'Backend Development' })).toHaveLength(1)
  fireEvent.change(track, { target: { value: 'Cybersecurity' } })
  fireEvent.click(screen.getByRole('button', { name: 'Search' }))
  await waitFor(() => expect(screen.getByLabelText('location').textContent).toContain('careerTrack=Cybersecurity'))
  expect(screen.getByLabelText('location').textContent).not.toContain('cursor=')
  fireEvent.click(screen.getAllByRole('button', { name: 'Clear filters' })[0])
  await waitFor(() => expect(screen.getByRole('combobox', { name: 'Career track' }).value).toBe(''))
})

it('loads career tracks beyond the first API page', async () => {
  apiRequest.mockImplementation(path => {
    if (path.includes('paged=true')) return Promise.resolve({ items: [], nextCursor: null })
    return Promise.resolve(path.includes('cursor=')
      ? [{ ...published, metadata: { ...published.metadata, careerTrack: 'Cybersecurity' } }]
      : Array.from({ length: 100 }, (_, index) => ({ ...published, id: String(index + 1) })))
  })
  renderRoute(<CoreCatalogPage />, '/simulations?q=missing')
  await screen.findByRole('option', { name: 'Cybersecurity' })
  expect(apiRequest).toHaveBeenCalledWith('/v2/scenarios?industry=IT&limit=100&cursor=100')
})

it('keeps filters collapsed by default and exposes the controlled region accessibly', async () => {
  apiRequest.mockResolvedValue({ items: [], nextCursor: null })
  renderRoute(<CoreCatalogPage />)
  const toggle = screen.getByRole('button', { name: 'Search and filters' })
  expect(toggle.getAttribute('aria-expanded')).toBe('false')
  expect(screen.queryByRole('searchbox', { name: 'Search simulations' })).toBeNull()
  fireEvent.click(toggle)
  expect(screen.getByRole('button', { name: 'Hide search and filters' }).getAttribute('aria-controls')).toBe('core-catalog-filters')
  expect(screen.getByRole('searchbox', { name: 'Search simulations' })).toBeTruthy()
})

it('distinguishes loading, no-match, and retryable error states', () => {
  const retry = vi.fn(), clear = vi.fn()
  const view = render(<I18nProvider initialLocale="en"><MemoryRouter><SimulationCatalog variant="core" status="loading" simulations={[]} /></MemoryRouter></I18nProvider>)
  expect(screen.getByLabelText('Loading simulations')).toBeTruthy()
  view.rerender(<I18nProvider initialLocale="en"><MemoryRouter><SimulationCatalog variant="core" status="ready" emptyState="no-match" simulations={[]} onClear={clear} /></MemoryRouter></I18nProvider>)
  expect(screen.getByText('No simulations match your search. Try changing the filters.')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))
  expect(clear).toHaveBeenCalledOnce()
  view.rerender(<I18nProvider initialLocale="en"><MemoryRouter><SimulationCatalog variant="core" status="ready" simulations={[]} /></MemoryRouter></I18nProvider>)
  expect(screen.getByText('No simulations are currently available. Please check back later.')).toBeTruthy()
  view.rerender(<I18nProvider initialLocale="en"><MemoryRouter><SimulationCatalog variant="core" status="error" simulations={[]} onRetry={retry} /></MemoryRouter></I18nProvider>)
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  expect(retry).toHaveBeenCalledOnce()
  expect(screen.getByText('The simulation catalogue could not be loaded. Please try again.')).toBeTruthy()
})

it('shows only public API metadata and ordered task outlines before enrollment', async () => {
  apiRequest.mockResolvedValue({ ...published, metadata: { ...published.metadata, learningObjectives: '<ul><li>Trace requests &amp; logs</li><li>Explain the failure<ul><li>Identify the root cause</li></ul></li></ul>' } })
  renderRoute(<CoreSimulationPage />, '/simulations/api-triage')
  expect(await screen.findByRole('heading', { level: 1, name: 'API Triage' })).toBeTruthy()
  expect(screen.getByText('CareerSim Studio')).toBeTruthy()
  expect(screen.getByText('Backend developer')).toBeTruthy()
  expect(screen.getByText('45 minutes')).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Start free simulation' })).toBeTruthy()
  expect(screen.getByText('Trace requests & logs')).toBeTruthy()
  expect(screen.getByText('Explain the failure')).toBeTruthy()
  expect(screen.getByText('Identify the root cause')).toBeTruthy()
  expect(screen.queryByText(/<ul>|<li>/)).toBeNull()
  expect(screen.getByRole('heading', { name: 'Why complete this Job Simulation' })).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'Available Achievements' })).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'How it works' })).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'Skills you will learn and practice' })).toBeTruthy()
  expect(screen.getByText('API debugging')).toBeTruthy()
  expect(screen.getByText('Incident triage')).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'Video' })).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'Simulation tasks' })).toBeTruthy()
  expect(screen.getByRole('link', { name: 'Overview' }).getAttribute('href')).toBe('#simulation-overview')
  expect(screen.getByRole('link', { name: 'Video' }).getAttribute('href')).toBe('#simulation-video')
  expect(screen.getByRole('link', { name: 'Tasks' }).getAttribute('href')).toBe('#simulation-tasks')
  const taskToggle = screen.getByRole('button', { name: /Inspect the incident/ })
  expect(taskToggle.getAttribute('aria-expanded')).toBe('true')
  expect(screen.getByText('Review the incident context & identify the affected API.')).toBeTruthy()
  expect(screen.queryByText(/<p>|<\/p>/)).toBeNull()
  fireEvent.click(taskToggle)
  expect(screen.queryByText('Review the incident context & identify the affected API.')).toBeNull()
  expect(screen.queryByText(/model answer/i)).toBeNull()
  expect(screen.queryByRole('heading', { name: /reviews|ratings/i })).toBeNull()
  expect(screen.queryByText(/recommend/i)).toBeNull()
  expect(apiRequest).toHaveBeenCalledWith('/v2/scenarios/api-triage')
})

it.each([null, 'https://evil.example/embed/iframe/abc123'])('shows the Video tab unavailable state for non-public media %s', async overviewVideoUrl => {
  apiRequest.mockResolvedValue({ ...published, media: { ...published.media, overviewVideoUrl } })
  renderRoute(<CoreSimulationPage />, '/simulations/api-triage')
  expect(await screen.findByText('No overview video is available for this simulation.')).toBeTruthy()
  expect(screen.queryByTitle('Simulation overview video')).toBeNull()
})

it('embeds a validated public Wistia overview video', async () => {
  apiRequest.mockResolvedValue({ ...published, media: { ...published.media, overviewVideoUrl: 'https://fast.wistia.net/embed/iframe/abc123' } })
  renderRoute(<CoreSimulationPage />, '/simulations/api-triage')
  expect((await screen.findByTitle('Simulation overview video')).getAttribute('src')).toBe('https://fast.wistia.net/embed/iframe/abc123')
})

it.each([
  [404, 'This simulation is unavailable.', false],
  [500, 'Simulation details could not be loaded. Please try again.', true],
])('shows the approved public detail error for status %s', async (status, message, retryable) => {
  apiRequest.mockRejectedValue(Object.assign(new Error('Raw API error'), { status }))
  renderRoute(<CoreSimulationPage />, '/simulations/api-triage')
  expect((await screen.findByRole('alert')).textContent).toContain(message)
  expect(Boolean(screen.queryByRole('button', { name: 'Try again' }))).toBe(retryable)
  expect(screen.getByRole('link', { name: 'Back to catalog' })).toBeTruthy()
})

it('omits learning objectives when public metadata does not supply them', async () => {
  apiRequest.mockResolvedValue(published)
  renderRoute(<CoreSimulationPage />, '/simulations/api-triage')
  await screen.findByRole('heading', { level: 1, name: 'API Triage' })
  expect(screen.queryByRole('heading', { name: 'Learning objectives' })).toBeNull()
})

it('stores the selected detail path and opens account when Start is unauthenticated', async () => {
  apiRequest.mockImplementation(path => path === '/v2/scenarios/api-triage' ? Promise.resolve(published) : Promise.reject(Object.assign(new Error('Sign in is required.'), { status: 401 })))
  renderRoute(<CoreSimulationPage />, '/simulations/api-triage')
  fireEvent.click(await screen.findByRole('button', { name: 'Start free simulation' }))
  expect(await screen.findByText('Account destination')).toBeTruthy()
  expect(sessionStorage.getItem('careersim_pending_simulation_path')).toBe('/simulations/api-triage')
  expect(apiRequest.mock.calls.some(([path]) => path.includes('/enrollments'))).toBe(false)
})

it('returns to the selected detail after successful regular login without enrolling', async () => {
  sessionStorage.setItem('careersim_pending_simulation_path', '/simulations/api-triage')
  let signedIn = false
  apiRequest.mockImplementation((path, options = {}) => {
    if (path === '/v2/auth/providers') return Promise.resolve({ google: false })
    if (path === '/v2/me') return signedIn ? Promise.resolve({ id: '7', displayName: 'Student', role: 'STUDENT' }) : Promise.reject(Object.assign(new Error('Sign in is required.'), { status: 401 }))
    if (path === '/v2/auth/login' && options.method === 'POST') { signedIn = true; return Promise.resolve({ id: '7', displayName: 'Student', role: 'STUDENT' }) }
    return Promise.reject(new Error('Unexpected request'))
  })
  renderAccount()
  fireEvent.change(await screen.findByRole('textbox', { name: 'Email *' }), { target: { value: 'student@example.test' } })
  fireEvent.change(screen.getByLabelText('Password *'), { target: { value: 'test-password' } })
  fireEvent.submit(screen.getByLabelText('Password *').closest('form'))
  expect(await screen.findByText('Returned simulation detail')).toBeTruthy()
  expect(sessionStorage.getItem('careersim_pending_simulation_path')).toBeNull()
  expect(apiRequest.mock.calls.some(([path]) => path.includes('/enrollments'))).toBe(false)
})

it('returns after OAuth success is confirmed by the current-session request', async () => {
  sessionStorage.setItem('careersim_pending_simulation_path', '/simulations/api-triage')
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? Promise.resolve({ google: true }) : Promise.resolve({ id: '7', displayName: 'Student', role: 'STUDENT' }))
  renderAccount()
  expect(await screen.findByText('Returned simulation detail')).toBeTruthy()
  expect(sessionStorage.getItem('careersim_pending_simulation_path')).toBeNull()
})

it('clears and ignores an invalid stored destination after authentication', async () => {
  sessionStorage.setItem('careersim_pending_simulation_path', 'https://evil.example/simulations/api-triage')
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? Promise.resolve({ google: true }) : Promise.resolve({ id: '7', displayName: 'Student', role: 'STUDENT' }))
  renderAccount()
  expect(await screen.findByText('Student workspace')).toBeTruthy()
  expect(sessionStorage.getItem('careersim_pending_simulation_path')).toBeNull()
  expect(screen.queryByText('Returned simulation detail')).toBeNull()
})
