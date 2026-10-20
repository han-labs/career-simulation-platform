import { API_BASE_URL, ApiError, getAccountGeneration, subscribeAccount } from '../../shared/api/httpClient.js'
export const sessionPath = id => `/v2/agent-sessions/${encodeURIComponent(id)}`
export const runPath = id => `/v2/agent-executions/${encodeURIComponent(id)}`
// Read-only replay stream; POST admission uses the shared CSRF client. Never put credentials in URLs.
export async function streamExecution(id, { after = 0, signal, onEvent }) {
  const generation = getAccountGeneration(), controller = new AbortController()
  const unsubscribe = subscribeAccount(() => controller.abort())
  const stop = () => controller.abort()
  signal?.addEventListener('abort', stop, { once: true })
  let cursor = after
  try {
    if (signal?.aborted) controller.abort()
    const response = await fetch(`${API_BASE_URL}${runPath(id)}/events`, { credentials: 'same-origin', signal: controller.signal, headers: { Accept: 'text/event-stream', 'Last-Event-ID': String(after) } })
    if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream') || !response.body) throw new ApiError('Agent stream is unavailable. Resume the saved execution.', { status: response.status })
    const reader = response.body.getReader(), decoder = new TextDecoder()
    let buffer = ''
    function dispatch(block) {
      const lines = block.split('\n'), data = lines.filter(l => l.startsWith('data:')).map(l => l.slice(5).trimStart()).join('\n')
      if (!data) return
      const sequence = Number(lines.find(l => l.startsWith('id:'))?.slice(3)), type = lines.find(l => l.startsWith('event:'))?.slice(6).trim()
      if (!Number.isSafeInteger(sequence) || sequence <= cursor || !type) return
      if (generation !== getAccountGeneration() || controller.signal.aborted) throw new ApiError('The account changed. Reload your saved work.', { code: 'STALE_ACCOUNT' })
      const payload = JSON.parse(data); cursor = sequence; onEvent({ sequenceNumber: cursor, type, payload })
    }
    try {
      for (;;) {
        const { value, done } = await reader.read()
        buffer += decoder.decode(value, { stream: !done }).replace(/\r\n/g, '\n')
        if (buffer.length > 48000) throw new ApiError('Agent stream exceeded its safe size.')
        let end
        while ((end = buffer.indexOf('\n\n')) >= 0) { dispatch(buffer.slice(0, end)); buffer = buffer.slice(end + 2) }
        if (done) { if (buffer.trim()) dispatch(buffer); break }
      }
    } finally { await reader.cancel().catch(() => {}) }
    return cursor
  } finally { unsubscribe(); signal?.removeEventListener('abort', stop) }
}
