import { Sparkles } from 'lucide-react'
import { useI18n } from '../i18n/useI18n.js'
import './ui.css'

export default function AgentActivity() {
  const { t } = useI18n()
  return <span className="agent-activity" role="status" aria-label={t('Agent is preparing a response')}>
    <Sparkles size={19} aria-hidden="true" />
    <span className="agent-activity-dots" aria-hidden="true"><i /><i /><i /></span>
    <span className="sr-only">{t('Agent is preparing a response')}</span>
  </span>
}
