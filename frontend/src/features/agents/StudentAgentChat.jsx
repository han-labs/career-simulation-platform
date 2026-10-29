import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { ArrowDown, ArrowUp, History, Maximize2, Minimize2, Plus, Square, Trash2, X, FileText, Download } from 'lucide-react'
import { apiRequest } from '../../shared/api/httpClient.js'
import { command, draftKey, readDraft, writeDraft } from '../../shared/api/coreApi.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import { runPath, sessionPath, streamExecution } from './studentAgentApi.js'
import AgentQuickActions from './AgentQuickActions.jsx'
import useChatViewport from './useChatViewport.js'
import InsightReportView, { SavedInsightReports } from './InsightReportView.jsx'
import { downloadInsightReport } from './insightReportHtml.js'
import Button from '../../shared/ui/Button.jsx'
import Dialog from '../../shared/ui/Dialog.jsx'
import FormField from '../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../shared/ui/FeedbackPanel.jsx'
import AgentActivity from '../../shared/ui/AgentActivity.jsx'
import './studentAgent.css'

export default function StudentAgentChat({ owner, agent, id, onSessionChange, onClose, visible = true, expanded = false, onExpand }) {
  const { t } = useI18n()
  const [sessions, setSessions] = useState(null), [session, setSession] = useState(null), [historyOpen, setHistoryOpen] = useState(false)
  const [error, setError] = useState(''), [busy, setBusy] = useState(false), [deleting, setDeleting] = useState(null)
  const alive = useRef(false), pending = useRef(false), createRequest = useRef(null), historyTrigger = useRef(null), drawer = useRef(null), initialised = useRef(false)
  const refresh = useCallback(async () => { const rows = await apiRequest(`/v2/agent-sessions?agentId=${encodeURIComponent(agent.id)}&limit=100`); if (alive.current) setSessions(rows) }, [agent.id])
  useEffect(() => { alive.current = true; refresh().catch(failure => { if (alive.current) setError(failure.message) }); return () => { alive.current = false } }, [refresh])
  useEffect(() => { if (historyOpen) { const previous = document.activeElement, trigger = historyTrigger.current; drawer.current?.querySelector('button')?.focus(); return () => { if (trigger?.isConnected) trigger.focus(); else if (previous?.isConnected) previous.focus() } } }, [historyOpen])
  const create = useCallback(async () => {
    if (pending.current) return
    pending.current = true; setBusy(true); setError('')
    if (!createRequest.current) createRequest.current = { requestId: crypto.randomUUID(), title: t('New conversation'), contextSessionIds: [] }
    try {
      const created = await command(`/v2/agents/${encodeURIComponent(agent.id)}/sessions`, createRequest.current)
      if (!alive.current) return
      createRequest.current = null; setSession(created); onSessionChange(created.id); setHistoryOpen(false); await refresh()
    } catch (failure) { if (alive.current) setError(failure.message) }
    finally { pending.current = false; if (alive.current) setBusy(false) }
  }, [agent.id, onSessionChange, refresh, t])
  useEffect(() => {
    if (!sessions || initialised.current) return
    initialised.current = true
    if (id) return
    const previous = readDraft(draftKey(owner, 'agent-selection', agent.id))?.id
    const latest = sessions.find(row => row.id === previous) ?? sessions[0]
    if (latest) onSessionChange(latest.id)
    else create()
  }, [sessions, id, onSessionChange, create, owner, agent.id])
  useEffect(() => { if (id) writeDraft(draftKey(owner, 'agent-selection', agent.id), { id }) }, [id, owner, agent.id])
  async function remove() {
    if (pending.current || !deleting) return
    pending.current = true; setBusy(true); setError('')
    try { await apiRequest(sessionPath(deleting.id), { method: 'DELETE' }); if (!alive.current) return; if (deleting.id === id) onSessionChange(null); setDeleting(null); await refresh() }
    catch (failure) { if (alive.current) setError(failure.message) }
    finally { pending.current = false; if (alive.current) setBusy(false) }
  }
  function drawerKeys(event) {
    if (event.key === 'Escape') { event.stopPropagation(); setHistoryOpen(false); return }
    if (event.key !== 'Tab') return
    const controls = [...drawer.current.querySelectorAll('button:not([disabled])')], first = controls[0], last = controls.at(-1)
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
  }
  const name = session?.id === id ? session.agentName ?? agent.name : agent.name
  return <section className="student-agent-chat cs-ui" aria-label={t('Career Agent conversation')} onKeyDown={event => { if (event.key === 'Escape' && !event.defaultPrevented && !deleting) { if (historyOpen) setHistoryOpen(false); else onClose?.() } }}>
    <header className="student-chat-header"><h2 title={name}>{name}</h2><div className="student-header-actions">
      <Button ref={historyTrigger} className="student-icon-button" aria-label={t('History')} title={t('History')} icon={<History size={18} />} aria-expanded={historyOpen} disabled={busy} onClick={() => { setHistoryOpen(true); refresh().catch(failure => { if (alive.current) setError(failure.message) }) }} />
      <Button className="student-icon-button" aria-label={t('New Chat')} title={t('New Chat')} icon={<Plus size={19} />} disabled={busy} busy={busy} onClick={create} />
      {onExpand && <Button className="student-icon-button" aria-label={t(expanded ? 'Restore' : 'Expand')} title={t(expanded ? 'Restore' : 'Expand')} icon={expanded ? <Minimize2 size={17} /> : <Maximize2 size={17} />} aria-pressed={expanded} onClick={onExpand} />}
      {onClose && <Button className="student-icon-button" aria-label={t('Close')} title={t('Close')} icon={<X size={19} />} onClick={onClose} />}
    </div></header>
    {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
    {id ? <Conversation key={`${owner}:${id}`} owner={owner} id={id} agentName={name} visible={visible} expanded={expanded} onSessionLoaded={setSession} /> : <div className="student-chat-empty"><p>{t('Start a conversation with the published Agent.')}</p><Button variant="primary" disabled={busy} onClick={create}>{t('Start conversation')}</Button></div>}
    {historyOpen && <div className="student-history-overlay"><section ref={drawer} className="student-history-drawer" role="dialog" aria-modal="true" aria-label={t('Conversation history')} onKeyDown={drawerKeys}>
      <header><h3>{t('Conversation history')}</h3><Button className="student-icon-button" aria-label={t('Close history')} title={t('Close history')} icon={<X size={18} />} onClick={() => setHistoryOpen(false)} /></header>
      {!sessions && <p role="status">{t('Loading conversations...')}</p>}{sessions?.length === 0 && <p>{t('No conversations yet.')}</p>}
      <ul>{sessions?.map(row => <li key={row.id} className={row.id === id ? 'student-history-row student-history-row--active' : 'student-history-row'}><Button className="student-history-resume" title={row.title} aria-label={`${t('Resume')} · ${row.title}`} aria-pressed={row.id === id} onClick={() => { onSessionChange(row.id); setHistoryOpen(false) }}>{row.title}</Button><Button className="student-icon-button student-history-delete" aria-label={`${t('Delete')} · ${row.title}`} title={t('Delete')} icon={<Trash2 size={16} />} disabled={busy} onClick={() => setDeleting(row)} /></li>)}</ul>
    </section></div>}
    {deleting && <Dialog title={t('Confirm conversation deletion')} closeLabel={t('Close dialog')} busy={busy} onRequestClose={() => { if (!pending.current) setDeleting(null) }} footer={<><Button disabled={busy} onClick={() => setDeleting(null)}>{t('Keep conversation')}</Button><Button variant="danger" busy={busy} onClick={remove}>{t('Confirm deletion')}</Button></>}><p>{deleting.title}</p><p>{t('Deletion also invalidates dependent summaries and stops pending responses. Preserved local drafts remain private to this account.')}</p></Dialog>}
  </section>
}
function Conversation({ owner, id, agentName, visible, expanded, onSessionLoaded }) {
  const { t } = useI18n(), key = draftKey(owner, 'agent-message', id)
  const [saved] = useState(() => readDraft(key) ?? {}), [text, setText] = useState(saved.text ?? ''), [requestId, setRequestId] = useState(saved.requestId ?? crypto.randomUUID())
  const [session, setSession] = useState(null), [history, setHistory] = useState([]), [run, setRun] = useState(null), [streamId, setStreamId] = useState(saved.executionId ?? null), [segments, setSegments] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [retry, setRetry] = useState(0), [requestTask, setRequestTask] = useState(saved.requestTask ?? null), [requestText, setRequestText] = useState(saved.requestText ?? null)
  const mounted = useRef(true), cursor = useRef(0), pending = useRef(false), composing = useRef(false), composer = useRef(null), morePending = useRef(false), draftRevision = useRef(0)
  const [sent, setSent] = useState(null), [historyLoaded, setHistoryLoaded] = useState(false), [hasMore, setHasMore] = useState(false), [loadingMore, setLoadingMore] = useState(false), [reportsOpen, setReportsOpen] = useState(false)
  const { viewport: transcript, tail, following, markSent, observeScroll, jump } = useChatViewport({ ready: historyLoaded, visible, scope: id, content: history, stream: segments, pending: sent, status: run?.status, layout: expanded })
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  const reload = useCallback(async () => { const [s, rows] = await Promise.all([apiRequest(sessionPath(id)), apiRequest(`${sessionPath(id)}/interactions`)]); if (mounted.current) { setSession(s); onSessionLoaded(s); setHistory(rows); setHasMore(rows.length === 100); setHistoryLoaded(true) } }, [id, onSessionLoaded])
  useEffect(() => { let active = true; Promise.all([apiRequest(sessionPath(id)), apiRequest(`${sessionPath(id)}/interactions`)]).then(([s, rows]) => { if (active) { setSession(s); onSessionLoaded(s); setHistory(rows); setHasMore(rows.length === 100); setHistoryLoaded(true) } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [id, onSessionLoaded])
  useEffect(() => {
    if (!streamId) return
    const controller = new AbortController(); let active = true
    async function listen() {
      try {
        const current = await apiRequest(runPath(streamId)); if (!active) return; setRun(current)
        if (!['ACCEPTED', 'RUNNING'].includes(current.status)) { await reload(); return }
        await streamExecution(streamId, { after: cursor.current, signal: controller.signal, onEvent(e) { if (!active) return; cursor.current = e.sequenceNumber; if (e.type === 'answer.segment') setSegments(s => s + e.payload.text) } })
        if (active) { const canonical = await apiRequest(runPath(streamId)); setRun(canonical); if (['ACCEPTED', 'RUNNING'].includes(canonical.status)) setError('The stream ended before completion. Reconnect the saved execution.'); await reload() }
      } catch (e) { if (active && !controller.signal.aborted) { setError(e.message); const canonical = await apiRequest(runPath(streamId)).catch(() => null); if (active) { if (canonical) setRun(canonical); else { setRun(null); setSegments('') } await reload().catch(() => { if (active) setHistory([]) }) } } }
    }
    listen(); return () => { active = false; controller.abort() }
  }, [streamId, retry, reload])
  function edit(value) { draftRevision.current++; const next = crypto.randomUUID(); setText(value); setRequestId(next); setRequestText(null); writeDraft(key, { text: value, requestId: next, requestTask, requestText: null, executionId: streamId }) }
  async function submit(task) {
    if (pending.current || !session || (streamId && (!run || ['ACCEPTED', 'RUNNING'].includes(run.status))) || (task === 'messages' && !text.trim())) return
    pending.current = true; const submittedRevision = draftRevision.current
    setBusy(true); setError(''); const content = task === requestTask && requestText ? requestText : task === 'messages' ? text.trim() : task === 'insight-reports' ? t('Create an evidence-based insight report for me.') : t('Suggest an exploration plan draft for me.')
    const actualId = task === requestTask && (task === 'messages' || requestText) ? requestId : crypto.randomUUID(); setRequestId(actualId); setRequestTask(task); setRequestText(content); writeDraft(key, { text, requestId: actualId, requestTask: task, requestText: content, executionId: streamId });
    setSent({ key: actualId, text: content, executionId: null }); markSent(actualId)
    let accepted = false
    try {
      const e = await command(`${sessionPath(id)}/${task}`, { requestId: actualId, text: content })
      accepted = true
      const clear = task === 'messages' && draftRevision.current === submittedRevision
      const next = clear ? { text: '', requestId: crypto.randomUUID(), requestTask: null, requestText: null } : readDraft(key) ?? {}
      // Reconcile the accepted run even if navigation unmounted the composer. Never replay its POST.
      writeDraft(key, { ...next, executionId: e.id })
      if (!mounted.current) return
      setSent({ key: actualId, text: content, executionId: e.id })
      if (clear) { setText(''); setRequestId(next.requestId); setRequestTask(null); setRequestText(null) }
      setRun(e); setSegments(''); cursor.current = 0; setStreamId(e.id); await reload()
    } catch (e) { if (mounted.current) { setError(e.message); if (!accepted) setSent(null) } } finally { pending.current = false; if (mounted.current) setBusy(false) }
  }
  async function confirm() {
    if (pending.current) return
    pending.current = true; setBusy(true); setError('')
    try { const dashboard = await apiRequest('/v2/guidance/dashboard'); const old = dashboard.activePlan; await command('/v2/exploration-plans/confirm-suggestion', { sourceExecutionId: run.id, requestId: crypto.randomUUID(), contentHash: run.planContentHash, expectedActivePlanId: old?.id ?? null, expectedRevision: old?.revision ?? null }); if (mounted.current) setError(t('Exploration plan confirmed. View it on your dashboard.')) } catch (e) { if (mounted.current) setError(e.message) } finally { pending.current = false; if (mounted.current) setBusy(false) }
  }
  const working = run && ['ACCEPTED', 'RUNNING'].includes(run.status), running = busy || working || Boolean(streamId && !run)
  useLayoutEffect(() => { const node = composer.current; if (node && visible) { node.style.height = '0px'; node.style.height = `${Math.min(104, Math.max(40, node.scrollHeight))}px` } }, [text, expanded, visible])
  async function more() {
    if (morePending.current) return
    morePending.current = true; setLoadingMore(true)
    try { const rows = await apiRequest(`${sessionPath(id)}/interactions?afterSequence=${history.at(-1).sequenceNumber}`); if (mounted.current) { setHistory(current => [...current, ...rows.filter(row => !current.some(existing => existing.id === row.id))]); setHasMore(rows.length === 100) } }
    catch (failure) { if (mounted.current) setError(failure.message) }
    finally { morePending.current = false; if (mounted.current) setLoadingMore(false) }
  }
  const name = session?.agentName ?? agentName
  const hasAssistant = history.some(item => item.executionId === run?.id && item.actorType === 'ASSISTANT')
  return <>
    <div className="student-transcript-wrap"><div ref={transcript} className="student-chat-transcript" role="region" aria-label={t('Messages')} tabIndex={0} onScroll={observeScroll}>
      {session && <p className="student-conversation-meta">{session.title} · <span title={t('Pinned configuration version')}>v{session.configurationVersion}</span></p>}
      {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
      {history.length === 0 && <p className="cs-caption">{t('Ask about your assessments, published simulations or your progress.')}</p>}
      <ol className="student-chat-history">{history.map(item => <li data-chat-anchor={item.actorType === 'USER' && sent?.executionId && item.executionId === sent.executionId ? sent?.key : undefined} className={`student-chat-turn student-chat-turn--${item.actorType === 'USER' ? 'user' : 'agent'}`} key={item.id}>
        {item.actorType === 'USER' ? <article className="student-chat-bubble"><strong className="student-chat-speaker">{t('You')}</strong><p>{item.content.text}</p></article> : <AssistantMessage name={name} item={item} reportId={item.executionId === run?.id ? run?.reportId : null} />}
      </li>)}</ol>
      {sent && !history.some(item => item.actorType === 'USER' && item.executionId && item.executionId === sent.executionId) && <div data-chat-anchor={sent.key} className="student-chat-turn student-chat-turn--user"><article className="student-chat-bubble"><strong className="student-chat-speaker">{t('You')}</strong><p>{sent.text}</p>{!sent.executionId && <small role="status">{t('Sending...')}</small>}</article></div>}
      {working && <div className="student-chat-turn student-chat-turn--agent"><div className="student-chat-bubble"><strong className="student-chat-speaker">{name}</strong>{segments && <p>{segments}</p>}<AgentActivity /><Button className="student-icon-button student-stop" aria-label={t('Cancel execution')} title={t('Cancel execution')} icon={<Square size={12} fill="currentColor" />} onClick={async () => { try { setRun(await command(`${runPath(run.id)}/cancel`, {})) } catch (failure) { setError(failure.message) } }} /></div></div>}
      {!running && run && !hasAssistant && <div className="student-chat-turn student-chat-turn--agent"><AssistantMessage name={name} item={{ id: run.id, executionId: run.id, content: run.output ?? { text: '' } }} reportId={run.reportId} /></div>}
      {run?.status === 'FAILED' && <FeedbackPanel kind="error" title={t('Request failed')}>{t(run.errorCode === 'ITERATION_LIMIT' ? 'This request reached the Agent execution limit. Try a more focused question; your conversation is still saved.' : run.errorCode === 'MALFORMED_OUTPUT' ? 'The model response did not pass validation. No answer was accepted.' : 'AI assistance is temporarily unavailable. Please try again later.')}</FeedbackPanel>}
      {streamId && error && !busy && <Button onClick={() => setRetry(value => value + 1)}>{t('Reconnect saved execution')}</Button>}
      {hasMore && <Button className="student-text-button" busy={loadingMore} onClick={more}>{t('Show next messages')}</Button>}
      {(run?.taskType === 'INSIGHT_REPORT' || run?.output?.requestedAction === 'INSIGHT_REPORT') && ['FAILED', 'REFUSED', 'CANCELLED'].includes(run.status) && <p className="student-report-unavailable">{t('No report was saved for this request. Open saved reports or edit your request to try again.')}</p>}
      {run?.output?.planSuggestion && <section className="student-plan-draft"><h3>{t('Draft exploration plan')}</h3><h4>{run.output.planSuggestion.title}</h4><ol>{run.output.planSuggestion.steps.map((step, index) => <li key={index}>{step}</li>)}</ol><p>{t('Nothing is saved as a plan until you confirm this exact suggestion.')}</p><Button disabled={busy || run.status !== 'SUCCEEDED'} variant="primary" onClick={confirm}>{t('Confirm and save this plan')}</Button></section>}
      <div ref={tail} className="chat-viewport-tail" aria-hidden="true" />
    </div>
    {!following && <Button className="student-icon-button student-jump-latest" aria-label={t('Jump to latest')} title={t('Jump to latest')} icon={<ArrowDown size={17} />} onClick={jump} />}</div>
    <div className="student-chat-composer"><AgentQuickActions kind="student" disabled={running || !session} onChoose={edit} onReport={() => submit('insight-reports')} onPlan={() => submit('plan-suggestions')} onSavedReports={() => setReportsOpen(true)} />
      <form onSubmit={event => { event.preventDefault(); submit('messages') }}><FormField className="student-composer-field" label={t('Message to Agent')}><textarea ref={composer} placeholder={t('Message to Agent')} rows={1} maxLength={2000} value={text} onChange={event => edit(event.target.value)} onCompositionStart={() => { composing.current = true }} onCompositionEnd={() => { composing.current = false }} onKeyDown={event => { if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing && !composing.current && event.keyCode !== 229) { event.preventDefault(); submit('messages') } }} /></FormField>
        <div className="cs-actions"><Button className="student-icon-button student-send" type="submit" variant="primary" aria-label={t('Send message')} title={t('Send message')} icon={<ArrowUp size={19} />} busy={running} disabled={!session || !text.trim()} /></div>
      </form>
    </div>
    {reportsOpen && <SavedInsightReports onClose={() => setReportsOpen(false)} />}
  </>
}
function AssistantMessage({ name, item, reportId }) {
  const { t } = useI18n(), citations = item.unavailable ? [] : item.content?.citations ?? []
  return <article className="student-chat-bubble"><strong className="student-chat-speaker">{name}</strong><p>{item.unavailable ? t('This response is no longer available under the current authorization.') : item.content?.text}</p>
    {citations.length > 0 && <details className="student-chat-citation"><summary>{citations.length} {t('sources')}</summary>{citations.map((source, index) => <blockquote key={`${source.referenceId}:${index}`}><cite>{source.referenceId}</cite><p>{source.quote}</p></blockquote>)}</details>}
    {!item.unavailable && item.executionId && <MessageRunDetails key={item.executionId} executionId={item.executionId} />}
    {!item.unavailable && (item.content?.report || reportId) && <ReportActions reportId={reportId} report={item.content?.report} executionId={item.executionId} />}
  </article>
}
function MessageRunDetails({ executionId }) {
  const { t } = useI18n(), [result, setResult] = useState(null), [error, setError] = useState(''), [loading, setLoading] = useState(false)
  const alive = useRef(false), pending = useRef(false)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  async function load() {
    if (pending.current) return
    pending.current = true; setLoading(true); setError('')
    try { const value = await apiRequest(runPath(executionId)); if (alive.current) setResult(value) }
    catch (failure) { if (alive.current) { setResult(null); setError(failure.message) } }
    finally { pending.current = false; if (alive.current) setLoading(false) }
  }
  const fields = result ? [['Run', result.status], ['Execution ID', executionId], ['Requested model', result.requestedModelKey], ['Effective model', result.effectiveModelKey], ['Safe error category', result.errorCode], ['Measured latency', result.latencyMs == null ? null : `${result.latencyMs} ms`], ['Input tokens', result.inputTokens ?? t('Unknown')], ['Output tokens', result.outputTokens ?? t('Unknown')], ['Cost status', result.costStatus]] : []
  return <details className="student-message-run" onToggle={event => { if (event.currentTarget.open && !result && !pending.current) load() }}><summary>{t('View run details')}</summary>
    {loading && <p role="status">{t('Loading saved execution...')}</p>}{error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
    {result && <><dl>{fields.filter(([, value]) => value != null && typeof value !== 'object').map(([label, value]) => <div key={label}><dt>{t(label)}</dt><dd>{t(value)}</dd></div>)}</dl><ul>{result.toolStepsAndSources?.map((step, index) => <li key={index}>{[step.type, step.name, step.status, step.errorCode, step.validationCheck].filter(value => typeof value === 'string').map(value => t(value)).join(' · ')}{typeof step.elapsedMs === 'number' ? ` · ${step.elapsedMs} ms` : ''}</li>)}</ul></>}
    {(result || error) && <Button disabled={loading} onClick={load}>{t('Reload saved execution')}</Button>}
  </details>
}
function ReportActions({ report, reportId, executionId }) {
  const { t } = useI18n(), [opened, setOpened] = useState(null), [error, setError] = useState(''), [busy, setBusy] = useState(false)
  const alive = useRef(false), pending = useRef(false)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  async function show(download) {
    if (pending.current) return
    pending.current = true; setBusy(true); setError('')
    try { const savedId = reportId ?? (executionId ? (await apiRequest(runPath(executionId))).reportId : null); const value = savedId ? await apiRequest(`/v2/insight-reports/${encodeURIComponent(savedId)}`) : { structuredInsights: report }; if (!value?.structuredInsights?.summary) throw new Error('The report is not available. Please try again.'); if (!alive.current) return; if (download) downloadInsightReport(value, t); else setOpened(value) }
    catch (failure) { if (alive.current) setError(failure.message) }
    finally { pending.current = false; if (alive.current) setBusy(false) }
  }
  return <section className="student-report-actions" aria-label={t('Insight report')}><strong>{t('Insight report')}</strong><div className="cs-actions"><Button className="student-text-button" icon={<FileText size={15} />} disabled={busy} onClick={() => show(false)}>{t('Open report')}</Button><Button className="student-text-button" icon={<Download size={15} />} disabled={busy} onClick={() => show(true)}>{t('Download report')}</Button></div>{error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}{opened && <InsightReportView report={opened} onClose={() => setOpened(null)} />}</section>
}
