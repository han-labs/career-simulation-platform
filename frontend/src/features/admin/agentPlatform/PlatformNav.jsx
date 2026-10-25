import { useRef, useState, useSyncExternalStore } from 'react'
import { Bot, ChartNoAxesCombined, Library, Menu, PanelLeftClose, PanelLeftOpen } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import './agentPlatform.css'

// Reuse the UC-07 sidebar/drawer mechanics for the three Admin product areas.
const query = '(max-width: 980px)'
function subscribeViewport(listener) {
  const media = window.matchMedia?.(query)
  media?.addEventListener('change', listener)
  return () => media?.removeEventListener('change', listener)
}
function isMobile() { return window.matchMedia?.(query).matches ?? false }

function NavigationContent({ onNavigate, collapsed = false }) {
  const { t } = useI18n()
  return <nav aria-label={t('Admin Studio navigation')} className="admin-navigation">
    {[
      { to: '/admin/knowledge', label: 'Knowledge Bases', icon: <Library size={20} /> },
      { to: '/admin/agents', label: 'AI Agents', icon: <Bot size={20} /> },
      { to: '/admin/usage', label: 'AI Usage', icon: <ChartNoAxesCombined size={20} /> },
    ].map(item => <NavLink key={item.to} to={item.to} className="admin-nav-link" aria-label={t(item.label)} title={collapsed ? t(item.label) : undefined} onClick={onNavigate}>
      <span aria-hidden="true" className="admin-nav-icon">{item.icon}</span><span className="admin-nav-label">{t(item.label)}</span>
    </NavLink>)}
  </nav>
}

function DesktopNavigation() {
  const { t } = useI18n(), [collapsed, setCollapsed] = useState(false)
  return <aside className={'cs-ui admin-sidebar' + (collapsed ? ' admin-sidebar--collapsed' : '')} aria-label={t('Admin Studio')}>
    <div className="admin-sidebar-heading"><span>{t('Admin Studio')}</span><Button variant="quiet" aria-expanded={!collapsed} aria-controls="admin-desktop-navigation" aria-label={t(collapsed ? 'Expand Admin navigation' : 'Collapse Admin navigation')} title={t(collapsed ? 'Expand Admin navigation' : 'Collapse Admin navigation')} icon={collapsed ? <PanelLeftOpen size={20} /> : <PanelLeftClose size={20} />} onClick={() => setCollapsed(value => !value)} /></div>
    <div id="admin-desktop-navigation" className="admin-sidebar-body"><NavigationContent collapsed={collapsed} /></div>
  </aside>
}

function MobileNavigation() {
  const { t } = useI18n(), [open, setOpen] = useState(false), trigger = useRef(null)
  return <div className="cs-ui admin-mobile-navigation">
    <Button ref={trigger} aria-haspopup="dialog" aria-expanded={open} icon={<Menu size={20} />} onClick={() => setOpen(true)}>{t('Open Admin navigation')}</Button>
    {open && <Dialog className="admin-navigation-drawer" title={t('Admin Studio')} closeLabel={t('Close Admin navigation')} restoreFocusRef={trigger} onRequestClose={() => setOpen(false)}>
      <NavigationContent onNavigate={() => setOpen(false)} />
    </Dialog>}
  </div>
}

export default function PlatformNav() {
  const mobile = useSyncExternalStore(subscribeViewport, isMobile, () => false)
  return mobile ? <MobileNavigation /> : <DesktopNavigation />
}
