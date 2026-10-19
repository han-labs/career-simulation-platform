export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? '/api'
).replace(/\/$/, '')

export class ApiError extends Error {
  constructor(message, { status = 0, code = 'NETWORK_ERROR', payload = null } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.payload = payload
  }
}

let csrfToken = null
let csrfLoading = null
let accountGeneration = 0
let invalidated = false
const listeners = new Set()
export const getAccountGeneration = () => accountGeneration
export function subscribeAccount(listener) { listeners.add(listener); return () => listeners.delete(listener) }
export function invalidateAccount() {
  accountGeneration += 1; csrfToken = null; csrfLoading = null
  listeners.forEach(listener => listener())
  if (typeof window !== 'undefined') window.dispatchEvent(new Event('careersim:account-changed'))
}
if (typeof window !== 'undefined') window.addEventListener('storage', event => {
  if (event.key === 'careersim_auth_epoch') { invalidated = true; invalidateAccount() }
})
export async function refreshCsrf() {
  const generation = accountGeneration
  csrfToken = null
  if (!csrfLoading) csrfLoading = fetch(`${API_BASE_URL}/v2/auth/csrf`, { credentials: 'same-origin', headers: { Accept: 'application/json' } })
    .then(async (response) => {
      const payload = await response.json()
      if (!response.ok || !payload?.data?.token) throw new ApiError('Unable to establish a secure session.', { status: response.status })
      if (generation !== accountGeneration) throw new ApiError('The account changed. Reload your saved work.', { code: 'STALE_ACCOUNT' })
      csrfToken = payload.data.token
      return csrfToken
    }).catch(error => { throw error instanceof ApiError ? error : new ApiError('The platform is not reachable. Please try again shortly.') }).finally(() => { csrfLoading = null })
  return csrfLoading
}

export async function apiRequest(path, options = {}) {
  const generation = accountGeneration
  let response
  const unsafe = !['GET', 'HEAD', 'OPTIONS'].includes((options.method ?? 'GET').toUpperCase())
  if (unsafe && !csrfToken) await refreshCsrf()
  if (generation !== accountGeneration) throw new ApiError('The account changed. Reload your saved work.', { code: 'STALE_ACCOUNT' })

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        ...(unsafe ? { 'X-CSRF-TOKEN': csrfToken } : {}),
        ...options.headers,
      },
    })
  } catch {
    throw new ApiError('The platform is not reachable. Please try again shortly.')
  }

  const contentType = response.headers.get('content-type') ?? ''
  const payload = contentType.includes('application/json')
    ? await response.json()
    : null

  if (generation !== accountGeneration) throw new ApiError('The account changed. Reload your saved work.', { code: 'STALE_ACCOUNT' })

  if (!response.ok || payload?.success === false) {
    if (response.status === 401 || response.status === 403) csrfToken = null
    if (response.status === 401 && !invalidated && path !== '/v2/auth/login') { invalidated = true; invalidateAccount() }
    throw new ApiError(
      payload?.message ?? `Request failed with status ${response.status}.`,
      {
        status: response.status,
        code: payload?.code ?? 'REQUEST_FAILED',
        payload,
      },
    )
  }

  if (path === '/v2/me') invalidated = false
  if (path === '/v2/auth/login' || path === '/v2/auth/logout') {
    invalidated = path === '/v2/auth/logout'
    invalidateAccount()
    try { localStorage.setItem('careersim_auth_epoch', crypto.randomUUID()) } catch { /* Same-tab isolation still applies. */ }
  }

  return payload?.data
}
