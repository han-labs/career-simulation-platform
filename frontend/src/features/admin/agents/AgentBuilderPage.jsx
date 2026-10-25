import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import StatusBadge from '../../../shared/ui/StatusBadge.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import ExecutionResult from '../agentPlatform/ExecutionResult.jsx'
import useExecution from '../agentPlatform/useExecution.js'
import { agentPath, platformCommand } from '../agentPlatform/platformApi.js'
import AgentConfigurationForm from './AgentConfigurationForm.jsx'
import { configurationDraft, emptyConfiguration, validateConfiguration } from './agentConfiguration.js'
import './agentBuilder.css'

export default function AgentBuilderPage() {
  const { t } = useI18n(), [search, setSearch] = useSearchParams()
  const [data, setData] = useState(null), [error, setError] = useState(''), [busy, setBusy] = useState(false)
  const [name, setName] = useState(''), [purpose, setPurpose] = useState('')
  const alive = useRef(false), pending = useRef(false)
  const refresh = useCallback(async () => { const agents = await apiRequest('/v2/admin/agents'); if (alive.current) setData(current => current && { ...current, agents }) }, [])
  useEffect(() => {
    alive.current = true
    Promise.all([apiRequest('/v2/me'), apiRequest('/v2/admin/agents'), apiRequest('/v2/admin/knowledge-bases'), apiRequest('/v2/admin/agent-platform/options')])
      .then(([me, agents, bases, options]) => { if (alive.current) setData({ me, agents, bases, options }) })
      .catch(failure => { if (alive.current) setError(failure.message) })
    return () => { alive.current = false }
  }, [])
  async function create(event) {
    event.preventDefault()
    if (pending.current || data?.me.role !== 'ADMIN') return
    pending.current = true; setBusy(true); setError('')
    try {
      const agent = await platformCommand('/v2/admin/agents', { name, purpose })
      if (!alive.current) return
      setData(current => ({ ...current, agents: [...current.agents.filter(a => a.id !== agent.id), agent] })); setSearch({ agent: agent.id }); setName(''); setPurpose('')
    } catch (failure) { if (alive.current) setError(failure.message) }
    finally { pending.current = false; if (alive.current) setBusy(false) }
  }
  const id = search.get('agent')
  return <section className="cs-ui agent-builder core-flow">
    <header className="cs-page-header"><div><h1>{t('Agent Builder')}</h1><p>{t('Configure a career exploration Agent, save an immutable version and preview exactly what you saved.')}</p></div></header>
    {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
    {!data && !error && <FeedbackPanel kind="loading">{t('Loading Agent Builder...')}</FeedbackPanel>}
    <div className="agent-builder-grid"><aside className="cs-surface cs-stack"><h2>{t('Agents')}</h2>
      <ul className="platform-list">{data?.agents.map(agent => <li key={agent.id}><button type="button" aria-pressed={agent.id === id} onClick={() => setSearch({ agent: agent.id })}>{agent.name} · {agent.status}</button></li>)}</ul>
      {data && !data.agents.length && <p>{t('No Agents yet. Create one to configure its first version.')}</p>}
      <form onSubmit={create} className="cs-stack"><FormField label={t('New Agent name')} required><input maxLength={180} value={name} onChange={e => setName(e.target.value)} /></FormField><FormField label={t('Agent purpose')} required><textarea maxLength={2000} value={purpose} onChange={e => setPurpose(e.target.value)} /></FormField><Button type="submit" variant="primary" busy={busy} disabled={data?.me.role !== 'ADMIN'}>{t('Create Agent')}</Button></form>
    </aside><div>{id && data?.me.role === 'ADMIN' ? <AgentWorkspace key={`${data.me.id}:${id}`} owner={data.me.id} id={id} bases={data.bases} options={data.options} onChanged={refresh} /> : <FeedbackPanel kind="empty">{t('Choose an Agent to edit saved versions and assignments.')}</FeedbackPanel>}</div></div>
  </section>
}

function AgentWorkspace({ owner, id, bases, options, onChanged }) {
  const { t } = useI18n(), localKey = draftKey(owner, 'agent-config', id), previewKey = draftKey(owner, 'agent-preview-execution', id)
  const [agent, setAgent] = useState(null), [configs, setConfigs] = useState([]), [assignments, setAssignments] = useState([]), [selected, setSelected] = useState(null)
  const [name, setName] = useState(''), [purpose, setPurpose] = useState(''), [agentStatus, setAgentStatus] = useState('DRAFT')
  const [draft, setDraft] = useState(() => emptyConfiguration(options)), [expectedRevision, setExpectedRevision] = useState(null)
  const [error, setError] = useState(''), [notice, setNotice] = useState(''), [errors, setErrors] = useState({}), [busy, setBusy] = useState(false), [conflict, setConflict] = useState(false)
  const [query, setQuery] = useState(''), [requestId, setRequestId] = useState(crypto.randomUUID()), [student, setStudent] = useState(''), [pendingSelection, setPendingSelection] = useState(null)
  const [accepted, setAccepted] = useState(() => readDraft(previewKey))
  const alive = useRef(false), inFlight = useRef(false)
  const result = useExecution(accepted?.id ?? null)
  const baseline = selected ? configurationDraft(selected) : emptyConfiguration(options)
  const dirty = JSON.stringify(draft) !== JSON.stringify(baseline)
  const matchingRun = result.execution?.configurationId === selected?.id && result.execution?.configurationHash === selected?.contentHash
  const running = Boolean(accepted?.id && !result.error && (!result.execution || ['ACCEPTED', 'RUNNING'].includes(result.execution.status)))
  const tested = matchingRun && result.execution?.status === 'SUCCEEDED'
  const published = configs.find(c => c.id === agent?.publishedConfigurationId)
  const readWorkspace = useCallback(() => Promise.all([apiRequest(agentPath(id)), apiRequest(`${agentPath(id)}/configurations`), apiRequest(`${agentPath(id)}/assignments`)]), [id])
  useEffect(() => {
    alive.current = true
    readWorkspace().then(([a, versions, members]) => {
      if (!alive.current) return
      const local = readDraft(localKey), saved = versions.find(c => c.id === local?.configurationId) ?? (!local?.dirty ? versions[0] : null)
      setAgent(a); setConfigs(versions); setAssignments(members); setName(a.name); setPurpose(a.purpose); setAgentStatus(a.status)
      setSelected(saved ?? null); setDraft(local?.draft ?? (saved ? configurationDraft(saved) : emptyConfiguration(options)))
      setExpectedRevision(local?.dirty && Number.isInteger(local.baseRevision) ? local.baseRevision : a.revision)
      if (local?.dirty) setNotice('Your preserved local draft has been restored. Save before Preview.')
    }).catch(failure => { if (alive.current) setError(failure.message) })
    return () => { alive.current = false }
  }, [localKey, options, readWorkspace])
  function preserve(next, config = selected, revision = expectedRevision) {
    writeDraft(localKey, { draft: next, configurationId: config?.id ?? null, baseRevision: revision, dirty: JSON.stringify(next) !== JSON.stringify(config ? configurationDraft(config) : emptyConfiguration(options)) })
  }
  function edit(next) { setDraft(next); preserve(next); setNotice(''); setErrors({}) }
  function resetPreview() { setAccepted(null); writeDraft(previewKey, null); setRequestId(crypto.randomUUID()) }
  function select(config) {
    const next = config ? configurationDraft(config) : emptyConfiguration(options)
    setSelected(config); setDraft(next); setErrors({}); setError(''); setNotice(''); resetPreview(); preserve(next, config)
  }
  async function refresh() {
    const [a, versions, members] = await readWorkspace()
    if (!alive.current) return
    setAgent(a); setConfigs(versions); setAssignments(members)
    return a
  }
  async function action(work, refreshAfter = true) {
    if (inFlight.current) return
    inFlight.current = true; setBusy(true); setError(''); setNotice('')
    try {
      await work()
      if (!alive.current) return
      if (refreshAfter) { const a = await refresh(); if (a) setExpectedRevision(a.revision); await onChanged() }
    } catch (failure) {
      if (alive.current) { setError(failure.status === 409 ? 'This Agent has changed. Reload the latest Agent before saving. Your draft is preserved.' : failure.message); if (failure.status === 409) setConflict(true) }
    } finally { inFlight.current = false; if (alive.current) setBusy(false) }
  }
  async function save(event) {
    event.preventDefault()
    const invalid = validateConfiguration(draft, options, bases)
    setErrors(invalid)
    if (Object.keys(invalid).length) { setError('Complete the required agent settings before saving.'); return }
    if (conflict) return
    await action(async () => {
      const config = await platformCommand(`${agentPath(id)}/configurations`, draft, expectedRevision)
      if (!alive.current) return
      const next = configurationDraft(config)
      setSelected(config); setConfigs(current => [config, ...current.filter(c => c.id !== config.id)]); setDraft(next); resetPreview(); preserve(next, config)
      setNotice('A new immutable version was saved. Nothing was published or assigned; no provider was called.')
    })
  }
  async function preview(event) {
    event.preventDefault()
    if (dirty || !selected || running || result.error || !query.trim()) return
    const retry = accepted?.id && ['FAILED', 'REFUSED', 'CANCELLED'].includes(result.execution?.status)
    const token = retry ? crypto.randomUUID() : requestId
    setRequestId(token)
    await action(async () => {
      const execution = await platformCommand(`${agentPath(id)}/configurations/${selected.id}/previews`, { requestId: token, query })
      if (!alive.current) return
      const snapshot = { id: execution.id, configurationId: selected.id, configurationHash: selected.contentHash, query, requestId: token }
      setAccepted(snapshot); writeDraft(previewKey, snapshot)
    }, false)
  }
  if (!agent) return <FeedbackPanel kind={error ? 'error' : 'loading'}>{t(error || 'Loading Agent...')}</FeedbackPanel>
  return <div className="cs-stack">
    <header className="cs-surface cs-stack"><div className="cs-section-header"><h2>{agent.name}</h2><StatusBadge>{agent.status}</StatusBadge></div>
      <p>{t('Selected saved version')}: {selected ? `${t('Version')} ${selected.versionNumber}` : t('None selected')} · {t('Published version')}: {published ? `${t('Version')} ${published.versionNumber}` : agent.publishedConfigurationId ?? t('Not published')}</p>
      <p role="status">{t(dirty ? 'Unsaved changes — Save a new version before Preview.' : 'Preview uses the selected saved configuration only.')}</p>
      {selected && <details className="cs-disclosure"><summary>{t('Exact configuration hash')}</summary><code>{selected.contentHash}</code></details>}
      <div className="cs-actions"><Button disabled={busy} onClick={() => action(async () => { const a = await refresh(); if (a) { setExpectedRevision(a.revision); setConflict(false); preserve(draft, selected, a.revision); setNotice('Latest Agent revision loaded. Your unsaved input is preserved.') } }, false)}>{t('Reload latest Agent')}</Button><Button disabled={busy || !dirty} onClick={() => setPendingSelection({ config: selected })}>{t('Discard unsaved changes')}</Button></div>
    </header>
    {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}{notice && <FeedbackPanel kind="success">{t(notice)}</FeedbackPanel>}
    <section className="cs-surface cs-stack"><h3>{t('Identity')}</h3><form className="cs-stack" onSubmit={event => { event.preventDefault(); action(() => platformCommand(agentPath(id), { name, purpose, status: agentStatus }, agent.revision, 'PATCH')) }}>
      <FormField label={t('Agent name')} required><input maxLength={180} value={name} onChange={e => setName(e.target.value)} /></FormField><FormField label={t('Agent description / purpose')} required><textarea maxLength={2000} value={purpose} onChange={e => setPurpose(e.target.value)} /></FormField>
      <details className="cs-disclosure"><summary>{t('Agent identity and availability')}</summary><FormField label={t('Agent status')}><select value={agentStatus} onChange={e => setAgentStatus(e.target.value)}><option value="DRAFT">{t('Draft')}</option><option value="ACTIVE">{t('Active')}</option><option value="ARCHIVED">{t('Archived')}</option></select></FormField></details><div className="cs-actions"><Button type="submit" disabled={busy}>{t('Save Agent metadata')}</Button></div>
    </form></section>
    <section className="cs-surface"><h3>{t('Immutable saved configurations')}</h3><ul className="platform-list">{configs.map(config => <li key={config.id}><button type="button" disabled={busy} aria-pressed={selected?.id === config.id} onClick={() => dirty ? setPendingSelection({ config }) : select(config)}>{t('Version')} {config.versionNumber} · {config.status} · {config.modelKey}</button></li>)}</ul>{!configs.length && <p>{t('No saved version yet. Complete the settings below and Save.')}</p>}</section>
    <AgentConfigurationForm draft={draft} edit={edit} options={options} bases={bases} busy={busy || conflict} archived={agent.status === 'ARCHIVED'} errors={errors} onSave={save} />
    <section className="cs-surface cs-stack"><h3>{t('Preview saved version')}</h3><p>{t('Preview uses public knowledge only and never impersonates a Student. Provider unavailability is reported as a failed execution.')}</p>
      <p>{t('Selected saved version')}: {selected ? `${t('Version')} ${selected.versionNumber}` : t('None selected')}</p>
      {dirty && <FeedbackPanel kind="warning">{t('Save your revised configuration before running Preview. Any displayed result belongs to the previously saved version.')}</FeedbackPanel>}
      <form onSubmit={preview} className="cs-stack"><FormField label={t('Preview question')} required><textarea maxLength={2000} disabled={running} value={query} onChange={e => { setQuery(e.target.value); setRequestId(crypto.randomUUID()) }} /></FormField><div className="cs-actions"><Button type="submit" variant="primary" disabled={busy || dirty || !selected || running || Boolean(result.error) || agent.status === 'ARCHIVED'}>{t(accepted?.id && ['FAILED', 'REFUSED', 'CANCELLED'].includes(result.execution?.status) ? 'Retry preview' : 'Run saved configuration preview')}</Button></div></form>
      {accepted?.query && <p className="cs-caption">{t('Executed preview question')}: {accepted.query}</p>}
      {result.execution && !matchingRun ? <FeedbackPanel kind="warning">{t('This execution belongs to another saved configuration. Select its version to inspect it.')}</FeedbackPanel> : <ExecutionResult key={accepted?.id ?? 'none'} presentation="builder" id={accepted?.id} {...result} />}
      <Button disabled={busy || dirty || !tested || selected?.status !== 'SAVED'} onClick={() => action(async () => { await platformCommand(`${agentPath(id)}/configurations/${selected.id}/publish`, { confirmed: true }, agent.revision); const config = await apiRequest(`${agentPath(id)}/configurations/${selected.id}`); if (alive.current) setSelected(config) })}>{t('Publish this tested configuration')}</Button>
    </section>
    <section className="cs-surface cs-stack"><h3>{t('Student assignments')}</h3><p>{t('Only explicitly assigned active Students can see this active Agent.')}</p><form className="cs-stack" onSubmit={event => { event.preventDefault(); action(() => platformCommand(`${agentPath(id)}/assignments`, { studentUserId: student })) }}><FormField label={t('Student account ID')} required><input inputMode="numeric" pattern="[1-9][0-9]*" value={student} onChange={e => setStudent(e.target.value)} /></FormField><div className="cs-actions"><Button type="submit" disabled={busy || agent.status !== 'ACTIVE'}>{t('Assign this Agent')}</Button></div></form><ul className="platform-list">{assignments.map(a => <li key={a.id}><p>{t('Student account ID')}: {a.studentUserId} · {a.status}</p>{a.status === 'ACTIVE' && <Button disabled={busy} onClick={() => action(() => platformCommand(`${agentPath(id)}/assignments/${a.id}/revoke`, {}))}>{t('Revoke assignment')}</Button>}</li>)}</ul></section>
    {pendingSelection && <Dialog title={t('Discard unsaved configuration changes?')} description={t('The selected saved configuration is unchanged. Discarding restores saved settings.')} closeLabel={t('Keep editing')} onRequestClose={() => setPendingSelection(null)} footer={<><Button onClick={() => setPendingSelection(null)}>{t('Keep editing')}</Button><Button variant="danger" onClick={() => { select(pendingSelection.config); setPendingSelection(null) }}>{t('Discard and restore')}</Button></>} />}
  </div>
}
