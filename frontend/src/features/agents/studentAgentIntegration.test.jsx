// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../shared/api/httpClient.js'
import { streamExecution } from './studentAgentApi.js'
import StudentAgentPage from './StudentAgentPage.jsx'
import AgentUsagePage from '../admin/agentPlatform/AgentUsagePage.jsx'
vi.mock('../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
vi.mock('./studentAgentApi.js', () => ({ sessionPath: id => `/v2/agent-sessions/${id}`, runPath: id => `/v2/agent-executions/${id}`, streamExecution: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() }); afterEach(cleanup)
function page() { return render(<I18nProvider><MemoryRouter initialEntries={['/agents/a1?session=s1']}><Routes><Route path="/agents/:agentId" element={<StudentAgentPage />} /></Routes></MemoryRouter></I18nProvider>) }
function fixture() {
 const plan = { id: 'run-plan', status: 'SUCCEEDED', taskType: 'PLAN_SUGGESTION', planContentHash: 'canonical-hash', output: { text: 'Review this draft.', planSuggestion: { title: 'Explore a task', steps: ['Try one practice task.'] } }, toolStepsAndSources: [] }
 apiRequest.mockImplementation(async (path, request = {}) => {
  if (path === '/v2/me') return { id: '7', role: 'STUDENT' }
  if (path.startsWith('/v2/agent-sessions?')) return [{ id: 's1', title: 'My conversation', configurationVersion: 1 }]
  if (path === '/v2/agent-sessions/s1') return { title: 'My conversation', configurationVersion: 1 }
  if (path === '/v2/agent-sessions/s1/interactions') return []
  if (path.endsWith('/plan-suggestions')) return plan
  if (path === '/v2/agent-executions/run-plan') return plan
  if (path === '/v2/guidance/dashboard') return { activePlan: { id: 'old-plan', revision: 4 } }
  if (path.endsWith('/confirm-suggestion')) return { id: 'confirmed-plan' }
  if (path === '/v2/insight-reports') return []
  throw new Error(`Unexpected route ${path} ${request.method}`)
 })
 return plan
}
it('keeps a suggested plan a draft until exact source/hash confirmation and preserves the message draft', async () => {
 fixture(); page(); await screen.findByText(/Pinned configuration version/); fireEvent.change(await screen.findByLabelText('Message to Agent'), { target: { value: 'My original exploration question' } }); fireEvent.click(screen.getByRole('button', { name: 'Suggest draft plan' }))
 await screen.findByText('Explore a task'); expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/confirm-suggestion'))).toBe(false)
 fireEvent.click(screen.getByRole('button', { name: 'Confirm and save this plan' }))
 await waitFor(() => expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/confirm-suggestion'))).toBe(true))
 const body = JSON.parse(apiRequest.mock.calls.find(([path]) => path.endsWith('/confirm-suggestion'))[1].body)
 expect(body).toMatchObject({ sourceExecutionId: 'run-plan', contentHash: 'canonical-hash', expectedActivePlanId: 'old-plan', expectedRevision: 4 }); expect(body.title).toBeUndefined()
 expect(JSON.parse(localStorage.getItem('careersim_core_7_agent-message_s1')).text).toBe('My original exploration question')
})
it('shows real incremental text and reconnects the same execution without re-posting a message', async () => {
 fixture(); const run = { id: 'running', status: 'RUNNING', toolStepsAndSources: [] }
 const original = apiRequest.getMockImplementation(); apiRequest.mockImplementation(async (path, request) => path.endsWith('/messages') || path.endsWith('/running') ? run : original(path, request))
 streamExecution.mockImplementation(async (id, options) => { options.onEvent({ sequenceNumber: 3, type: 'answer.segment', payload: { text: 'First actual segment.' } }); throw new Error('Connection interrupted') })
 page(); await screen.findByText(/Pinned configuration version/); fireEvent.change(await screen.findByLabelText('Message to Agent'), { target: { value: 'Explore this task' } }); fireEvent.click(screen.getByRole('button', { name: 'Send message' })); await screen.findByText('First actual segment.')
 fireEvent.click(screen.getByRole('button', { name: 'Reconnect saved execution' })); await waitFor(() => expect(streamExecution).toHaveBeenCalledTimes(2))
 expect(streamExecution.mock.calls[1][1].after).toBe(3); expect(apiRequest.mock.calls.filter(([p]) => p.endsWith('/messages'))).toHaveLength(1)
})
it('does not render an earlier account response after the workspace is unmounted', async () => {
 fixture(); let deliver
 const original = apiRequest.getMockImplementation(); apiRequest.mockImplementation((path, request) => path === '/v2/agent-sessions/s1/interactions' ? new Promise(resolve => { deliver = resolve }) : original(path, request))
 const view = page(); await screen.findByLabelText('New conversation title'); view.unmount(); render(<p>Second account workspace</p>); deliver([{ id: 'old', actorType: 'ASSISTANT', content: { text: 'Private previous account narrative' } }]); await Promise.resolve()
 expect(screen.queryByText('Private previous account narrative')).toBeNull()
})
it('renders unknown Admin metrics honestly and only requests the aggregate usage contract', async () => {
 apiRequest.mockResolvedValue({ groups: [{ mode: 'STUDENT_CHAT', status: 'SUCCEEDED', executions: 2, mean_latency_ms: 12, known_input_tokens: null, known_output_tokens: null, unknown_token_executions: 2, known_estimated_subtotal_usd: null, unknown_cost_executions: 1, partial_cost_executions: 1 }], nativeTools: [{ mode: 'STUDENT_CHAT', tool: 'assessment.lookup', status: 'SUCCEEDED', native_calls: 1 }] })
 render(<I18nProvider><MemoryRouter><AgentUsagePage /></MemoryRouter></I18nProvider>); fireEvent.click(screen.getByRole('button', { name: 'Load usage' })); await screen.findByText('STUDENT_CHAT', { selector: 'td' }); expect(screen.getAllByText('Unknown').length).toBeGreaterThan(0); expect(apiRequest.mock.calls[0][0]).toMatch(/^\/v2\/admin\/agent-usage\?/); expect(apiRequest.mock.calls.some(([p]) => p.includes('insight-reports') || p.includes('interactions'))).toBe(false)
})
