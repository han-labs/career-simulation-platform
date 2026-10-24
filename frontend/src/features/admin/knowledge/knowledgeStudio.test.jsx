// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
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
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Create knowledge base' }))
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
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Second KB · ACTIVE' }))
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
  const buttons = await screen.findAllByRole('button', { name: 'Inspect extracted version' })
  fireEvent.click(buttons[0]); fireEvent.click(buttons[1])
  await screen.findByText('Khám phá 🔎 — bảng dữ liệu')
  resolveFirst({ ...versions[0], provenance: { extractionSnapshot: { blocks: [{ text: 'Late stale text' }] } } })
  await waitFor(() => expect(screen.queryByText('Late stale text')).toBeNull())
  expect(screen.getByLabelText('Normalized extracted text').textContent).toContain('Khám phá 🔎')
})

it('labels missing historical extraction honestly rather than joining chunks', async () => {
  const old = { id: '1', assetKey: 'guide', title: 'Historical guide', status: 'READY', chunks: [{ chunkId: '3', versionId: '1', text: 'A chunk is not full extraction', retrievalMethod: 'INSPECTION' }] }
  fixture(path => path.endsWith('/assets') ? [old] : path.endsWith('/versions/1') ? old : undefined)
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Inspect extracted version' }))
  await screen.findByText('No extraction snapshot was recorded for this version or attempt. Chunk text is not the original extraction snapshot.')
  expect(screen.queryByLabelText('Normalized extracted text')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Chunks' }))
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
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Inspect extracted version' }))
  await screen.findByRole('heading', { name: 'Source version inspection' })
  current = { ...initial, revision: 2 }
  fireEvent.click(screen.getByRole('button', { name: 'Reload knowledge base' }))
  await waitFor(() => expect(apiRequest.mock.calls.filter(([p]) => p.endsWith('/kb-1')).length).toBeGreaterThan(1))
  fireEvent.click(screen.getByRole('button', { name: 'Publish inspected source version' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm source action' }))
  await screen.findByText('Publication revision conflict')
})

it('resumes observation of a running job loaded from the server and displays an unchanged index honestly', async () => {
  let job = { id: 'job-1', assetKey: 'guide', state: 'EMBEDDING', revision: 1, attempts: [] }, reads = 0
  fixture(path => {
    if (path.endsWith('/ingestions')) { reads++; return [job] }
    if (path.endsWith('/retry')) return { ...job, attempts: [{ outcome: 'UNCHANGED' }] }
  })
  page(); await screen.findByText(/guide · EMBEDDING/)
  job = { ...job, state: 'READY' }
  await waitFor(() => expect(reads).toBeGreaterThan(1), { timeout: 4000 })
  fireEvent.click(await screen.findByRole('button', { name: 'Check existing ingestion' }))
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
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Test KB' }))
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
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Inspect ingestion attempt' }))
  await screen.findByText('Saved extraction after failure 🔎')
  fireEvent.click(screen.getByRole('button', { name: 'Chunks' }))
  await screen.findByText('Attempt extraction is not an indexed version. Inspect its produced version for stored chunks.')
})
