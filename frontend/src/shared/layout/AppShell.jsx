import { Menu, X } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import BrandMark from '../ui/BrandMark.jsx'

const navigation = [
  { label: 'How it works', href: '/#how-it-works' },
  { label: 'Career simulations', href: '/#starter-catalog' },
  { label: 'RIASEC assessment', to: '/assessment' },
  { label: 'Dashboard', to: '/dashboard' },
]

function AppShell() {
  const [menuOpen, setMenuOpen] = useState(false)

  const handleStartExploring = (e) => {
    e.preventDefault();
    setMenuOpen(false);
    
    const startY = window.scrollY;
    const endY = document.body.scrollHeight - window.innerHeight;
    const distance = endY - startY;
    const duration = 5000;
    let startTime = null;

    const step = (timestamp) => {
      if (!startTime) startTime = timestamp;
      const progress = timestamp - startTime;
      const percent = Math.min(progress / duration, 1);
      
      window.scrollTo(0, startY + distance * percent);
      
      if (progress < duration) {
        window.requestAnimationFrame(step);
      }
    };
    
    window.requestAnimationFrame(step);
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="site-header__inner">
          <Link className="brand-link" to="/" aria-label="CareerSim home">
            <BrandMark />
          </Link>

          <button
            className="menu-button"
            type="button"
            aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((current) => !current)}
          >
            {menuOpen ? <X size={22} /> : <Menu size={22} />}
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
              className="button button--small"
              onClick={handleStartExploring}
              style={{ cursor: 'pointer' }}
            >
              Start exploring
            </button>
          </nav>
        </div>
      </header>

      <main>
        <Outlet />
      </main>

      <footer className="site-footer">
        <div className="site-footer__inner">
          <div className="site-footer__brand">
            <BrandMark compact />
            <p>Explore IT careers through interests, practice, and evidence.</p>
          </div>

          <nav className="site-footer__nav" aria-label="Footer navigation">
            <div>
              <strong>Explore</strong>
              <Link to="/assessment">RIASEC assessment</Link>
              <Link to="/simulations">Career simulations</Link>
            </div>
            <div>
              <strong>Your journey</strong>
              <Link to="/dashboard">Dashboard &amp; Syn</Link>
              <a href="/#how-it-works">How it works</a>
            </div>
          </nav>
        </div>
        <div className="site-footer__bottom">
          <span>© {new Date().getFullYear()} CareerSim</span>
          <span>Academic project · Guidance supports exploration, not final decisions.</span>
        </div>
      </footer>
    </div>
  )
}

export default AppShell
