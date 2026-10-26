import { useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import SourcePassages from '../agentPlatform/SourcePassages.jsx'
import { executionPath, platformCommand } from '../agentPlatform/platformApi.js'

export default function PreviewResult({ id, execution, error, reload, showStop = true, showExcerpts = true }) {
  const { t } = useI18n(), [stopError, setStopError] = useState(''), [stopping, setStopping] = useState(false), pendingStop = useRef(false)
  const pending = !execution || ['ACCEPTED', 'RUNNING'].includes(execution.status)
  const passages = [...new Map((execution?.toolStepsAndSources ?? []).flatMap(step => step.sources ?? []).map(p => [p.referenceId, p])).values()]
  const steps = (execution?.toolStepsAndSources ?? []).filter(step=>step.type||step.name||step.status).map(step => ({ type: step.type, name: step.name, status: step.status, errorCode: step.errorCode, elapsedMs: step.elapsedMs }))
  const answer = execution?.status === 'SUCCEEDED' && execution.outputSnapshot?.outcome === 'ANSWER'
  async function stop() {
    if (pendingStop.current || !pending) return
    pendingStop.current = true; setStopping(true); setStopError('')
    try { await platformCommand(executionPath(id) + '/cancel', {}); reload() }
    catch { setStopError('Cancellation could not be confirmed. Reload this run before trying again.'); reload() }
    finally { pendingStop.current = false; setStopping(false) }
  }
  return <section className="preview-run" aria-label={t('Persisted execution result')} aria-busy={pending && !error}>
    <p className="cs-caption" role="status">{t('Run')}: {t(({ SUCCEEDED: 'Completed', FAILED: 'Failed', REFUSED: 'Refused', CANCELLED: 'Cancelled', ACCEPTED: 'Accepted', RUNNING: 'Running' })[execution?.status] ?? 'Accepted')}</p>
    {(error || stopError) && <FeedbackPanel kind="error">{t(error || stopError)}</FeedbackPanel>}
    {!execution && !error && <FeedbackPanel kind="loading">{t('Loading saved execution...')}</FeedbackPanel>}
    {answer && <section aria-label={t('Generated answer')}><p className="cs-prose">{execution.outputSnapshot.text}</p><SourcePassages key={id} presentation="citations" showExcerpts={showExcerpts} passages={passages} citations={execution.outputSnapshot.citations ?? []} onUnavailable={reload} /></section>}
    {execution?.status === 'FAILED' && <FeedbackPanel kind="error" title={t('Request failed')}>{t('The provider could not complete this request.')}</FeedbackPanel>}
    {execution?.status === 'REFUSED' && <FeedbackPanel kind="warning" title={t('Request refused')}>{t(execution.outputSnapshot?.text ?? 'This request could not be answered within the permitted scope.')}</FeedbackPanel>}
    {execution?.status === 'CANCELLED' && <p className="cs-caption">{t('This run was cancelled. You can send another message.')}</p>}
    <details className="cs-disclosure agent-run-details"><summary>{t('View run details')}</summary>
      {execution && <><dl className="cs-metadata">{[
        ['Execution ID', id], ['Saved configuration ID', execution.configurationId], ['Exact configuration hash', execution.configurationHash],
        ['Execution mode', execution.executionMode], ['Measured latency', execution.latencyMs == null ? t('Not reported') : execution.latencyMs + ' ms'],
        ['Reported input tokens', execution.inputTokens], ['Reported output tokens', execution.outputTokens],
        ['Estimated generation cost', execution.estimatedCost == null ? t('Not available') : 'USD ' + execution.estimatedCost], ['Cost status', execution.costStatus],
        ['Safe error category', execution.errorCode],
      ].filter(([,value]) => value != null).map(([label,value]) => <div key={label}><dt>{t(label)}</dt><dd>{String(value)}</dd></div>)}</dl>
        <p className="cs-caption">{t('Actual tool calls')}: {steps.filter(step => step.type === 'ACTUAL_NATIVE_CALL').length}</p>
        {steps.length>0&&<ol className="agent-safe-timeline">{steps.map((step,index) => <li key={index}>{step.type} {['kb.search','assessment.lookup','simulation.explore','simulation.myEvidence'].includes(step.name) ? step.name : ''} · {step.status}{step.elapsedMs != null ? ` · ${step.elapsedMs} ms` : ''}</li>)}</ol>}
        {passages.some(p=>!execution.outputSnapshot?.citations?.some(c=>c.referenceId===p.referenceId))&&<SourcePassages presentation="studio" passages={passages.filter(p => !execution.outputSnapshot?.citations?.some(c => c.referenceId === p.referenceId))} onUnavailable={reload} />}
      </>}
      <Button onClick={reload}>{t('Reload saved execution')}</Button>
    </details>
    {pending && showStop && <Button disabled={stopping || execution?.cancellationRequested} busy={stopping} onClick={stop}>{t(execution?.cancellationRequested ? 'Stopping...' : 'Stop')}</Button>}
  </section>
}
