// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../../shared/api/httpClient.js'
import PublishAssignPanel from './PublishAssignPanel.jsx'

vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => vi.resetAllMocks())
afterEach(cleanup)
const agent = { id: 'agent-a', name: 'Career guide', purpose: 'Explore public career knowledge', status: 'ACTIVE', revision: 4, publishedConfigurationId: 'config-1' }
const selected = { id: 'config-2', versionNumber: 2, status: 'SAVED', contentHash: 'hash-2', knowledgeBaseIds: ['kb-a'], toolPolicy: { enabled: ['kb.search'] } }
const published = { ...selected, id: 'config-1', versionNumber: 1, status: 'PUBLISHED', contentHash: 'hash-1' }
const student = { id: '7', displayName: 'Synthetic Student' }
function fixture() {
  const state = { readiness: { configurationId: selected.id, configurationHash: selected.contentHash, agentRevision: 4, agentEditable: true, knowledgeAvailable: true, previewSucceeded: true, publishable: true }, students: { items: [student], nextAfterId: null }, assignments: [], failure: '', searchFailure: false }
  apiRequest.mockImplementation(async (path, request = {}) => {
    if (path.endsWith('/publication-readiness')) return state.readiness
    if (path.includes('/eligible-students?')) { if (state.searchFailure) throw new Error('Student search unavailable'); return state.students }
    if (path.endsWith('/assignments') && !request.method) return state.assignments
    if (request.method) {
      if (state.failure) throw new Error(state.failure)
      if (path.endsWith('/publish')) return { ...agent, revision: 5, publishedConfigurationId: selected.id }
      if (path.endsWith('/assignments')) { state.assignments = [{ id: 'assignment-a', studentUserId: student.id, studentDisplayName: student.displayName, status: 'ACTIVE', assignedAt: '2026-10-26T02:00:00Z' }]; return state.assignments[0] }
      if (path.endsWith('/revoke')) { state.assignments = [{ ...state.assignments[0], status: 'REVOKED' }]; return state.assignments[0] }
    }
    throw new Error(`Unexpected fixture route ${path}`)
  })
  return state
}
function page(overrides = {}) {
  const props = { active: true, agent, selected, published, dirty: false, execution: null, onChanged: vi.fn(), ...overrides }
  const view = render(<I18nProvider initialLocale="en"><PublishAssignPanel {...props} /></I18nProvider>)
  return { ...view, props, update: changes => { Object.assign(props, changes); view.rerender(<I18nProvider initialLocale="en"><PublishAssignPanel {...props} /></I18nProvider>) } }
}
const mutations = () => apiRequest.mock.calls.filter(([, request]) => request?.method)
async function ready() { await waitFor(() => expect(screen.getByRole('button', { name: 'Publish version 2' }).disabled).toBe(false)); await screen.findByLabelText('Eligible Student') }
function choose() { fireEvent.change(screen.getByLabelText('Eligible Student'), { target: { value: student.id } }) }
it('shows exact selected and published versions and authoritative readiness without lifecycle side effects', async () => {
  fixture(); page(); await ready()
  const publication = screen.getByRole('region', { name: 'Publication', exact: true })
  expect(publication.textContent).toContain('Version 2 · SAVED'); expect(publication.textContent).toContain('Version 1')
  expect(within(screen.getByRole('list', { name: 'Publication readiness' })).getAllByText('Satisfied')).toHaveLength(6)
  expect(screen.getByRole('region', { name: 'Student Experience Preview' })).toBeTruthy()
  expect(mutations()).toHaveLength(0)
})
it.each(['previewSucceeded', 'knowledgeAvailable', 'agentEditable', 'publishable'])('blocks publication when backend %s is false', async key => {
  const state = fixture(); state.readiness[key] = false; state.readiness.publishable = false; page()
  await screen.findByText('Choose a Student')
  expect(screen.getByRole('button', { name: 'Publish version 2' }).disabled).toBe(true)
  expect(screen.getAllByText('Blocked').length).toBeGreaterThan(0)
  expect(mutations()).toHaveLength(0)
})
it('blocks dirty or mismatched readiness and does not let a confirmation publish stale state', async () => {
  fixture(); const view = page(); await ready()
  fireEvent.click(screen.getByRole('button', { name: 'Publish version 2' }))
  expect(screen.getByRole('dialog', { name: 'Confirm publication' }).textContent).toContain('Career guide · Version 2')
  view.update({ dirty: true })
  fireEvent.click(screen.getByRole('button', { name: 'Confirm publish' }))
  expect(screen.getByText('Publication state changed. Refresh readiness before publishing.')).toBeTruthy()
  expect(screen.getByText('Unsaved changes must be saved and previewed before publication.')).toBeTruthy()
  expect(mutations()).toHaveLength(0)
  view.update({ dirty: false, selected: { ...selected, contentHash: 'different-hash' } })
  expect(screen.getByRole('button', { name: 'Publish version 2' }).disabled).toBe(true)
})
it('requires explicit publication confirmation, preserves If-Match and never assigns on publish', async () => {
  fixture(); const view = page(); await ready()
  fireEvent.click(screen.getByRole('button', { name: 'Publish version 2' }))
  fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel', exact: true }))
  expect(mutations()).toHaveLength(0)
  fireEvent.click(screen.getByRole('button', { name: 'Publish version 2' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm publish' }))
  await screen.findByText('Publication saved. No Students were assigned automatically.')
  expect(mutations()).toHaveLength(1)
  const [path, request] = mutations()[0]
  expect(path).toBe('/v2/admin/agents/agent-a/configurations/config-2/publish')
  expect(request.headers['If-Match']).toBe('4'); expect(JSON.parse(request.body)).toEqual({ confirmed: true })
  expect(view.props.onChanged).toHaveBeenCalledTimes(1)
})
it('does not fake publication success on backend failure or change the published version', async () => {
  const state = fixture(); state.failure = 'Successful preview of this exact configuration is required.'; page(); await ready()
  fireEvent.click(screen.getByRole('button', { name: 'Publish version 2' })); fireEvent.click(screen.getByRole('button', { name: 'Confirm publish' }))
  await screen.findByText(state.failure)
  expect(screen.queryByText('Publication saved. No Students were assigned automatically.')).toBeNull()
  expect(screen.getByRole('region', { name: 'Publication', exact: true }).textContent).toContain('Version 1')
})
it('searches with encoded name and bounded pagination and handles empty/error/retry states', async () => {
  const state = fixture(); page(); await ready()
  fireEvent.change(screen.getByLabelText('Search Students by name'), { target: { value: 'An & Bình' } })
  state.students = { items: [student], nextAfterId: '7' }
  fireEvent.click(screen.getByRole('button', { name: 'Search Students', exact: true }))
  await screen.findByRole('button', { name: 'Load more Students' })
  expect(apiRequest.mock.calls.some(([path]) => path.includes('q=An+%26+B%C3%ACnh&limit=20'))).toBe(true)
  state.students = { items: [{ id: '8', displayName: 'Other Student' }], nextAfterId: null }
  fireEvent.click(screen.getByRole('button', { name: 'Load more Students' }))
  await screen.findByRole('option', { name: 'Other Student · Account ID 8' })
  expect(screen.getByRole('option', { name: 'Synthetic Student · Account ID 7' })).toBeTruthy()
  expect(apiRequest.mock.calls.some(([path]) => path.endsWith('afterId=7'))).toBe(true)
  state.students = { items: [], nextAfterId: null }; fireEvent.click(screen.getByRole('button', { name: 'Search Students', exact: true }))
  await screen.findByText('No eligible Students match this search.')
  state.searchFailure = true; fireEvent.click(screen.getByRole('button', { name: 'Search Students', exact: true }))
  await screen.findByText('Student search unavailable')
  state.searchFailure = false; state.students = { items: [student], nextAfterId: null }; fireEvent.click(screen.getByRole('button', { name: 'Retry Student search' }))
  await screen.findByLabelText('Eligible Student'); expect(mutations()).toHaveLength(0)
})
it('shows picker loading and ignores obsolete searches after a newer search', async () => {
  fixture(); const original = apiRequest.getMockImplementation(); let resolveOld
  apiRequest.mockImplementation((path, request) => path.includes('/eligible-students?') ? new Promise(resolve => { resolveOld = resolve }) : original(path, request))
  const view = page(); await screen.findByText('Loading eligible Students...')
  view.update({ active: false }); view.update({ active: true })
  // Unmounting never lets a late candidate list reappear on another Agent.
  view.unmount(); await act(async () => resolveOld({ items: [student], nextAfterId: null }))
  expect(screen.queryByLabelText('Eligible Student')).toBeNull()
})
it('confirms assignment, displays identity/status, prevents duplicate and confirms idempotent revoke', async () => {
  fixture(); const view = page(); await ready(); choose()
  fireEvent.click(screen.getByRole('button', { name: 'Assign selected Student' }))
  expect(screen.getByRole('dialog').textContent).toContain('Career guide · Synthetic Student · Account ID 7')
  expect(mutations()).toHaveLength(0)
  fireEvent.click(screen.getByRole('button', { name: 'Confirm assignment' }))
  await screen.findByText('Assignment is active. No Student conversation was created.')
  expect(await screen.findByText('ACTIVE')).toBeTruthy(); expect(screen.getByText('Synthetic Student')).toBeTruthy()
  choose(); expect(screen.getByText('This Student already has an active assignment.')).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Assign selected Student' }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Revoke assignment' }))
  expect(screen.getByRole('dialog', { name: 'Confirm assignment revocation' })).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Confirm revoke' }))
  await screen.findByText('REVOKED'); expect(screen.queryByRole('button', { name: 'Revoke assignment' })).toBeNull()
  expect(mutations().map(([path]) => path)).toEqual(['/v2/admin/agents/agent-a/assignments', '/v2/admin/agents/agent-a/assignments/assignment-a/revoke'])
  expect(view.props.onChanged).toHaveBeenCalledTimes(2)
})
it.each(['Student is no longer eligible.', 'Duplicate active assignment.'])('shows assignment rejection safely: %s', async failure => {
  const state = fixture(); state.failure = failure; page(); await ready(); choose()
  fireEvent.click(screen.getByRole('button', { name: 'Assign selected Student' })); fireEvent.click(screen.getByRole('button', { name: 'Confirm assignment' }))
  await screen.findByText(failure)
  expect(screen.queryByText('Assignment is active. No Student conversation was created.')).toBeNull()
  expect(screen.queryByText('ACTIVE')).toBeNull()
})
it('renders only the selected exact successful Admin result in presentation preview with no runtime dispatch', async () => {
  fixture()
  const execution = { id: 'preview-a', executionMode: 'ADMIN_PREVIEW', configurationId: selected.id, configurationHash: selected.contentHash, status: 'SUCCEEDED', toolStepsAndSources: [], outputSnapshot: { outcome: 'ANSWER', text: 'Reviewed public answer.', citations: [{ referenceId: 'K1', quote: 'Exact source quotation.' }] } }
  const view = page({ execution }); await ready()
  const surface = screen.getByRole('region', { name: 'Student Experience Preview' })
  expect(within(surface).getByText('Reviewed public answer.')).toBeTruthy(); expect(surface.querySelector('q').textContent).toBe('Exact source quotation.')
  expect(within(surface).queryByRole('textbox')).toBeNull(); expect(within(surface).queryByRole('button', { name: 'Send' })).toBeNull()
  view.update({ execution: { ...execution, configurationHash: 'wrong' } })
  expect(within(surface).queryByText('Reviewed public answer.')).toBeNull()
  expect(within(surface).getByText(/No successful answer for this exact version/)).toBeTruthy()
  expect(mutations()).toHaveLength(0)
  expect(apiRequest.mock.calls.every(([path]) => path.startsWith('/v2/admin/agents/'))).toBe(true)
})
it('shows readiness and assignment read errors with explicit reload and never uses a failed result as success', async () => {
  fixture(); const original = apiRequest.getMockImplementation(); let fail = true
  apiRequest.mockImplementation((path, request) => fail && (path.endsWith('/assignments') || path.endsWith('/publication-readiness')) ? Promise.reject(new Error('Status read unavailable')) : original(path, request))
  page({ execution: { status: 'FAILED', errorCode: 'PROVIDER_UNAVAILABLE' } })
  await screen.findAllByText('Status read unavailable')
  expect(screen.getByRole('button', { name: 'Publish version 2' }).disabled).toBe(true)
  expect(screen.queryByRole('region', { name: 'Example Agent answer' })).toBeNull()
  fail = false; fireEvent.click(screen.getByRole('button', { name: 'Refresh status' })); await ready()
})
