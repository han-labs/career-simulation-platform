import { useI18n } from '../../shared/i18n/useI18n.js'

import { quickActions } from './agentQuickActions.js'

export default function AgentQuickActions({ kind, onChoose, disabled }) {
  const { t } = useI18n()
  return <details className="agent-quick-actions"><summary>{t('Quick Actions')}</summary><div>{quickActions[kind].map(text => <button type="button" className="button button--small button--secondary" disabled={disabled} key={text} onClick={() => onChoose(t(text))}>{t(text)}</button>)}</div></details>
}
