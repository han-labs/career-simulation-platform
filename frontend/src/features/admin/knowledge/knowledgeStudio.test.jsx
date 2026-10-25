// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { createTranslator } from '../../../shared/i18n/translate.js'
import { apiRequest } from '../../../shared/api/httpClient.js'
import KnowledgeStudioPage from './KnowledgeStudioPage.jsx'
vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(cleanup)
const initial = { id: 'kb-1', name: 'Reviewed KB', status: 'ACTIVE', revision: 0, publicationRevision: 0 }
const options = { models: [{ key: 'gemini/synthetic', available: false }] }
function fixture(extra = () => undefined) {
  apiRequest.mockImplementation(async (path, request = {}) => {
    const value = extra(path, request)
    if (value !== undefined) return value
    if (path === '/v2/me') return { id: '5', role: 'ADMIN' }
    if (path.endsWith('/options')) return options
    if (path === '/v2/admin/knowledge-bases') return [initial]
    if (path.endsWith('/kb-1')) return initial
    if (path.endsWith('/assets') || path.endsWith('/ingestions')) return []
    throw new Error(`Unexpected route ${path}`)
  })
}
function page(url = '/admin/knowledge?kb=kb-1') { return render(<I18nProvider><MemoryRouter initialEntries={[url]}><KnowledgeStudioPage /></MemoryRouter></I18nProvider>) }
it('pins the editing revision across refresh, preserves conflict drafts and explicitly discards them', async () => {
  let current = { ...initial }
  fixture((path, r) => {
    if (path.endsWith('/kb-1') && r.method === 'PATCH') { expect(r.headers['If-Match']).toBe('0'); throw new Error('Revision conflict. Reload before continuing.') }
    if (path.endsWith('/kb-1') && !r.method) return current
  })
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Edit knowledge base' }))
  fireEvent.change(screen.getByLabelText('Knowledge base name'), { target: { value: 'My unsaved draft' } })
  current = { ...initial, revision: 1, name: 'Other admin edit' }
  fireEvent.click(screen.getByRole('button', { name: 'Close and keep draft' }))
  fireEvent.click(screen.getByRole('button', { name: 'Reload knowledge base' }))
  await screen.findByRole('heading', { name: 'Other admin edit' })
  fireEvent.click(screen.getByRole('button', { name: 'Edit knowledge base' }))
  expect(screen.getByLabelText('Knowledge base name').value).toBe('My unsaved draft')
  fireEvent.click(screen.getByRole('button', { name: 'Save knowledge base settings' }))
  await screen.findByText('Revision conflict. Reload before continuing.')
  expect(screen.getByLabelText('Knowledge base name').value).toBe('My unsaved draft')
  fireEvent.click(screen.getByRole('button', { name: 'Discard draft and use refreshed version' }))
  await waitFor(() => expect(screen.getByLabelText('Knowledge base name').value).toBe('Other admin edit'))
})
it('does not offer duplicate creation after a successful write followed by a list failure', async () => {
  let written = false
  fixture((path, r) => {
    if (path === '/v2/admin/knowledge-bases' && r.method === 'POST') { written = true; return { ...initial, id: 'kb-2', name: 'Created once' } }
    if (path === '/v2/admin/knowledge-bases' && written) return Promise.reject(new Error('List unavailable'))
    if (path.endsWith('/kb-2')) return { ...initial, id: 'kb-2', name: 'Created once' }
  })
  page('/admin/knowledge'); fireEvent.click(await screen.findByRole('button', { name: 'Create knowledge base' }))
  const dialog = screen.getByRole('dialog')
  fireEvent.change(within(dialog).getByLabelText('New knowledge base name'), { target: { value: 'Created once' } })
  fireEvent.click(within(dialog).getByRole('button', { name: 'Create knowledge base' }))
  await screen.findByText('List unavailable')
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(apiRequest.mock.calls.filter(([, r]) => r?.method === 'POST')).toHaveLength(1)
})
it('ignores a late workspace response after selecting another KB', async () => {
  let resolveOld
  fixture(path => {
    if (path === '/v2/admin/knowledge-bases') return [initial, { ...initial, id: 'kb-2', name: 'Second KB' }]
    if (path.endsWith('/kb-1')) return new Promise(resolve => { resolveOld = resolve })
    if (path.endsWith('/kb-2')) return { ...initial, id: 'kb-2', name: 'Second KB' }
  })
  page()
  fireEvent.click(await screen.findByRole('link', { name: 'Back to knowledge bases' }))
  fireEvent.click(await screen.findByRole('link', { name: 'Open knowledge base: Second KB' }))
  await screen.findByRole('heading', { name: 'Second KB' })
  resolveOld({ ...initial, name: 'Late private workspace' })
  await waitFor(() => expect(screen.queryByRole('heading', { name: 'Late private workspace' })).toBeNull())
})

it('inspects exact extraction with Unicode and keeps a late version from replacing the selection', async () => {
  let resolveFirst
  const versions = ['1', '2'].map(id => ({ id, assetKey: 'guide', title: `Guide ${id}`, versionNumber: id, status: 'READY' }))
  fixture(path => {
    if (path.endsWith('/assets')) return versions
    if (path.endsWith('/versions/1')) return new Promise(resolve => { resolveFirst = resolve })
    if (path.endsWith('/versions/2')) return { ...versions[1], chunks: [], provenance: { extractionSnapshot: { blocks: [{ text: 'Khám phá 🔎 — bảng dữ liệu', locator: { paragraph: 2 } }], warnings: [] } } }
  })
  page()
  await screen.findByRole('button', { name: 'Review and publish' })
  fireEvent.click(screen.getByText('Document options'))
  const chooser = screen.getByLabelText('Inspect another version')
  fireEvent.change(chooser, { target: { value: '1' } }); fireEvent.change(chooser, { target: { value: '2' } })
  await screen.findByText('Khám phá 🔎 — bảng dữ liệu')
  resolveFirst({ ...versions[0], provenance: { extractionSnapshot: { blocks: [{ text: 'Late stale text' }] } } })
  await waitFor(() => expect(screen.queryByText('Late stale text')).toBeNull())
  expect(screen.getByLabelText('Normalized extracted text').textContent).toContain('Khám phá 🔎')
})

it('labels missing historical extraction honestly rather than joining chunks', async () => {
  const old = { id: '1', assetKey: 'guide', title: 'Historical guide', status: 'READY', chunks: [{ chunkId: '3', versionId: '1', text: 'A chunk is not full extraction', retrievalMethod: 'INSPECTION' }] }
  fixture(path => path.endsWith('/assets') ? [old] : path.endsWith('/versions/1') ? old : undefined)
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Review and publish' }))
  await screen.findByText('No extraction snapshot was recorded for this version or attempt. Chunk text is not the original extraction snapshot.')
  expect(screen.queryByLabelText('Normalized extracted text')).toBeNull()
  fireEvent.click(screen.getByRole('tab', { name: 'Chunks' }))
  await screen.findByText('A chunk is not full extraction')
})

it('keeps publication bound to the reviewed revision even after refreshing the workspace', async () => {
  let current = { ...initial }
  const version = { id: '1', assetKey: 'guide', title: 'Review first', status: 'READY', chunks: [] }
  fixture((path, request) => {
    if (path.endsWith('/kb-1')) return current
    if (path.endsWith('/assets')) return [version]
    if (path.endsWith('/versions/1')) return version
    if (path.endsWith('/publish')) { expect(request.headers['If-Match']).toBe('0'); return Promise.reject(new Error('Publication revision conflict')) }
  })
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Review and publish' }))
  await screen.findByRole('heading', { name: 'Source version inspection' })
  current = { ...initial, revision: 2 }
  fireEvent.click(screen.getByRole('button', { name: 'Reload knowledge base' }))
  await waitFor(() => expect(apiRequest.mock.calls.filter(([p]) => p.endsWith('/kb-1')).length).toBeGreaterThan(1))
  fireEvent.click(screen.getByRole('button', { name: 'Publish document' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm source action' }))
  await screen.findByText('Publication revision conflict')
})

it('resumes observation of a running job loaded from the server and displays an unchanged index honestly', async () => {
  let job = { id: 'job-1', assetKey: 'guide', state: 'EMBEDDING', revision: 1, attempts: [] }, reads = 0
  fixture(path => {
    if (path.endsWith('/ingestions')) { reads++; return [job] }
    if (path.endsWith('/retry')) return { ...job, attempts: [{ outcome: 'UNCHANGED' }] }
  })
  page(); await screen.findByText('Processing', { exact: true })
  job = { ...job, state: 'READY' }
  await waitFor(() => expect(reads).toBeGreaterThan(1), { timeout: 4000 })
  await screen.findByText('Ready for review', { exact: true })
  fireEvent.click(screen.getByText('Document options'))
  fireEvent.click(screen.getByRole('button', { name: 'Check existing ingestion' }))
  await screen.findByText('Index unchanged. The existing version was reused; no rebuild or new version was created.')
})

it('pins KB Test results to the submitted query and revalidates revoked sources without regenerating', async () => {
  let unavailable = false, calls = 0
  fixture((path, request) => {
    if (path.endsWith('/tests')) { const input = JSON.parse(request.body); expect(input.retrievalOnly).toBe(true); expect(input.modelKey).toBeNull(); calls++; return { id: 'test-1' } }
    if (path.endsWith('/agent-executions/test-1')) {
      if (unavailable) return Promise.reject(new Error('Source revoked'))
      return { executionMode: 'KB_TEST', status: 'SUCCEEDED', inputSnapshot: { query: 'Original question', retrievalOnly: true, topK: 5 }, toolStepsAndSources: [{ sources: [{ referenceId: 'K1', chunkId: '1', versionId: '1', text: 'Authorized passage', knowledgeBaseId: 'kb-1' }] }], outputSnapshot: { outcome: 'RETRIEVAL_ONLY', text: 'Retrieval only', citations: [] } }
    }
  })
  page(); fireEvent.click(await screen.findByRole('tab', { name: 'Test KB' }))
  fireEvent.change(screen.getByLabelText('Knowledge test question'), { target: { value: 'Original question' } })
  fireEvent.click(screen.getByRole('button', { name: 'Retrieve passages' }))
  await screen.findByText('Authorized passage')
  fireEvent.change(screen.getByLabelText('Knowledge test question'), { target: { value: 'Different draft question' } })
  expect(within(screen.getByLabelText('Submitted test request')).getByText('Original question')).toBeTruthy()
  unavailable = true; fireEvent.focus(window)
  await screen.findByText('Source revoked')
  expect(screen.queryByText('Authorized passage')).toBeNull()
  expect(calls).toBe(1)
})

it('inspects a failed attempt after reload without inventing indexed chunks', async () => {
  const job = { id: 'job-failed', assetKey: 'guide', state: 'FAILED', errorCode: 'PROVIDER_UNAVAILABLE', revision: 2, attempts: [{ generation: 1, stage: 'EMBEDDING', extractionSnapshot: { blocks: [{ text: 'Saved extraction after failure 🔎' }] } }] }
  fixture(path => path.endsWith('/ingestions') ? [job] : path.endsWith('/ingestions/job-failed') ? job : undefined)
  page(); fireEvent.click(await screen.findByText('Document options'))
  fireEvent.click(screen.getByRole('button', { name: 'Inspect processing details' }))
  await screen.findByText('Saved extraction after failure 🔎')
  fireEvent.click(screen.getByRole('tab', { name: 'Chunks' }))
  await screen.findByText('Attempt extraction is not an indexed version. Inspect its produced version for stored chunks.')
})

it('renders the collection without a competing page navigation, invented counts or per-item asset requests', async () => {
  fixture()
  page('/admin/knowledge')
  expect(await screen.findByRole('link', { name: 'Open knowledge base: Reviewed KB' })).toBeTruthy()
  expect(screen.queryByRole('navigation')).toBeNull()
  expect(screen.queryByRole('complementary')).toBeNull()
  expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/assets'))).toBe(false)
  fireEvent.click(screen.getByRole('button', { name: 'Edit knowledge base: Reviewed KB' }))
  expect(screen.getByLabelText('Knowledge base name').value).toBe('Reviewed KB')
})

it('shows loading, recoverable list errors and a truthful empty collection', async () => {
  let reject
  fixture(path => path === '/v2/admin/knowledge-bases' ? new Promise((_, failure) => { reject = failure }) : undefined)
  page('/admin/knowledge')
  expect(screen.getByRole('status').textContent).toContain('Loading')
  await waitFor(() => expect(reject).toBeTypeOf('function'))
  reject(new Error('List unavailable'))
  await screen.findByRole('alert')
  fixture(path => path === '/v2/admin/knowledge-bases' ? [] : undefined)
  fireEvent.click(screen.getAllByRole('button', { name: 'Refresh knowledge bases' })[0])
  await screen.findByText('No knowledge bases yet. Create an empty knowledge base to begin.')
  expect(screen.queryByRole('alert')).toBeNull()
})

it('keeps a busy create dialog open on Escape and dispatches the write only once', async () => {
  let complete
  fixture((path, request) => path === '/v2/admin/knowledge-bases' && request.method === 'POST' ? new Promise(resolve => { complete = resolve }) : undefined)
  page('/admin/knowledge')
  fireEvent.click(await screen.findByRole('button', { name: 'Create knowledge base' }))
  fireEvent.change(screen.getByLabelText('New knowledge base name'), { target: { value: 'Busy draft' } })
  const dialog = screen.getByRole('dialog')
  fireEvent.click(within(dialog).getByRole('button', { name: 'Create knowledge base' }))
  await waitFor(() => expect(within(dialog).getByRole('button', { name: 'Create knowledge base' }).disabled).toBe(true))
  expect(within(dialog).getByRole('status').textContent).toBe('Saving...')
  fireEvent.keyDown(dialog, { key: 'Escape' })
  fireEvent.submit(dialog.querySelector('form'))
  expect(screen.getByRole('dialog')).toBe(dialog)
  expect(apiRequest.mock.calls.filter(([, request]) => request?.method === 'POST')).toHaveLength(1)
  complete({ ...initial, name: 'Busy draft' })
  await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
})

it('requires explicit lifecycle confirmation and focuses the safe action without changing its target', async () => {
  const version = { id: '11', assetKey: 'review-guide', title: 'Published guide', versionNumber: 3, status: 'PUBLISHED' }
  fixture((path, request) => path.endsWith('/assets') ? [version] : path.endsWith('/versions/11') ? version : path.endsWith('/retire') ? (expect(request.method).toBe('POST'), version) : undefined)
  page()
  await screen.findByText('Published guide')
  fireEvent.click(screen.getByRole('button', { name: 'View document' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Retire source version' }))
  expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/retire'))).toBe(false)
  expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Keep current publication' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm source action' }))
  await waitFor(() => expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/assets/review-guide/versions/11/retire'))).toBe(true))
})

it('continues to show the published version while another version is ready and its replacement attempt failed', async () => {
  const versions = [{ id: '1', assetKey: 'guide', title: 'Current source', status: 'PUBLISHED', versionNumber: 1 }, { id: '2', assetKey: 'guide', title: 'Reviewed replacement', status: 'READY', versionNumber: 2 }]
  fixture(path => path.endsWith('/assets') ? versions : path.endsWith('/ingestions') ? [{ id: 'failed', assetKey: 'guide', state: 'FAILED', errorCode: 'EMBEDDING_UNAVAILABLE' }] : undefined)
  page()
  await screen.findByText('Reviewed replacement')
  const row = screen.getByText('Reviewed replacement').closest('li')
  expect(within(row).getByText('Published version: 1')).toBeTruthy()
  expect(within(row).getByText('Replacement did not complete. The published version is still in use.')).toBeTruthy()
  expect(within(row).getByText('Failed')).toBeTruthy()
  expect(within(row).queryByRole('button', { name: 'Review and publish' })).toBeNull()
  expect(within(row).getByRole('button', { name: 'Retry processing' })).toBeTruthy()
})

it('confirms cancellation against the exact attempt and preserves the safe dismissal path', async () => {
  let job = { id: 'queued-job', assetKey: 'guide', state: 'QUEUED', attempts: [] }
  fixture((path, request) => {
    if (path.endsWith('/ingestions')) return [job]
    if (path.endsWith('/queued-job/cancel')) { expect(JSON.parse(request.body)).toEqual({}); job = { ...job, state: 'CANCELLED' }; return job }
  })
  page()
  fireEvent.click(await screen.findByText('Document options'))
  fireEvent.click(await screen.findByRole('button', { name: 'Cancel ingestion' }))
  fireEvent.click(screen.getByRole('button', { name: 'Keep current publication' }))
  expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/cancel'))).toBe(false)
  fireEvent.click(screen.getByRole('button', { name: 'Cancel ingestion' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm source action' }))
  await screen.findByText('Cancelled')
  expect(apiRequest.mock.calls.filter(([path]) => path.endsWith('/queued-job/cancel'))).toHaveLength(1)
})

it('keeps a validated answer separate from its exact retrieved evidence and requested mode', async () => {
  fixture((path, request) => {
    if (path.endsWith('/tests')) { expect(JSON.parse(request.body).retrievalOnly).toBe(false); return { id: 'answer-test' } }
    if (path.endsWith('/agent-executions/answer-test')) return { status: 'SUCCEEDED', executionMode: 'KB_TEST', inputSnapshot: { query: 'Answer question', retrievalOnly: false, topK: 5 }, outputSnapshot: { outcome: 'ANSWER', text: 'A validated synthetic answer.', citations: [{ referenceId: 'K1', quote: 'Exact reviewed evidence' }] }, toolStepsAndSources: [{ sources: [{ chunkId: '1', versionId: '1', text: 'Exact reviewed evidence', referenceId: 'K1' }] }] }
  })
  page()
  fireEvent.click(await screen.findByRole('tab', { name: 'Test KB' }))
  fireEvent.click(screen.getByRole('radio', { name: 'Answer with sources' }))
  fireEvent.change(screen.getByLabelText('Knowledge test question'), { target: { value: 'Answer question' } })
  fireEvent.click(screen.getByRole('button', { name: 'Answer with sources' }))
  expect(await screen.findByRole('heading', { name: 'Validated answer' })).toBeTruthy()
  expect(within(screen.getByLabelText('Generated answer')).getByText('A validated synthetic answer.')).toBeTruthy()
  expect(within(screen.getByLabelText('Retrieved source passages')).getByText('Exact reviewed evidence')).toBeTruthy()
  expect(within(screen.getByLabelText('Submitted test request')).getByText(/Answer with sources/)).toBeTruthy()
})

it.each(['en', 'vi'])('shows the real refused/no-evidence contract without a validated answer (%s)', async locale => {
  const t = createTranslator(locale)
  fixture(path => {
    if (path.endsWith('/tests')) return { id: 'empty-test' }
    if (path.endsWith('/agent-executions/empty-test')) return { status: 'REFUSED', executionMode: 'KB_TEST', errorCode: 'EMPTY_EVIDENCE', outputSnapshot: { outcome: 'INSUFFICIENT_EVIDENCE', text: 'There is insufficient authorized knowledge to answer this question.', citations: [] }, toolStepsAndSources: [] }
  })
  render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={['/admin/knowledge?kb=kb-1']}><KnowledgeStudioPage /></MemoryRouter></I18nProvider>)
  fireEvent.click(await screen.findByRole('tab', { name: t('Test KB') }))
  fireEvent.click(screen.getByRole('radio', { name: t('Answer with sources') }))
  fireEvent.change(screen.getByLabelText(t('Knowledge test question')), { target: { value: 'No evidence question' } })
  fireEvent.click(screen.getByRole('button', { name: t('Answer with sources') }))
  expect(await screen.findByText(t('Refused'), { exact: true })).toBeTruthy()
  expect(screen.getByText(t('There is insufficient authorized knowledge to answer this question.'))).toBeTruthy()
  expect(screen.getByText(t('No authorized passage was retrieved.'))).toBeTruthy()
  expect(screen.queryByRole('heading', { name: t('Validated answer') })).toBeNull()
  expect(apiRequest.mock.calls.filter(([path]) => path.endsWith('/tests'))).toHaveLength(1)
})
