import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../../shared/api/httpClient.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
export default function AssignedAgentsPage() {
  const { t } = useI18n(), [agents, setAgents] = useState(null), [error, setError] = useState('')
  useEffect(() => { let active = true; apiRequest('/v2/agents').then(rows => { if (active) setAgents(rows) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [])
  return <section className="feature-page section-shell"><Link to="/account">{t('Account and privacy')}</Link><h1>{t('My assigned Agents')}</h1><p>{t('These Agents are assigned to your account by an Administrator.')}</p>{error && <p role="alert">{t(error)}</p>}{!agents && !error && <p role="status">{t('Loading assigned Agents...')}</p>}{agents?.length === 0 && <p>{t('No active Agent is currently assigned to your account.')}</p>}<ul>{agents?.map(a => <li key={a.id}><h2>{a.name}</h2><p>{a.purpose}</p></li>)}</ul></section>
}
