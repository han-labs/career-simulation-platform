import { useI18n } from '../i18n/useI18n.js'
import { Menu, X, ArrowRight, Compass, ShieldCheck } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import BrandMark from '../ui/BrandMark.jsx'
import LanguageSwitcher from '../i18n/LanguageSwitcher.jsx'
import { apiRequest } from '../api/httpClient.js'
import { command } from '../api/coreApi.js'

const navigation = [
  { label: 'Home', to: '/' },
  { label: 'How It Works', href: '/#how-it-works' },
  { label: 'IT Career Tracks', to: '/simulations' },
  { label: 'Career Assessments', to: '/assessment' },
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'Guidance', to: '/guidance' },
]

function AppShell() {
  const { t } = useI18n()
  const [menuOpen, setMenuOpen] = useState(false)
  const [account, setAccount] = useState(null)
  const [localWorkspace,setLocalWorkspace]=useState(null)
  const [logoutError, setLogoutError] = useState('')
  const [loggingOut, setLoggingOut] = useState(false)
  const logoutPending = useRef(false)
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
      navigate('/account', { replace: true })
    } catch (failure) { setLogoutError(failure.message) }
    finally { logoutPending.current = false; setLoggingOut(false) }
  }

  const handleStartExploring = () => {
    setMenuOpen(false)
    navigate('/assessment')
  }

  return (
    <div className="app-shell">
      {/* Top institutional strip */}
      <div className="institutional-bar">
        <div className="institutional-bar__inner">
          <span className="institutional-bar__badge">
            <Compass size={14} aria-hidden="true" /> {t("Ho Chi Minh City University of Technology and Engineering (HCM-UTE) · Career Simulation Platform")} </span>
          {!adminStudio && <span className="institutional-bar__tagline"> {t("Empowering students to explore tech careers and shape their future")} </span>}
        </div>
      </div>

      <header className="site-header">
        <div className="site-header__inner">
          <Link className="brand-link" to={adminStudio ? "/admin" : "/"} aria-label={t("HCM-UTE Career Simulation Platform Home")}>
            <BrandMark />
          </Link>

          <LanguageSwitcher />
          {account && localWorkspace === true && <span className="session-local-workspace" role="status" title={t('Local mode automatically provides Administrator access. Use auth-test to test sign-in and sign-out.')}>{t('Local workspace')}</span>}
          {account && localWorkspace === false && <button type="button" className="button button--small session-signout" disabled={loggingOut} onClick={logout}>{t('Sign out')}</button>}

          {!adminStudio && <button
            className="menu-button"
            type="button"
            aria-label={t(menuOpen ? 'Close menu' : 'Open menu')}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((current) => !current)}
          >
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>}

          {!adminStudio && <nav className={`site-nav ${menuOpen ? 'site-nav--open' : ''}`}>
            {account?.role === 'ADMIN' && <NavLink to="/admin" onClick={() => setMenuOpen(false)}>{t('Admin Studio')}</NavLink>}
            {!adminStudio && navigation.map((item) =>
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
            {!adminStudio && <button
              className="button button--small button--cta-header"
              type="button"
              onClick={handleStartExploring}
            > {t("Career Assessment")} <ArrowRight size={15} aria-hidden="true" />
            </button>}
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
              <Link to="/dashboard">{t("Progress Dashboard")}</Link>
              <a href="/#how-it-works">{t("3-Step Process")}</a>
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
