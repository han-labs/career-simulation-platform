import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { apiRequest } from '../../shared/api/httpClient.js'
import { command, draftKey, readDraft, writeDraft } from '../../shared/api/coreApi.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import { runPath, sessionPath, streamExecution } from './studentAgentApi.js'
import '../admin/agentPlatform/agentPlatform.css'

export default function StudentAgentPage() {
  const { agentId } = useParams(), { t } = useI18n()
  const [me, setMe] = useState(null), [error, setError] = useState('')
  useEffect(() => { let active = true; apiRequest('/v2/me').then(user => { if (active) setMe(user) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [])
  return <section className="feature-page section-shell core-flow"><Link to="/agents">{t('My assigned Agents')}</Link><h1>{t('Student Agent')}</h1><p>{t('Exploration support does not certify professional competence. Your assessments and simulations remain independent of AI.')}</p>{error && <p role="alert">{t(error)}</p>}{me && <Workspace key={`${me.id}:${agentId}`} owner={me.id} agentId={agentId} />}</section>
}
function Workspace({ owner, agentId }) {
  const { t } = useI18n(), [search, setSearch] = useSearchParams(), id = search.get('session')
  const [sessions, setSessions] = useState([]), [title, setTitle] = useState(''), [sources, setSources] = useState([]), [error, setError] = useState(''), [busy, setBusy] = useState(false), [deleting, setDeleting] = useState(false)
  const refresh = useCallback(async () => { const rows = await apiRequest(`/v2/agent-sessions?agentId=${encodeURIComponent(agentId)}&limit=100`); setSessions(rows) }, [agentId])
  useEffect(() => { let active = true; apiRequest(`/v2/agent-sessions?agentId=${encodeURIComponent(agentId)}&limit=100`).then(rows => { if (active) setSessions(rows) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [agentId])
  async function create(e) { e.preventDefault(); setBusy(true); setError(''); try { const s = await command(`/v2/agents/${encodeURIComponent(agentId)}/sessions`, { requestId: crypto.randomUUID(), title, contextSessionIds: sources }); await refresh(); setSearch({ session: s.id }); setTitle(''); setSources([]) } catch (e) { setError(e.message) } finally { setBusy(false) } }
  async function remove() { setBusy(true); try { await apiRequest(sessionPath(id), { method: 'DELETE' }); setSearch({}); setDeleting(false); await refresh() } catch (e) { setError(e.message) } finally { setBusy(false) } }
  return <><div className="platform-grid"><aside><h2>{t('Conversations')}</h2><ul className="platform-list">{sessions.map(s => <li key={s.id}><button aria-pressed={s.id === id} onClick={() => { setSearch({ session: s.id }); setDeleting(false) }}>{s.title} · V{s.configurationVersion}</button></li>)}</ul><form onSubmit={create}><label>{t('New conversation title')}<input required maxLength={180} value={title} onChange={e => setTitle(e.target.value)} /></label><details><summary>{t('Optional context from my conversations')}</summary><p>{t('Requires memory enabled in the assigned configuration. Deleted source conversations invalidate derived context.')}</p>{sessions.map(s => <label key={s.id}><input type="checkbox" checked={sources.includes(s.id)} disabled={!sources.includes(s.id) && sources.length >= 3} onChange={e => setSources(e.target.checked ? [...sources, s.id] : sources.filter(x => x !== s.id))} />{s.title}</label>)}</details><button disabled={busy}>{t('Start conversation')}</button></form></aside><div>{error && <p role="alert">{t(error)}</p>}{id && <><button disabled={busy} onClick={() => setDeleting(true)}>{t('Delete my conversation')}</button>{deleting && <div role="group" aria-label={t('Confirm conversation deletion')}><p>{t('Deletion also invalidates dependent summaries and stops pending responses. Preserved local drafts remain private to this account.')}</p><button disabled={busy} onClick={remove}>{t('Confirm deletion')}</button><button onClick={() => setDeleting(false)}>{t('Keep conversation')}</button></div>}<Conversation key={`${owner}:${id}`} owner={owner} id={id} /></>}</div></div><Reports key={id ?? 'no-session'} /></>
}
function Conversation({ owner, id }) {
  const { t } = useI18n(), key = draftKey(owner, 'agent-message', id)
  const [saved] = useState(() => readDraft(key) ?? {}), [text, setText] = useState(saved.text ?? ''), [requestId, setRequestId] = useState(saved.requestId ?? crypto.randomUUID())
  const [session, setSession] = useState(null), [history, setHistory] = useState([]), [run, setRun] = useState(null), [streamId, setStreamId] = useState(saved.executionId ?? null), [segments, setSegments] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [retry, setRetry] = useState(0), [requestTask, setRequestTask] = useState(saved.requestTask ?? null)
  const mounted = useRef(true), cursor = useRef(0)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  const reload = useCallback(async () => { const [s, rows] = await Promise.all([apiRequest(sessionPath(id)), apiRequest(`${sessionPath(id)}/interactions`)]); if (mounted.current) { setSession(s); setHistory(rows) } }, [id])
  useEffect(() => { let active = true; Promise.all([apiRequest(sessionPath(id)), apiRequest(`${sessionPath(id)}/interactions`)]).then(([s, rows]) => { if (active) { setSession(s); setHistory(rows) } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [id])
  useEffect(() => {
    if (!streamId) return
    const controller = new AbortController(); let active = true
    async function listen() {
      try {
        const current = await apiRequest(runPath(streamId)); if (!active) return; setRun(current)
        if (!['ACCEPTED', 'RUNNING'].includes(current.status)) { await reload(); return }
        await streamExecution(streamId, { after: cursor.current, signal: controller.signal, onEvent(e) { if (!active) return; cursor.current = e.sequenceNumber; if (e.type === 'answer.segment') setSegments(s => s + e.payload.text) } })
        if (active) { const canonical = await apiRequest(runPath(streamId)); setRun(canonical); if (['ACCEPTED', 'RUNNING'].includes(canonical.status)) setError(t('The stream ended before completion. Reconnect the saved execution.')); await reload() }
      } catch (e) { if (active && !controller.signal.aborted) { setError(e.message); const canonical = await apiRequest(runPath(streamId)).catch(() => null); if (active) { if (canonical) setRun(canonical); else { setRun(null); setSegments('') } await reload().catch(() => { if (active) setHistory([]) }) } } }
    }
    listen(); return () => { active = false; controller.abort() }
  }, [streamId, retry, reload, t])
  function edit(value) { const next = crypto.randomUUID(); setText(value); setRequestId(next); writeDraft(key, { text: value, requestId: next, executionId: streamId }) }
  async function submit(task) {
    setBusy(true); setError(''); const content = text.trim() || (task === 'insight-reports' ? t('Create an evidence-based insight report for me.') : t('Suggest an exploration plan draft for me.'))
    const actualId = task === requestTask ? requestId : crypto.randomUUID(); setRequestId(actualId); setRequestTask(task); writeDraft(key, { text, requestId: actualId, requestTask: task, executionId: streamId });
    try { const e = await command(`${sessionPath(id)}/${task}`, { requestId: actualId, text: content }); if (!mounted.current) return; setRun(e); setSegments(''); cursor.current = 0; setStreamId(e.id); writeDraft(key, { text, requestId: actualId, requestTask: task, executionId: e.id }); await reload() } catch (e) { if (mounted.current) setError(e.message) } finally { if (mounted.current) setBusy(false) }
  }
  async function confirm() {
    setBusy(true); setError('')
    try { const dashboard = await apiRequest('/v2/guidance/dashboard'); const old = dashboard.activePlan; await command('/v2/exploration-plans/confirm-suggestion', { sourceExecutionId: run.id, requestId: crypto.randomUUID(), contentHash: run.planContentHash, expectedActivePlanId: old?.id ?? null, expectedRevision: old?.revision ?? null }); if (mounted.current) setError(t('Exploration plan confirmed. View it on your dashboard.')) } catch (e) { if (mounted.current) setError(e.message) } finally { if (mounted.current) setBusy(false) }
  }
  const working = run && ['ACCEPTED', 'RUNNING'].includes(run.status), running = busy || working
  return <section className="platform-card"><h2>{session?.title ?? t('Conversation')}</h2>{session && <p>{t('Pinned configuration version')}: {session.configurationVersion}</p>}{error && <p role="alert">{t(error)}</p>}<ol className="agent-history">{history.map(i => <li key={i.id}><strong>{t(i.actorType === 'USER' ? 'You' : 'Agent response')}</strong><p>{i.unavailable ? t('This response is no longer available under the current authorization.') : i.content.text}</p>{i.content.citations?.map(c => <blockquote key={c.referenceId}><cite>{c.referenceId}</cite> {c.quote}</blockquote>)}</li>)}</ol>{history.length > 0 && <button onClick={async () => { try { const rows = await apiRequest(`${sessionPath(id)}/interactions?afterSequence=${history.at(-1).sequenceNumber}`); if (mounted.current) setHistory(h => [...h, ...rows]) } catch (e) { if (mounted.current) setError(e.message) } }}>{t('Load more messages')}</button>}
    {run && <div role="status"><p>{t('Execution ID')}: {run.id} · {run.status}</p>{working && <p className="agent-stream">{segments}</p>}{!running && run.output?.text && !history.some(i => i.executionId === run.id && i.actorType === 'ASSISTANT') && <p>{run.output.text}</p>}<button onClick={() => setRetry(n => n + 1)}>{t('Reconnect saved execution')}</button>{working && <button onClick={async () => { try { setRun(await command(`${runPath(run.id)}/cancel`, {})) } catch (e) { setError(e.message) } }}>{t('Cancel execution')}</button>}<p>{t('Input tokens')}: {run.inputTokens ?? t('Unknown')} · {t('Output tokens')}: {run.outputTokens ?? t('Unknown')} · {t('Estimated generation cost')}: {run.estimatedCost ?? t('Unknown')} · {run.costStatus}</p>{run.toolStepsAndSources?.map((s, index) => <details key={index}><summary>{s.type} · {s.name} · {s.status}</summary>{s.sources?.map(p => <blockquote key={p.referenceId}><cite>{p.referenceId} · {p.title} · {p.section}</cite><p>{p.text}</p><pre>{JSON.stringify(p.sourceLocator, null, 2)}</pre></blockquote>)}{s.evidence?.map(d => <blockquote key={d.referenceId}><cite>{d.referenceId} · {d.kind}</cite><p>{d.text}</p></blockquote>)}</details>)}</div>}
    <form onSubmit={e => { e.preventDefault(); submit('messages') }}><label>{t('Message to Agent')}<textarea maxLength={2000} value={text} onChange={e => edit(e.target.value)} /></label><p>{t('Drafts stay with this account. A repeated request resumes its original execution; edit the message to start another.')}</p><div className="platform-actions"><button disabled={running || !session || !text.trim()}>{t('Send message')}</button><button type="button" disabled={running || !session} onClick={() => submit('insight-reports')}>{t('Request insight report')}</button><button type="button" disabled={running || !session} onClick={() => submit('plan-suggestions')}>{t('Suggest draft plan')}</button></div></form>
    {run?.output?.planSuggestion && <section><h3>{t('Draft exploration plan')}</h3><h4>{run.output.planSuggestion.title}</h4><ol>{run.output.planSuggestion.steps.map((s, i) => <li key={i}>{s}</li>)}</ol><p>{t('Nothing is saved as a plan until you confirm this exact suggestion.')}</p><button disabled={busy || run.status !== 'SUCCEEDED'} onClick={confirm}>{t('Confirm and save this plan')}</button></section>}
  </section>
}
function Reports() {
  const { t } = useI18n(), [rows, setRows] = useState([]), [error, setError] = useState('')
  async function load() { try { setRows(await apiRequest('/v2/insight-reports')); setError('') } catch (e) { setError(e.message) } }
  return <section className="platform-card"><h2>{t('My insight reports')}</h2><button onClick={load}>{t('Load saved reports')}</button>{error && <p role="alert">{t(error)}</p>}{rows.map(r => <article key={r.id}><h3>{r.structuredInsights.summary}</h3><p>{r.structuredInsights.disclaimer}</p><p>{t('Execution ID')}: {r.sourceExecutionId}</p>{r.structuredInsights.hypotheses.map((h, i) => <div key={i}><h4>{h.direction}</h4><p>{h.reason}</p><p>{h.evidenceRefs.join(', ')}</p></div>)}{['strengthsPracticed', 'difficulties', 'limitations', 'nextSteps'].map(k => <div key={k}><h4>{t({ strengthsPracticed: 'Skills practiced', difficulties: 'Difficulties', limitations: 'Limitations', nextSteps: 'Suggested next steps' }[k])}</h4><ul>{r.structuredInsights[k].map((s, i) => <li key={i}>{s}</li>)}</ul></div>)}</article>)}</section>
}
