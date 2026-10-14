import { useEffect, useState } from 'react'
import { apiRequest } from '../../shared/api/httpClient.js'
import { command } from '../../shared/api/coreApi.js'
import { useI18n } from '../../shared/i18n/useI18n.js'

export default function AccountPage() {
  const { t } = useI18n()
  const [account, setAccount] = useState(null)
  const [mode, setMode] = useState('login')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => { apiRequest('/v2/me').then(setAccount).catch(() => {}) }, [])
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    const form = new FormData(event.currentTarget)
    try {
      if (mode === 'register') {
        await command('/v2/auth/register', Object.fromEntries(form))
        setMode('login'); setNotice('Account created. Sign in to continue.')
      } else setAccount(await command('/v2/auth/login', { email: form.get('email'), password: form.get('password') }))
      event.target.reset()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
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
  return <section className="feature-page section-shell core-flow"><h1>{t('Account and privacy')}</h1>
    {error && <p role="alert">{t(error)}</p>}{notice && <p role="status">{t(notice)}</p>}
    {!account ? <><div><button type="button" onClick={() => setMode('login')}>{t('Sign in')}</button> <button type="button" onClick={() => setMode('register')}>{t('Register')}</button></div>
      <form onSubmit={submit}><label>{t('Email')}<input type="email" name="email" required autoComplete="username" maxLength={255} /></label><label>{t('Password')}<input type="password" name="password" required minLength={mode === 'register' ? 12 : undefined} maxLength={128} autoComplete={mode === 'register' ? 'new-password' : 'current-password'} /></label>
        {mode === 'register' && <><label>{t('Display name')}<input name="displayName" required maxLength={150} /></label><label>{t('Role')}<select name="role"><option value="STUDENT">{t('Student')}</option><option value="ENTERPRISE">{t('Enterprise')}</option></select></label><label>{t('Enterprise name (enterprise accounts only)')}<input name="enterpriseName" maxLength={240} /></label></>}
        <button className="button" disabled={busy}>{t(mode === 'register' ? 'Register' : 'Sign in')}</button></form></> : <>
      <p>{account.displayName} · {account.role}</p><button onClick={logout}>{t('Sign out')}</button>
      <h2>{t('Purpose-specific consent')}</h2><p>{t('Consent is optional. Assessment and simulations work without external AI consent. Revoking consent blocks future access; it cannot recall data already delivered.')}</p>
      <ul>{account.consents?.map(g => <li key={g.id}>{g.purpose} · {g.recipientKey} · {t(g.effective ? 'Active' : 'Revoked or expired')} {g.effective && <button disabled={busy} onClick={() => revoke(g.id)}>{t('Revoke')}</button>}</li>)}</ul>
      <form onSubmit={grant}><label>{t('Purpose')}<select name="purpose"><option value="EXTERNAL_AI">{t('External AI processing')}</option><option value="PROFILE_SHARE">{t('Share profile')}</option><option value="COMPLETION_SHARE">{t('Share completions')}</option><option value="ASSESSMENT_SHARE">{t('Share assessment results')}</option><option value="SUBMISSION_SHARE">{t('Share submissions with their owner enterprise')}</option></select></label>
        <label>{t('AI provider')}<select name="recipientKey"><option value="provider:gemini">Gemini</option><option value="provider:openai">OpenAI</option></select></label>
        <label>{t('Recipient enterprise ID (sharing only)')}<input name="enterpriseId" inputMode="numeric" pattern="[1-9][0-9]*" /></label>
        <label><input type="checkbox" name="confirmed" required />{t('I explicitly confirm this purpose and recipient under policy careersim-purpose-v1.')}</label><button disabled={busy}>{t('Confirm consent')}</button></form>
    </>}
  </section>
}
