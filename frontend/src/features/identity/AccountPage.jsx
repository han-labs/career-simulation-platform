import { useEffect, useState } from 'react'
import { apiRequest } from '../../shared/api/httpClient.js'
import { command, coreEnabled } from '../../shared/api/coreApi.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { agentPlatformEnabled } from '../admin/agentPlatform/platformApi.js'
import AuthenticationForm from './AuthenticationForm.jsx'

const oauthMessages = {
  'link-required': 'Sign in to your existing account, then explicitly link Google.',
  cancelled: 'Google sign-in was cancelled. You can try again or use email and password.',
  'provider-unavailable': 'Sign-in is temporarily unavailable. Please try again later.',
  'account-disabled': 'You do not have permission to access this account.',
}

export default function AccountPage() {
  const { t } = useI18n()
  const [search] = useSearchParams()
  const navigate = useNavigate()
  const oauthSuccess = search.get('oauth') === 'success'
  const oauthError = search.has('oauthError') ? (oauthMessages[search.get('oauthError')] ?? 'Your sign-in could not be verified. Please sign in again.') : ''
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [retry, setRetry] = useState(0)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [google, setGoogle] = useState(null)
  const [providerError, setProviderError] = useState(false)
  const [providerRetry, setProviderRetry] = useState(0)
  useEffect(() => {
    let active = true
    apiRequest('/v2/auth/providers').then(p => { if (typeof p.google !== 'boolean') throw new Error('Invalid provider availability'); if (active) setGoogle(p.google) }).catch(() => { if (active) setProviderError(true) })
    return () => { active = false }
  }, [providerRetry])
  useEffect(() => {
    let active = true
    apiRequest('/v2/me').then(value => { if (active) setAccount(value) }).catch(e => {
      if (active && (e.status !== 401 || oauthSuccess)) setLoadError(e.status === 401 ? 'Your sign-in could not be verified. Please sign in again.' : e.status === 403 ? 'You do not have permission to access this account.' : 'The platform is not reachable. Please try again shortly.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [retry, oauthSuccess])
  useEffect(() => {
    if (!oauthSuccess || !account) return
    const destination = { STUDENT: '/assessment', ENTERPRISE: coreEnabled ? '/enterprise' : '/account', ADMIN: coreEnabled ? '/admin/assessments' : '/account' }[account.role]
    if (destination) navigate(destination, { replace: true })
  }, [oauthSuccess, account, navigate])
  async function grant(event) {
    event.preventDefault(); setBusy(true); setError('')
    const form = new FormData(event.currentTarget)
    try {
      const purpose = form.get('purpose'), recipient = form.get('recipientKey')
      const enterpriseId = purpose.endsWith('_SHARE') ? form.get('enterpriseId') : null
      await command('/v2/me/consents', { purpose, enterpriseId, recipientKey: enterpriseId ? `enterprise:${enterpriseId}` : recipient, policyVersion: 'careersim-purpose-v1', confirmed: form.get('confirmed') === 'on' })
      setAccount(await apiRequest('/v2/me')); event.target.reset()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function revoke(id) { setBusy(true); setError(''); try { await command(`/v2/me/consents/${id}/revoke`, {}); setAccount(await apiRequest('/v2/me')) } catch (e) { setError(e.message) } finally { setBusy(false) } }
  async function logout() { try { await command('/v2/auth/logout', {}); setAccount(null) } catch (e) { setError(e.message) } }
  async function linkGoogle() { setBusy(true); try { const r = await command('/v2/auth/google/link', {}); window.location.assign(r.authorizationUrl) } catch (e) { setError(e.message); setBusy(false) } }
  return <section className="feature-page section-shell core-flow"><h1>{t('Account and privacy')}</h1>
    {error && <p role="alert">{t(error)}</p>}
    {oauthError && <p role="alert">{t(oauthError)}</p>}
    {loading ? <p role="status">{t(oauthSuccess ? 'Restoring your Google sign-in session…' : 'Checking your session…')}</p> : loadError ? <><p role="alert">{t(loadError)}</p><button onClick={() => { setLoadError(''); setLoading(true); setRetry(value => value + 1) }}>{t('Try again')}</button>{oauthSuccess && <Link to="/account" onClick={() => setLoadError('')}>{t('Sign in')}</Link>}</> : !account ? <AuthenticationForm google={google} providerError={providerError} onRetryProvider={() => { setGoogle(null); setProviderError(false); setProviderRetry(value => value + 1) }} onAuthenticated={setAccount} /> : <>
      <p>{account.displayName} · {account.role}</p><button onClick={logout}>{t('Sign out')}</button>
      {account.role === 'STUDENT' && <p><Link className="button button--primary" to="/assessment">{t('Career Assessments')}</Link></p>}
      {coreEnabled && account.role === 'ENTERPRISE' && <Link to="/enterprise">{t('Enterprise workspace')}</Link>}
      {coreEnabled && account.role === 'ADMIN' && <Link to="/admin/assessments">{t('Assessment administration')}</Link>}
      {agentPlatformEnabled && account.role === 'ADMIN' && <><Link to="/admin/knowledge">{t('Knowledge Base Studio')}</Link><Link to="/admin/agents">{t('Agent Builder')}</Link></>}
      {agentPlatformEnabled && account.role === 'STUDENT' && <Link to="/agents">{t('My assigned Agents')}</Link>}
      {google && <button disabled={busy} onClick={linkGoogle}>{t('Link Google to this account')}</button>}
      <h2>{t('Purpose-specific consent')}</h2><p>{t('Consent is optional. Assessment and simulations work without external AI consent. Revoking consent blocks future access; it cannot recall data already delivered.')}</p>
      <ul>{account.consents?.map(g => <li key={g.id}>{g.purpose} · {g.recipientKey} · {t(g.effective ? 'Active' : 'Revoked or expired')} {g.effective && <button disabled={busy} onClick={() => revoke(g.id)}>{t('Revoke')}</button>}</li>)}</ul>
      <form onSubmit={grant}><label>{t('Purpose')}<select name="purpose"><option value="EXTERNAL_AI">{t('External AI processing')}</option><option value="PROFILE_SHARE">{t('Share profile')}</option><option value="COMPLETION_SHARE">{t('Share completions')}</option><option value="ASSESSMENT_SHARE">{t('Share assessment results')}</option><option value="SUBMISSION_SHARE">{t('Share submissions with their owner enterprise')}</option></select></label>
        <label>{t('AI provider')}<select name="recipientKey"><option value="provider:gemini">Gemini</option><option value="provider:openai">OpenAI</option></select></label>
        <label>{t('Recipient enterprise ID (sharing only)')}<input name="enterpriseId" inputMode="numeric" pattern="[1-9][0-9]*" /></label>
        <label><input type="checkbox" name="confirmed" required />{t('I explicitly confirm this purpose and recipient under policy careersim-purpose-v1.')}</label><button disabled={busy}>{t('Confirm consent')}</button></form>
    </>}
  </section>
}
