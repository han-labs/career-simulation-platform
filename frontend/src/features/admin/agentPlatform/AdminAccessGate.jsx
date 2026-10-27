import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'

// UI guard complements (never replaces) server-side Admin authorization.
export default function AdminAccessGate({ children }) {
  const { t } = useI18n()
  const [state, setState] = useState('loading'), [retry, setRetry] = useState(0)
  useEffect(() => {
    let active = true
    apiRequest('/v2/me').then(account => { if (active) setState(account.role === 'ADMIN' ? 'admin' : 'denied') })
      .catch(error => { if (active) setState(error.status === 401 ? 'anonymous' : error.status === 403 ? 'denied' : 'error') })
    return () => { active = false }
  }, [retry])
  if (state === 'admin') return children
  if (state === 'anonymous') return <Navigate to="/account" replace />
  return <section className="feature-page section-shell core-flow">
    {state === 'loading' ? <p role="status">{t('Checking your session…')}</p>
      : state === 'denied' ? <p role="alert">{t('Administrator access is required.')}</p>
        : <><p role="alert">{t('The platform is not reachable. Please try again shortly.')}</p><button onClick={() => { setState('loading'); setRetry(value => value + 1) }}>{t('Retry')}</button></>}
    <Link to="/">{t('Home')}</Link>
  </section>
}
