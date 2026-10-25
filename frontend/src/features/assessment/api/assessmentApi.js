import { apiRequest } from '../../../shared/api/httpClient.js'
import { command } from '../../../shared/api/coreApi.js'

const base = '/v2/assessment-attempts'
export const assessmentApi = {
  actor: () => apiRequest('/v2/me'),
  catalog: () => apiRequest('/v2/assessment-types'),
  history: () => apiRequest(base),
  start: (version, requestId) => command(`/v2/assessment-versions/${version}/attempts`, { requestId }),
  attempt: id => apiRequest(`${base}/${id}`),
  result: id => apiRequest(`${base}/${id}/result`),
  save: (attempt, items, final) => command(`${base}/${attempt.id}/${final ? 'submit' : 'responses'}`, { items }, attempt.revision, final ? 'POST' : 'PUT'),
}
