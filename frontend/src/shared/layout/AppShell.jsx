import { useI18n } from '../i18n/useI18n.js'
import { Menu, X, ShieldCheck } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import BrandMark from '../ui/BrandMark.jsx'
import LanguageSwitcher from '../i18n/LanguageSwitcher.jsx'
import { apiRequest } from '../api/httpClient.js'
import { command, coreEnabled } from '../api/coreApi.js'

const navigation = [
  { label: 'Home', to: '/' },
  { label: 'How It Works', href: '/#how-it-works' },
  { label: 'Career Simulations', to: '/simulations' },
  { label: 'Career Quizzes', to: '/assessment' },
  { label: 'For Enterprise', to: '/enterprise', coreOnly: true },
  { label: 'Dashboard', to: '/guidance', studentOnly: true },
]

function AppShell() {
  const { t } = useI18n()
  const [menuOpen, setMenuOpen] = useState(false)
  const [account, setAccount] = useState(null)
  const [localWorkspace,setLocalWorkspace]=useState(null)
  const [logoutError, setLogoutError] = useState('')
  const [loggingOut, setLoggingOut] = useState(false)
  const logoutPending = useRef(false)
  const menuTrigger = useRef(null)
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const adminStudio = /^\/admin(?:\/|$)/.test(pathname) || pathname === '/account'
  useEffect(() => {
    let active = true
    apiRequest('/v2/auth/providers').then(value=>{if(active)setLocalWorkspace(value.localWorkspace===true)}).catch(()=>{if(active)setLocalWorkspace(null)})
    apiRequest('/v2/me').then(value => { if (active) setAccount(value) }).catch(() => {})
    return () => { active = false }
  }, [])

  async function logout() {
    if (logoutPending.current) return
    logoutPending.current = true; setLoggingOut(true); setLogoutError('')
    try {
      await command('/v2/auth/logout', {})
      // httpClient invalidates the account generation and clears CSRF/session UI.
      setAccount(null)
      try { sessionStorage.removeItem('careersim_pending_simulation_path') } catch { /* Optional tab state. */ }
      navigate('/', { replace: true })
    } catch (failure) { setLogoutError(failure.message) }
    finally { logoutPending.current = false; setLoggingOut(false) }
  }

  return (
    <div className="app-shell">
      <header className="site-header hcmute-header" onKeyDown={event => { if (event.key === 'Escape' && menuOpen) { setMenuOpen(false); menuTrigger.current?.focus() } }}>
        <div className="site-header__inner">
          <Link className="brand-link" to="/" aria-label={t("HCM-UTE Career Simulation Platform Home")}>
            <BrandMark />
          </Link>

          {!adminStudio && !account && <Link className="button button--primary header-auth" to="/account" onClick={() => setMenuOpen(false)}>{t('Sign In / Sign Up')}</Link>}
          <LanguageSwitcher />
          {account && localWorkspace === true && <span className="session-local-workspace" role="status" title={t('Developer auto-admin is explicitly enabled. Disable LOCAL_DEMO_MODE to use real authentication.')}>{t('Local demo')}</span>}
          {account && localWorkspace === false && <button type="button" className="button button--small session-signout" disabled={loggingOut} onClick={logout}>{t('Sign out')}</button>}

          {!adminStudio && <button
            ref={menuTrigger}
            className="menu-button"
            aria-controls="site-navigation"
            type="button"
            aria-label={t(menuOpen ? 'Close menu' : 'Open menu')}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((current) => !current)}
          >
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>}

          {!adminStudio && <nav id="site-navigation" aria-label={t('Main navigation')} className={`site-nav ${menuOpen ? 'site-nav--open' : ''}`}>
            {account?.role === 'ADMIN' && <NavLink to="/admin" onClick={() => setMenuOpen(false)}>{t('Admin Studio')}</NavLink>}
            {!adminStudio && navigation.filter(item => (!item.coreOnly || coreEnabled) && (!item.studentOnly || account?.role === 'STUDENT')).map((item) =>
              item.to ? (
                <NavLink
                  key={item.label}
                  to={item.to}
                  onClick={() => setMenuOpen(false)}
                >
                  {t(item.label)}
                </NavLink>
              ) : (
                <a
                  key={item.label}
                  href={item.href}
                  onClick={() => setMenuOpen(false)}
                >
                  {t(item.label)}
                </a>
              ),
            )}
          </nav>}
        </div>
      </header>

      <main id="main-content" tabIndex={-1}>
        {logoutError && <p role="alert">{t(logoutError)}</p>}
        <Outlet />
      </main>

      {!adminStudio && <footer className="site-footer">
        <div className="site-footer__inner">
          <div className="site-footer__brand">
            <BrandMark compact />
            <p className="site-footer__description"> {t("Explore careers through practical tasks, career assessment and guidance from Ho Chi Minh City University of Technology and Engineering (HCM-UTE).")} </p>
            <div className="site-footer__trust">
              <ShieldCheck size={16} aria-hidden="true" />
              <span>{t("Objective scores · Guidance you control")}</span>
            </div>
          </div>

          <nav className="site-footer__nav" aria-label={t("Footer navigation links")}>
            <div>
              <strong>{t("Career Exploration")}</strong>
              <Link to="/assessment">{t("Career Assessments")}</Link>
              <Link to="/simulations">{t("IT Career Simulations")}</Link>
              <a href="/#starter-catalog">{t("Starter Projects")}</a>
            </div>
            <div>
              <strong>{t("Learning Journey")}</strong>
              {account?.role === 'STUDENT' && <Link to="/guidance">{t('Dashboard')}</Link>}
              <a href="/#how-it-works">{t('Four-step journey')}</a>
            </div>
          </nav>
        </div>

        <div className="site-footer__bottom">
          <div className="site-footer__bottom-inner">
            <span>© {t(new Date().getFullYear())} {t("Ho Chi Minh City University of Technology and Engineering (HCM-UTE). Developed for OOSE / Scientific Research Project.")}</span>
            <span className="site-footer__disclaimer"> {t("Explore your options. Your career decisions remain yours.")} </span>
          </div>
        </div>
      </footer>}
    </div>
  )
}

export default AppShell
