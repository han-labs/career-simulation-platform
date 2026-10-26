import { useI18n } from '../../../shared/i18n/useI18n.js'
import StatusBadge from '../../../shared/ui/StatusBadge.jsx'
import { money, number } from './usageFormat.js'

const labels = { SUCCEEDED: 'Succeeded', FAILED: 'Failed', REFUSED: 'Refused', CANCELLED: 'Cancelled', ACCEPTED: 'Accepted', RUNNING: 'Running' }
const categories = new Set(['TIMEOUT_OR_TRANSPORT', 'PROVIDER_REJECTED', 'PROVIDER_RATE_LIMITED', 'PROVIDER_UNAVAILABLE', 'MALFORMED_OUTPUT', 'UNAVAILABLE_TOOL', 'EMPTY_EVIDENCE', 'CONSENT_REVOKED', 'ITERATION_LIMIT', 'EXECUTION_FAILED', 'EXTERNAL_AI_CONSENT_REQUIRED', 'INSUFFICIENT_EVIDENCE', 'REFUSED', 'OTHER'])
export default function UsageExecutions({ rows }) {
  const { t, locale } = useI18n()
  const n = value => value == null ? t('Not available') : number(value, locale)
  const date = value => new Intl.DateTimeFormat(locale === 'vi' ? 'vi-VN' : 'en-GB', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'UTC' }).format(new Date(value))
  return <section className="cs-surface usage-executions"><h2>{t('Recent AI executions')}</h2><p className="cs-caption">{t('Operational metadata only. No Student identity, messages, prompts or tool arguments.')}</p>
    {!rows.length && <p>{t('No executions remain on this page. Return to the previous page or apply filters again.')}</p>}
    <table className="usage-table" role="table"><caption>{t('Recent executions, newest first (UTC)')}</caption><thead><tr>{['Time', 'Agent', 'Model', 'Tools', 'Latency', 'Tokens', 'Cost', 'Outcome'].map(label => <th key={label} scope="col">{t(label)}</th>)}</tr></thead><tbody>
      {rows.map(row => <tr key={row.id}>
        <td data-label={t('Time')}><time dateTime={row.startedAt}>{date(row.startedAt)}</time></td>
        <td data-label={t('Agent')}>{row.agentName ?? t(row.mode === 'KB_TEST' ? 'Knowledge Base test' : 'Historical Agent')}
          <details className="usage-details"><summary>{t('Details')}</summary><dl className="cs-metadata">
            <div><dt>{t('Execution ID')}</dt><dd><code>{row.id}</code></dd></div><div><dt>{t('Execution mode')}</dt><dd>{row.mode}</dd></div>
            <div><dt>{t('Configuration ID')}</dt><dd><code>{row.configurationId ?? t('Not applicable')}</code></dd></div>
            <div><dt>{t('Configuration version')}</dt><dd>{row.configurationVersion ?? t('Not available')}</dd></div>
            <div><dt>{t('Prompt fingerprint')}</dt><dd><code>{row.promptHash ?? t('Not available')}</code></dd></div>
            <div><dt>{t('Recorded model attempts')}</dt><dd>{n(row.modelAttempts)}</dd></div>
            <div><dt>{t('Server retrieval calls')}</dt><dd>{n(row.serverRetrievalCount)}</dd></div>
            {row.failureCategory && <div><dt>{t('Failure or refusal category')}</dt><dd>{categories.has(row.failureCategory) ? row.failureCategory : t('Other')}</dd></div>}
          </dl></details>
        </td>
        <td data-label={t('Model')}><span>{row.model ?? t(row.costStatus === 'NOT_APPLICABLE' ? 'No answer model' : 'Not available')}</span>{row.provider && <small>{row.provider}</small>}{row.modelAttempts === 0 && <small>{t('No model call recorded')}</small>}</td>
        <td data-label={t('Tools')}>{n(row.nativeToolCount)} {t('native attempts')}{row.tools?.length > 0 && <small>{row.tools.filter(tool => ['kb.search', 'assessment.lookup', 'simulation.explore', 'simulation.myEvidence', 'UNKNOWN'].includes(tool)).join(' · ')}</small>}{row.serverRetrievalCount > 0 && <small>{n(row.serverRetrievalCount)} {t('server retrieval')}</small>}</td>
        <td data-label={t('Latency')}>{row.finishedAt && row.latencyMs != null ? n(row.latencyMs) + ' ms' : t('Not available')}</td>
        <td data-label={t('Tokens')}>{row.costStatus === 'NOT_APPLICABLE' ? t('Not applicable') : <>{t('In')}: {n(row.inputTokens)}<small>{t('Out')}: {n(row.outputTokens)}</small></>}</td>
        <td data-label={t('Cost')}>{row.costStatus === 'NOT_APPLICABLE' ? t('Not applicable') : row.costStatus === 'UNKNOWN' ? t('Not available') : money(row.estimatedCostUsd, locale, t)}{['ESTIMATED', 'PARTIAL'].includes(row.costStatus) && <small>{t(row.costStatus === 'PARTIAL' ? 'Partial estimate' : 'Estimated')}</small>}</td>
        <td data-label={t('Outcome')}><StatusBadge tone={row.outcome === 'SUCCEEDED' ? 'success' : row.outcome === 'FAILED' ? 'danger' : row.outcome === 'REFUSED' ? 'warning' : 'info'}>{t(labels[row.outcome] ?? 'Unknown')}</StatusBadge></td>
      </tr>)}
    </tbody></table>
  </section>
}
