// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import I18nProvider from '../i18n/I18nProvider.jsx'
import { apiRequest } from './httpClient.js'
import { draftKey } from './coreApi.js'
import CoreSimulationPage from '../../features/simulations/pages/CoreSimulationPage.jsx'
import CoreSimulationResultsPage from '../../features/simulations/pages/CoreSimulationResultsPage.jsx'
import TaskRichText from '../../features/simulations/components/TaskRichText.jsx'
import VersionedAssessmentPage from '../../features/assessment/pages/VersionedAssessmentPage.jsx'
import AccountPage from '../../features/identity/AccountPage.jsx'
vi.mock('./httpClient.js', () => ({ apiRequest: vi.fn(), API_BASE_URL: '/api' }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(cleanup)
function page(component, path = '/test', locale = 'en') { return render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={[path]}><Routes><Route path="*" element={component} /></Routes></MemoryRouter></I18nProvider>) }
function resultsPage(locale = 'en', slug = 'test') { return render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={[`/simulations/${slug}/results?enrollment=2`]}><Routes><Route path="/simulations/:slug/results" element={<CoreSimulationResultsPage />} /></Routes></MemoryRouter></I18nProvider>) }
const definition = { id: '9', name: 'Response', format: 'TEXT', required: true, validationConfig: { maxLength: 1000 } }
const task = { id: '4', title: 'Fixture task', instructions: 'Write actual work', resources: [], definitions: [definition], translations: {} }
const enrollment = { id: '2', scenarioVersionId: '3', requiredTasks: 1, finalRequiredTasks: 0, metadata: { title: 'Fixture', translations: {} }, tasks: [task] }
it('opens Dashboard alias results using owned pinned data without requesting a nonexistent public slug', async () => {
  apiRequest.mockImplementation(path => {
    if (path === '/v2/me') return Promise.resolve({ id: '7', role: 'STUDENT' })
    if (path === '/v2/enrollments/2') return Promise.resolve(enrollment)
    if (path === '/v2/enrollments/2/results') return Promise.resolve({ tasks: [{ taskId: '4', status: 'FINAL' }], practicedSkills: [] })
    return Promise.reject(Object.assign(new Error('No public alias scenario'), { status: 404 }))
  })
  resultsPage('en', 'enrolled')
  expect(await screen.findByRole('heading', { name: 'Fixture' })).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'Fixture task' })).toBeTruthy()
  expect(apiRequest.mock.calls.map(([path]) => path)).toEqual(['/v2/me', '/v2/enrollments/2', '/v2/enrollments/2/results'])
  expect(screen.queryByRole('alert')).toBeNull()
})
it.each([401, 403, 404])('does not turn denied Dashboard alias data into public results (%s)', async status => {
  apiRequest.mockRejectedValue(Object.assign(new Error('Denied'), { status }))
  resultsPage('en', 'enrolled')
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(screen.queryByRole('heading', { name: 'Fixture' })).toBeNull()
})
it('keeps a scoped draft after failed persistence and never displays a fabricated final result', async () => {
  apiRequest.mockImplementation(path => path === '/v2/me' ? Promise.resolve({ id: '7' }) : path === '/v2/enrollments/2' ? Promise.resolve(enrollment) : path.endsWith('/results') ? Promise.resolve({ tasks: [], practicedSkills: [] }) : path.endsWith('/submission') ? Promise.resolve({ status: 'DRAFT', revision: 0, items: [] }) : Promise.reject(new Error('Unavailable')))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  const input = await screen.findByRole('textbox', { name: 'Response *' })
  fireEvent.change(input, { target: { value: 'Keep my work' } })
  apiRequest.mockRejectedValueOnce(new Error('Synthetic save failure'))
  fireEvent.click(screen.getByRole('button', { name: 'Save draft' }))
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(screen.getByRole('status').textContent).toContain('Your work is preserved locally. Review the error and try again.')
  expect(input.value).toBe('Keep my work')
  expect(JSON.parse(localStorage.getItem(draftKey('7', 'task', '2_4'))).values['9'].contentText).toBe('Keep my work')
  expect(screen.queryByText('Simulation completed')).toBeNull()
  expect(screen.queryByRole('button', { name: 'View model answer' })).toBeNull()
})
it('renders immutable final work without merging a later local draft', async () => {
  localStorage.setItem(draftKey('7', 'task', '2_4'), JSON.stringify({ values: { 9: { contentText: 'Unaccepted local text' } } }))
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/me' ? { id: '7' } : path === '/v2/enrollments/2' ? enrollment : { status: 'FINAL', revision: 2, items: [{ definitionId: '9', contentText: 'Accepted work' }] }))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  const input = await screen.findByRole('textbox', { name: 'Response *' })
  expect(input.value).toBe('Accepted work')
  expect(input.closest('fieldset').disabled).toBe(true)
  expect(screen.getByRole('button', { name: 'View model answer' })).toBeTruthy()
})
it('requires an accessible explicit final confirmation and preserves work when cancelled', async () => {
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/me' ? { id: '7' } : path === '/v2/enrollments/2' ? enrollment : { status: 'DRAFT', revision: 0, items: [] }))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  fireEvent.change(await screen.findByRole('textbox', { name: 'Response *' }), { target: { value: 'Actual final work' } })
  fireEvent.click(screen.getByRole('button', { name: 'Submit task' }))
  expect(screen.getByRole('dialog', { name: 'Submit this task?' })).toBeTruthy()
  expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Cancel' }))
  expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/finalize'))).toBe(false)
  fireEvent.keyDown(document, { key: 'Escape' })
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(screen.getByRole('textbox', { name: 'Response *' }).value).toBe('Actual final work')
  fireEvent.click(screen.getByRole('button', { name: 'Submit task' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm submit' }))
  await waitFor(() => expect(apiRequest).toHaveBeenCalledWith('/v2/enrollments/2/tasks/4/submission/finalize', expect.objectContaining({ method: 'POST', body: expect.stringContaining('Actual final work') })))
  expect(apiRequest.mock.calls.filter(([path]) => path.endsWith('/finalize'))).toHaveLength(1)
})
it('isolates owner drafts and exposes an empty assessment catalog truthfully in Vietnamese', async () => {
  localStorage.setItem(draftKey('8', 'assessment', '1'), JSON.stringify({ answers: { 1: 5 } }))
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/me' ? { id: '7' } : []))
  page(<VersionedAssessmentPage />, '/assessment', 'vi')
  expect(await screen.findByText('Chưa có phiên bản đánh giá đã rà soát được công bố.')).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Bắt đầu hoặc tiếp tục' })).toBeNull()
  expect(screen.queryByRole('combobox')).toBeNull()
  expect(screen.queryByRole('button', { name: /^Đọc hướng dẫn/ })).toBeNull()
  expect(localStorage.getItem(draftKey('8', 'assessment', '1'))).toContain('5')
})
it.each(['work-activities', 'WORK_ACTIVITIES'])('opens the exact published type through its card for the supported deep link %s', async slug => {
  const published = [{ id: '1', code: 'RIASEC', name: 'RIASEC', activeVersionId: '11' }, { id: '2', code: 'WORK_ACTIVITIES', name: 'Work activities', activeVersionId: '22' }]
  const draft = { id: '44', assessmentVersionId: '22', status: 'IN_PROGRESS', revision: 0, questions: [], responses: [] }
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/me' ? { id: '7' } : path === '/v2/assessment-types' ? published : path === '/v2/assessment-attempts' ? [] : draft))
  render(<I18nProvider><MemoryRouter initialEntries={[`/assessment/${slug}`]}><Routes><Route path="/assessment/:assessmentType" element={<VersionedAssessmentPage />} /></Routes></MemoryRouter></I18nProvider>)
  const card = await screen.findByRole('button', { name: /^Read instructions.*Work Activities Test$/ })
  expect(screen.queryByRole('combobox')).toBeNull()
  expect(screen.queryByRole('button', { name: 'Start or resume' })).toBeNull()
  expect(apiRequest.mock.calls.some(([path]) => path.includes('/attempts'))).toBe(false)
  fireEvent.click(card)
  expect(screen.getByRole('heading', { name: 'Instructions · Work Activities Test' })).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Start or resume' }))
  await waitFor(() => expect(apiRequest).toHaveBeenCalledWith('/v2/assessment-versions/22/attempts', expect.objectContaining({ method: 'POST' })))
  expect(apiRequest.mock.calls.some(([path]) => path === '/v2/assessment-versions/11/attempts')).toBe(false)
})
it('does not substitute RIASEC when a typed deep link has no published version', async () => {
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/me' ? { id: '7' } : [{ id: '1', code: 'RIASEC', name: 'RIASEC', activeVersionId: '11' }]))
  render(<I18nProvider><MemoryRouter initialEntries={['/assessment/work-activities']}><Routes><Route path="/assessment/:assessmentType" element={<VersionedAssessmentPage />} /></Routes></MemoryRouter></I18nProvider>)
  expect(await screen.findByText('The requested assessment type has no published version.')).toBeTruthy()
  expect(screen.queryByRole('combobox')).toBeNull()
  expect(screen.queryByRole('button', { name: 'Start or resume' })).toBeNull()
  expect(screen.queryByRole('button', { name: /Read instructions.*Work Activities Test/ })).toBeNull()
  expect(apiRequest.mock.calls.some(([path]) => path.includes('/attempts'))).toBe(false)
})
it('keeps zero-score final work complete and requires explicit review to unlock the next task', async () => {
  let reviewed = false
  const final = { status: 'FINAL', revision: 2, items: [{ definitionId: '9', contentText: 'Accepted work' }], quizResult: { correct: 0, total: 1 }, reviewedAt: null }
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/me' ? { id: '7' } : path === '/v2/enrollments/2' ? { ...enrollment, progressionMode: 'ORDERED_SELF_REVIEW', tasks: [task, { ...task, id: '5', title: 'Next task' }], progression: [{ taskId: '4', accessible: true, finalized: true, reviewed }, { taskId: '5', accessible: reviewed }] } : path.endsWith('/model-answer') ? { answer: null, resources: [] } : path.endsWith('/self-review') ? (reviewed = true, { ...final, reviewedAt: '2026-10-19T00:00:00Z' }) : final))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  fireEvent.click(await screen.findByRole('button', { name: 'View model answer' }))
  expect(await screen.findByText('No model answer was provided.')).toBeTruthy()
  const review = screen.getByRole('button', { name: 'Confirm self-review' })
  expect(screen.getByRole('button', { name: /2\. Next task — Locked/ }).disabled).toBe(true)
  expect(screen.getByText(/Completion does not depend on this score/)).toBeTruthy()
  fireEvent.click(review)
  await waitFor(() => expect(screen.getByRole('button', { name: /2\. Next task — Not started/ }).disabled).toBe(false))
  expect(screen.getByRole('button', { name: 'Self-review recorded' }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Next task' }))
  expect(await screen.findByRole('heading', { level: 1, name: 'Next task' })).toBeTruthy()
})
it('explains an OAuth email collision and exposes only an explicit sign-in/link path', async () => {
  apiRequest.mockImplementation(path => path === '/v2/auth/providers' ? Promise.resolve({ google: true }) : Promise.reject(Object.assign(new Error('Sign in is required.'), { status: 401 })))
  page(<AccountPage />, '/account?oauthError=link-required')
  expect(await screen.findByText('Sign in to your existing account, then explicitly link Google.')).toBeTruthy()
  expect(await screen.findByRole('link', { name: 'Continue with Google' })).toBeTruthy()
  expect(screen.queryByRole('checkbox')).toBeNull()
})

it('renders allowlisted task formatting, preserves code, and drops executable markup and unsafe links', () => {
  const { container } = render(<TaskRichText value={'<h1>Task guide</h1><p>Read <strong>carefully</strong></p><ul><li>First step</li></ul><pre><code>&lt;div&gt;code&lt;/div&gt;</code></pre><a href="javascript:alert(1)">Unsafe link</a><a href="https://example.test/guide" onclick="alert(1)">Safe guide</a><img src="https://evil.example/tracker" onerror="alert(1)"><script>alert(1)</script><iframe src="https://evil.example"></iframe>'} />)
  expect(screen.getByRole('heading', { level: 3, name: 'Task guide' })).toBeTruthy()
  expect(container.querySelector('strong').textContent).toBe('carefully')
  expect(container.querySelector('li').textContent).toBe('First step')
  expect(container.querySelector('code').textContent).toBe('<div>code</div>')
  expect(screen.queryByRole('link', { name: 'Unsafe link' })).toBeNull()
  expect(screen.getByRole('link', { name: 'Safe guide' }).getAttribute('onclick')).toBeNull()
  expect(container.querySelector('script,iframe,img,[onerror]')).toBeNull()
})

it('keeps plain text and encoded examples literal instead of repeatedly decoding markup', () => {
  render(<TaskRichText value={'List<T>\n&lt;script&gt;example&lt;/script&gt;'} />)
  expect(screen.getByText(/List<T>/).textContent).toContain('&lt;script&gt;example&lt;/script&gt;')
})

function workspaceApi(path, extra = {}) {
  if (path === '/v2/me') return Promise.resolve({ id: '7' })
  if (path === '/v2/enrollments/2') return Promise.resolve(extra.enrollment ?? enrollment)
  if (path.endsWith('/results')) return Promise.resolve(extra.results ?? { tasks: [], practicedSkills: [], completion: null })
  return Promise.resolve(extra.submission ?? { status: 'DRAFT', revision: 0, items: [] })
}

it('restores the server draft and renders HTML instructions in the workspace', async () => {
  apiRequest.mockImplementation(path => workspaceApi(path, { enrollment: { ...enrollment, tasks: [{ ...task, instructions: '<p>Use <strong>unit tests</strong></p>' }] }, submission: { status: 'DRAFT', revision: 1, items: [{ definitionId: '9', contentText: 'Saved response' }] } }))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  expect((await screen.findByRole('textbox', { name: 'Response *' })).value).toBe('Saved response')
  expect(screen.getByText('unit tests').tagName).toBe('STRONG')
  expect(screen.queryByText(/<p>/)).toBeNull()
  expect(screen.queryByRole('button', { name: 'Reload saved work' })).toBeNull()
})

it('keeps the workspace visible and reports a missing FILE MIME allowlist', async () => {
  const fileTask = { ...task, definitions: [{ id: '12', name: 'Submit your Excel worksheet here', format: 'FILE', required: true, validationConfig: {} }] }
  apiRequest.mockImplementation(path => workspaceApi(path, { enrollment: { ...enrollment, tasks: [fileTask], progression: [{ taskId: '4', accessible: true }] } }))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  expect(await screen.findByRole('heading', { name: 'Fixture task' })).toBeTruthy()
  expect((await screen.findByRole('alert')).textContent).toBe('This task has an invalid file-upload configuration. Please contact the administrator.')
  expect(screen.queryByLabelText('Submit your Excel worksheet here *')).toBeNull()
  expect(screen.getByRole('button', { name: 'Next task' })).toBeTruthy()
})

it('shows the allowed FILE formats and size limit', async () => {
  const fileTask = { ...task, definitions: [{ id: '12', name: 'Upload evidence', format: 'FILE', required: true, validationConfig: { schemaVersion: 1, maxBytes: 10485760, allowedMimeTypes: ['application/pdf', 'image/png'] } }] }
  apiRequest.mockImplementation(path => workspaceApi(path, { enrollment: { ...enrollment, tasks: [fileTask], progression: [{ taskId: '4', accessible: true }] } }))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  expect(await screen.findByText('Allowed formats: PDF, PNG · Maximum file size: 10 MiB')).toBeTruthy()
  expect(screen.getByLabelText('Upload evidence *').getAttribute('accept')).toBe('application/pdf,image/png')
})

it('rejects a FILE over 10 MiB before sending it to the API', async () => {
  const fileTask = { ...task, definitions: [{ id: '12', name: 'Upload evidence', format: 'FILE', required: true, validationConfig: { schemaVersion: 1, maxBytes: 10485760, allowedMimeTypes: ['application/pdf'] } }] }
  apiRequest.mockImplementation(path => workspaceApi(path, { enrollment: { ...enrollment, tasks: [fileTask], progression: [{ taskId: '4', accessible: true }] } }))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  const input = await screen.findByLabelText('Upload evidence *')
  const oversized = new File(['content'], 'oversized.pdf', { type: 'application/pdf' })
  Object.defineProperty(oversized, 'size', { value: 10485761 })
  fireEvent.change(input, { target: { files: [oversized] } })
  expect((await screen.findByRole('alert')).textContent).toContain('The selected file exceeds the 10 MiB limit.')
  expect(apiRequest.mock.calls.some(([path]) => path === '/v2/files')).toBe(false)
})

it('shows active/locked sidebar tasks and explains why Next cannot advance before review', async () => {
  apiRequest.mockImplementation(path => workspaceApi(path, { enrollment: { ...enrollment, progressionMode: 'ORDERED_SELF_REVIEW', tasks: [task, { ...task, id: '5', title: 'Locked task' }], progression: [{ taskId: '4', accessible: true }, { taskId: '5', accessible: false }] } }))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  await screen.findByRole('textbox', { name: 'Response *' })
  fireEvent.change(screen.getByRole('textbox', { name: 'Response *' }), { target: { value: 'Preserved local work' } })
  expect(screen.getByRole('status').textContent).toContain('Local draft')
  expect(screen.getByRole('button', { name: /1\. Fixture task/ }).getAttribute('aria-current')).toBe('step')
  expect(screen.getByRole('button', { name: /2\. Locked task/ }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Next task' }))
  expect(await screen.findByText('Please confirm your self-review before continuing.')).toBeTruthy()
  expect(screen.queryByRole('status')).toBeNull()
  expect(screen.queryByRole('link', { name: 'Sign in' })).toBeNull()
  expect(apiRequest.mock.calls.some(([path]) => path.includes('/tasks/5/'))).toBe(false)
})

it('preserves the response and request UUID when retrying an unconfirmed final submission', async () => {
  let attempts = 0
  apiRequest.mockImplementation((path, options) => {
    if (path.endsWith('/finalize')) {
      if (++attempts === 1) return Promise.reject(new Error('Network interrupted'))
      return Promise.resolve({ status: 'FINAL', revision: 1, items: [{ definitionId: '9', contentText: 'Retry this response' }] })
    }
    return workspaceApi(path, options)
  })
  page(<CoreSimulationPage />, '/test?enrollment=2')
  fireEvent.change(await screen.findByRole('textbox', { name: 'Response *' }), { target: { value: 'Retry this response' } })
  fireEvent.click(screen.getByRole('button', { name: 'Submit task' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm submit' }))
  await screen.findByText('Your submission could not be confirmed. Please try again.')
  expect(screen.getByRole('textbox', { name: 'Response *' }).value).toBe('Retry this response')
  fireEvent.click(screen.getByRole('button', { name: 'Submit task' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm submit' }))
  await screen.findByRole('button', { name: 'View model answer' })
  const calls = apiRequest.mock.calls.filter(([path]) => path.endsWith('/finalize')).map(([, options]) => JSON.parse(options.body))
  expect(calls).toHaveLength(2)
  expect(calls[0]).toEqual(calls[1])
})

it('keeps final submission unavailable until required deliverables have a preliminary valid value', async () => {
  apiRequest.mockImplementation(path => workspaceApi(path))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  expect(await screen.findByText('Complete all required deliverables to submit this task as final.')).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Submit task' })).toBeNull()
  fireEvent.change(screen.getByRole('textbox', { name: 'Response *' }), { target: { value: 'Ready to submit' } })
  expect(screen.getByRole('button', { name: 'Submit task' })).toBeTruthy()
  expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/finalize'))).toBe(false)
  expect(screen.queryByRole('button', { name: 'View model answer' })).toBeNull()
})

it('reloads a newer revision after conflict without losing local work, then retries with the fresh If-Match', async () => {
  let reads = 0
  let saves = 0
  apiRequest.mockImplementation((path, options = {}) => {
    if (path.endsWith('/submission') && options.method === 'PUT') {
      saves += 1
      if (saves === 1) return Promise.reject(Object.assign(new Error('The draft changed. Reload before saving.'), { status: 409, code: 'CONFLICT' }))
      return Promise.resolve({ status: 'DRAFT', revision: 3, items: [{ definitionId: '9', contentText: 'Local work' }] })
    }
    if (path.endsWith('/submission')) {
      reads += 1
      return Promise.resolve({ status: 'DRAFT', revision: reads === 1 ? 1 : 2, items: [] })
    }
    return workspaceApi(path)
  })
  page(<CoreSimulationPage />, '/test?enrollment=2')
  const input = await screen.findByRole('textbox', { name: 'Response *' })
  fireEvent.change(input, { target: { value: 'Local work' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save draft' }))
  expect(await screen.findByText('The latest server revision was loaded. Review your preserved work and try again.')).toBeTruthy()
  expect(input.value).toBe('Local work')
  fireEvent.click(screen.getByRole('button', { name: 'Save draft' }))
  await waitFor(() => expect(apiRequest).toHaveBeenCalledWith('/v2/enrollments/2/tasks/4/submission', expect.objectContaining({ method: 'PUT', headers: expect.objectContaining({ 'If-Match': '2' }), body: expect.stringContaining('Local work') })))
})

it('shows completed task status, a zero quiz score and practiced skills without inventing artifacts', async () => {
  apiRequest.mockImplementation(path => path === '/v2/scenarios/test' ? Promise.resolve({ enterpriseName: 'Example Company' }) : workspaceApi(path, { results: { tasks: [{ taskId: '4', status: 'FINAL', quizResult: { correct: 0, total: 2 } }], practicedSkills: ['Unit testing'], completion: null } }))
  resultsPage()
  expect(await screen.findByText('Unit testing')).toBeTruthy()
  expect(screen.getByText(/Quiz score.*0\/2/)).toBeTruthy()
  expect(screen.getByRole('region', { name: 'Simulation results' }).textContent).toContain('Final submission')
  expect(screen.queryByRole('link', { name: 'View certificate' })).toBeNull()
  expect(screen.queryByRole('textbox', { name: 'Fixed resume snippet' })).toBeNull()
})

it('retries results independently and renders practical work without a numeric score', async () => {
  let reads = 0
  apiRequest.mockImplementation(path => path === '/v2/scenarios/test' ? Promise.resolve({ enterpriseName: 'Example Company' }) : path.endsWith('/results')
    ? ++reads === 1 ? Promise.reject(new Error('Unavailable')) : Promise.resolve({ tasks: [{ taskId: '4', status: 'FINAL', quizResult: null }], practicedSkills: [], completion: { id: '3', resumeSnippetText: 'Practiced a simulation', certificateCode: null } })
    : workspaceApi(path))
  resultsPage()
  await screen.findByText('Simulation results could not be loaded. Please try again.')
  fireEvent.click(screen.getByRole('button', { name: 'Retry results' }))
  expect(await screen.findByRole('heading', { name: 'Completion record' })).toBeTruthy()
  expect(screen.queryByText(/Quiz score/)).toBeNull()
  expect(screen.queryByRole('link', { name: 'View certificate' })).toBeNull()
})

it('denies a foreign enrollment without showing its workspace and offers own history', async () => {
  apiRequest.mockImplementation(path => path === '/v2/me' ? Promise.resolve({ id: '7' }) : Promise.reject(Object.assign(new Error('Hidden'), { status: 404 })))
  page(<CoreSimulationPage />, '/test?enrollment=99')
  expect(await screen.findByText('You do not have permission to view these simulation results.')).toBeTruthy()
  expect(screen.getByRole('link', { name: 'Return to my simulation history' }).getAttribute('href')).toBe('/dashboard')
  expect(screen.queryByRole('textbox', { name: 'Response *' })).toBeNull()
})

it('keeps new workspace and results controls available in Vietnamese', async () => {
  apiRequest.mockImplementation(path => path === '/v2/scenarios/test' ? Promise.resolve({ enterpriseName: 'Example Company' }) : workspaceApi(path))
  resultsPage('vi')
  expect(await screen.findByRole('heading', { name: 'Kết quả mô phỏng' })).toBeTruthy()
  expect(await screen.findByText('Chưa ghi nhận kỹ năng đã thực hành.')).toBeTruthy()
})
