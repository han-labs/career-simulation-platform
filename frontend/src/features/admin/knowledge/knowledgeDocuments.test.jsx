// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { createTranslator } from '../../../shared/i18n/translate.js'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { draftKey } from '../../../shared/api/coreApi.js'
import { groupDocuments, MAX_SOURCE_BYTES, selectSource, sourceFileError, uploadInput } from './knowledgeDocuments.js'
import KnowledgeStudioPage from './KnowledgeStudioPage.jsx'

vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(cleanup)
const base = { id: 'kb-1', name: 'Reviewed knowledge', status: 'ACTIVE', revision: 0 }
const file = () => new File(['Original synthetic content'], 'Hướng dẫn nghề nghiệp.txt', { type: 'text/plain', lastModified: 123 })
const staged = { id: 'job-new', assetKey: 'new-source', knowledgeBaseId: base.id, state: 'QUEUED', metadata: { title: 'New source' }, attempts: [] }
const version = { id: '51', knowledgeBaseId: base.id, assetKey: staged.assetKey, title: 'New source', status: 'READY', versionNumber: 1, chunks: [] }
function fixture(extra = () => undefined) {
  apiRequest.mockImplementation(async (path, request = {}) => {
    const override = extra(path, request)
    if (override !== undefined) return override
    if (path === '/v2/me') return { id: '5', role: 'ADMIN' }
    if (path.endsWith('/options')) return { models: [] }
    if (path === '/v2/admin/knowledge-bases') return [base]
    if (path.endsWith('/kb-1')) return base
    if (path.endsWith('/assets') || path.endsWith('/ingestions')) return []
    throw new Error('Unexpected fixture request ' + path)
  })
}
function page(locale = 'en') { return render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={['/admin/knowledge?kb=kb-1']}><KnowledgeStudioPage /></MemoryRouter></I18nProvider>) }
async function selectFile(selected = file()) {
  fireEvent.change(await screen.findByLabelText('Source file'), { target: { files: [selected] } })
}
function complete() {
  fireEvent.change(screen.getByLabelText('Source usage rights'), { target: { value: 'other' } })
  fireEvent.change(screen.getByLabelText('Source usage rights details'), { target: { value: 'Original synthetic fixture' } })
  fireEvent.change(screen.getByLabelText('Topic / domain'), { target: { value: 'BACKEND' } })
  fireEvent.change(screen.getByLabelText('Document language'), { target: { value: 'VI' } })
  fireEvent.click(screen.getByRole('checkbox'))
  return screen.getByRole('button', { name: 'Upload and process' }).closest('form')
}
const writes = suffix => apiRequest.mock.calls.filter(([path, request]) => path.endsWith(suffix) && request?.method === 'POST')

it('deletes a published logical document only after confirmation, safely retires every version and hides it', async () => {
  let versions = [{ ...version, status: 'PUBLISHED' }, { ...version, id: '52', versionNumber: 2 }]
  fixture((path, request) => {
    if (path.endsWith('/assets')) return versions
    if (path.endsWith('/retire')) { const id = path.split('/').at(-2); versions = versions.map(v => v.id === id ? { ...v, status: 'RETIRED' } : v); return versions.find(v => v.id === id) }
    if (request.method) throw new Error('Unexpected deletion endpoint')
  })
  page(); await screen.findByLabelText('Document list')
  fireEvent.click(screen.getByRole('button', { name: 'Delete document' }))
  expect(writes('/retire')).toHaveLength(0)
  expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Keep document' }))
  fireEvent.click(screen.getByRole('button', { name: 'Keep document' }))
  expect(writes('/retire')).toHaveLength(0)
  fireEvent.click(screen.getByRole('button', { name: 'Delete document' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm delete document' }))
  await screen.findByText('No documents yet. Upload a file to begin.')
  expect(writes('/retire')).toHaveLength(2)
  expect(versions.every(v => v.status === 'RETIRED')).toBe(true)
  expect(groupDocuments(versions, [], base.id)).toEqual([])
})

it('does not restore or poll a saved Test execution while Documents is selected', async () => {
  localStorage.setItem(draftKey('5', 'kb-test-execution', 'kb-1'), JSON.stringify({ id: 'saved-execution' }))
  fixture()
  page(); await screen.findByText('No documents yet. Upload a file to begin.')
  fireEvent(window, new Event('focus'))
  fireEvent.click(screen.getByRole('button', { name: 'Reload knowledge base' }))
  await waitFor(() => expect(apiRequest.mock.calls.filter(([path]) => path.endsWith('/assets')).length).toBeGreaterThan(1))
  expect(apiRequest.mock.calls.some(([path]) => path.includes('/agent-executions/'))).toBe(false)
})

it('generates valid distinct stable keys, filename titles and no assumed language or rights', () => {
  const first = selectSource(file()), second = selectSource(file())
  expect(first.assetKey).toMatch(/^[a-z0-9][a-z0-9-]{0,99}$/)
  expect(first.assetKey).not.toBe(second.assetKey)
  expect(first.title).toBe('Hướng dẫn nghề nghiệp')
  expect(first.language).toBe(''); expect(first.license).toBe(''); expect(first.reviewed).toBe(false)
  const resumed = selectSource(file(), { ...first, reviewed: true, stagedJob: staged })
  expect(resumed.assetKey).toBe(first.assetKey); expect(resumed.requestId).toBe(first.requestId)
  expect(resumed.stagedJob).toEqual(staged); expect(resumed.reviewed).toBe(false)
  expect(selectSource(file(), { replacementKey: 'original-key', license: 'Reviewed license', language: 'EN', track: 'DATA' }).assetKey).toBe('original-key')
  expect(Object.keys(uploadInput(first)).sort()).toEqual(['assetKey', 'title', 'license', 'track', 'language', 'reviewed', 'requestId', 'sourceUri'].sort())
})

it('enforces one supported non-empty file and the real 8 MiB parser boundary', () => {
  expect(sourceFileError([file(), file()])).toContain('one file')
  expect(sourceFileError([{ name: 'bad.exe', size: 1 }])).toContain('TXT')
  expect(sourceFileError([{ name: 'empty.txt', size: 0 }])).toContain('non-empty')
  expect(sourceFileError([{ name: 'limit.txt', size: MAX_SOURCE_BYTES }])).toBe('')
  expect(sourceFileError([{ name: 'large.txt', size: MAX_SOURCE_BYTES + 1 }])).toContain('8 MiB')
})

it('maps server keys/produced version IDs into one row, including staged-only jobs and failed replacements', () => {
  const published = { ...version, id: '50', status: 'PUBLISHED' }
  const failed = { ...staged, state: 'FAILED' }
  const rows = groupDocuments([published, { ...version }], [failed, { ...staged, id: 'old-job', producedVersionId: '51', state: 'READY' }, { ...staged, id: 'other-job', assetKey: 'only-staged' }], base.id)
  expect(rows).toHaveLength(2)
  expect(rows[0].job.id).toBe(failed.id); expect(rows[0].published.id).toBe('50')
  expect(rows[0].state).toBe('FAILED'); expect(rows[1].versions).toHaveLength(0)
  const linked = groupDocuments([published, version], [{ ...staged, producedVersionId: '50', state: 'READY' }], base.id)[0]
  expect(linked.linked.id).toBe('50'); expect(linked.state).toBe('PUBLISHED')
  const sharedFile = groupDocuments([{ ...published, versionNumber: 1, sourceFileId: 'file-1' }, { ...version, versionNumber: 2, sourceFileId: 'file-1' }], [{ ...staged, producedVersionId: '50', sourceFileId: 'file-1', state: 'PUBLISHED' }], base.id)[0]
  expect(sharedFile.linked.id).toBe('50'); expect(sharedFile.state).toBe('READY')
  expect(sharedFile.ready.id).toBe(version.id)
  const retiredReplacement = groupDocuments([published, { ...version, versionNumber: 2, status: 'RETIRED' }], [], base.id)[0]
  expect(retiredReplacement.state).toBe('PUBLISHED')
  expect(groupDocuments([{ ...version, knowledgeBaseId: 'other-kb' }], [], base.id)).toHaveLength(0)
})

it('shows the upload area immediately, reveals metadata only on selection and supports a single-file drop', async () => {
  fixture(); page()
  const drop = (await screen.findByText('Drop a document here')).closest('.kb-dropzone')
  expect(screen.getByRole('button', { name: 'Browse files' })).toBeTruthy()
  expect(screen.queryByLabelText('Source usage rights')).toBeNull()
  fireEvent.drop(drop, { dataTransfer: { files: [file()] } })
  expect(screen.getByLabelText('Document title').value).toBe('Hướng dẫn nghề nghiệp')
  expect(document.activeElement).toBe(screen.getByLabelText('Document title'))
  expect(screen.queryByLabelText('Stable asset key')).toBeNull()
  expect(screen.getByLabelText('Document language').value).toBe('')
  expect(screen.getByRole('checkbox').checked).toBe(false)
  expect(screen.getByLabelText('Source HTTPS reference (optional)').closest('details').open).toBe(false)
  expect(writes('/assets')).toHaveLength(0)
})

it('requires metadata and explicit source confirmation before any upload or processing call', async () => {
  fixture(); page(); await selectFile()
  const form = screen.getByRole('button', { name: 'Upload and process' }).closest('form')
  fireEvent.submit(form)
  expect(await screen.findByText('Complete the required document information and source authorization confirmation.')).toBeTruthy()
  expect(writes('/assets')).toHaveLength(0); expect(writes('/retry')).toHaveLength(0)
  complete(); fireEvent.click(screen.getByRole('checkbox')); fireEvent.submit(form)
  expect(writes('/assets')).toHaveLength(0)
})

it('uploads then processes exactly once under repeated submit, with no automatic publication', async () => {
  let resolveUpload, saved = null, ready = false, payload
  fixture((path, request) => {
    if (path.endsWith('/assets') && request.method === 'POST') { payload = request.body; return new Promise(resolve => { resolveUpload = resolve }) }
    if (path.endsWith('/retry') || path.endsWith('/reprocess')) { ready = true; return { ...staged, state: 'READY', producedVersionId: version.id } }
    if (path.endsWith('/ingestions')) return saved ? [{ ...saved, state: ready ? 'READY' : 'QUEUED' }] : []
    if (path.endsWith('/assets')) return ready ? [version] : []
  })
  page(); await selectFile(); const form = complete()
  fireEvent.submit(form); fireEvent.submit(form)
  await waitFor(() => expect(writes('/assets')).toHaveLength(1))
  expect(writes('/retry')).toHaveLength(0)
  const input = JSON.parse(await payload.get('metadata').text())
  expect(input.assetKey).toMatch(/^[a-z0-9][a-z0-9-]{0,99}$/); expect(input.reviewed).toBe(true)
  expect(input.fileIdentity).toBeUndefined(); expect(input.language).toBe('VI')
  saved = staged; resolveUpload(staged)
  await screen.findByRole('button', { name: 'Review and publish' })
  expect(writes('/retry')).toHaveLength(1); expect(writes('/publish')).toHaveLength(0)
  expect(within(screen.getByLabelText('Document list')).getAllByRole('listitem')).toHaveLength(1)
})

it('keeps the staged job on processing failure and retries it without uploading again, including refresh', async () => {
  let job = null, ready = false, calls = 0
  fixture((path, request) => {
    if (path.endsWith('/assets') && request.method === 'POST') { job = { ...staged }; return job }
    if (path.endsWith('/retry') || path.endsWith('/reprocess')) { calls++; job = { ...job, state: calls === 1 ? 'FAILED' : 'READY', producedVersionId: calls === 1 ? null : version.id }; ready = calls > 1; return job }
    if (path.endsWith('/assets')) return ready ? [version] : []
    if (path.endsWith('/ingestions')) return job ? [job] : []
  })
  const view = page(); await selectFile(); fireEvent.submit(complete())
  await screen.findByText('Processing failed. Your uploaded file is saved. Retry processing from the document list.')
  expect(screen.getByRole('button', { name: 'Reprocess' })).toBeTruthy()
  view.unmount(); page()
  fireEvent.click(await screen.findByRole('button', { name: 'Reprocess' }))
  await screen.findByRole('button', { name: 'Review and publish' })
  expect(writes('/assets')).toHaveLength(1); expect(writes('/retry')).toHaveLength(1); expect(writes('/reprocess')).toHaveLength(1)
})

it('preserves the upload request ID after an ambiguous upload failure and file reselection after remount', async () => {
  const ids = []
  fixture((path, request) => {
    if (path.endsWith('/assets') && request.method === 'POST') return request.body.get('metadata').text().then(text => { ids.push(JSON.parse(text).requestId); throw new Error('Network response lost') })
  })
  const view = page(); await selectFile(); fireEvent.submit(complete())
  await screen.findByText('Network response lost'); view.unmount(); page(); await selectFile()
  expect(screen.getByRole('checkbox').checked).toBe(false)
  fireEvent.click(screen.getByRole('checkbox'))
  fireEvent.submit(screen.getByRole('button', { name: 'Upload and process' }).closest('form'))
  await waitFor(() => expect(ids).toHaveLength(2)); expect(ids[0]).toBe(ids[1])
  expect(writes('/retry')).toHaveLength(0)
})

it('does not start ingestion for a late upload response after leaving the workspace', async () => {
  let resolve
  fixture((path, request) => path.endsWith('/assets') && request.method === 'POST' ? new Promise(done => { resolve = done }) : undefined)
  const view = page(); await selectFile(); fireEvent.submit(complete())
  await waitFor(() => expect(resolve).toBeTypeOf('function')); view.unmount(); resolve(staged)
  await Promise.resolve(); await Promise.resolve()
  expect(writes('/retry')).toHaveLength(0)
})

it('retains a saved upload when the processing response is lost and never retries upload automatically', async () => {
  let job = null
  fixture((path, request) => {
    if (path.endsWith('/assets') && request.method === 'POST') { job = staged; return job }
    if (path.endsWith('/retry') || path.endsWith('/reprocess')) return Promise.reject(new Error('Processing transport unavailable'))
    if (path.endsWith('/ingestions')) return job ? [job] : []
  })
  page(); await selectFile(); fireEvent.submit(complete())
  await screen.findByText('The processing request could not finish. Your uploaded file is saved. Refresh or retry from the document list.')
  expect(screen.getByRole('button', { name: 'Reprocess' })).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Reload knowledge base' }))
  await waitFor(() => expect(screen.getByRole('button', { name: 'Reload knowledge base' }).disabled).toBe(false))
  await selectFile()
  expect(screen.getByText('This file is already uploaded. Use the document list to retry processing.')).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Upload and process' })).toBeNull()
  expect(writes('/assets')).toHaveLength(1); expect(writes('/retry')).toHaveLength(1)
})

it('reuses the published document key when replacing and keeps publication separate from processing', async () => {
  let input
  const published = { ...version, assetKey: 'keep-published-key', status: 'PUBLISHED', license: 'Original license', language: 'VI', provenance: { track: 'DATA' } }
  fixture((path, request) => {
    if (path.endsWith('/assets') && request.method === 'POST') return request.body.get('metadata').text().then(text => { input = JSON.parse(text); return staged })
    if (path.endsWith('/retry') || path.endsWith('/reprocess')) return { ...staged, state: 'FAILED' }
    if (path.endsWith('/assets')) return [published]
    if (path.endsWith('/versions/51')) return published
  })
  page(); await screen.findByLabelText('Document list')
  fireEvent.click(screen.getByRole('button', { name: 'Detail' })); fireEvent.click(await screen.findByRole('button', { name: 'Replace file' }))
  await selectFile()
  expect(screen.getByLabelText('Source usage rights').value).toBe('other')
  expect(screen.getByLabelText('Source usage rights details').value).toBe('Original license')
  expect(screen.getByRole('checkbox').checked).toBe(false)
  fireEvent.click(screen.getByRole('checkbox'))
  fireEvent.submit(screen.getByRole('button', { name: 'Upload and process' }).closest('form'))
  await waitFor(() => expect(input?.assetKey).toBe('keep-published-key'))
  expect(writes('/publish')).toHaveLength(0)
  expect(screen.getByText('Published version: 1')).toBeTruthy()
})

it('keeps publication disabled until the exact READY version is loaded for review', async () => {
  let resolve
  fixture(path => {
    if (path.endsWith('/assets')) return [version]
    if (path.endsWith('/versions/51')) return new Promise(done => { resolve = done })
  })
  page(); fireEvent.click(await screen.findByRole('button', { name: 'Review and publish' }))
  expect(screen.getByRole('button', { name: 'Publish document' }).disabled).toBe(true)
  await waitFor(() => expect(resolve).toBeTypeOf('function')); resolve(version)
  await waitFor(() => expect(screen.getByRole('button', { name: 'Publish document' }).disabled).toBe(false))
  expect(writes('/publish')).toHaveLength(0)
})


it.each([
  ['internal', 'Internal / organization-owned document'],
  ['public', 'Public or official source'],
  ['permission', 'Permission granted'],
])('requires an explicit rights choice and maps %s to the existing license field only', async (choice, label) => {
  let payload
  fixture((path, request) => {
    if (path.endsWith('/assets') && request.method === 'POST') return request.body.get('metadata').text().then(text => { payload = JSON.parse(text); return staged })
    if (path.endsWith('/retry') || path.endsWith('/reprocess')) return { ...staged, state: 'FAILED' }
  })
  page(); await selectFile()
  const rights = screen.getByRole('combobox', { name: 'Source usage rights' })
  expect(rights.value).toBe(''); expect(rights.required).toBe(true)
  expect(within(rights).getAllByRole('option').map(option => option.value)).toEqual(['', 'internal', 'public', 'permission', 'other'])
  expect(screen.getByRole('checkbox').checked).toBe(false)
  fireEvent.change(screen.getByLabelText('Topic / domain'), { target: { value: 'DATA' } })
  fireEvent.change(screen.getByLabelText('Document language'), { target: { value: 'EN' } })
  fireEvent.click(screen.getByRole('checkbox'))
  const form = screen.getByRole('button', { name: 'Upload and process' }).closest('form')
  fireEvent.submit(form); expect(writes('/assets')).toHaveLength(0)
  fireEvent.change(rights, { target: { value: choice } })
  expect(screen.queryByLabelText('Source usage rights details')).toBeNull()
  fireEvent.submit(form)
  await waitFor(() => expect(payload?.license).toBe(label))
  expect(payload.track).toBe('DATA'); expect(payload.reviewed).toBe(true)
  expect(Object.keys(payload).sort()).toEqual(['assetKey','title','license','track','language','reviewed','requestId','sourceUri'].sort())
})

it('requires Other details and preserves an unchecked review when selecting another file', async () => {
  fixture(); page(); await selectFile()
  fireEvent.change(screen.getByLabelText('Source usage rights'), { target: { value: 'other' } })
  const detail = screen.getByLabelText('Source usage rights details')
  expect(detail.required).toBe(true); expect(detail.maxLength).toBe(240)
  fireEvent.change(screen.getByLabelText('Topic / domain'), { target: { value: 'DATA' } })
  fireEvent.change(screen.getByLabelText('Document language'), { target: { value: 'EN' } })
  fireEvent.click(screen.getByRole('checkbox'))
  fireEvent.submit(screen.getByRole('button', { name: 'Upload and process' }).closest('form'))
  expect(writes('/assets')).toHaveLength(0)
  await selectFile(new File(['Another original synthetic source'], 'another.txt', { type: 'text/plain' }))
  expect(screen.getByLabelText('Document title').value).toBe('another')
  expect(screen.getByLabelText('Source usage rights').value).toBe('')
  expect(screen.getByRole('checkbox').checked).toBe(false)
})

it('starts a distinct logical document after a staged replacement but retains same-file retry receipt', () => {
  const replacement = selectSource(file(), { replacementKey: 'published-key', license: 'Permission granted', track: 'DATA', language: 'EN' })
  const uploaded = { ...replacement, stagedJob: staged }
  const same = selectSource(file(), uploaded)
  expect(same.assetKey).toBe('published-key'); expect(same.stagedJob).toBe(staged)
  const next = selectSource(new File(['A different source'], 'second.txt', { type: 'text/plain' }), uploaded)
  expect(next.assetKey).not.toBe('published-key'); expect(next.replacementKey).toBeUndefined()
  expect(next.license).toBe(''); expect(next.language).toBe(''); expect(next.stagedJob).toBeUndefined()
  expect(next.requestId).not.toBe(uploaded.requestId)
})

it('uploads additional one-file documents into the same KB with independent titles and metadata', async () => {
  const savedJobs = [], savedVersions = [], inputs = []
  fixture((path, request) => {
    if (path.endsWith('/assets') && request.method === 'POST') return request.body.get('metadata').text().then(text => {
      const input = JSON.parse(text); inputs.push(input)
      const job = { ...staged, id: 'job-' + inputs.length, assetKey: input.assetKey, metadata: input }
      savedJobs.unshift(job); return job
    })
    if (path.endsWith('/retry') || path.endsWith('/reprocess')) {
      const job = savedJobs.find(item => path.includes('/' + item.id + '/'))
      job.state = 'READY'; job.producedVersionId = String(50 + inputs.length)
      savedVersions.unshift({ ...version, id: job.producedVersionId, assetKey: job.assetKey, title: job.metadata.title })
      return { ...job }
    }
    if (path.endsWith('/assets')) return savedVersions
    if (path.endsWith('/ingestions')) return savedJobs
  })
  page(); await selectFile(); fireEvent.submit(complete())
  await screen.findByRole('button', { name: 'Review and publish' })
  await selectFile(new File(['Another source'], 'Second source.txt', { type: 'text/plain', lastModified: 456 }))
  expect(screen.getByRole('checkbox').checked).toBe(false)
  fireEvent.submit(complete())
  await waitFor(() => expect(screen.getAllByRole('button', { name: 'Review and publish' })).toHaveLength(2))
  expect(inputs).toHaveLength(2); expect(inputs[0].assetKey).not.toBe(inputs[1].assetKey)
  expect(inputs.map(input => input.title)).toEqual(['Hướng dẫn nghề nghiệp', 'Second source'])
  expect(within(screen.getByLabelText('Document list')).getAllByRole('listitem')).toHaveLength(2)
  expect(writes('/retry')).toHaveLength(2); expect(writes('/reprocess')).toHaveLength(0)
})


it('localizes the controlled metadata choices into Vietnamese without assuming rights or review', async () => {
  fixture(); page('vi')
  fireEvent.change(await screen.findByLabelText(createTranslator('vi')('Source file')), { target: { files: [file()] } })
  const rights = screen.getByRole('combobox', { name: 'Quyền sử dụng nguồn' })
  expect(rights.value).toBe('')
  expect(within(rights).getByRole('option', { name: 'Tài liệu nội bộ / thuộc sở hữu tổ chức' })).toBeTruthy()
  expect(within(rights).getByRole('option', { name: 'Nguồn công khai hoặc chính thức' })).toBeTruthy()
  expect(within(rights).getByRole('option', { name: 'Đã được cấp phép sử dụng' })).toBeTruthy()
  expect(within(rights).getByRole('option', { name: 'Khác' })).toBeTruthy()
  expect(screen.getByLabelText('Lĩnh vực / chủ đề').required).toBe(true)
  expect(screen.getByRole('checkbox').checked).toBe(false)
})
