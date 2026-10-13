export function safeSourceUrl(value) {
  if (typeof value !== 'string') return null
  if (/^\/api\/v1\/guidance\/knowledge\/chunks\/\d+$/.test(value)) return value
  try {
    const url = new URL(value)
    return url.protocol === 'https:' && !url.username && !url.password ? url.href : null
  } catch { return null }
}
