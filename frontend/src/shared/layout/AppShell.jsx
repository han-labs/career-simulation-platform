import { useI18n } from '../i18n/useI18n.js'
import { Menu, X, ArrowRight, Compass, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import BrandMark from '../ui/BrandMark.jsx'
import LanguageSwitcher from '../i18n/LanguageSwitcher.jsx'

const navigation = [
  { label: 'How It Works', href: '/#how-it-works' },
  { label: 'IT Career Tracks', to: '/simulations' },
  { label: 'Career Assessment', to: '/assessment' },
  { label: 'Dashboard & Syn', to: '/dashboard' },
]

function AppShell() {
  const { t } = useI18n()
  const [menuOpen, setMenuOpen] = useState(false)
  const navigate = useNavigate()

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
          <span className="institutional-bar__tagline"> {t("Empowering students to explore tech careers and shape their future")} </span>
        </div>
      </div>

      <header className="site-header">
        <div className="site-header__inner">
          <Link className="brand-link" to="/" aria-label={t("HCM-UTE Career Simulation Platform Home")}>
            <BrandMark />
          </Link>

          <LanguageSwitcher />

          <button
            className="menu-button"
            type="button"
            aria-label={t(menuOpen ? 'Close menu' : 'Open menu')}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((current) => !current)}
          >
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>

          <nav className={`site-nav ${menuOpen ? 'site-nav--open' : ''}`}>
            <NavLink to="/account" onClick={() => setMenuOpen(false)}>{t('Account and privacy')}</NavLink>
            {navigation.map((item) =>
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
            <button
              className="button button--small button--cta-header"
              type="button"
              onClick={handleStartExploring}
            > {t("Career Assessment")} <ArrowRight size={15} aria-hidden="true" />
            </button>
          </nav>
        </div>
      </header>

      <main id="main-content">
        <Outlet />
      </main>

      <footer className="site-footer">
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
              <Link to="/assessment">{t("Career Assessment")}</Link>
              <Link to="/simulations">{t("IT Career Simulations")}</Link>
              <a href="/#starter-catalog">{t("Starter Projects")}</a>
            </div>
            <div>
              <strong>{t("Learning Journey")}</strong>
              <Link to="/dashboard">{t("Progress Dashboard")}</Link>
              <Link to="/dashboard">{t("Syn Guidance Assistant")}</Link>
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
      </footer>
    </div>
  )
}

export default AppShell
