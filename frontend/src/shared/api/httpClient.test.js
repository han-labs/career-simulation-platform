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
it('rejects a late private response after an account change', async () => {
  let finish
  vi.stubGlobal('fetch', vi.fn(() => new Promise(resolve => { finish = resolve })))
  const pending = client.apiRequest('/v2/assessment-attempts/1/result')
  client.invalidateAccount()
  finish(response({ privateResult: 'owner A' }))
  await expect(pending).rejects.toMatchObject({ code: 'STALE_ACCOUNT' })
})
it('does not send an old mutation if the account changes while CSRF is loading', async () => {
  let finish
  const fetcher = vi.fn(() => new Promise(resolve => { finish = resolve }))
  vi.stubGlobal('fetch', fetcher)
  const pending = client.apiRequest('/v2/me/consents', { method: 'POST' })
  client.invalidateAccount()
  finish(response({ token: 'owner-A-token' }))
  await expect(pending).rejects.toMatchObject({ code: 'STALE_ACCOUNT' })
  expect(fetcher).toHaveBeenCalledTimes(1)
})
