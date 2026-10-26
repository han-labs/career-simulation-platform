export const initialFilters = { range: '7d', agent: '', provider: '', model: '', outcome: '' }
export function usageRequest(filters, now = new Date()) {
  const days = ({ '24h': 1, '7d': 7, '30d': 30, '366d': 366 })[filters.range] ?? 7
  return { ...filters, from: new Date(now.getTime() - days * 86400000).toISOString(), to: now.toISOString() }
}
export function usageUrl(request, cursor) {
  const query = new URLSearchParams({ view: 'dashboard', from: request.from, to: request.to, limit: '20' })
  for (const key of ['agent', 'provider', 'model', 'outcome']) if (request[key]) query.set(key, request[key])
  if (cursor) query.set('cursor', cursor)
  return '/v2/admin/agent-usage?' + query
}
