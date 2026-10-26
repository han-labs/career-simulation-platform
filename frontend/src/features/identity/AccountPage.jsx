import { useEffect, useState } from 'react'
import { apiRequest } from '../../shared/api/httpClient.js'
import { command, coreEnabled } from '../../shared/api/coreApi.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import AuthenticationForm from './AuthenticationForm.jsx'

const oauthMessages = {
  'link-required': 'Sign in to your existing account, then explicitly link Google.',
  cancelled: 'Google sign-in was cancelled. You can try again or use email and password.',
  'provider-unavailable': 'Sign-in is temporarily unavailable. Please try again later.',
  'account-disabled': 'You do not have permission to access this account.',
}

const PENDING_SIMULATION_PATH = 'careersim_pending_simulation_path'

function consumePendingSimulation() {
  let pending
  try { pending = sessionStorage.getItem(PENDING_SIMULATION_PATH); sessionStorage.removeItem(PENDING_SIMULATION_PATH) } catch { return }
  const match = typeof pending === 'string' && pending.match(/^\/simulations\/([^/?#]+)$/)
  if (!match) return
  try {
    const slug = decodeURIComponent(match[1])
    if (slug && encodeURIComponent(slug) === match[1]) return pending
  } catch { /* Invalid encoded destinations are cleared and ignored. */ }
}

export default function AccountPage() {
  const { t } = useI18n()
  const [pendingDestination, setPendingDestination] = useState(null)
  const [search] = useSearchParams()
  const oauthSuccess = search.get('oauth') === 'success'
  const oauthError = search.has('oauthError') ? (oauthMessages[search.get('oauthError')] ?? 'Your sign-in could not be verified. Please sign in again.') : ''
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [retry, setRetry] = useState(0)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [google, setGoogle] = useState(null)
  const [localWorkspace,setLocalWorkspace]=useState(false)
  const [providerError, setProviderError] = useState(false)
  const [providerRetry, setProviderRetry] = useState(0)
  useEffect(() => {
    let active = true
    apiRequest('/v2/auth/providers').then(p => { if (typeof p.google !== 'boolean') throw new Error('Invalid provider availability'); if (active) {setGoogle(p.google);setLocalWorkspace(p.localWorkspace===true)} }).catch(() => { if (active) setProviderError(true) })
    return () => { active = false }
  }, [providerRetry])
  useEffect(() => {
    let active = true
    apiRequest('/v2/me').then(value => { if (active) { if (value.role === 'STUDENT') setPendingDestination(consumePendingSimulation()); setAccount(value) } }).catch(e => {
      if (active && (e.status !== 401 || oauthSuccess)) setLoadError(e.status === 401 ? 'Your sign-in could not be verified. Please sign in again.' : e.status === 403 ? 'You do not have permission to access this account.' : 'The platform is not reachable. Please try again shortly.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [retry, oauthSuccess])
  async function logout() {
    if (busy) return
    setBusy(true); setError('')
    try { await command('/v2/auth/logout', {}); setAccount(null) }
    catch (e) { setError(e.message) }
    finally { setBusy(false) }
  }
  // The authenticated server account is the only source for role routing.
  // After login, restore /me before consuming pending navigation. Account-generation
  // remounts may discard this instance; only the active restoration effect consumes it.
  const destination = account && (account.role === 'STUDENT' ? (pendingDestination || '/assessment')
    : account.role === 'ENTERPRISE' && coreEnabled ? '/enterprise'
      : account.role === 'ADMIN' ? '/admin' : null)
  if (destination) return <Navigate to={destination} replace />
  return <section className="feature-page section-shell core-flow"><h1>{t('Account access')}</h1>
    {error && <p role="alert">{t(error)}</p>}
    {oauthError && <p role="alert">{t(oauthError)}</p>}
    {loading ? <p role="status">{t(oauthSuccess ? 'Restoring your Google sign-in session…' : 'Checking your session…')}</p> : loadError ? <><p role="alert">{t(loadError)}</p><button onClick={() => { setLoadError(''); setLoading(true); setRetry(value => value + 1) }}>{t('Try again')}</button>{oauthSuccess && <Link to="/account" onClick={() => setLoadError('')}>{t('Sign in')}</Link>}</> : !account ? <AuthenticationForm google={google} providerError={providerError} onRetryProvider={() => { setGoogle(null); setProviderError(false); setProviderRetry(value => value + 1) }} onAuthenticated={() => { setLoading(true); setRetry(value => value + 1) }} /> : <>
      <p>{account.displayName} · {account.role}</p>
      {account.role !== 'ENTERPRISE' && <p role="alert">{t('You do not have permission to access this account.')}</p>}
      {localWorkspace ? <p role="status">{t('Local workspace')}</p> : <button disabled={busy} onClick={logout}>{t('Sign out')}</button>}
    </>}
  </section>
}
