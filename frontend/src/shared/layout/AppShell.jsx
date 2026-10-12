import { Menu, X, ArrowRight, Compass, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import BrandMark from '../ui/BrandMark.jsx'

const navigation = [
  { label: 'How It Works', href: '/#how-it-works' },
  { label: 'IT Career Tracks', to: '/simulations' },
  { label: 'RIASEC Assessment', to: '/assessment' },
  { label: 'Dashboard & Syn', to: '/dashboard' },
]

function AppShell() {
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
            <Compass size={14} aria-hidden="true" />
            Ho Chi Minh City University of Technology and Education (HCM-UTE) · IT Career Exploration Platform
          </span>
          <span className="institutional-bar__tagline">
            Empowering high school students to explore tech careers and shape their future
          </span>
        </div>
      </div>

      <header className="site-header">
        <div className="site-header__inner">
          <Link className="brand-link" to="/" aria-label="HCM-UTE Career Simulation Platform Home">
            <BrandMark />
          </Link>

          <button
            className="menu-button"
            type="button"
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((current) => !current)}
          >
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>

          <nav className={`site-nav ${menuOpen ? 'site-nav--open' : ''}`}>
            {navigation.map((item) =>
              item.to ? (
                <NavLink
                  key={item.label}
                  to={item.to}
                  onClick={() => setMenuOpen(false)}
                >
                  {item.label}
                </NavLink>
              ) : (
                <a
                  key={item.label}
                  href={item.href}
                  onClick={() => setMenuOpen(false)}
                >
                  {item.label}
                </a>
              ),
            )}
            <button
              className="button button--small button--cta-header"
              type="button"
              onClick={handleStartExploring}
            >
              Get Started <ArrowRight size={15} aria-hidden="true" />
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
            <p className="site-footer__description">
              IT career exploration and simulation platform for high school students developed by Ho Chi Minh City University of Technology and Education (HCM-UTE), fostering informed decisions through experiential learning, standardized assessments, and transparent evidence.
            </p>
            <div className="site-footer__trust">
              <ShieldCheck size={16} aria-hidden="true" />
              <span>Holland RIASEC Framework · Objective Evaluation · Supervised AI Guidance</span>
            </div>
          </div>

          <nav className="site-footer__nav" aria-label="Footer navigation links">
            <div>
              <strong>Career Exploration</strong>
              <Link to="/assessment">RIASEC Assessment</Link>
              <Link to="/simulations">IT Career Simulations</Link>
              <a href="/#starter-catalog">Starter Projects</a>
            </div>
            <div>
              <strong>Learning Journey</strong>
              <Link to="/dashboard">Progress Dashboard</Link>
              <Link to="/dashboard">Syn Guidance Assistant</Link>
              <a href="/#how-it-works">3-Step Process</a>
            </div>
          </nav>
        </div>

        <div className="site-footer__bottom">
          <div className="site-footer__bottom-inner">
            <span>© {new Date().getFullYear()} Ho Chi Minh City University of Technology and Education (HCM-UTE). Developed for OOSE / Scientific Research Project.</span>
            <span className="site-footer__disclaimer">
              Notice: This platform provides exploratory guidance and hands-on experience; it does not replace personal career decisions.
            </span>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default AppShell
