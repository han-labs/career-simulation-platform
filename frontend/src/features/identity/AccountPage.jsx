import { useEffect, useState } from 'react'
import { apiRequest } from '../../shared/api/httpClient.js'
import { command } from '../../shared/api/coreApi.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import { Link, useSearchParams } from 'react-router-dom'
import { agentPlatformEnabled } from '../admin/agentPlatform/platformApi.js'
import AuthenticationForm from './AuthenticationForm.jsx'

export default function AccountPage() {
  const { t } = useI18n()
  const [search] = useSearchParams()
  const oauthError = search.has('oauthError') ? (search.get('oauthError') === 'link-required' ? 'Sign in to your existing account, then explicitly link Google.' : 'Google sign-in was not completed. Please try again.') : ''
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [retry, setRetry] = useState(0)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [google, setGoogle] = useState(false)
  useEffect(() => { let active = true; apiRequest('/v2/auth/providers').then(p => { if (active) setGoogle(p.google) }).catch(() => {}); return () => { active = false } }, [])
  useEffect(() => {
    let active = true
    apiRequest('/v2/me').then(value => { if (active) setAccount(value) }).catch(e => {
      if (active && e.status !== 401) setLoadError('The platform is not reachable. Please try again shortly.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [retry])
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
    {loading ? <p role="status">{t('Checking your session…')}</p> : loadError ? <><p role="alert">{t(loadError)}</p><button onClick={() => { setLoadError(''); setLoading(true); setRetry(value => value + 1) }}>{t('Try again')}</button></> : !account ? <AuthenticationForm google={google} onAuthenticated={setAccount} /> : <>
      <p>{account.displayName} · {account.role}</p><button onClick={logout}>{t('Sign out')}</button>
      {account.role === 'STUDENT' && <p><Link className="button button--primary" to="/assessment">{t('Career Assessments')}</Link></p>}
      {account.role === 'ENTERPRISE' && <Link to="/enterprise">{t('Enterprise workspace')}</Link>}
      {account.role === 'ADMIN' && <Link to="/admin/assessments">{t('Assessment administration')}</Link>}
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
