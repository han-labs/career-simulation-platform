// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { useAssessment } from './useAssessment.js'
import { apiRequest, getAccountGeneration } from '../../../shared/api/httpClient.js'
vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn(), getAccountGeneration: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear(); getAccountGeneration.mockReturnValue(0) })
afterEach(cleanup)
it('never adopts an unowned draft and preserves owner A when owner B signs in', async () => {
  localStorage.setItem('fixture', JSON.stringify({ status: 'completed', attemptId: 5, result: 'Unowned private work' }))
  apiRequest.mockResolvedValue({ id: 'A' })
  const first = renderHook(() => useAssessment({ storageKey: 'fixture' }))
  await waitFor(() => expect(apiRequest).toHaveBeenCalledWith('/v2/me'))
  await act(async () => {})
  expect(first.result.current.status).toBe('intro')
  act(() => first.result.current.setAnswer('1', 5))
  await waitFor(() => expect(localStorage.getItem('fixture:owner:A')).toContain('5'))
  first.unmount()
  apiRequest.mockResolvedValue({ id: 'B' }); getAccountGeneration.mockReturnValue(1)
  const second = renderHook(() => useAssessment({ storageKey: 'fixture' }))
  await act(async () => {})
  expect(second.result.current.answers).toEqual({})
  expect(localStorage.getItem('fixture:owner:A')).toContain('5')
  expect(localStorage.getItem('fixture')).toContain('Unowned private work')
})
it('ignores a delayed legacy start response from the prior account', async () => {
  let resolveStart
  apiRequest.mockImplementation(path => path === '/v2/me' ? Promise.resolve({ id: 'A' }) : new Promise(resolve => { resolveStart = resolve }))
  const hook = renderHook(() => useAssessment({ storageKey: 'fixture' }))
  await act(async () => {})
  let pending
  act(() => { pending = hook.result.current.start() })
  getAccountGeneration.mockReturnValue(1)
  await act(async () => { resolveStart({ id: 999 }); await pending })
  expect(hook.result.current.attemptId).toBeNull()
})
