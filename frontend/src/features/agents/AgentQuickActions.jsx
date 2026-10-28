import { FileText, FolderOpen, ListTodo } from 'lucide-react'
import { useI18n } from '../../shared/i18n/useI18n.js'
import { quickActions, studentQuickActions } from './agentQuickActions.js'

export default function AgentQuickActions({ kind, compact = false, onChoose, disabled, onReport, onPlan, onSavedReports }) {
  const { t } = useI18n()
  const suggestions = kind === 'student' ? studentQuickActions : quickActions[kind].map(prompt => ({ label: prompt, prompt }))
  const buttons = <div className="agent-quick-action-chips">
    {onReport && <button type="button" disabled={disabled} aria-label={t('Request insight report')} title={t('Request insight report')} onClick={onReport}><FileText size={13} aria-hidden="true" />{t('Insight report')}</button>}
    {onPlan && <button type="button" disabled={disabled} aria-label={t('Suggest draft plan')} title={t('Suggest draft plan')} onClick={onPlan}><ListTodo size={13} aria-hidden="true" />{t('Draft plan')}</button>}
    {onSavedReports && <button type="button" title={t('Saved reports')} onClick={onSavedReports}><FolderOpen size={13} aria-hidden="true" />{t('Saved reports')}</button>}
    {suggestions.map(({ label, prompt }) => <button type="button" className={kind === 'admin' ? 'button button--small button--secondary' : undefined} disabled={disabled} aria-label={t(prompt)} title={t(prompt)} key={prompt} onClick={() => onChoose(t(prompt))}>{t(label)}</button>)}
  </div>
  return kind === 'student' || compact ? <section className="agent-quick-actions" aria-label={t('Quick Actions')}>{buttons}</section> : <details className="agent-quick-actions"><summary>{t('Quick Actions')}</summary>{buttons}</details>
}
