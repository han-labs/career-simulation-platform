import { NavLink } from 'react-router-dom'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import './agentPlatform.css'
export default function PlatformNav() {
  const { t } = useI18n()
  return <nav className="platform-nav" aria-label={t('Agent platform administration')}><NavLink to="/admin/knowledge">{t('Knowledge Base Studio')}</NavLink><NavLink to="/admin/agents">{t('Agent Builder')}</NavLink><NavLink to="/account">{t('Account and privacy')}</NavLink></nav>
}
