// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { Link, MemoryRouter } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { draftKey } from '../../../shared/api/coreApi.js'
import VersionedAssessmentPage from './VersionedAssessmentPage.jsx'
import { quizLabels } from '../data/profileMetadata.js'

vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
const types = [{ id: '1', code: 'RIASEC', name: 'RIASEC', activeVersionId: '11' }]
const questions = ['R', 'I', 'A', 'S', 'E', 'C'].map((dimension, i) => ({ id: String(i + 1), prompt: `Activity ${dimension}`, position: i + 1, required: true, responseType: 'LIKERT', translations: {}, responseConfig: { schemaVersion: 1, min: 1, max: 5, dimension } }))
const attempt = { id: '2', assessmentVersionId: '11', revision: 0, status: 'IN_PROGRESS', questions, responses: [] }
const stored = { attemptId: '2', assessmentTypeCode: 'RIASEC', assessmentVersionId: '11', evaluatorKey: 'riasec-sum', evaluatorVersion: '1', calculatedAt: '2026-10-23T00:00:00Z', resultData: { dimensions: questions.map(q => ({ code: q.responseConfig.dimension, raw: 5, min: 1, max: 5 })), topDimensions: ['R', 'I', 'A'] } }
let history, current, saveError, startError, resultError
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); history = []; current = attempt; saveError = null; startError = null; resultError = null
  Element.prototype.scrollIntoView = vi.fn()
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  apiRequest.mockImplementation(async (path, options) => {
    if (path === '/v2/me') return { id: '7' }
    if (path === '/v2/assessment-types') return types
    if (path === '/v2/assessment-attempts') return history
    if (path.endsWith('/attempts')) { if (startError) throw startError; return current }
    if (path.endsWith('/result')) { if (resultError) throw resultError; return stored }
    if (path.endsWith('/submit') || path.endsWith('/responses')) {
      if (saveError) throw saveError
      return path.endsWith('/submit') ? stored : { ...current, revision: 1, responses: JSON.parse(options.body).items }
    }
    return current
  })
})
afterEach(() => { cleanup(); vi.restoreAllMocks() })
function page(path = '/assessment', locale = 'en') { render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={[path]}><VersionedAssessmentPage /></MemoryRouter></I18nProvider>) }
async function clickStart(locale = 'en') {
  const label = locale === 'vi' ? 'Bắt đầu hoặc tiếp tục' : 'Start or resume'
  if (!screen.queryByRole('button', { name: label })) {
    fireEvent.click((await screen.findAllByRole('button', { name: locale === 'vi' ? /^Đọc hướng dẫn/ : /^Read instructions/ }))[0])
  }
  fireEvent.click(await screen.findByRole('button', { name: label }))
}
async function start() { await clickStart(); await screen.findByRole('button', { name: 'Submit Answers' }); await waitFor(() => expect(screen.getByRole('button', { name: 'Save draft' }).disabled).toBe(false)) }
function answerAll() { screen.getAllByRole('radio', { name: 'Strongly Like' }).forEach(radio => fireEvent.click(radio)) }
function profileStored(code) {
  const codes = Object.keys(quizLabels[code].categories)
  return { ...stored, assessmentTypeCode: code, resultData: {
    categories: codes.map(code => ({ code, raw: 12, min: 3, max: 15 })), topCategories: codes,
  } }
}

it.each(['en', 'vi'])('shows only cards by default and changes instructions in place without scrolling or requesting data (%s)', async locale => {
  const catalogue = ['RIASEC', ...Object.keys(quizLabels)].map((code, i) => ({ id: String(i + 1), code, name: code, activeVersionId: String(11 + i) }))
  const original = apiRequest.getMockImplementation()
  apiRequest.mockImplementation((path, ...args) => path === '/v2/assessment-types' ? Promise.resolve(catalogue) : original(path, ...args))
  page('/assessment', locale)
  const cards = await screen.findAllByRole('button', { name: locale === 'vi' ? /^Đọc hướng dẫn/ : /^Read instructions/ })
  expect(cards).toHaveLength(4)
  expect(screen.queryByRole('combobox')).toBeNull()
  expect(document.getElementById('assessment-instructions-panel')).toBeNull()
  expect(screen.queryByRole('button', { name: locale === 'vi' ? 'Bắt đầu hoặc tiếp tục' : 'Start or resume' })).toBeNull()
  const scrollCount = window.scrollTo.mock.calls.length
  const requestCount = apiRequest.mock.calls.length
  const focused = document.activeElement
  for (const card of cards) {
    fireEvent.click(card)
    expect(card.getAttribute('aria-expanded')).toBe('true')
    expect(cards.filter(item => item.getAttribute('aria-expanded') === 'true')).toHaveLength(1)
    expect(document.getElementById('assessment-instructions-panel')).not.toBeNull()
    expect(screen.getAllByRole('button', { name: locale === 'vi' ? /^Đọc hướng dẫn/ : /^Read instructions/ })).toHaveLength(4)
    expect(window.scrollTo).toHaveBeenCalledTimes(scrollCount)
    expect(document.activeElement).toBe(focused)
    expect(apiRequest).toHaveBeenCalledTimes(requestCount)
  }
  const panel = document.getElementById('assessment-instructions-panel')
  expect(panel.textContent).toContain(locale === 'vi' ? 'Bài đánh giá hoạt động công việc' : 'Work Activities Test')
  fireEvent.click(screen.getByRole('button', { name: locale === 'vi' ? 'Trở về chọn bài đánh giá' : 'Back to assessment selection' }))
  expect(document.getElementById('assessment-instructions-panel')).toBeNull()
  expect(window.scrollTo).toHaveBeenCalledTimes(scrollCount)
  expect(apiRequest).toHaveBeenCalledTimes(requestCount)
})

it('starts the assessment chosen through cards after switching instructions', async () => {
  const catalogue = ['RIASEC', ...Object.keys(quizLabels)].map((code, i) => ({ id: String(i + 1), code, name: code, activeVersionId: String(11 + i) }))
  const original = apiRequest.getMockImplementation()
  apiRequest.mockImplementation((path, ...args) => path === '/v2/assessment-types' ? Promise.resolve(catalogue) : original(path, ...args))
  current = { ...attempt, assessmentVersionId: '12' }
  page()
  fireEvent.click(await screen.findByRole('button', { name: /^Read instructions.*Work Activities Test$/ }))
  fireEvent.click(screen.getByRole('button', { name: /^Read instructions.*Interests Quiz$/ }))
  const selectionScrolls = window.scrollTo.mock.calls.length
  await start()
  expect(apiRequest.mock.calls.some(([path]) => path === '/v2/assessment-versions/12/attempts')).toBe(true)
  expect(screen.getAllByRole('radio', { name: 'Extremely interested' })).toHaveLength(6)
  expect(window.scrollTo.mock.calls.length).toBeGreaterThan(selectionScrolls)
})

it('resets only committed assessment navigation, preserving position on answers and draft saves', async () => {
  page(); await start()
  const opened = window.scrollTo.mock.calls.length
  answerAll()
  expect(window.scrollTo).toHaveBeenCalledTimes(opened)
  fireEvent.click(screen.getByRole('button', { name: 'Save draft' }))
  await screen.findByText('Saved on the server.')
  expect(window.scrollTo).toHaveBeenCalledTimes(opened)
  fireEvent.click(screen.getByRole('button', { name: 'Submit Answers' }))
  await screen.findByText('Saved result')
  expect(window.scrollTo.mock.calls.length).toBeGreaterThan(opened)
  expect(document.activeElement.tagName).toBe('H1')
  const submitted = window.scrollTo.mock.calls.length
  fireEvent.click(screen.getByRole('button', { name: 'Back to assessment selection' }))
  await screen.findByRole('button', { name: /^Read instructions/ })
  expect(screen.queryByRole('button', { name: 'Start or resume' })).toBeNull()
  expect(window.scrollTo.mock.calls.length).toBeGreaterThan(submitted)
})

it('reopens an owner result from the URL after reload without starting or submitting an attempt', async () => {
  current = { ...attempt, status: 'SUBMITTED' }
  page('/assessment?attempt=2')
  await screen.findByText('Saved result')
  expect(screen.getAllByRole('progressbar')).toHaveLength(6)
  expect(apiRequest.mock.calls.some(([p]) => p.endsWith('/attempts') || p.endsWith('/submit'))).toBe(false)
})

it('honors the latest history target when navigation changes during an outstanding read', async () => {
  let finish
  const original = apiRequest.getMockImplementation()
  apiRequest.mockImplementation((path, ...args) => {
    if (path === '/v2/assessment-attempts/2') return new Promise(resolve => { finish = resolve })
    if (path === '/v2/assessment-attempts/3') return Promise.resolve({ ...attempt, id: '3', status: 'SUBMITTED' })
    if (path === '/v2/assessment-attempts/3/result') return Promise.resolve({ ...stored, attemptId: '3', resultData: { ...stored.resultData, dimensions: stored.resultData.dimensions.map(d => ({ ...d, raw: 4 })) } })
    return original(path, ...args)
  })
  render(<I18nProvider><MemoryRouter initialEntries={['/assessment?attempt=2']}><Link to="/assessment?attempt=3">Another result</Link><VersionedAssessmentPage /></MemoryRouter></I18nProvider>)
  await waitFor(() => expect(finish).toBeTypeOf('function'))
  fireEvent.click(screen.getByRole('link', { name: 'Another result' }))
  finish({ ...attempt, status: 'SUBMITTED' })
  await waitFor(() => expect(screen.getAllByRole('progressbar').every(bar => bar.getAttribute('aria-valuenow') === '4')).toBe(true))
  expect(apiRequest).toHaveBeenCalledWith('/v2/assessment-attempts/3')
})

it('retries the requested history entry after a read failure without showing the previous form as that entry', async () => {
  page(); await start(); answerAll()
  history = [{ id: '3', assessmentTypeCode: 'RIASEC', assessmentVersionId: '11', status: 'SUBMITTED', startedAt: stored.calculatedAt }]
  fireEvent.click(screen.getByRole('button', { name: 'Refresh history' }))
  const button = await screen.findByRole('button', { name: 'View saved result · #3' })
  const original = apiRequest.getMockImplementation()
  apiRequest.mockImplementation((path, ...args) => path === '/v2/assessment-attempts/3' ? Promise.reject(new Error('Offline')) : original(path, ...args))
  fireEvent.click(button)
  await screen.findByRole('button', { name: 'Try again' })
  expect(screen.queryByRole('radio')).toBeNull()
  expect(JSON.parse(localStorage.getItem(draftKey('7', 'assessment', '2'))).answers).toEqual(Object.fromEntries(questions.map(q => [q.id, 5])))
  apiRequest.mockImplementation((path, ...args) => path === '/v2/assessment-attempts/3' ? Promise.resolve({ ...attempt, id: '3', status: 'SUBMITTED' }) : original(path, ...args))
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  await screen.findByText('Saved result')
})

it('provides loading, retry and independent simulation access when catalogue loading fails', async () => {
  apiRequest.mockRejectedValueOnce(new Error('Offline'))
  page(); expect(screen.getByRole('status').textContent).toBe('Loading assessments…')
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Offline')
  expect(screen.queryByText('No reviewed assessment version is published yet.')).toBeNull()
  expect(screen.queryByText('No assessment attempts yet.')).toBeNull()
  expect(screen.getByRole('link', { name: 'Explore IT simulations' }).getAttribute('href')).toBe('/simulations')
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  await waitFor(() => expect(screen.getByRole('button', { name: /^Read instructions/ }).disabled).toBe(false))
})
it('focuses missing questions, shows Vietnamese field guidance and sends no invalid submit', async () => {
  page('/assessment', 'vi')
  await clickStart('vi')
  const submit = await screen.findByRole('button', { name: 'Gửi câu trả lời' })
  await waitFor(() => expect(submit.disabled).toBe(false))
  fireEvent.click(submit)
  expect(await screen.findByText('Vui lòng trả lời tất cả câu hỏi bắt buộc trước khi nộp bài đánh giá.')).toBeTruthy()
  expect(screen.getAllByText('Chọn một câu trả lời trước khi nộp.')).toHaveLength(6)
  await waitFor(() => expect(document.activeElement).toBe(screen.getAllByRole('radio')[0]))
  expect(apiRequest.mock.calls.some(([p]) => p.endsWith('/submit'))).toBe(false)
})
it('rejects malformed local values instead of counting them as completed answers', async () => {
  localStorage.setItem(draftKey('7', 'assessment', '2'), JSON.stringify({ revision: 0, answers: { 1: '5', 2: 7 } }))
  page(); await start(); fireEvent.click(screen.getByRole('button', { name: 'Submit Answers' }))
  expect(screen.getAllByRole('radiogroup').every(group => group.getAttribute('aria-invalid') === 'true')).toBe(true)
})
it('keeps answers after save/submit failure and displays only persisted results after retry', async () => {
  page(); await start(); answerAll(); saveError = new Error('Offline')
  fireEvent.click(screen.getByRole('button', { name: 'Submit Answers' }))
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Your career quiz result could not be saved. Please try again.')
  expect(screen.getAllByRole('radio').filter(r => r.checked)).toHaveLength(6)
  expect(screen.queryByText('Saved result')).toBeNull()
  saveError = null; fireEvent.click(screen.getByRole('button', { name: 'Submit Answers' }))
  expect(await screen.findByText(/Saved result/)).toBeTruthy()
  expect(screen.getAllByRole('progressbar').every(bar => bar.getAttribute('aria-valuemax') === '5')).toBe(true)
  expect(screen.queryByRole('radio')).toBeNull()
})
it('preserves failed drafts and offers explicit revision conflict resolution', async () => {
  page(); await start(); answerAll(); saveError = Object.assign(new Error('Conflict'), { status: 409 })
  fireEvent.click(screen.getByRole('button', { name: 'Save draft' }))
  await screen.findByRole('button', { name: 'Keep my local answers' })
  expect(screen.getByRole('button', { name: 'Save draft' }).disabled).toBe(true)
  current = { ...attempt, revision: 2, responses: [{ questionId: '1', answerData: { value: 1 } }] }
  saveError = null; fireEvent.click(screen.getByRole('button', { name: 'Keep my local answers' }))
  await waitFor(() => expect(screen.getByRole('button', { name: 'Save draft' }).disabled).toBe(false))
  fireEvent.click(screen.getByRole('button', { name: 'Save draft' }))
  await waitFor(() => expect(apiRequest).toHaveBeenCalledWith('/v2/assessment-attempts/2/responses', expect.objectContaining({ headers: expect.objectContaining({ 'If-Match': '2' }) })))
  expect(screen.getAllByRole('radio').filter(r => r.checked)).toHaveLength(6)
})
it('detects stale draft recovery and can explicitly choose server answers', async () => {
  localStorage.setItem(draftKey('7', 'assessment', '2'), JSON.stringify({ revision: 9, answers: { 1: 5 } }))
  page(); await clickStart()
  fireEvent.click(await screen.findByRole('button', { name: 'Use saved server answers' }))
  await waitFor(() => expect(screen.queryByRole('button', { name: 'Keep my local answers' })).toBeNull())
  expect(screen.getAllByRole('radio').some(r => r.checked)).toBe(false)
})
it('returns to selection for unavailable versions without creating a result', async () => {
  startError = Object.assign(new Error('Missing'), { status: 404 })
  page(); await clickStart()
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'This career quiz is temporarily unavailable. Please select another quiz.')
  expect(screen.queryByRole('radio')).toBeNull()
})
it('postpones without submitting and resumes an owner draft from history', async () => {
  history = [{ id: '2', assessmentTypeCode: 'RIASEC', assessmentVersionId: '11', status: 'IN_PROGRESS', startedAt: '2026-10-23T00:00:00Z' }]
  page(); await start(); answerAll(); fireEvent.click(screen.getByRole('button', { name: 'Postpone assessment' }))
  fireEvent.click(screen.getByRole('button', { name: /Resume draft/ }))
  await waitFor(() => expect(screen.getAllByRole('radio').filter(r => r.checked)).toHaveLength(6))
  expect(apiRequest.mock.calls.some(([p]) => p.endsWith('/submit'))).toBe(false)
})
it('retries stored result reads, ignores later local drafts and rotates request IDs on retake', async () => {
  current = { ...attempt, status: 'SUBMITTED' }; resultError = new Error('Result unavailable')
  localStorage.setItem(draftKey('7', 'assessment', '2'), JSON.stringify({ revision: 0, answers: { 1: 1 } }))
  page(); await clickStart()
  await screen.findByText('Your saved result could not be loaded. Use Reload saved work to try again.')
  expect(screen.queryByRole('radio')).toBeNull()
  resultError = null; fireEvent.click(screen.getByRole('button', { name: 'Reload saved work' }))
  await screen.findByText(/Saved result/)
  const oldId = JSON.parse(localStorage.getItem(draftKey('7', 'assessment-start', '11'))).requestId
  fireEvent.click(screen.getByRole('button', { name: 'Retake assessment' }))
  expect(JSON.parse(localStorage.getItem(draftKey('7', 'assessment-start', '11'))).requestId).not.toBe(oldId)
  expect(screen.queryByRole('button', { name: 'Start or resume' })).toBeNull()
  expect(screen.getByRole('button', { name: /^Read instructions/ })).toBeTruthy()
})
it('keeps server saving usable when browser storage fails', async () => {
  page(); await start()
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Quota') })
  answerAll()
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Browser storage is unavailable. Keep this page open and save your draft on the server.')
  fireEvent.click(screen.getByRole('button', { name: 'Save draft' }))
  expect(await screen.findByText('Saved on the server.')).toBeTruthy()
})
it('keeps v2 quick demos empty until the student answers the published questions', async () => {
  page('/assessment?demo=backend'); await start()
  expect(screen.getByText(/Quick demo profiles belong/)).toBeTruthy()
  expect(screen.getAllByRole('radio').some(r => r.checked)).toBe(false)
})

it('recovers a malformed cached start UUID without sending invalid metadata to the backend', async () => {
  localStorage.setItem(draftKey('7', 'assessment-start', '11'), JSON.stringify({ requestId: 'damaged-cache' }))
  page(); await start()
  const sent = JSON.parse(apiRequest.mock.calls.find(([path]) => path.endsWith('/attempts'))[1].body)
  expect(sent.requestId).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)
})

it('retains a start request ID across a lost response and sends only one pending submit', async () => {
  page(); startError = new Error('Lost response')
  await clickStart()
  await screen.findByRole('alert')
  const requestId = JSON.parse(apiRequest.mock.calls.find(([p]) => p.endsWith('/attempts'))[1].body).requestId
  startError = null; await start()
  expect(JSON.parse(apiRequest.mock.calls.filter(([p]) => p.endsWith('/attempts')).at(-1)[1].body).requestId).toBe(requestId)
  answerAll()
  let finish
  apiRequest.mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
  fireEvent.click(screen.getByRole('button', { name: 'Submit Answers' }))
  fireEvent.click(screen.getByRole('button', { name: /Submitting/ }))
  expect(apiRequest.mock.calls.filter(([p]) => p.endsWith('/submit'))).toHaveLength(1)
  finish(stored)
  await screen.findByText(/Saved result/)
})

it('keeps different result types and versions separately in history', async () => {
  history = [
    { id: '2', assessmentTypeCode: 'RIASEC', assessmentVersionId: '11', status: 'SUBMITTED', startedAt: '2026-10-23T00:00:00Z' },
    { id: '3', assessmentTypeCode: 'ABILITIES', assessmentVersionId: '15', status: 'SUBMITTED', startedAt: '2026-10-23T01:00:00Z' },
  ]
  page()
  const buttons = await screen.findAllByRole('button', { name: /View saved result/ })
  expect(buttons).toHaveLength(2)
  current = { ...attempt, id: '3', assessmentVersionId: '15', status: 'SUBMITTED' }
  apiRequest.mockImplementationOnce(async () => current).mockImplementationOnce(async () => ({ ...profileStored('ABILITIES'), attemptId: '3', assessmentVersionId: '15' }))
  fireEvent.click(buttons[1])
  const bar = await screen.findByRole('progressbar', { name: 'Technical learning' })
  expect(bar.getAttribute('aria-valuenow')).toBe('12')
  expect(bar.getAttribute('aria-valuemax')).toBe('15')
  expect(screen.getAllByRole('button', { name: /View saved result/ })).toHaveLength(2)
  expect(screen.queryByText('Your six interest dimensions')).toBeNull()
})

it('renders published profile category labels with the server ranges', async () => {
  current = { ...attempt, status: 'SUBMITTED' }
  const original = apiRequest.getMockImplementation()
  apiRequest.mockImplementation((path, ...args) => path.endsWith('/result')
    ? Promise.resolve(profileStored('INTERESTS'))
    : original(path, ...args))
  page(); await clickStart()
  const bar = await screen.findByRole('progressbar', { name: 'Analytical' })
  expect(bar.getAttribute('aria-valuenow')).toBe('12')
  expect(bar.getAttribute('aria-valuemin')).toBe('3')
  expect(bar.getAttribute('aria-valuemax')).toBe('15')
  expect(document.body.textContent).not.toMatch(/ANALYTICAL|profile-sum/)
})

it.each([
  ['INTERESTS', 'Rate how much you would enjoy each activity.', 'Extremely interested'],
  ['ABILITIES', 'Rate how confident you feel doing each activity today.', 'Highly confident'],
  ['WORK_ACTIVITIES', 'Rate how much you would prefer each kind of work activity.', 'Strongly prefer this'],
])('uses the %s instructions and response labels', async (code, description, label) => {
  apiRequest.mockImplementationOnce(async () => ({ id: '7' })).mockImplementationOnce(async () => [{ ...types[0], code }])
  page(); await start()
  expect(screen.getByText(description)).toBeTruthy()
  expect(screen.getAllByRole('radio', { name: label })).toHaveLength(6)
})
