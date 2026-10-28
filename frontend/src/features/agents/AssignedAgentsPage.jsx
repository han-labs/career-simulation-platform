import { useEffect, useState } from 'react'
import { Link, useOutletContext } from 'react-router-dom'
import { apiRequest } from '../../shared/api/httpClient.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
export default function AssignedAgentsPage() {
  const { t } = useI18n(), [agents, setAgents] = useState(null), [error, setError] = useState('')
  const open = useOutletContext()?.openStudentAgent
  useEffect(() => { let active = true; apiRequest('/v2/agents').then(rows => { if (active) setAgents(rows) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [])
  useEffect(() => { const agent = agents?.find(row => row.runtimeAvailable !== false); if (agent) open?.({ agentId: agent.id }) }, [agents, open])
  return <section className="feature-page section-shell"><h1>{t('Career Agent')}</h1><p>{t('The published Agent available to your role.')}</p>{error && <p role="alert">{t(error)}</p>}{!agents && !error && <p role="status">{t('Loading available Agent...')}</p>}{agents?.length === 0 && <p>{t('No published Agent is active for your role.')}</p>}<ul>{agents?.map(a => <li key={a.id}><h2>{a.runtimeAvailable === false ? a.name : <Link to={`/agents/${encodeURIComponent(a.id)}`}>{a.name}</Link>}</h2><p>{a.purpose}</p>{a.runtimeAvailable === false && <p role="status">{t('Enterprise chat runtime is not available. Role publication does not grant Student-private tools.')}</p>}</li>)}</ul></section>
}
