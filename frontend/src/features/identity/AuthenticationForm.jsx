import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { command } from '../../shared/api/coreApi.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import './identity.css'
import googleLogo from './assets/google-g.png'

const duplicateMessage = 'An account with this email already exists. Please sign in.'
const unavailableMessage = 'Registration is temporarily unavailable. Please try again later.'

export default function AuthenticationForm({ google, providerError = false, onRetryProvider, onAuthenticated }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const formRef = useRef(null)
  const pending = useRef(false)
  const [mode, setMode] = useState('login')
  const [role, setRole] = useState('STUDENT')
  const [busy, setBusy] = useState(false)
  const [authorizing, setAuthorizing] = useState(false)
  const [visible, setVisible] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [fields, setFields] = useState({})
  const registering = mode === 'register'

  useEffect(() => {
    if (busy) return
    const first = Object.keys(fields)[0] ?? (notice ? 'email' : null)
    if (first) formRef.current?.elements.namedItem(first)?.focus()
  }, [fields, busy, notice])

  function changeMode(next) {
    if (pending.current || authorizing) return
    formRef.current?.reset()
    setMode(next); setRole('STUDENT'); setVisible(false); setFields({}); setError(''); setNotice('')
  }

  function showFields(errors, message) {
    setFields(errors); setError(message)
  }

  async function submit(event) {
    event.preventDefault()
    if (pending.current || authorizing) return
    const form = formRef.current
    const values = Object.fromEntries(new FormData(form))
    const errors = {}
    for (const field of Array.from(form.elements)) {
      if (!field.name || field.validity.valid) continue
      errors[field.name] = field.validity.valueMissing || !field.value.trim()
        ? 'This field is required.'
        : field.name === 'email' ? 'Enter a valid email address.' : 'Check this field.'
    }
    if (registering) {
      if (!values.displayName.trim()) errors.displayName = 'This field is required.'
      if (values.password.length < 12 || values.password.length > 128) errors.password = 'Use between 12 and 128 characters.'
      if (role === 'ENTERPRISE' && !values.enterpriseName.trim()) errors.enterpriseName = 'This field is required.'
    }
    setNotice('')
    if (Object.keys(errors).length) { showFields(errors, 'Please check the highlighted fields.'); return }
    pending.current = true; setBusy(true); setError(''); setFields({})
    try {
      if (registering) {
        await command('/v2/auth/register', { ...values, email: values.email.trim(), displayName: values.displayName.trim() })
        form.reset(); setMode('login'); setVisible(false); setRole('STUDENT')
        setNotice('Account created. Sign in to continue.')
        form.elements.namedItem('email')?.focus()
      } else {
        const account = await command('/v2/auth/login', { email: values.email.trim(), password: values.password })
        form.reset(); onAuthenticated(account)
      }
    } catch (failure) {
      const serverFields = Object.fromEntries((failure.payload?.fieldErrors ?? []).map(f => [f.field, f.message]))
      if (registering && failure.status === 409) showFields({ email: duplicateMessage }, duplicateMessage)
      else if (Object.keys(serverFields).length) showFields(serverFields, 'Please check the highlighted fields.')
      else setError(registering && (!failure.status || failure.status >= 500) ? unavailableMessage : failure.message)
    } finally { pending.current = false; setBusy(false) }
  }

  function attributes(name, hint) {
    return { id: `identity-${name}`, name, 'aria-invalid': fields[name] ? true : undefined,
      'aria-describedby': [hint, fields[name] && `identity-${name}-error`].filter(Boolean).join(' ') || undefined }
  }
  function fieldError(name) { return fields[name] && <span className="identity-field-error" id={`identity-${name}-error`}>{t(fields[name])}</span> }

  const locked = busy || authorizing
  const providerNote = providerError ? 'Google availability could not be checked. Try again or use email and password.'
    : google == null ? 'Checking Google availability…' : !google ? 'Google sign-in is not configured in this environment. You can use email and password.' : ''

  return <div className="identity-layout">
    <aside className="identity-intro"><p className="identity-eyebrow">CareerSim</p><h2>{t('Your space to explore what comes next')}</h2>
      <p>{t('Save your career assessments and return to your exploration at your own pace.')}</p>
      <p>{t('You can take assessments and explore simulations without enabling AI processing.')}</p>
      <Link to="/simulations">{t('Explore simulations')}</Link>
    </aside>
    <div className="identity-card">
      <div className="identity-modes" aria-label={t('Account access')}>
        <button type="button" aria-pressed={!registering} disabled={locked} onClick={() => changeMode('login')}>{t('Sign in')}</button>
        <button type="button" aria-pressed={registering} disabled={locked} onClick={() => changeMode('register')}>{t('Register')}</button>
      </div>
      <h2>{t(registering ? 'Create your account' : 'Welcome back')}</h2>
      <p>{t(registering ? 'Required fields are marked with an asterisk.' : 'Sign in to continue your career exploration.')}</p>
      {notice && <p role="status">{t(notice)}</p>}
      {error && <p role="alert">{t(error)}</p>}
      <form key={mode} ref={formRef} noValidate onSubmit={submit} aria-busy={locked}>
        <fieldset disabled={locked} className="identity-fields">
          {registering && <div><label htmlFor="identity-displayName">{t('Display name')} *</label><input {...attributes('displayName')} required maxLength={150} autoComplete="nickname" />{fieldError('displayName')}</div>}
          <div><label htmlFor="identity-email">{t('Email')} *</label><input {...attributes('email')} type="email" required autoComplete="username" maxLength={255} />{fieldError('email')}</div>
          <div><label htmlFor="identity-password">{t('Password')} *</label>
            <div className="identity-password"><input {...attributes('password', registering ? 'identity-password-hint' : undefined)} type={visible ? 'text' : 'password'} required minLength={registering ? 12 : undefined} maxLength={128} autoComplete={registering ? 'new-password' : 'current-password'} />
              <button type="button" aria-controls="identity-password" aria-pressed={visible} onClick={() => setVisible(v => !v)}>{t(visible ? 'Hide password' : 'Show password')}</button></div>
            {registering && <small id="identity-password-hint">{t('Use between 12 and 128 characters.')}</small>}{fieldError('password')}
          </div>
          {registering && <><div><label htmlFor="identity-role">{t('Role')} *</label><select {...attributes('role')} value={role} onChange={e => { setRole(e.target.value); setFields({}); setError('') }}><option value="STUDENT">{t('Student')}</option><option value="ENTERPRISE">{t('Enterprise')}</option></select></div>
            {role === 'ENTERPRISE' && <div><label htmlFor="identity-enterpriseName">{t('Enterprise name')} *</label><input {...attributes('enterpriseName')} required maxLength={240} autoComplete="organization" />{fieldError('enterpriseName')}</div>}</>}
          <button className="button button--primary" type="submit">{t(busy ? (registering ? 'Creating account…' : 'Signing in…') : registering ? 'Create account' : 'Sign in')}</button>
        </fieldset>
      </form>
      <div className="identity-separator"><span>{t('or')}</span></div>
      {google ? <a className="identity-google" href="/api/oauth2/authorization/google" aria-disabled={locked || undefined}
        onClick={e => { if (pending.current || authorizing) { e.preventDefault(); return } setAuthorizing(true) }}>
        <img src={googleLogo} width="20" height="20" alt="" />{t(authorizing ? 'Connecting to Google…' : 'Continue with Google')}</a>
        : <button type="button" className="identity-google" disabled aria-describedby="identity-provider-note"><img src={googleLogo} width="20" height="20" alt="" />{t('Continue with Google')}</button>}
      {providerNote && <p id="identity-provider-note" className="identity-provider-note" aria-live="polite">{t(providerNote)}
        {providerError && <button type="button" className="identity-provider-retry" onClick={onRetryProvider}>{t('Try again')}</button>}</p>}
      {authorizing && <p className="identity-provider-note" role="status">{t('Opening Google securely. If navigation is blocked, try again.')} <button type="button" className="identity-provider-retry" onClick={() => setAuthorizing(false)}>{t('Try again')}</button></p>}
      {registering && <button type="button" disabled={locked} className="identity-cancel" onClick={() => { formRef.current?.reset(); navigate('/') }}>{t('Cancel registration')}</button>}
    </div>
  </div>
}
