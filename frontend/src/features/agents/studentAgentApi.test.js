import { afterEach, expect, it, vi } from 'vitest'
import { streamExecution } from './studentAgentApi.js'
const state = vi.hoisted(() => ({ generation: 0, listeners: new Set() }))
vi.mock('../../shared/api/httpClient.js', () => ({ API_BASE_URL: '/api', ApiError: class extends Error {}, getAccountGeneration: () => state.generation, subscribeAccount: fn => { state.listeners.add(fn); return () => state.listeners.delete(fn) } }))
afterEach(() => { vi.unstubAllGlobals(); state.generation = 0; state.listeners.clear() })
function response(chunks) { return new Response(new ReadableStream({ start(controller) { for (const chunk of chunks) controller.enqueue(new TextEncoder().encode(chunk)); controller.close() } }), { headers: { 'content-type': 'text/event-stream' } }) }
it('parses incremental semantic events with monotonic replay IDs and cookie-only credentials', async () => {
 const request = vi.fn(async () => response(['id: 2\nevent: answer.segment\ndata: {"text":"Xin ', 'chào."}\n\n', 'id: 2\nevent: answer.segment\ndata: {"text":"duplicate"}\n\n', 'id: 3\nevent: execution.completed\ndata: {"status":"SUCCEEDED"}\n\n']))
 vi.stubGlobal('fetch', request); const events = []
 expect(await streamExecution('run 1', { after: 1, onEvent: e => events.push(e) })).toBe(3)
 expect(events).toHaveLength(2); expect(events[0].payload.text).toBe('Xin chào.')
 expect(request.mock.calls[0][0]).toBe('/api/v2/agent-executions/run%201/events'); expect(request.mock.calls[0][1].headers['Last-Event-ID']).toBe('1'); expect(request.mock.calls[0][1].credentials).toBe('same-origin'); expect(request.mock.calls[0][1].headers.Authorization).toBeUndefined()
})
it('blocks delayed private events after an account switch and removes its listener', async () => {
 vi.stubGlobal('fetch', vi.fn(async () => response(['id: 1\nevent: answer.segment\ndata: {"text":"Owner A"}\n\nid: 2\nevent: answer.segment\ndata: {"text":"private late event"}\n\n'])))
 const events = []
 await expect(streamExecution('run', { onEvent(e) { events.push(e); state.generation++; state.listeners.forEach(fn => fn()) } })).rejects.toThrow('The account changed')
 expect(events).toHaveLength(1); expect(state.listeners.size).toBe(0)
})
it('does not treat a JSON authorization failure as a successful stream', async () => {
 vi.stubGlobal('fetch', vi.fn(async () => new Response('{}', { status: 403, headers: { 'content-type': 'application/json' } })))
 await expect(streamExecution('run', { onEvent: vi.fn() })).rejects.toThrow('Agent stream is unavailable')
})
