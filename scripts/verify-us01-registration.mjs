import assert from 'node:assert/strict'
import { randomBytes, randomUUID } from 'node:crypto'

// Explicit opt-in: creates synthetic Students and assessment results in a disposable stack.
const origin = process.env.US01_SMOKE_ORIGIN ?? 'http://127.0.0.1:18080'
assert.equal(process.env.US01_ISOLATED_TEST, 'true', 'Set US01_ISOLATED_TEST=true only for a disposable stack.')
assert.ok(['127.0.0.1', 'localhost'].includes(new URL(origin).hostname), 'Only loopback test stacks are supported.')
const checks = []
class Browser {
  cookies = new Map()
  csrf = null
  async request(path, { method = 'GET', body, revision, expected = 200, csrf = true } = {}) {
    const response = await fetch(`${origin}/api${path}`, {
      method, headers: {
        Cookie: [...this.cookies].map(([k, v]) => `${k}=${v}`).join('; '),
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(method === 'GET' || !csrf ? {} : { 'X-CSRF-TOKEN': this.csrf }),
        ...(revision === undefined ? {} : { 'If-Match': String(revision) }),
      }, body: body === undefined ? undefined : JSON.stringify(body),
    })
    for (const cookie of response.headers.getSetCookie()) {
      const [name, ...value] = cookie.split(';')[0].split('=')
      if (/max-age=0(?:;|$)/i.test(cookie)) this.cookies.delete(name)
      else this.cookies.set(name, value.join('='))
    }
    assert.equal(response.status, expected, `${method} ${path}`)
    const payload = await response.json()
    return payload.data ?? payload
  }
  async bootstrap() { this.csrf = (await this.request('/v2/auth/csrf')).token }
  async login(email, password) {
    const account = await this.request('/v2/auth/login', { method: 'POST', body: { email, password } })
    await this.bootstrap()
    return account
  }
}

const browser = new Browser()
await browser.request('/health')
checks.push('gateway health')
await browser.bootstrap()
const email = `us01-smoke-${randomUUID()}@example.test`
const password = randomBytes(24).toString('base64url')
const registration = { email, password, displayName: 'Synthetic Registration Student', role: 'STUDENT' }
await browser.request('/v2/auth/register', { method: 'POST', body: registration, csrf: false, expected: 403 })
await browser.request('/v2/auth/register', { method: 'POST', body: { ...registration, email: 'invalid', password: 'short' }, expected: 422 })
await browser.request('/v2/auth/register', { method: 'POST', body: registration })
await browser.request('/v2/me', { expected: 401 })
const duplicate = await browser.request('/v2/auth/register', { method: 'POST', body: { ...registration, email: email.toUpperCase() }, expected: 409 })
assert.equal(duplicate.message, 'An account with this email already exists. Please sign in.')
checks.push('registration validation, CSRF, uniqueness and separate sign-in')
const student = await browser.login(email, password)
assert.equal(student.role, 'STUDENT')
assert.deepEqual(student.consents, [])
assert.equal((await browser.request('/v2/me')).id, student.id)
checks.push('new Student login, JWT cookie and current session')
const types = await browser.request('/v2/assessment-types')
const selected = types.find(type => type.code === 'RIASEC' && type.activeVersionId) ?? types.find(type => type.activeVersionId)
assert.ok(selected, 'Publish a synthetic assessment in the isolated stack before running this smoke.')
const attempt = await browser.request(`/v2/assessment-versions/${selected.activeVersionId}/attempts`, { method: 'POST', body: { requestId: randomUUID() } })
await browser.request(`/v2/assessment-attempts/${attempt.id}/submit`, { method: 'POST', revision: attempt.revision, body: { items: [] }, expected: 422 })
const items = attempt.questions.map(question => ({ questionId: question.id, answerData: { schemaVersion: 1, value: question.responseConfig.min ?? 1 } }))
const result = await browser.request(`/v2/assessment-attempts/${attempt.id}/submit`, { method: 'POST', revision: attempt.revision, body: { items } })
assert.equal(result.attemptId, attempt.id)
assert.deepEqual(await browser.request(`/v2/assessment-attempts/${attempt.id}/result`), result)
checks.push('published assessment selection, required validation, scoring, persistence and result reload')
const outsider = new Browser()
await outsider.request(`/v2/assessment-attempts/${attempt.id}/result`, { expected: 401 })
await outsider.bootstrap()
const secondEmail = `us01-other-${randomUUID()}@example.test`
await outsider.request('/v2/auth/register', { method: 'POST', body: { ...registration, email: secondEmail } })
await outsider.login(secondEmail, password)
await outsider.request(`/v2/assessment-attempts/${attempt.id}/result`, { expected: 404 })
checks.push('anonymous and foreign-result isolation')
await browser.request('/v2/auth/logout', { method: 'POST', body: {} })
await browser.request(`/v2/assessment-attempts/${attempt.id}/result`, { expected: 401 })
await browser.bootstrap()
await browser.login(email, password)
assert.deepEqual(await browser.request(`/v2/assessment-attempts/${attempt.id}/result`), result)
await browser.request('/v2/auth/logout', { method: 'POST', body: {} })
await outsider.request('/v2/auth/logout', { method: 'POST', body: {} })
checks.push('logout, reauthentication and durable result retrieval')
console.log(JSON.stringify({ status: 'PASS', checks, provider: 'email/password; no live Google call' }, null, 2))
