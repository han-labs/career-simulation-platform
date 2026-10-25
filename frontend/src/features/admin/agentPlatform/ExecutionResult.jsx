import { useState } from 'react'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { platformCommand, executionPath } from './platformApi.js'
import SourcePassages from './SourcePassages.jsx'
import Button from '../../../shared/ui/Button.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import StatusBadge from '../../../shared/ui/StatusBadge.jsx'
export default function ExecutionResult({ id, execution, error, reload, presentation = 'legacy', detailsLabel }) {
  const { t } = useI18n()
  const [cancelError, setCancelError] = useState('')
  if (!id) return null
  const pending = !execution || ['ACCEPTED', 'RUNNING'].includes(execution.status)
  const passages = [...new Map((execution?.toolStepsAndSources ?? []).flatMap(s => s.sources ?? []).map(p => [p.chunkId, p])).values()]
  async function cancel() {
    setCancelError('')
    try { await platformCommand(executionPath(id) + '/cancel', {}); reload() }
    catch (failure) { setCancelError(failure.message); reload() }
  }
  const builder = presentation === 'builder'
  const safeSteps = (execution?.toolStepsAndSources ?? []).map(step => ({ type: step.type, name: step.name, status: step.status, errorCode: step.errorCode, elapsedMs: step.elapsedMs,
    sourceReferences: (Array.isArray(step.sources) ? step.sources : Array.isArray(step.evidence) ? step.evidence : []).map(source => ({ referenceId: source.referenceId, versionId: source.versionId, chunkId: source.chunkId })) }))
  if (presentation === 'studio' || builder) return <section className="cs-surface cs-stack" aria-label={t('Persisted execution result')} aria-busy={pending && !error}>
    <div className="cs-section-header"><h2>{t(builder ? 'Preview result' : 'Test results')}</h2>{execution && <StatusBadge tone={pending ? 'info' : execution.status === 'SUCCEEDED' ? 'success' : execution.status === 'REFUSED' ? 'warning' : 'danger'}>{t(({ SUCCEEDED: 'Succeeded', FAILED: 'Failed', REFUSED: 'Refused', CANCELLED: 'Cancelled', ACCEPTED: 'Accepted', RUNNING: 'Running' })[execution.status] ?? execution.status)}</StatusBadge>}</div>
    {(error || cancelError) && <FeedbackPanel kind="error">{t(error || cancelError)}</FeedbackPanel>}
    {!execution && !error && <FeedbackPanel kind="loading">{t('Loading saved execution...')}</FeedbackPanel>}
    {execution && <>
      {builder && <p className="cs-caption">{t('Actual tool calls')}: {safeSteps.filter(step => step.type === 'ACTUAL_NATIVE_CALL').length} · {t('Evidence events')}: {safeSteps.filter(step => step.sourceReferences.length > 0).length} · {t('Measured latency')}: {execution.latencyMs == null ? t('Not reported') : execution.latencyMs + ' ms'}</p>}
      {execution.errorCode && <FeedbackPanel kind="error" title={t('This request could not be completed.')}><p>{execution.errorCode}</p></FeedbackPanel>}
      {execution.outputSnapshot && (execution.outputSnapshot.outcome === 'RETRIEVAL_ONLY' ?
        <FeedbackPanel kind="info">{t(execution.outputSnapshot.text)}</FeedbackPanel> :
        <section className="cs-stack" aria-label={t('Generated answer')}><h3>{t(execution.status === 'SUCCEEDED' && execution.outputSnapshot.outcome === 'ANSWER' ? 'Validated answer' : 'Safe fallback or refusal')}</h3><p className="cs-prose">{execution.status === 'SUCCEEDED' ? execution.outputSnapshot.text : t(execution.outputSnapshot.text)}</p>
          <ul>{execution.outputSnapshot.citations?.map(citation => <li key={citation.referenceId}>[{citation.referenceId}] <q>{citation.quote}</q></li>)}</ul>
        </section>)}
      <SourcePassages key={id} presentation="studio" passages={passages} onUnavailable={reload} />
      <details className="cs-disclosure"><summary>{t(detailsLabel ?? (builder ? 'Safe run details' : 'Execution diagnostics'))}</summary>{builder && <p className="cs-caption">{t('Observable execution metadata only. Private model reasoning and provider protocol are never displayed.')}</p>}<dl className="cs-metadata">
        <div><dt>{t('Execution ID')}</dt><dd><code>{id}</code></dd></div><div><dt>{t('Execution mode')}</dt><dd>{execution.executionMode} · {execution.status}</dd></div>
        <div><dt>{t('Exact configuration hash')}</dt><dd><code>{execution.configurationHash ?? t('Not applicable')}</code></dd></div>
        {builder && <div><dt>{t('Saved configuration ID')}</dt><dd><code>{execution.configurationId ?? t('Not applicable')}</code></dd></div>}
        <div><dt>{t('Reported input tokens')}</dt><dd>{execution.inputTokens ?? t('Not reported')}</dd></div>
        <div><dt>{t('Reported output tokens')}</dt><dd>{execution.outputTokens ?? t('Not reported')}</dd></div>
        <div><dt>{t('Measured latency')}</dt><dd>{execution.latencyMs == null ? t('Not reported') : execution.latencyMs + ' ms'}</dd></div>
        <div><dt>{t('Estimated generation cost')}</dt><dd>{execution.estimatedCost == null ? t('Unknown or not applicable') : 'USD ' + execution.estimatedCost} · {execution.costStatus}</dd></div>
      </dl><details className="cs-disclosure"><summary>{t('Actual tool trace and resource usage')}</summary><pre tabIndex={0} aria-label={t('Actual tool trace and resource usage')}>{JSON.stringify(builder ? { steps: safeSteps, usage: { inputTokens: execution.inputTokens, outputTokens: execution.outputTokens, estimatedCost: execution.estimatedCost, costStatus: execution.costStatus } } : { steps: execution.toolStepsAndSources, usage: execution.pricingSnapshot }, null, 2)}</pre></details></details>
    </>}
    {execution?.cancellationRequested && <p role="status">{t('Cancellation requested')}</p>}
    <div className="cs-actions"><Button onClick={reload}>{t('Reload saved execution')}</Button>{pending && <Button disabled={execution?.cancellationRequested} onClick={cancel}>{t('Cancel execution')}</Button>}</div>
  </section>
  return <section className="platform-card" aria-label={t('Persisted execution result')} aria-busy={pending && !error}><h2>{t('Persisted execution result')}</h2><p>{t('Execution ID')}: <code>{id}</code></p>{(error || cancelError) && <p role="alert">{t(error || cancelError)}</p>}{!execution && !error && <p role="status">{t('Loading saved execution...')}</p>}{execution && <><p role="status">{execution.executionMode} · {execution.status}{execution.cancellationRequested ? ` · ${t('Cancellation requested')}` : ''}</p><p>{t('Exact configuration hash')}: <code>{execution.configurationHash ?? t('Not applicable')}</code></p>{execution.errorCode && <p role="alert">{execution.errorCode}</p>}{execution.outputSnapshot && <><h3>{t(execution.outputSnapshot.outcome === 'RETRIEVAL_ONLY' ? 'Retrieval diagnostic only' : execution.status === 'SUCCEEDED' ? 'Validated answer' : 'Safe fallback or refusal')}</h3><p className="core-prewrap">{execution.status === 'SUCCEEDED' && execution.outputSnapshot.outcome !== 'RETRIEVAL_ONLY' ? execution.outputSnapshot.text : t(execution.outputSnapshot.text)}</p><ul>{execution.outputSnapshot.citations?.map(c => <li key={c.referenceId}>[{c.referenceId}] <q>{c.quote}</q></li>)}</ul></>}<dl className="platform-metrics"><div><dt>{t('Reported input tokens')}</dt><dd>{execution.inputTokens ?? t('Not reported')}</dd></div><div><dt>{t('Reported output tokens')}</dt><dd>{execution.outputTokens ?? t('Not reported')}</dd></div><div><dt>{t('Measured latency')}</dt><dd>{execution.latencyMs === null ? t('Not reported') : `${execution.latencyMs} ms`}</dd></div><div><dt>{t('Estimated generation cost')}</dt><dd>{execution.estimatedCost === null ? t('Unknown or not applicable') : `USD ${execution.estimatedCost}`} · {execution.costStatus}</dd></div></dl><details><summary>{t('Actual tool trace and resource usage')}</summary><pre>{JSON.stringify({ steps: execution.toolStepsAndSources, usage: execution.pricingSnapshot }, null, 2)}</pre></details><SourcePassages key={id} passages={passages} onUnavailable={reload} /></>}<button type="button" onClick={reload}>{t('Reload saved execution')}</button>{pending && <button type="button" disabled={execution?.cancellationRequested} onClick={async () => { setCancelError(''); try { await platformCommand(`${executionPath(id)}/cancel`, {}); reload() } catch (e) { setCancelError(e.message); reload() } }}>{t('Cancel execution')}</button>}</section>
}
