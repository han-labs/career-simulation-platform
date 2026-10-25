// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { draftKey } from '../../../shared/api/coreApi.js'
import AgentBuilderPage from './AgentBuilderPage.jsx'
import { emptyConfiguration, validateConfiguration } from './agentConfiguration.js'

vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(() => { cleanup(); vi.restoreAllMocks() })
const options = {
  models: [{ key: 'gemini/allowed', available: true }, { key: 'openai/allowed', available: false }],
  tools: ['kb.search', 'assessment.lookup', 'simulation.explore', 'simulation.myEvidence'].map(code => ({ code })),
  promptTemplateRef: 'existing-template@hash', defaultToolPolicy: { enabled: [], maxRounds: 2, maxToolCalls: 8, topK: 5, timeoutSeconds: 30 },
  defaultMemoryPolicy: { enabled: false }, defaultOutputPolicy: { schemaVersion: 1, grounding: 'KB_ONLY', maxCharacters: 4000 },
}
const bases = [{ id: 'kb-a', name: 'Reviewed guide', status: 'ACTIVE' }, { id: 'kb-b', name: 'Other guide', status: 'ACTIVE' }]
const passage = { referenceId: 'K1', chunkId: 'chunk-1', versionId: 'source-v1', knowledgeBaseId: 'kb-a', title: 'Reviewed source', text: 'Exact reviewed quotation — tiếng Việt.', section: 'Introduction', retrievalMethod: 'LEXICAL', sourceLocator: { lineStart: 1 } }
function fixture() {
  const state = { owner: '5', agents: ['a', 'b'].map(id => ({ id, name: `Agent ${id.toUpperCase()}`, purpose: 'Career exploration', status: 'DRAFT', revision: 1, publishedConfigurationId: null })), versions: {}, runs: [], failSave: false, failPostOnce: false, nextRunStatus: 'SUCCEEDED', executionReadError: false }
  for (const agent of state.agents) state.versions[agent.id] = [{ ...emptyConfiguration(options), id: `${agent.id}-v1`, agentId: agent.id, versionNumber: 1, contentHash: `${agent.id}-hash1`, status: 'SAVED', promptTemplate: `Original instructions ${agent.id}` }]
  apiRequest.mockImplementation(async (path, request = {}) => {
    if (path === '/v2/me') return { id: state.owner, role: 'ADMIN' }
    if (path === '/v2/admin/agent-platform/options') return options
    if (path === '/v2/admin/knowledge-bases') return bases
    if (path === '/v2/admin/agents') {
      if (!request.method) return state.agents
      const agent = { ...JSON.parse(request.body), id: 'c', status: 'DRAFT', revision: 0 }; state.agents.push(agent); state.versions.c = []; return agent
    }
    if (path.startsWith('/v2/admin/agent-executions/')) {
      if (state.executionReadError && !request.method) throw new Error('Temporary execution read failure')
      const run = state.runs.find(r => path.includes(r.id))
      if (path.endsWith('/cancel')) run.status = 'CANCELLED'
      return run
    }
    const agent = state.agents.find(a => path.startsWith(`/v2/admin/agents/${a.id}`))
    if (!agent) throw new Error(`Unexpected fixture route ${path}`)
    if (path === `/v2/admin/agents/${agent.id}` && request.method === 'PATCH') {
      if (request.headers['If-Match'] !== String(agent.revision)) throw Object.assign(new Error('Conflict'), { status: 409 })
      Object.assign(agent, JSON.parse(request.body), { revision: agent.revision + 1 }); return { ...agent }
    }
    if (path.endsWith('/assignments')) return []
    if (path.endsWith('/configurations')) {
      if (!request.method) return state.versions[agent.id]
      if (state.failSave || request.headers['If-Match'] !== String(agent.revision)) throw Object.assign(new Error('Conflict'), { status: 409 })
      const n = state.versions[agent.id].length + 1, version = { ...JSON.parse(request.body), id: `${agent.id}-v${n}`, agentId: agent.id, versionNumber: n, status: 'SAVED', contentHash: `${agent.id}-hash${n}` }
      state.versions[agent.id] = [version, ...state.versions[agent.id]]; agent.revision += 1; return version
    }
    if (path.endsWith('/previews')) {
      const body = JSON.parse(request.body), version = state.versions[agent.id].find(v => path.includes(`/${v.id}/`))
      let run = state.runs.find(r => r.requestId === body.requestId)
      if (!run) {
        run = { id: `run-${state.runs.length + 1}`, requestId: body.requestId, configurationId: version.id, configurationHash: version.contentHash, executionMode: 'ADMIN_PREVIEW', status: state.nextRunStatus, errorCode: state.nextRunStatus === 'FAILED' ? 'PROVIDER_UNAVAILABLE' : null, toolStepsAndSources: [{ type: 'ACTUAL_NATIVE_CALL', name: 'kb.search', status: 'SUCCEEDED', sources: [passage], privateReasoning: 'DO_NOT_RENDER_PRIVATE_REASONING' }], outputSnapshot: { outcome: 'ANSWER', text: 'Validated preview answer.', citations: [{ referenceId: 'K1', quote: passage.text }] }, inputTokens: null, outputTokens: null, estimatedCost: null, costStatus: 'UNKNOWN', latencyMs: 22, pricingSnapshot: { privateProtocol: 'DO_NOT_RENDER_PROVIDER_PROTOCOL' } }
        state.runs.push(run)
      }
      if (state.failPostOnce) { state.failPostOnce = false; throw new Error('Acceptance response lost') }
      return { id: run.id, status: 'ACCEPTED' }
    }
    return agent
  })
  return state
}
function page(url = '/admin/agents?agent=a', locale = 'en') { return render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={[url]}><AgentBuilderPage /></MemoryRouter></I18nProvider>) }
async function loaded() { await screen.findByLabelText('Saved Agent instructions') }
function pane(name) { fireEvent.click(within(screen.getByRole('group', { name: 'Agent workspace view' })).getByRole('button', { name, exact: true })) }
function tab(name) { pane('Configuration'); fireEvent.click(screen.getByRole('tab', { name, exact: true })) }
function publication() { const summary = screen.getByText('Publication and assignments'); if (!summary.closest('details').open) fireEvent.click(summary) }
function input(label, value) { fireEvent.change(screen.getByLabelText(label), { target: { value: String(value) } }) }
function postCalls(suffix) { return apiRequest.mock.calls.filter(([path, request]) => path.endsWith(suffix) && request?.method === 'POST') }
async function preview() {
  pane('Preview')
  input('Preview question', 'Explain the authorized source')
  fireEvent.click(screen.getByRole('button', { name: 'Send' }))
  await screen.findByRole('heading', { name: 'Preview result' })
  await screen.findByText('Validated preview answer.')
}

it('creates an Agent without invoking a model, publishing or assigning, then selects it', async () => {
  const state = fixture(); page('/admin/agents'); await screen.findByRole('button', { name: 'Agent A · DRAFT' })
  expect(screen.queryByLabelText('New Agent name')).toBeNull()
  expect(screen.queryByLabelText('Description / purpose')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: '+ Create Agent' }))
  expect(screen.getByRole('dialog', { name: 'Create Agent' })).toBeTruthy()
  input('Name', 'New guide'); input('Description / purpose', 'Explore careers safely')
  fireEvent.click(screen.getByRole('button', { name: 'Create Agent' }))
  expect(await screen.findByRole('heading', { name: 'New guide' })).toBeTruthy()
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(screen.getByRole('tab', { name: 'Brain' }).getAttribute('aria-selected')).toBe('true')
  expect(state.versions.c).toEqual([]); expect(state.runs).toHaveLength(0)
  expect(postCalls('/assignments')).toHaveLength(0); expect(postCalls('/publish')).toHaveLength(0)
  fireEvent.click(screen.getByRole('button', { name: 'Agent A · DRAFT' }))
  expect(await screen.findByDisplayValue('Original instructions a')).toBeTruthy()
})
it('cancels modal creation without mutation and restores focus to its trigger', async () => {
  fixture(); page(); await loaded()
  const trigger = screen.getByRole('button', { name: '+ Create Agent' }); trigger.focus(); fireEvent.click(trigger)
  const dialog = screen.getByRole('dialog', { name: 'Create Agent' })
  expect(document.activeElement).toBe(within(dialog).getByLabelText('Name'))
  expect(within(dialog).getByLabelText('Name').required).toBe(true)
  expect(within(dialog).getByLabelText('Description / purpose').required).toBe(true)
  input('Name', 'Cancelled draft')
  fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel', exact: true }))
  expect(screen.queryByRole('dialog')).toBeNull(); expect(document.activeElement).toBe(trigger)
  expect(postCalls('/agents')).toHaveLength(0)
  fireEvent.click(trigger)
  expect(screen.getByLabelText('Name').value).toBe('')
  fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })
  expect(screen.queryByRole('dialog')).toBeNull()
})
it('keeps creation failures inside the modal and allows a safe retry', async () => {
  fixture(); const original = apiRequest.getMockImplementation(); let fail = true
  apiRequest.mockImplementation((path, request) => path === '/v2/admin/agents' && request?.method === 'POST' && fail ? Promise.reject(new Error('Creation unavailable')) : original(path, request))
  page(); await loaded(); fireEvent.click(screen.getByRole('button', { name: '+ Create Agent' }))
  input('Name', 'Retry guide'); input('Description / purpose', 'Career exploration')
  fireEvent.click(screen.getByRole('button', { name: 'Create Agent', exact: true }))
  await screen.findByText('Creation unavailable')
  expect(screen.getByLabelText('Name').value).toBe('Retry guide'); expect(screen.getByRole('dialog')).toBeTruthy()
  fail = false; fireEvent.click(screen.getByRole('button', { name: 'Create Agent', exact: true }))
  await screen.findByRole('heading', { name: 'Retry guide' }); expect(screen.queryByRole('dialog')).toBeNull()
})
it('validates creation fields inline before sending a mutation', async () => {
  fixture(); page(); await loaded(); fireEvent.click(screen.getByRole('button', { name: '+ Create Agent' }))
  input('Name', '   ')
  fireEvent.click(screen.getByRole('button', { name: 'Create Agent', exact: true }))
  expect(screen.getByLabelText('Name').getAttribute('aria-invalid')).toBe('true')
  expect(screen.getByText('Enter an Agent name.')).toBeTruthy()
  expect(screen.getByText('Enter an Agent purpose.')).toBeTruthy()
  expect(document.activeElement).toBe(screen.getByLabelText('Name'))
  expect(postCalls('/agents')).toHaveLength(0)
  input('Name', 'Valid name'); fireEvent.click(screen.getByRole('button', { name: 'Create Agent', exact: true }))
  expect(document.activeElement).toBe(screen.getByLabelText('Description / purpose'))
})
it('keeps Preview mounted alongside all three configuration tabs with one workspace Save and Discard', async () => {
  const state = fixture(); page(); await loaded(); await preview()
  const chat = screen.getByRole('region', { name: 'Admin Preview chat' })
  expect(screen.queryByRole('tab', { name: 'Chat', exact: true })).toBeNull()
  const response = within(chat).getByRole('article', { name: 'Agent response' })
  expect(response.querySelector('q').textContent).toBe(passage.text)
  expect(within(response).getByText('View run details').closest('details').open).toBe(false)
  input('Preview question', 'Keep composer input across settings')
  for (const name of ['Integrations', 'Advanced', 'Brain']) {
    tab(name)
    expect(screen.getByRole('region', { name: 'Admin Preview chat' })).toBe(chat)
    expect(screen.getByLabelText('Preview question').value).toBe('Keep composer input across settings')
    expect(screen.getAllByRole('button', { name: 'Save changes' })).toHaveLength(1)
    expect(screen.getByRole('button', { name: 'Save changes' }).closest('.agent-workspace-header')).toBeTruthy()
    expect(screen.getAllByRole('button', { name: 'Discard unsaved changes' })).toHaveLength(1)
    expect(within(chat).getByText('Validated preview answer.')).toBeTruthy()
  }
  expect(state.runs).toHaveLength(1); expect(postCalls('/previews')).toHaveLength(1)
  expect(screen.getByText('Publication and assignments').closest('[role="tabpanel"]')).toBeNull()
})
it('provides responsive pane controls without unmounting the draft or accepted Preview', async () => {
  fixture(); page(); await loaded(); await preview(); tab('Brain')
  input('Saved Agent instructions', 'Unsaved responsive draft')
  const chat = screen.getByRole('region', { name: 'Admin Preview chat' })
  const switcher = screen.getByRole('group', { name: 'Agent workspace view' })
  const previewSwitch = within(switcher).getByRole('button', { name: 'Preview', exact: true })
  const configurationSwitch = within(switcher).getByRole('button', { name: 'Configuration', exact: true })
  expect(document.getElementById(previewSwitch.getAttribute('aria-controls')).contains(chat)).toBe(true)
  pane('Preview')
  expect(previewSwitch.getAttribute('aria-pressed')).toBe('true')
  expect(document.getElementById(configurationSwitch.getAttribute('aria-controls')).classList.contains('agent-pane--inactive')).toBe(true)
  expect(within(chat).getByText('Unsaved changes are not used in Preview. Save the configuration to test the new settings.')).toBeTruthy()
  expect(within(chat).getByRole('button', { name: 'Send' }).disabled).toBe(true)
  pane('Configuration')
  expect(configurationSwitch.getAttribute('aria-pressed')).toBe('true')
  expect(screen.getByLabelText('Saved Agent instructions').value).toBe('Unsaved responsive draft')
  expect(screen.getByRole('region', { name: 'Admin Preview chat' })).toBe(chat)
  expect(postCalls('/previews')).toHaveLength(1)
})
it('does not attach an old answer or citations to a newly submitted Preview question', async () => {
  fixture(); page(); await loaded(); await preview()
  const original = apiRequest.getMockImplementation(); let rejectPreview
  apiRequest.mockImplementation((path, request) => path.endsWith('/previews') && request?.method === 'POST' ? new Promise((resolve, reject) => { rejectPreview = reject }) : original(path, request))
  input('Preview question', 'Different independent question')
  fireEvent.click(screen.getByRole('button', { name: 'Send' }))
  expect(screen.getByText('Sending Preview...')).toBeTruthy()
  expect(screen.queryByText('Validated preview answer.')).toBeNull()
  await act(async () => rejectPreview(new Error('Preview acceptance unavailable')))
  expect(screen.getByRole('article', { name: 'Agent response' }).textContent).toContain('Preview acceptance unavailable')
  expect(screen.queryByText('Validated preview answer.')).toBeNull()
  expect(screen.getByRole('button', { name: 'Send' }).disabled).toBe(false)
})
it('organizes controls into three accessible panels and preserves edits while changing tabs', async () => {
  fixture(); page(); await loaded()
  expect(screen.getAllByRole('tab').map(node => node.textContent)).toEqual(['Brain', 'Integrations', 'Advanced'])
  expect(screen.getAllByRole('tabpanel')).toHaveLength(1)
  expect(within(screen.getByRole('tabpanel', { name: 'Brain' })).getByLabelText('Agent name')).toBeTruthy()
  input('Saved Agent instructions', 'Preserve between tabs')
  tab('Integrations')
  expect(screen.getByRole('group', { name: 'Knowledge Bases' })).toBeTruthy()
  expect(screen.getByRole('group', { name: 'Domain tool grants' })).toBeTruthy()
  fireEvent.click(screen.getByRole('checkbox', { name: 'simulation.explore', exact: true }))
  tab('Advanced'); input('Model rounds', 3)
  fireEvent.click(screen.getByRole('checkbox', { name: 'Enable bounded Student context' }))
  pane('Preview'); expect(screen.getByRole('region', { name: 'Admin Preview chat' })).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Send' }).disabled).toBe(true)
  tab('Brain'); expect(screen.getByLabelText('Saved Agent instructions').value).toBe('Preserve between tabs')
  tab('Integrations'); expect(screen.getByRole('checkbox', { name: 'simulation.explore', exact: true }).checked).toBe(true)
  tab('Advanced'); expect(screen.getByLabelText('Model rounds').value).toBe('3')
  expect(screen.getByRole('checkbox', { name: 'Enable bounded Student context' }).checked).toBe(true)
  expect(postCalls('/configurations')).toHaveLength(0); expect(postCalls('/previews')).toHaveLength(0)
  const brain = screen.getByRole('tab', { name: 'Brain' })
  brain.focus(); fireEvent.keyDown(brain, { key: 'ArrowRight' })
  expect(document.activeElement).toBe(screen.getByRole('tab', { name: 'Integrations' }))
  expect(screen.getByRole('tab', { name: 'Advanced' }).getAttribute('aria-selected')).toBe('true')
  fireEvent.keyDown(document.activeElement, { key: 'Enter' })
  expect(screen.getByRole('tab', { name: 'Integrations' }).getAttribute('aria-selected')).toBe('true')
})
it('saves edited identity with the existing metadata contract before saving the immutable configuration', async () => {
  const state = fixture(); page(); await loaded(); input('Agent name', 'Updated identity'); input('Agent description / purpose', 'Updated purpose')
  input('Saved Agent instructions', 'New behavior'); pane('Preview')
  expect(screen.getByRole('button', { name: 'Send' }).disabled).toBe(true)
  tab('Brain'); fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  await screen.findByText('A new immutable version was saved. Nothing was published or assigned; no provider was called.')
  const mutations = apiRequest.mock.calls.filter(([, request]) => request?.method)
  expect(mutations.map(([path, request]) => [path, request.method, request.headers['If-Match']])).toEqual([
    ['/v2/admin/agents/a', 'PATCH', '1'], ['/v2/admin/agents/a/configurations', 'POST', '2'],
  ])
  expect(state.agents[0].name).toBe('Updated identity'); expect(state.versions.a[0].promptTemplate).toBe('New behavior')
  expect(state.versions.a[1].promptTemplate).toBe('Original instructions a'); expect(state.runs).toHaveLength(0)
  await preview(); expect(postCalls('/previews')[0][0]).toBe('/v2/admin/agents/a/configurations/a-v2/previews')
})
it('restores identity and configuration edits together without changing the saved version', async () => {
  fixture(); page(); await loaded(); input('Agent name', 'Unsaved identity'); input('Saved Agent instructions', 'Unsaved behavior')
  fireEvent.click(screen.getByRole('button', { name: 'Discard unsaved changes' }))
  fireEvent.click(screen.getByRole('button', { name: 'Discard and restore' }))
  expect(screen.getByLabelText('Agent name').value).toBe('Agent A')
  expect(screen.getByLabelText('Saved Agent instructions').value).toBe('Original instructions a')
  expect(apiRequest.mock.calls.filter(([, request]) => request?.method)).toHaveLength(0)
})
it('reveals and focuses an invalid policy in Advanced when Save is submitted from Brain', async () => {
  fixture(); page(); await loaded(); tab('Advanced'); input('Maximum retrieved passages', 11); tab('Brain')
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  expect(screen.getByRole('tab', { name: 'Advanced' }).getAttribute('aria-selected')).toBe('true')
  expect(document.activeElement).toBe(screen.getByLabelText('Maximum retrieved passages'))
  expect(postCalls('/configurations')).toHaveLength(0)
})
it('maps supported form controls exactly to the immutable API and Save has no execution side effects', async () => {
  const state = fixture(); page(); await loaded()
  expect(screen.getByRole('option', { name: /openai\/allowed.*Unavailable/ })).toBeTruthy()
  input('Supported model', 'openai/allowed'); input('Saved Agent instructions', 'Reply in Vietnamese using authorized evidence.')
  tab('Advanced')
  for (const [label, value] of [['Model rounds', 4], ['Maximum tool calls', 3], ['Maximum retrieved passages', 8], ['Execution timeout (seconds)', 45], ['Maximum answer characters', 1200]]) input(label, value)
  tab('Integrations')
  fireEvent.click(screen.getByRole('checkbox', { name: /Reviewed guide/ }))
  for (const code of options.tools.map(t => t.code)) fireEvent.click(screen.getByRole('checkbox', { name: code, exact: true }))
  tab('Advanced')
  fireEvent.click(screen.getByRole('checkbox', { name: 'Enable bounded Student context' }))
  input('Grounding policy', 'GENERAL_ALLOWED')
  expect(screen.getByText('Unsaved changes — Save a new version before Preview.')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  await screen.findByText('A new immutable version was saved. Nothing was published or assigned; no provider was called.')
  const [, request] = postCalls('/configurations')[0]
  expect(request.headers['If-Match']).toBe('1')
  expect(JSON.parse(request.body)).toEqual({ modelKey: 'openai/allowed', promptTemplate: 'Reply in Vietnamese using authorized evidence.', promptTemplateRef: options.promptTemplateRef, toolPolicy: { enabled: options.tools.map(t => t.code), maxRounds: 4, maxToolCalls: 3, topK: 8, timeoutSeconds: 45 }, memoryPolicy: { enabled: true }, outputPolicy: { schemaVersion: 1, grounding: 'GENERAL_ALLOWED', maxCharacters: 1200 }, knowledgeBaseIds: ['kb-a'] })
  expect(state.versions.a[1].promptTemplate).toBe('Original instructions a')
  expect(state.versions.a[0].id).toBe('a-v2'); expect(state.runs).toHaveLength(0)
  expect(postCalls('/previews')).toHaveLength(0); expect(postCalls('/publish')).toHaveLength(0); expect(postCalls('/assignments')).toHaveLength(0)
})
it('rejects invalid policy bounds, retains input and warns about one-round tool configurations', async () => {
  fixture(); page(); await loaded()
  tab('Integrations')
  fireEvent.click(screen.getByRole('checkbox', { name: 'kb.search', exact: true }))
  tab('Advanced')
  input('Model rounds', 1)
  expect(screen.getByText(/With rounds set to 1, tools cannot run/)).toBeTruthy()
  input('Maximum tool calls', 0); input('Maximum retrieved passages', 11)
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  expect(screen.getByLabelText('Maximum retrieved passages').getAttribute('aria-invalid')).toBe('true')
  expect(screen.getByLabelText('Maximum tool calls').getAttribute('aria-invalid')).toBe('true')
  expect(postCalls('/configurations')).toHaveLength(0)
  expect(screen.getByLabelText('Maximum retrieved passages').value).toBe('11')
})
it('requires confirmation to discard and restores the saved configuration', async () => {
  fixture(); page(); await loaded(); input('Saved Agent instructions', 'Unsaved changes')
  fireEvent.click(screen.getByRole('button', { name: 'Discard unsaved changes' }))
  expect(screen.getByRole('dialog')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Discard and restore' }))
  expect(screen.getByLabelText('Saved Agent instructions').value).toBe('Original instructions a')
  expect(screen.queryByRole('dialog')).toBeNull(); expect(postCalls('/configurations')).toHaveLength(0)
})
it('preserves draft and stale revision on conflict until the Admin explicitly reloads', async () => {
  const state = fixture(); page(); await loaded(); input('Saved Agent instructions', 'Keep these unsaved instructions')
  state.agents[0].revision = 9
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  await screen.findByText(/This Agent has changed/)
  expect(screen.getByLabelText('Saved Agent instructions').value).toBe('Keep these unsaved instructions')
  expect(state.versions.a).toHaveLength(1)
  expect(screen.getByRole('button', { name: 'Save changes' }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Reload latest Agent' }))
  await screen.findByText('Latest Agent revision loaded. Your unsaved input is preserved.')
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  await waitFor(() => expect(state.versions.a).toHaveLength(2))
  expect(postCalls('/configurations')[1][1].headers['If-Match']).toBe('9')
})
it('previews only the selected saved version and renders exact citations and safe observable details', async () => {
  const state = fixture(); page(); await loaded(); await preview()
  expect(postCalls('/previews')[0][0]).toBe('/v2/admin/agents/a/configurations/a-v1/previews')
  expect(JSON.parse(postCalls('/previews')[0][1].body)).toEqual({ requestId: expect.any(String), query: 'Explain the authorized source' })
  expect(screen.getAllByText(passage.text).length).toBeGreaterThan(0)
  const details = screen.getByText('View run details').closest('details')
  expect(details.open).toBe(false); fireEvent.click(screen.getByText('View run details'))
  expect(details.textContent).toContain('a-hash1'); expect(details.textContent).toContain('a-v1')
  expect(document.body.textContent).not.toContain('DO_NOT_RENDER_PRIVATE_REASONING')
  expect(document.body.textContent).not.toContain('DO_NOT_RENDER_PROVIDER_PROTOCOL')
  tab('Brain'); input('Saved Agent instructions', 'Revised config requires another Save'); pane('Preview')
  expect(screen.getByRole('button', { name: 'Send' }).disabled).toBe(true)
  publication()
  expect(screen.getByRole('button', { name: 'Publish this tested configuration' }).disabled).toBe(true)
  expect(state.runs).toHaveLength(1)
})
it('reuses an acceptance request ID after a lost POST response without creating duplicate executions', async () => {
  const state = fixture(); state.failPostOnce = true; page(); await loaded(); pane('Preview')
  input('Preview question', 'Retry acceptance safely')
  fireEvent.click(screen.getByRole('button', { name: 'Send' }))
  await screen.findByText('Acceptance response lost')
  fireEvent.click(screen.getByRole('button', { name: 'Send' }))
  await screen.findByText('Validated preview answer.')
  expect(postCalls('/previews')).toHaveLength(2)
  expect(postCalls('/previews')[0][1].body).toBe(postCalls('/previews')[1][1].body)
  expect(state.runs).toHaveLength(1)
})
it('reloads an accepted execution on read failure instead of dispatching again', async () => {
  const state = fixture(); state.executionReadError = true; page(); await loaded(); pane('Preview'); input('Preview question', 'Question')
  fireEvent.click(screen.getByRole('button', { name: 'Send' }))
  await screen.findByText('Temporary execution read failure')
  expect(screen.getByRole('button', { name: 'Send' }).disabled).toBe(true)
  state.executionReadError = false; fireEvent.click(screen.getByRole('button', { name: 'Reload saved execution' }))
  await screen.findByText('Validated preview answer.'); expect(state.runs).toHaveLength(1)
})
it('allows explicit retry of a failed preview as a fresh execution', async () => {
  const state = fixture(); state.nextRunStatus = 'FAILED'; page(); await loaded(); await preview()
  expect(screen.getByText('PROVIDER_UNAVAILABLE')).toBeTruthy()
  const chat = screen.getByRole('region', { name: 'Admin Preview chat' })
  expect(within(chat).getByRole('article', { name: 'Admin message' }).textContent).toContain('Explain the authorized source')
  expect(within(chat).getByRole('article', { name: 'Agent response' }).textContent).toContain('PROVIDER_UNAVAILABLE')
  expect(within(chat).getByLabelText('Preview question').disabled).toBe(false)
  expect(within(chat).getByText('View run details').closest('details').open).toBe(false)
  state.nextRunStatus = 'SUCCEEDED'; fireEvent.click(screen.getByRole('button', { name: 'Retry preview' }))
  await waitFor(() => expect(state.runs).toHaveLength(2))
  expect(state.runs[0].requestId).not.toBe(state.runs[1].requestId)
})
it('cancels an accepted running preview using the existing execution endpoint', async () => {
  const state = fixture(); state.nextRunStatus = 'RUNNING'; page(); await loaded(); await preview()
  expect(screen.getByRole('region', { name: 'Persisted execution result' }).getAttribute('aria-busy')).toBe('true')
  expect(screen.getByLabelText('Preview question').disabled).toBe(true)
  expect(screen.getByRole('button', { name: 'Send' }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Cancel execution' }))
  await screen.findByText('Cancelled'); expect(state.runs[0].status).toBe('CANCELLED')
  expect(postCalls('/cancel')).toHaveLength(1); expect(postCalls('/previews')).toHaveLength(1)
})
it('restores only the current owner/Agent draft and ignores late responses after switching Agents', async () => {
  const state = fixture(); const view = page(); await loaded(); input('Saved Agent instructions', 'Owner 5 private draft A')
  fireEvent.click(screen.getByRole('button', { name: 'Agent B · DRAFT' }))
  await screen.findByDisplayValue('Original instructions b')
  expect(document.body.textContent).not.toContain('Owner 5 private draft A')
  fireEvent.click(screen.getByRole('button', { name: 'Agent A · DRAFT' }))
  await screen.findByDisplayValue('Owner 5 private draft A')
  view.unmount(); state.owner = '6'; page(); await screen.findByDisplayValue('Original instructions a')
  expect(screen.getByLabelText('Saved Agent instructions').value).not.toBe('Owner 5 private draft A')
  expect(readDraftValue('5', 'a').draft.promptTemplate).toBe('Owner 5 private draft A')
})
it('never carries a previous Agent conversation or identity edit into the next Agent', async () => {
  fixture(); page(); await loaded(); await preview(); tab('Brain'); input('Agent name', 'Unsaved A identity')
  fireEvent.click(screen.getByRole('button', { name: 'Agent B · DRAFT' }))
  await screen.findByDisplayValue('Original instructions b')
  expect(screen.getByRole('tab', { name: 'Brain' }).getAttribute('aria-selected')).toBe('true')
  expect(screen.getByLabelText('Agent name').value).toBe('Agent B')
  pane('Preview'); expect(screen.queryByRole('article', { name: 'Admin message' })).toBeNull()
  expect(screen.queryByText('Validated preview answer.')).toBeNull()
  expect(screen.getByLabelText('Preview question').value).toBe('')
})
function readDraftValue(owner, id) { return JSON.parse(localStorage.getItem(draftKey(owner, 'agent-config', id))) }
it('ignores an obsolete save response after switching the selected Agent', async () => {
  fixture(); const original = apiRequest.getMockImplementation(); let resolveSave
  apiRequest.mockImplementation((path, request) => path.endsWith('/configurations') && request?.method === 'POST' ? new Promise(resolve => { resolveSave = resolve }) : original(path, request))
  page(); await loaded(); input('Saved Agent instructions', 'A pending save')
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  fireEvent.click(screen.getByRole('button', { name: 'Agent B · DRAFT' }))
  await screen.findByDisplayValue('Original instructions b')
  await act(async () => resolveSave({ ...emptyConfiguration(options), id: 'a-v2', agentId: 'a', versionNumber: 2, status: 'SAVED', contentHash: 'late-hash', promptTemplate: 'A pending save' }))
  expect(screen.getByLabelText('Saved Agent instructions').value).toBe('Original instructions b')
  expect(document.body.textContent).not.toContain('late-hash')
})
it('shows the same supported controls in Vietnamese', async () => {
  fixture(); page('/admin/agents?agent=a', 'vi')
  expect(await screen.findByLabelText('Số vòng mô hình')).toBeTruthy()
  fireEvent.click(screen.getByRole('tab', { name: 'Nâng cao' }))
  expect(screen.getByRole('checkbox', { name: 'Bật ngữ cảnh sinh viên có giới hạn' })).toBeTruthy()
  fireEvent.click(screen.getByRole('tab', { name: 'Bộ não' })); expect(screen.getByText('An toàn').closest('details').open).toBe(false)
})
it('blocks repeated Save submission while the first immutable version request is pending', async () => {
  const state = fixture(), original = apiRequest.getMockImplementation(); let complete
  apiRequest.mockImplementation((path, request) => path.endsWith('/configurations') && request?.method === 'POST' ? new Promise(resolve => { complete = () => resolve(original(path, request)) }) : original(path, request))
  page(); await loaded(); input('Saved Agent instructions', 'Save exactly once')
  const form = screen.getByRole('button', { name: 'Save changes' }).closest('form')
  fireEvent.submit(form); fireEvent.submit(form)
  expect(postCalls('/configurations')).toHaveLength(1)
  expect(screen.getByRole('button', { name: 'Save changes' }).disabled).toBe(true)
  await act(async () => complete())
  await waitFor(() => expect(state.versions.a).toHaveLength(2))
})
it('restores saved version selection and reads an accepted preview after reload without replaying the provider', async () => {
  fixture(); const view = page(); await loaded(); await preview()
  view.unmount(); page(); await loaded(); pane('Preview'); await screen.findByText('Validated preview answer.')
  expect(postCalls('/previews')).toHaveLength(1)
  expect(screen.getByLabelText('Saved Agent instructions').value).toBe('Original instructions a')
})
it('explicitly previews an older selected version rather than the newest version', async () => {
  const state = fixture()
  state.versions.a.unshift({ ...state.versions.a[0], id: 'a-v2', versionNumber: 2, contentHash: 'a-hash2', promptTemplate: 'Newer instructions' })
  page(); await screen.findByDisplayValue('Newer instructions')
  input('Selected saved version', 'a-v1')
  await screen.findByDisplayValue('Original instructions a'); await preview()
  expect(postCalls('/previews')[0][0]).toBe('/v2/admin/agents/a/configurations/a-v1/previews')
  expect(state.versions.a[0].promptTemplate).toBe('Newer instructions')
})
it.each([
  ['maxRounds', 0], ['maxRounds', 5], ['maxToolCalls', -1], ['maxToolCalls', 9], ['topK', 0], ['topK', 11], ['timeoutSeconds', 4], ['timeoutSeconds', 61], ['maxCharacters', 99], ['maxCharacters', 4001], ['topK', 1.5],
])('rejects unsupported %s value %s before Save', (key, value) => {
  const draft = emptyConfiguration(options); draft.promptTemplate = 'Reviewed instructions'
  if (key === 'maxCharacters') draft.outputPolicy[key] = value; else draft.toolPolicy[key] = value
  expect(validateConfiguration(draft, options, bases)[key]).toBeTruthy()
})
it('does not infer KB/tool permissions or silently replace unavailable grants', () => {
  const draft = emptyConfiguration(options); draft.promptTemplate = 'Reviewed instructions'
  expect(draft.knowledgeBaseIds).toEqual([]); expect(draft.toolPolicy.enabled).toEqual([])
  draft.knowledgeBaseIds = ['missing']; draft.toolPolicy.enabled = ['unimplemented.tool']
  const errors = validateConfiguration(draft, options, bases)
  expect(errors.knowledgeBaseIds).toBeTruthy(); expect(errors.tools).toBeTruthy()
})
