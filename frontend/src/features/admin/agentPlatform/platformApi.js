import { apiRequest } from '../../../shared/api/httpClient.js'
import { command } from '../../../shared/api/coreApi.js'
export const agentPlatformEnabled = import.meta.env.VITE_AGENT_PLATFORM_ENABLED === 'true'
export const kbPath = id => `/v2/admin/knowledge-bases/${encodeURIComponent(id)}`
export const agentPath = id => `/v2/admin/agents/${encodeURIComponent(id)}`
export const executionPath = id => `/v2/admin/agent-executions/${encodeURIComponent(id)}`
export const platformCommand = command
export async function uploadSource(kb, file, metadata) {
  const mime = { txt: 'text/plain', md: 'text/markdown', pdf: 'application/pdf', docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }
  const body = new FormData()
  body.append('file', file.type ? file : new File([file], file.name, { type: mime[file.name.split('.').pop().toLowerCase()] ?? 'application/octet-stream' }))
  body.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
  return apiRequest(`${kbPath(kb)}/assets`, { method: 'POST', body })
}
