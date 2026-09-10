import { Menu, X } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import BrandMark from '../ui/BrandMark.jsx'

const navigation = [
  { label: 'How it works', href: '/#how-it-works' },
  { label: 'Career simulations', to: '/simulations' },
  { label: 'RIASEC assessment', to: '/assessment' },
  { label: 'Dashboard', to: '/dashboard' },
]

function AppShell() {
  const [menuOpen, setMenuOpen] = useState(false)

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
            <Link
              className="button button--small"
              to="/assessment"
              onClick={() => setMenuOpen(false)}
            >
              Start exploring
            </Link>
          </nav>
        </div>
      </header>

      <main>
        <Outlet />
      </main>

      <footer className="site-footer">
        <div className="site-footer__inner">
          <BrandMark compact />
          <p>
            Evidence for exploration, not a decision made on a student&apos;s behalf.
          </p>
        </div>
      </footer>
    </div>
  )
}

export default AppShell
