// @vitest-environment jsdom
import { StrictMode } from 'react'
import { act, cleanup, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiRequest } from '../../../shared/api/httpClient.js'
import useKnowledgeData from './useKnowledgeData.js'

vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
beforeEach(() => { vi.useFakeTimers(); vi.resetAllMocks() })
afterEach(() => { cleanup(); vi.useRealTimers() })
function fixture(readJobs = () => [{ id: 'job', state: 'EMBEDDING' }]) {
  apiRequest.mockImplementation(async path => path.endsWith('/ingestions') ? readJobs() : path.endsWith('/assets') ? [] : { id: 'kb' })
}
const mount = (onError = vi.fn()) => renderHook(() => useKnowledgeData('kb', null, onError), { wrapper: StrictMode })
const flush = () => act(async () => {})
const tick = ms => act(() => vi.advanceTimersByTimeAsync(ms))

it('bootstraps once in StrictMode, polls only jobs, stops on terminal status and does not refresh on focus', async () => {
  let jobs = [{ id: 'job', state: 'EMBEDDING' }]
  fixture(() => jobs)
  mount(); await flush()
  expect(apiRequest).toHaveBeenCalledTimes(3)
  await tick(5000)
  expect(apiRequest).toHaveBeenCalledTimes(4)
  expect(apiRequest.mock.calls.at(-1)[0]).toBe('/v2/admin/knowledge-bases/kb/ingestions')
  jobs = [{ id: 'job', state: 'READY' }]
  await tick(5000)
  const terminal = apiRequest.mock.calls.length
  expect(terminal).toBe(8) // one jobs poll + one terminal full refresh
  window.dispatchEvent(new Event('focus')); await tick(60000)
  expect(apiRequest).toHaveBeenCalledTimes(terminal)
})

it('coalesces manual reloads, waits for the existing poll and aborts obsolete reads on unmount', async () => {
  let resolve, count = 0, signal
  fixture(() => { count++; return count === 1 ? [{ id: 'job', state: 'PARSING' }] : new Promise(done => { resolve = done }) })
  const view = mount(); await flush(); await tick(5000)
  expect(apiRequest).toHaveBeenCalledTimes(4)
  const first = view.result.current.refresh(), second = view.result.current.refresh()
  expect(first).toBe(second)
  await tick(20000); expect(apiRequest).toHaveBeenCalledTimes(4)
  signal = apiRequest.mock.calls.at(-1)[1].signal
  view.unmount(); expect(signal.aborted).toBe(true)
  resolve([{ id: 'job', state: 'READY' }]); await flush()
  expect(apiRequest).toHaveBeenCalledTimes(4)
})

it('honors a 429 Retry-After pause without another request or automatic processing retry', async () => {
  let count = 0
  const error = Object.assign(new Error('Rate limited'), { status: 429, retryAfterMs: 20000 })
  fixture(() => { if (++count === 2) throw error; return [{ id: 'job', state: 'PARSING' }] })
  const onError = vi.fn(); mount(onError); await flush(); await tick(5000)
  expect(onError).toHaveBeenCalledWith('Rate limited')
  expect(apiRequest).toHaveBeenCalledTimes(4)
  await tick(19999); expect(apiRequest).toHaveBeenCalledTimes(4)
  await tick(1); expect(apiRequest).toHaveBeenCalledTimes(5)
  expect(apiRequest.mock.calls.every(([, options]) => !options.method)).toBe(true)
})
