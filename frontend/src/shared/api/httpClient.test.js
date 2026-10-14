import { afterEach, beforeEach, expect, it, vi } from 'vitest'
let client
beforeEach(async () => { vi.resetModules(); client = await import('./httpClient.js') })
afterEach(() => vi.unstubAllGlobals())
const response = data => ({ ok: true, status: 200, headers: { get: () => 'application/json' }, json: async () => ({ success: true, data }) })
it('gets a CSRF token before writing and refreshes it after authentication rotation', async () => {
  const fetcher = vi.fn().mockResolvedValueOnce(response({ token: 'first' })).mockResolvedValueOnce(response({ id: '7' })).mockResolvedValueOnce(response({ token: 'second' })).mockResolvedValueOnce(response(true))
  vi.stubGlobal('fetch', fetcher)
  await client.apiRequest('/v2/auth/login', { method: 'POST', body: '{}' })
  await client.apiRequest('/v2/me/consents', { method: 'POST', body: '{}' })
  expect(fetcher.mock.calls[1][1]).toMatchObject({ credentials: 'same-origin', headers: { 'X-CSRF-TOKEN': 'first' } })
  expect(fetcher.mock.calls[3][1].headers['X-CSRF-TOKEN']).toBe('second')
})
it('fails closed on unavailable CSRF without sending the requested mutation', async () => {
  const fetcher = vi.fn().mockRejectedValue(new Error('offline')); vi.stubGlobal('fetch', fetcher)
  await expect(client.apiRequest('/v2/plans/confirm', { method: 'POST' })).rejects.toMatchObject({ code: 'NETWORK_ERROR' })
  expect(fetcher).toHaveBeenCalledTimes(1)
})
