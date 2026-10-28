import { useEffect } from 'react'
import { useOutletContext, useParams, useSearchParams } from 'react-router-dom'
import { useI18n } from '../../shared/i18n/useI18n.js'

// Route adapter only: AppShell owns the one Student chat and SSE lifecycle.
export default function StudentAgentPage() {
  const { agentId } = useParams(), [search] = useSearchParams(), context = useOutletContext(), { t } = useI18n()
  const open = context?.openStudentAgent, sessionId = search.get('session')
  useEffect(() => { open?.({ agentId, sessionId }) }, [open, agentId, sessionId])
  return <section className="feature-page section-shell"><h1>{t('Career Agent')}</h1><p>{t('Continue in the Career Agent popup. Closing it does not stop a response.')}</p></section>
}
