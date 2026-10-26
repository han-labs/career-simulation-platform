// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { draftKey } from '../../../shared/api/coreApi.js'
import KnowledgeStudioPage from '../knowledge/KnowledgeStudioPage.jsx'
import AgentBuilderPage from '../agents/AgentBuilderPage.jsx'
import AssignedAgentsPage from '../../agents/AssignedAgentsPage.jsx'
vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(cleanup)
const model = 'gemini/synthetic', options = { models: [{ key: model, available: true }], tools: [{ code: 'kb.search' }], promptTemplateRef: 'synthetic-template@hash', defaultToolPolicy: { enabled: [], maxRounds: 2, maxToolCalls: 8, topK: 5, timeoutSeconds: 30 }, defaultMemoryPolicy: { enabled: false }, defaultOutputPolicy: { schemaVersion: 1, grounding: 'KB_ONLY', maxCharacters: 4000 } }
const passage = { referenceId: 'K9', chunkId: '9', versionId: '21', knowledgeBaseId: 'kb-1', assetKey: 'guide', title: 'Synthetic guide', text: 'Original reviewed source evidence.', section: 'Guide', sourceLocator: { format: 'txt', lineStart: 1, lineEnd: 1 }, embeddingProfile: 'synthetic:768', score: 0.016, retrievalMethod: 'LEXICAL' }
const base = { id: 'kb-1', name: 'Public KB', status: 'ACTIVE', revision: 0, publicationRevision: 0 }
function page(component, url) { return render(<I18nProvider><MemoryRouter initialEntries={[url]}>{component}</MemoryRouter></I18nProvider>) }

it('stages, ingests, inspects and explicitly publishes a source, then preserves passages on answer failure', async () => {
  let job = null, version = null, current = { ...base }
  apiRequest.mockImplementation(async (path, request = {}) => {
    if (path === '/v2/me') return { id: '5', role: 'ADMIN' }
    if (path === '/v2/admin/agent-platform/options') return options
    if (path === '/v2/admin/knowledge-bases') return [current]
    if (path === '/v2/admin/knowledge-bases/kb-1') return current
    if (path.endsWith('/ingestions') && !request.method) return job ? [job] : []
    if (path.endsWith('/assets') && !request.method) return version ? [version] : []
    if (path.endsWith('/assets') && request.method === 'POST') { expect(request.body).toBeInstanceOf(FormData); expect(request.headers?.['Content-Type']).toBeUndefined(); expect(request.body.get('metadata').type).toBe('application/json'); job = { id: 'job-1', assetKey: 'guide', state: 'QUEUED', attempts: [] }; return job }
    if (path.endsWith('/retry')) { job = { ...job, state: 'READY' }; version = { ...passage, id: '21', versionNumber: 1, mimeType: 'text/plain', status: 'READY', chunks: [{ ...passage, retrievalMethod: 'INSPECTION' }], reviewer: 'Synthetic Admin', license: 'Original fixture', contentHash: 'source-hash' }; return job }
    if (path.endsWith('/versions/21')) return version
    if (path.endsWith('/publish')) { expect(JSON.parse(request.body).confirmed).toBe(true); expect(request.headers['If-Match']).toBe('0'); version = { ...version, status: 'PUBLISHED' }; current = { ...current, revision: 1, publicationRevision: 1 }; return version }
    if (path.endsWith('/tests')) return { id: 'execution-kb', status: 'ACCEPTED' }
    if (path === '/v2/admin/agent-executions/execution-kb') return { id: 'execution-kb', executionMode: 'KB_TEST', status: 'FAILED', errorCode: 'PROVIDER_UNAVAILABLE', configurationHash: null, inputTokens: null, outputTokens: null, estimatedCost: null, costStatus: 'UNKNOWN', latencyMs: 17, toolStepsAndSources: [{ type: 'SERVER_RETRIEVAL', sources: [passage] }], outputSnapshot: { outcome: 'UNAVAILABLE', text: 'The Agent could not complete this request safely. Core assessments and simulations remain available.', citations: [] } }
    throw new Error(`Unexpected fixture route ${path}`)
  })
  page(<KnowledgeStudioPage />, '/admin/knowledge?kb=kb-1')
  fireEvent.change(await screen.findByLabelText('Source file'), { target: { files: [new File(['original'], 'guide.txt', { type: 'text/plain' })] } })
  for (const [label, value] of [['Document title', 'Synthetic guide'], ['Source usage rights', 'other'], ['Source usage rights details', 'Original fixture'], ['Topic / domain', 'BACKEND'], ['Document language', 'EN']]) fireEvent.change(screen.getByLabelText(label), { target: { value } })
  fireEvent.click(screen.getByRole('checkbox', { name: /I reviewed this content/ }))
  fireEvent.submit(screen.getByRole('button', { name: 'Upload and process' }).closest('form'))
  const inspect = await screen.findByRole('button', { name: 'Review and publish' })
  expect(screen.queryByRole('button', { name: 'Publish document' })).toBeNull()
  fireEvent.click(inspect)
  fireEvent.click(await screen.findByRole('tab', { name: 'Chunks' }))
  expect(await screen.findByText('Original reviewed source evidence.')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Publish document' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm source action' }))
  await waitFor(() => expect(version.status).toBe('PUBLISHED'))
  fireEvent.click(screen.getByRole('tab', { name: 'Test KB' }))
  fireEvent.click(screen.getByRole('radio', { name: 'Answer with sources' }))
  fireEvent.change(screen.getByLabelText('Knowledge test question'), { target: { value: 'Explain the reviewed source' } })
  fireEvent.click(screen.getByRole('button', { name: 'Answer with sources' }))
  expect(await screen.findByText('Safe fallback or refusal')).toBeTruthy()
  expect(screen.getAllByText('Original reviewed source evidence.').length).toBeGreaterThan(0)
  expect(screen.getAllByText('Not reported')).toHaveLength(2)
  expect(screen.queryByText('Validated answer')).toBeNull()
})

function builderFixture() {
  let agent = { id: 'agent-1', name: 'Synthetic Agent', purpose: 'Public practice', status: 'DRAFT', revision: 0 }, configs = [], assignments = [], lastPreview = null
  apiRequest.mockImplementation(async (path, request = {}) => {
    if (path === '/v2/me') return { id: '5', role: 'ADMIN' }
    if (path === '/v2/admin/agent-platform/options') return options
    if (path === '/v2/admin/knowledge-bases') return [base]
    if (path === '/v2/admin/agents') return [agent]
    if (path === '/v2/admin/agents/agent-1') return agent
    if (path.endsWith('/configurations') && request.method === 'POST') { const input = JSON.parse(request.body); const c = { ...input, id: `config-${configs.length + 1}`, agentId: agent.id, versionNumber: configs.length + 1, status: 'SAVED', contentHash: `hash-${configs.length + 1}` }; configs.unshift(c); agent = { ...agent, revision: agent.revision + 1 }; return c }
    if (path.endsWith('/configurations')) return configs
    if (path.endsWith('/previews')) { const config = configs.find(c => path.includes(`/${c.id}/`)); lastPreview = { id: `preview-${config.id}`, configurationId: config.id, configurationHash: config.contentHash, status: 'SUCCEEDED', executionMode: 'ADMIN_PREVIEW', inputTokens: 10, outputTokens: 5, estimatedCost: null, costStatus: 'UNKNOWN', latencyMs: 9, toolStepsAndSources: [], outputSnapshot: { outcome: 'ANSWER', text: 'Synthetic validated preview.', citations: [] } }; return { id: lastPreview.id, status: 'ACCEPTED' } }
    if (path.startsWith('/v2/admin/agent-executions/')) return lastPreview
    if (path.includes('/eligible-students?')) return { items: [{ id: '7', displayName: 'Synthetic Student' }], nextAfterId: null }
    if (path.endsWith('/publication-readiness')) { const c = configs.find(c => path.includes(`/${c.id}/`)); return { configurationId: c.id, configurationHash: c.contentHash, agentRevision: agent.revision, agentEditable: true, knowledgeAvailable: true, previewSucceeded: lastPreview?.configurationHash === c.contentHash, publishable: lastPreview?.configurationHash === c.contentHash } }
    if (path.endsWith('/publish')) { const c = configs.find(c => path.includes(`/${c.id}/`)); expect(lastPreview.configurationHash).toBe(c.contentHash); c.status = 'PUBLISHED'; agent = { ...agent, status: 'ACTIVE', publishedConfigurationId: c.id, revision: agent.revision + 1 }; return agent }
    if (path.includes('/configurations/config-')) return configs.find(c => path.endsWith(c.id))
    if (path.endsWith('/assignments') && request.method === 'POST') { assignments = [{ id: 'assignment-1', studentUserId: JSON.parse(request.body).studentUserId, status: 'ACTIVE' }]; return assignments[0] }
    if (path.endsWith('/assignments')) return assignments
    if (path.endsWith('/revoke')) { assignments[0] = { ...assignments[0], status: 'REVOKED' }; return assignments[0] }
    throw new Error(`Unexpected fixture route ${path}`)
  })
}
function pane(name) { fireEvent.click(screen.getByRole('button', { name, exact: true })) }
function tab(name) { pane('Configuration'); fireEvent.click(screen.getByRole('tab', { name, exact: true })) }
function publication() { tab('Publish & Assign') }
async function saveAndPreview() {
  fireEvent.change(await screen.findByLabelText('Saved Agent instructions'), { target: { value: 'Explain authorized evidence only.' } })
  tab('Integrations')
  expect(screen.getByRole('checkbox', { name: /Public KB/ }).checked).toBe(false)
  expect(screen.getByRole('checkbox', { name: 'kb.search' }).checked).toBe(false)
  fireEvent.click(screen.getByRole('checkbox', { name: /Public KB/ }))
  fireEvent.click(screen.getByRole('checkbox', { name: 'kb.search' }))
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  pane('Preview')
  await waitFor(() => expect(screen.getByRole('button', { name: 'Send' }).disabled).toBe(false))
  fireEvent.change(screen.getByLabelText('Preview question'), { target: { value: 'Explain the practice guide' } })
  fireEvent.click(screen.getByRole('button', { name: 'Send' }))
  await screen.findByText('Synthetic validated preview.')
}
it('saves and previews exact versions, blocks dirty publication, then publishes and assigns explicitly', async () => {
  builderFixture(); page(<AgentBuilderPage />, '/admin/agents?agent=agent-1'); await saveAndPreview()
  publication()
  await waitFor(() => expect(screen.getByRole('button', { name: /Publish version/ }).disabled).toBe(false))
  tab('Brain'); fireEvent.change(screen.getByLabelText('Saved Agent instructions'), { target: { value: 'New instructions require a new tested version.' } }); publication()
  expect(screen.getByRole('button', { name: /Publish version/ }).disabled).toBe(true)
  pane('Preview'); expect(screen.getByRole('button', { name: 'Send' }).disabled).toBe(true)
  tab('Brain'); fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  await screen.findByText('hash-2')
  publication()
  expect(screen.getByRole('button', { name: /Publish version/ }).disabled).toBe(true)
  pane('Preview'); fireEvent.click(screen.getByRole('button', { name: 'Send' })); publication()
  await waitFor(() => expect(screen.getByRole('button', { name: /Publish version/ }).disabled).toBe(false))
  fireEvent.click(screen.getByRole('button', { name: /Publish version/ }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm publish' }))
  await screen.findByText('Publication saved. No Students were assigned automatically.')
  fireEvent.change(screen.getByLabelText('Eligible Student'), { target: { value: '7' } })
  await waitFor(() => expect(screen.getByRole('button', { name: 'Assign selected Student' }).disabled).toBe(false))
  expect(screen.getByLabelText('Agent status').value).toBe('ACTIVE')
  fireEvent.click(screen.getByRole('button', { name: 'Assign selected Student' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm assignment' }))
  await waitFor(() => expect(screen.getByRole('button', { name: 'Revoke assignment' }).disabled).toBe(false))
  fireEvent.click(screen.getByRole('button', { name: 'Revoke assignment' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm revoke' }))
  await waitFor(() => expect(screen.queryByRole('button', { name: 'Revoke assignment' })).toBeNull())
  expect(apiRequest.mock.calls.filter(([path, request]) => path.endsWith('/previews') && request?.method === 'POST')).toHaveLength(2)
})
it('preserves invalid policy form edits without authorizing an old successful preview', async () => {
  builderFixture(); page(<AgentBuilderPage />, '/admin/agents?agent=agent-1'); await saveAndPreview()
  tab('Advanced'); publication()
  fireEvent.change(screen.getByLabelText('Model rounds'), { target: { value: '5' } })
  expect(screen.getByRole('button', { name: /Publish version/ }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
  expect(screen.getByLabelText('Model rounds').getAttribute('aria-invalid')).toBe('true')
  expect(screen.getByLabelText('Model rounds').value).toBe('5')
  pane('Preview')
  expect(screen.getByRole('button', { name: 'Send' }).disabled).toBe(true)
  expect(JSON.parse(localStorage.getItem(draftKey('5', 'agent-config', 'agent-1'))).draft.toolPolicy.maxRounds).toBe(5)
  expect(apiRequest.mock.calls.filter(([path, request]) => path.endsWith('/configurations') && request?.method === 'POST')).toHaveLength(1)
})
it('shows assigned public Agent identity only and distinguishes a permission failure from an empty list', async () => {
  apiRequest.mockResolvedValue([{ id: 'a', name: 'Assigned guide', purpose: 'Public exploration', promptTemplate: 'Never render this accidental private field' }])
  const view = page(<AssignedAgentsPage />, '/agents')
  expect(await screen.findByText('Assigned guide')).toBeTruthy()
  expect(screen.queryByText('Never render this accidental private field')).toBeNull()
  expect(apiRequest).toHaveBeenCalledWith('/v2/agents')
  view.unmount(); apiRequest.mockRejectedValue(new Error('Sign in is required.'))
  page(<AssignedAgentsPage />, '/agents')
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(screen.queryByText('No active Agent is currently assigned to your account.')).toBeNull()
})
