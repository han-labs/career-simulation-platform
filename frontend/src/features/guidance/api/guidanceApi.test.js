import { afterEach, expect, it, vi } from 'vitest'
import { apiRequest, ApiError } from '../../../shared/api/httpClient.js'
import { getExplorationDashboard, normalizeDashboard } from './guidanceApi.js'
vi.mock('../../../shared/api/httpClient.js', async original => ({ ...await original(), apiRequest: vi.fn() }))
afterEach(() => vi.resetAllMocks())
it('normalizes partial dashboard data without inventing objective results', () => {
  expect(normalizeDashboard({ studentName: 'Synthetic' })).toMatchObject({ source: 'LIVE', recentResults: [], skills: [], plan: null })
})
it('preserves the active Student dashboard read contract', async () => {
  apiRequest.mockResolvedValue({ recentResults: [{ id: '1', score: 0 }] })
  expect((await getExplorationDashboard()).recentResults).toEqual([{ id: '1', score: 0 }])
  expect(apiRequest).toHaveBeenCalledExactlyOnceWith('/v1/guidance/dashboard')
})
it('never turns a denied dashboard read into preview or Syn activity', async () => {
  apiRequest.mockRejectedValue(new ApiError('Forbidden', { status: 403 }))
  await expect(getExplorationDashboard()).rejects.toMatchObject({ status: 403 })
  expect(apiRequest).toHaveBeenCalledTimes(1)
})
