// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import SynWidget from './SynWidget.jsx'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import LanguageSwitcher from '../../../shared/i18n/LanguageSwitcher.jsx'
import { clearSynSession, getExplorationDashboard, sendSynMessage, saveExplorationPlan } from '../api/guidanceApi.js'
vi.mock('../api/guidanceApi.js', () => ({
  clearSynSession: vi.fn(), getExplorationDashboard: vi.fn(), sendSynMessage: vi.fn(),
  saveExplorationPlan: vi.fn(), buildSynResponseMarkdown: vi.fn(),
}))
const dashboard = { source: 'LIVE', studentName: 'Synthetic', interestSignals: [], recentResults: [], skills: [], progress: { completed: 0 } }
beforeEach(() => {
  vi.resetAllMocks()
  localStorage.clear()
  sessionStorage.clear()
  window.matchMedia = () => ({ matches: true })
  Element.prototype.scrollTo = vi.fn()
  getExplorationDashboard.mockResolvedValue(dashboard)
})
afterEach(cleanup)
it('discards a private late reply after the authenticated account changes', async () => {
  let complete
  sendSynMessage.mockImplementation(() => new Promise(resolve => { complete = resolve }))
  await open()
  fireEvent.change(screen.getByRole('textbox', { name: 'Ask Syn' }), { target: { value: 'Private pending question' } })
  fireEvent.click(screen.getByRole('button', { name: 'Send message' }))
  await waitFor(() => expect(sendSynMessage).toHaveBeenCalledTimes(1))
  fireEvent(window, new Event('careersim:account-changed'))
  await act(async () => { complete({ id: 'late', text: 'Private old-account reply', provenance: 'AI' }) })
  expect(screen.queryByText('Private old-account reply')).toBeNull()
  expect(screen.queryByText('Private pending question')).toBeNull()
})

it('labels live sources and isolates provider suggestions from the application', async () => {
  sendSynMessage.mockResolvedValue({ id: 'reply', text: 'Reviewed fixture [K1]', provenance: 'AI',
    webSearch: { status: 'LIVE_WEB', retrievedAt: '2026-09-29T00:00:00Z',
      sources: [{ referenceId: 'W1', title: 'Spring guide', url: 'https://spring.io/guides', excerpt: 'REST practice' },
        { referenceId: 'W2', title: 'Unsafe', url: 'javascript:alert(1)' }],
      searchSuggestionsHtml: '<div>Google Search suggestions</div>' } })
  await open()
  await ask()
  expect(screen.getByText(/Not internally reviewed/)).toBeTruthy()
  expect(screen.getByRole('link', { name: /Spring guide/ }).href).toBe('https://spring.io/guides')
  expect(screen.queryByRole('link', { name: /Unsafe/ })).toBeNull()
  const frame = screen.getByTitle('Google Search suggestions')
  expect(frame.getAttribute('sandbox')).not.toContain('allow-scripts')
  expect(frame.getAttribute('sandbox')).not.toContain('allow-same-origin')
})

it('retains an explicitly reviewed draft after a failed save and only confirms a successful retry', async () => {
  sendSynMessage.mockResolvedValue({ id: 'reply', sessionId: 'session', text: 'Reviewed fixture [K1]', provenance: 'STANDARD',
    planDraft: { title: 'Synthetic plan', steps: ['Try SQL'] } })
  saveExplorationPlan.mockRejectedValueOnce(new Error('forbidden')).mockResolvedValueOnce({ saved: true })
  await open()
  await ask()
  fireEvent.click(screen.getByRole('button', { name: 'Review before saving' }))
  expect(saveExplorationPlan).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Confirm and save' }))
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(screen.getByRole('region', { name: 'Confirm exploration plan' })).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Confirm and save' }))
  await screen.findByText('Your exploration plan has been saved. You can revise it as you gather evidence.')
  expect(saveExplorationPlan).toHaveBeenCalledTimes(2)
  expect(screen.queryByRole('region', { name: 'Confirm exploration plan' })).toBeNull()
})
async function open() {
  render(<SynWidget />)
  fireEvent.click(screen.getByRole('button', { name: 'Open Syn assistant' }))
  await screen.findByRole('textbox', { name: 'Ask Syn' })
}

it('keeps comparison and objective results collapsed and shows search quota clearly', async () => {
  sendSynMessage.mockResolvedValue({ id: 'reply', text: 'Reviewed fixture [K1]', provenance: 'AI', intent: 'COMPARE_PATHS', responseLanguage: 'VI',
    comparisonCard: { title: 'Evidence comparison', paths: [{ code: 'BACKEND', title: 'Backend', evidenceSummary: 'Stored evidence' }] },
    resultCards: [{ id: 1, title: 'Backend test', score: 67, outcomes: [] }],
    webSearch: { status: 'RATE_LIMITED', sources: [] } })
  render(<I18nProvider initialLocale="vi"><SynWidget /></I18nProvider>)
  fireEvent.click(screen.getByRole('button', { name: 'Mở trợ lý Syn' }))
  fireEvent.change(await screen.findByRole('textbox', { name: 'Hỏi Syn' }), { target: { value: 'What is SQL?' } })
  fireEvent.click(screen.getByRole('button', { name: 'Gửi tin nhắn' }))
  await screen.findByText('Reviewed fixture [K1]')
  expect(screen.getByText('Xem dữ liệu đối chiếu').closest('details').open).toBe(false)
  expect(screen.getByText('Xem kết quả làm căn cứ').closest('details').open).toBe(false)
  expect(screen.getByRole('status').textContent).toContain('quota')
  expect(sendSynMessage.mock.calls[0][0].actionType).toBe('FREE_TEXT')
  expect(sendSynMessage.mock.calls[0][0].responseLanguage).toBe('VI')
})
async function ask() {
  fireEvent.change(screen.getByRole('textbox', { name: 'Ask Syn' }), { target: { value: 'What is SQL?' } })
  fireEvent.click(screen.getByRole('button', { name: 'Send message' }))
  await screen.findByText('Reviewed fixture [K1]')
}
it('shows source identity and excerpt without executable source links', async () => {
  sendSynMessage.mockResolvedValue({ id: 'reply', sessionId: 'session', role: 'assistant', text: 'Reviewed fixture [K1]', provenance: 'STANDARD',
    evidenceReferences: [{ referenceId: 'K1', label: 'SQL fixture', section: 'Join', version: '2', excerpt: 'Synthetic passage.', url: 'javascript:alert(1)' }] })
  await open()
  await ask()
  const source = screen.getByText('[K1]')
  expect(source.closest('details').open).toBe(false)
  fireEvent.click(source)
  expect(screen.getByText('SQL fixture')).toBeTruthy()
  expect(screen.getByText('Synthetic passage.')).toBeTruthy()
  expect(screen.queryByRole('link', { name: 'Open source' })).toBeNull()
})
it('preserves conversation on clear failure and resets only after server confirmation', async () => {
  sendSynMessage.mockResolvedValue({ id: 'reply', sessionId: 'session', role: 'assistant', text: 'Reviewed fixture [K1]', provenance: 'STANDARD' })
  clearSynSession.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(true)
  await open()
  await ask()
  fireEvent.click(screen.getByRole('button', { name: 'Clear Syn conversation memory' }))
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(screen.getByText('Reviewed fixture [K1]')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Clear Syn conversation memory' }))
  await waitFor(() => expect(screen.queryByText('Reviewed fixture [K1]')).toBeNull())
  expect(clearSynSession).toHaveBeenCalledTimes(2)
})
it('blocks duplicate sends and clear while a reply is pending', async () => {
  let finish
  sendSynMessage.mockReturnValue(new Promise(resolve => { finish = resolve }))
  await open()
  fireEvent.change(screen.getByRole('textbox', { name: 'Ask Syn' }), { target: { value: 'Hi' } })
  fireEvent.click(screen.getByRole('button', { name: 'Send message' }))
  expect(screen.getByRole('button', { name: 'Clear Syn conversation memory' }).disabled).toBe(true)
  expect(sendSynMessage).toHaveBeenCalledTimes(1)
  finish({ id: 'reply', role: 'assistant', text: 'Ready', provenance: 'STANDARD' })
  await screen.findByText('Ready')
})
it('removes legacy unscoped browser caches and does not reload a private transcript', async () => {
  localStorage.setItem('careersim_syn_session_v1', 'other-user')
  sessionStorage.setItem('careersim_syn_transcript_v1', JSON.stringify([{ role: 'assistant', text: 'Private old transcript' }]))
  await open()
  expect(screen.queryByText('Private old transcript')).toBeNull()
  expect(localStorage.getItem('careersim_syn_session_v1')).toBeNull()
  expect(sessionStorage.getItem('careersim_syn_transcript_v1')).toBeNull()
})

it('switches UI and future reply language without rewriting a transcript, draft or cited passage', async () => {
  sendSynMessage.mockResolvedValue({ id: 'reply', text: 'Original answer [K1]', provenance: 'AI',
    evidenceReferences: [{ referenceId: 'K1', label: 'Source original', excerpt: 'Original quoted passage.' }] })
  render(<I18nProvider><LanguageSwitcher /><SynWidget /></I18nProvider>)
  fireEvent.click(screen.getByRole('button', { name: 'Open Syn assistant' }))
  const input = await screen.findByRole('textbox', { name: 'Ask Syn' })
  fireEvent.change(input, { target: { value: 'Original student question' } })
  fireEvent.click(screen.getByRole('button', { name: 'Send message' }))
  await screen.findByText('Original answer [K1]')
  expect(sendSynMessage.mock.calls[0][0].responseLanguage).toBe('EN')
  fireEvent.change(input, { target: { value: 'Unsent student draft' } })
  fireEvent.change(screen.getByRole('combobox', { name: 'Website language' }), { target: { value: 'vi' } })
  expect(screen.getByText('Original student question')).toBeTruthy()
  expect(screen.getByText('Original answer [K1]')).toBeTruthy()
  expect(screen.getByText('Original quoted passage.')).toBeTruthy()
  expect(screen.getByRole('textbox', { name: 'Hỏi Syn' }).value).toBe('Unsent student draft')
  fireEvent.click(screen.getByRole('button', { name: 'Gửi tin nhắn' }))
  await waitFor(() => expect(sendSynMessage).toHaveBeenCalledTimes(2))
  expect(sendSynMessage.mock.calls[1][0].responseLanguage).toBe('VI')
})

it('sends localized quick-action text and permits an explicit Syn-only reply-language override', async () => {
  sendSynMessage.mockResolvedValue({ id: 'reply', text: 'Response fixture', provenance: 'STANDARD' })
  render(<I18nProvider initialLocale="vi"><SynWidget /></I18nProvider>)
  fireEvent.click(screen.getByRole('button', { name: 'Mở trợ lý Syn' }))
  await screen.findByRole('textbox', { name: 'Hỏi Syn' })
  fireEvent.change(screen.getByRole('combobox', { name: 'Ngôn ngữ trả lời của Syn' }), { target: { value: 'EN' } })
  fireEvent.click(screen.getByRole('button', { name: 'Giúp tôi bắt đầu' }))
  await screen.findByText('Response fixture')
  expect(sendSynMessage.mock.calls[0][0]).toMatchObject({ message: 'Giúp tôi bắt đầu', actionType: 'GET_STARTED', responseLanguage: 'EN' })
})
it('keeps retry idempotency language stable when the website language changes after a transport failure', async () => {
  sendSynMessage.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({ id: 'reply', text: 'Retried original response', provenance: 'STANDARD' })
  render(<I18nProvider><LanguageSwitcher /><SynWidget /></I18nProvider>)
  fireEvent.click(screen.getByRole('button', { name: 'Open Syn assistant' }))
  fireEvent.change(await screen.findByRole('textbox', { name: 'Ask Syn' }), { target: { value: 'Original request' } })
  fireEvent.click(screen.getByRole('button', { name: 'Send message' }))
  await screen.findByRole('button', { name: 'Retry this message' })
  const original = sendSynMessage.mock.calls[0][0]
  fireEvent.change(screen.getByRole('combobox', { name: 'Website language' }), { target: { value: 'vi' } })
  fireEvent.click(screen.getByRole('button', { name: 'Gửi lại tin nhắn này' }))
  await screen.findByText('Retried original response')
  expect(sendSynMessage.mock.calls[1][0]).toMatchObject({ clientMessageId: original.clientMessageId, responseLanguage: original.responseLanguage, message: original.message })
})
