import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import StatusBadge from '../../../shared/ui/StatusBadge.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import Tabs from '../../../shared/ui/Tabs.jsx'
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
  const [creating, setCreating] = useState(false), [createError, setCreateError] = useState('')
  const createFormId = useId(), createName = useRef(null), createPurpose = useRef(null)
  const [createErrors, setCreateErrors] = useState({})
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
    const invalid = { ...(!name.trim() && { name: 'Enter an Agent name.' }), ...(!purpose.trim() && { purpose: 'Enter an Agent purpose.' }) }
    setCreateErrors(invalid)
    if (Object.keys(invalid).length) { (invalid.name ? createName : createPurpose).current?.focus(); return }
    pending.current = true; setBusy(true); setCreateError('')
    try {
      const agent = await platformCommand('/v2/admin/agents', { name, purpose })
      if (!alive.current) return
      setData(current => ({ ...current, agents: [...current.agents.filter(a => a.id !== agent.id), agent] })); setSearch({ agent: agent.id }); setName(''); setPurpose(''); setCreating(false)
    } catch (failure) { if (alive.current) setCreateError(failure.message) }
    finally { pending.current = false; if (alive.current) setBusy(false) }
  }
  const id = search.get('agent')
  return <section className="cs-ui agent-builder core-flow">
    <header className="cs-page-header"><div><h1>{t('Agent Studio')}</h1><p>{t('Configure a career exploration Agent, save an immutable version and preview exactly what you saved.')}</p></div></header>
    {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
    {!data && !error && <FeedbackPanel kind="loading">{t('Loading Agent Builder...')}</FeedbackPanel>}
    <div className="agent-builder-grid"><aside className="cs-surface cs-stack agent-list" aria-label={t('Agents')}><div className="cs-section-header"><h2>{t('Agents')}</h2></div>
      <Button variant="primary" disabled={data?.me.role !== 'ADMIN'} onClick={() => { setCreateError(''); setCreateErrors({}); setCreating(true) }}>{t('+ Create Agent')}</Button>
      <ul className="platform-list">{data?.agents.map(agent => <li key={agent.id}><button type="button" aria-label={`${agent.name} · ${agent.status}`} aria-pressed={agent.id === id} onClick={() => setSearch({ agent: agent.id })}><strong>{agent.name}</strong><span className="agent-list-purpose">{agent.purpose}</span><span className="cs-caption">{agent.status}</span></button></li>)}</ul>
      {data && !data.agents.length && <p>{t('No Agents yet. Create one to configure its first version.')}</p>}
    </aside><div>{id && data?.me.role === 'ADMIN' ? <AgentWorkspace key={`${data.me.id}:${id}`} owner={data.me.id} id={id} bases={data.bases} options={data.options} onChanged={refresh} /> : <FeedbackPanel kind="empty">{t('Choose an Agent to edit saved versions and assignments.')}</FeedbackPanel>}</div></div>
    {creating && <Dialog title={t('Create Agent')} closeLabel={t('Close dialog')} initialFocusRef={createName} busy={busy} onRequestClose={() => { if (!pending.current) { setCreating(false); setName(''); setPurpose('') } }} footer={<><Button disabled={busy} onClick={() => { setCreating(false); setName(''); setPurpose('') }}>{t('Cancel')}</Button><Button form={createFormId} type="submit" variant="primary" busy={busy}>{t('Create Agent')}</Button></>}>
      <form id={createFormId} onSubmit={create} noValidate className="cs-stack">
        {createError && <FeedbackPanel kind="error">{t(createError)}</FeedbackPanel>}
        <FormField label={t('Name')} required error={t(createErrors.name)}><input ref={createName} disabled={busy} maxLength={180} value={name} onChange={e => setName(e.target.value)} /></FormField>
        <FormField label={t('Description / purpose')} required error={t(createErrors.purpose)}><textarea ref={createPurpose} disabled={busy} rows={3} maxLength={2000} value={purpose} onChange={e => setPurpose(e.target.value)} /></FormField>
      </form>
    </Dialog>}
  </section>
}

function AgentWorkspace({ owner, id, bases, options, onChanged }) {
  const { t } = useI18n(), localKey = draftKey(owner, 'agent-config', id), previewKey = draftKey(owner, 'agent-preview-execution', id)
  const [agent, setAgent] = useState(null), [configs, setConfigs] = useState([]), [assignments, setAssignments] = useState([]), [selected, setSelected] = useState(null)
  const [name, setName] = useState(''), [purpose, setPurpose] = useState(''), [agentStatus, setAgentStatus] = useState('DRAFT')
  const [draft, setDraft] = useState(() => emptyConfiguration(options)), [expectedRevision, setExpectedRevision] = useState(null)
  const [error, setError] = useState(''), [notice, setNotice] = useState(''), [errors, setErrors] = useState({}), [busy, setBusy] = useState(false), [conflict, setConflict] = useState(false), [errorScope, setErrorScope] = useState('configuration')
  const [query, setQuery] = useState(''), [requestId, setRequestId] = useState(crypto.randomUUID()), [student, setStudent] = useState(''), [pendingSelection, setPendingSelection] = useState(null)
  const [accepted, setAccepted] = useState(() => readDraft(previewKey))
  const [tab, setTab] = useState('brain'), [pane, setPane] = useState('configuration'), [submittedQuery, setSubmittedQuery] = useState('')
  const alive = useRef(false), inFlight = useRef(false)
  const result = useExecution(accepted?.id ?? null)
  const baseline = selected ? configurationDraft(selected) : emptyConfiguration(options)
  const dirty = JSON.stringify(draft) !== JSON.stringify(baseline)
  const identityDirty = Boolean(agent && (name !== agent.name || purpose !== agent.purpose || agentStatus !== agent.status))
  const hasEdits = dirty || identityDirty
  const matchingRun = result.execution?.configurationId === selected?.id && result.execution?.configurationHash === selected?.contentHash
  const running = Boolean(accepted?.id && !result.error && (!result.execution || ['ACCEPTED', 'RUNNING'].includes(result.execution.status)))
  const awaitingPreview = busy && errorScope === 'preview'
  const showAcceptedRun = accepted?.id && !awaitingPreview && (!submittedQuery || submittedQuery === accepted.query)
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
  function resetPreview() { setSubmittedQuery(''); setAccepted(null); writeDraft(previewKey, null); setRequestId(crypto.randomUUID()) }
  function select(config) {
    const next = config ? configurationDraft(config) : emptyConfiguration(options)
    setSelected(config); setDraft(next); setErrors({}); setError(''); setNotice(''); resetPreview(); preserve(next, config)
    setName(agent.name); setPurpose(agent.purpose); setAgentStatus(agent.status); setSubmittedQuery('')
  }
  async function refresh() {
    const [a, versions, members] = await readWorkspace()
    if (!alive.current) return
    setAgent(a); setConfigs(versions); setAssignments(members)
    return a
  }
  async function action(work, refreshAfter = true, scope = 'configuration') {
    if (inFlight.current) return
    inFlight.current = true; setBusy(true); setError(''); setErrorScope(scope); setNotice('')
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
    if (!name.trim() || !purpose.trim()) { setPane('configuration'); setTab('brain'); setErrorScope('configuration'); setError('Agent name and purpose are required.'); return }
    const invalid = validateConfiguration(draft, options, bases)
    setErrors(invalid)
    if (Object.keys(invalid).length) {
      setPane('configuration')
      setTab(invalid.modelKey || invalid.promptTemplate ? 'brain' : invalid.knowledgeBaseIds || invalid.tools ? 'integrations' : 'advanced')
      setErrorScope('configuration'); setError('Complete the required agent settings before saving.'); return
    }
    if (conflict) return
    await action(async () => {
      let revision = expectedRevision
      if (identityDirty) {
        const updated = await platformCommand(agentPath(id), { name, purpose, status: agentStatus }, revision, 'PATCH')
        if (!alive.current) return
        setAgent(updated); revision = updated.revision; setExpectedRevision(revision)
      }
      const config = await platformCommand(`${agentPath(id)}/configurations`, draft, revision)
      if (!alive.current) return
      const next = configurationDraft(config)
      setSelected(config); setConfigs(current => [config, ...current.filter(c => c.id !== config.id)]); setDraft(next); resetPreview(); preserve(next, config)
      setNotice('A new immutable version was saved. Nothing was published or assigned; no provider was called.')
    })
  }
  async function preview(event) {
    event.preventDefault()
    if (hasEdits || !selected || running || result.error || !query.trim()) return
    setSubmittedQuery(query)
    const retry = accepted?.id && ['FAILED', 'REFUSED', 'CANCELLED'].includes(result.execution?.status)
    const token = retry ? crypto.randomUUID() : requestId
    setRequestId(token)
    await action(async () => {
      const execution = await platformCommand(`${agentPath(id)}/configurations/${selected.id}/previews`, { requestId: token, query })
      if (!alive.current) return
      const snapshot = { id: execution.id, configurationId: selected.id, configurationHash: selected.contentHash, query, requestId: token }
      setAccepted(snapshot); writeDraft(previewKey, snapshot)
    }, false, 'preview')
  }
  if (!agent) return <FeedbackPanel kind={error ? 'error' : 'loading'}>{t(error || 'Loading Agent...')}</FeedbackPanel>
  const configurationArea = area => <AgentConfigurationForm area={area} active={tab === area} draft={draft} edit={edit} options={options} bases={bases} busy={busy || conflict} archived={agent.status === 'ARCHIVED'} errors={errors} />
  const identity = <section className="cs-surface cs-stack"><h3>{t('Identity')}</h3><form className="cs-stack" onSubmit={event => { event.preventDefault(); action(() => platformCommand(agentPath(id), { name, purpose, status: agentStatus }, agent.revision, 'PATCH')) }}>
    <fieldset disabled={busy} className="agent-builder-fields cs-fields">
      <FormField label={t('Agent name')} required><input maxLength={180} value={name} onChange={e => setName(e.target.value)} /></FormField>
      <FormField label={t('Agent description / purpose')} required><textarea rows={2} maxLength={2000} value={purpose} onChange={e => setPurpose(e.target.value)} /></FormField>
    </fieldset>
    <details className="cs-disclosure"><summary>{t('Agent identity and availability')}</summary><FormField label={t('Agent status')}><select disabled={busy} value={agentStatus} onChange={e => setAgentStatus(e.target.value)}><option value="DRAFT">{t('Draft')}</option><option value="ACTIVE">{t('Active')}</option><option value="ARCHIVED">{t('Archived')}</option></select></FormField><Button type="submit" disabled={busy}>{t('Save Agent metadata')}</Button></details>
  </form></section>
  const chat = <section className="cs-surface agent-chat" aria-label={t('Admin Preview chat')}>
    <header className="agent-chat-header"><p className="agent-eyebrow">{t('PREVIEW · SAVED CONFIGURATION')}</p><h3>{t('Chat Preview')}</h3><p className="cs-caption">{selected ? `${t('Saved configuration')} · ${t('Version')} ${selected.versionNumber}` : t('None selected')}</p><p className="cs-caption">{t('Each Preview is an independent run of the selected saved version. No Student session or personal Student evidence is used.')}</p>
    {hasEdits && <FeedbackPanel kind="warning">{t('Unsaved changes are not used in Preview. Save the configuration to test the new settings.')}</FeedbackPanel>}
    {!selected && <FeedbackPanel kind="info">{t('Save a configuration in Brain before opening a Preview.')}</FeedbackPanel>}
    </header>
    <div className="agent-chat-messages" role="log" aria-label={t('Preview messages')} aria-live="polite" aria-relevant="additions text">
      {!(accepted?.query || submittedQuery) && !accepted?.id && <div className="agent-chat-empty"><h4>{t('Try your saved Agent')}</h4><p>{t('Ask a question to test its instructions, granted knowledge and authorized tools.')}</p></div>}
      {(submittedQuery || accepted?.query) && <article className="agent-message agent-message--user" aria-label={t('Admin message')}><strong>{t('You')}</strong><p className="cs-prose">{submittedQuery || accepted.query}</p></article>}
      {(accepted?.id || awaitingPreview || (error && errorScope === 'preview')) && <article className="agent-message agent-message--agent" aria-label={t('Agent response')}><strong>{agent.name}</strong>
        {error && errorScope === 'preview' && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
        {awaitingPreview && <FeedbackPanel kind="loading">{t('Sending Preview...')}</FeedbackPanel>}
        {showAcceptedRun && (result.execution && !matchingRun ? <FeedbackPanel kind="warning">{t('This execution belongs to another saved configuration. Select its version to inspect it.')}</FeedbackPanel> : <ExecutionResult key={accepted?.id ?? 'none'} presentation="builder" detailsLabel="View run details" id={accepted?.id} {...result} />)}
      </article>}
    </div>
    <form onSubmit={preview} className="agent-chat-composer"><FormField label={t('Preview question')} required><textarea rows={3} maxLength={2000} disabled={running} value={query} onChange={e => { setQuery(e.target.value); setRequestId(crypto.randomUUID()) }} /></FormField><div className="cs-actions"><Button type="submit" variant="primary" busy={busy} disabled={busy || hasEdits || !selected || running || Boolean(result.error) || agent.status === 'ARCHIVED'}>{t(accepted?.id && ['FAILED', 'REFUSED', 'CANCELLED'].includes(result.execution?.status) ? 'Retry preview' : 'Send')}</Button></div></form>
  </section>
  const publication = <details className="cs-surface agent-publication"><summary>{t('Publication and assignments')}</summary><div className="cs-stack">
    <Button disabled={busy || hasEdits || !tested || selected?.status !== 'SAVED'} onClick={() => action(async () => { await platformCommand(`${agentPath(id)}/configurations/${selected.id}/publish`, { confirmed: true }, agent.revision); const config = await apiRequest(`${agentPath(id)}/configurations/${selected.id}`); if (alive.current) setSelected(config) })}>{t('Publish this tested configuration')}</Button>
    <section className="cs-stack"><h3>{t('Student assignments')}</h3><p>{t('Only explicitly assigned active Students can see this active Agent.')}</p><form className="cs-stack" onSubmit={event => { event.preventDefault(); action(() => platformCommand(`${agentPath(id)}/assignments`, { studentUserId: student })) }}><FormField label={t('Student account ID')} required><input inputMode="numeric" pattern="[1-9][0-9]*" value={student} onChange={e => setStudent(e.target.value)} /></FormField><div className="cs-actions"><Button type="submit" disabled={busy || agent.status !== 'ACTIVE'}>{t('Assign this Agent')}</Button></div></form><ul className="platform-list">{assignments.map(a => <li key={a.id}><p>{t('Student account ID')}: {a.studentUserId} · {a.status}</p>{a.status === 'ACTIVE' && <Button disabled={busy} onClick={() => action(() => platformCommand(`${agentPath(id)}/assignments/${a.id}/revoke`, {}))}>{t('Revoke assignment')}</Button>}</li>)}</ul></section>
  </div></details>
  return <div className="cs-stack agent-workspace">
    <form onSubmit={save} className="cs-surface agent-workspace-header"><div className="cs-section-header"><h2>{agent.name}</h2><StatusBadge>{agent.status}</StatusBadge></div>
      <div className="agent-version-controls"><FormField label={t('Selected saved version')}><select disabled={busy} value={selected?.id ?? ''} onChange={e => { const config = configs.find(c => c.id === e.target.value); if (hasEdits) setPendingSelection({ config }); else select(config) }}>{!selected && <option value="">{t('None selected')}</option>}{configs.map(config => <option key={config.id} value={config.id}>{t('Version')} {config.versionNumber} · {config.status}</option>)}</select></FormField>
        {agent.publishedConfigurationId && <p>{t('Published version')}: {published ? `${t('Version')} ${published.versionNumber}` : agent.publishedConfigurationId}</p>}
        <Button disabled={busy} onClick={() => action(async () => { const a = await refresh(); if (a) { setExpectedRevision(a.revision); setConflict(false); preserve(draft, selected, a.revision); setNotice('Latest Agent revision loaded. Your unsaved input is preserved.') } }, false)}>{t('Reload latest Agent')}</Button>
      </div>
      <p className="agent-draft-status" role="status">{t(hasEdits ? 'Unsaved changes — Save a new version before Preview.' : 'Preview uses the selected saved configuration only.')}</p>
      {selected && <details className="cs-disclosure"><summary>{t('Exact configuration hash')}</summary><code>{selected.contentHash}</code></details>}
      <div className="agent-save-bar"><div className="cs-actions"><Button disabled={busy || !hasEdits} onClick={() => setPendingSelection({ config: selected })}>{t('Discard unsaved changes')}</Button><Button type="submit" variant="primary" busy={busy} disabled={busy || conflict || agent.status === 'ARCHIVED'}>{t('Save changes')}</Button></div><p className="cs-caption">{t('Save creates a version only. It does not publish, assign Students or call the AI provider.')}</p></div>
    </form>
    {error && errorScope !== 'preview' && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}{notice && <FeedbackPanel kind="success">{t(notice)}</FeedbackPanel>}
    <div className="agent-pane-toggle" role="group" aria-label={t('Agent workspace view')}>
      <Button aria-pressed={pane === 'configuration'} aria-controls={`${id}-configuration-pane`} onClick={() => setPane('configuration')}>{t('Configuration')}</Button>
      <Button aria-pressed={pane === 'preview'} aria-controls={`${id}-preview-pane`} onClick={() => setPane('preview')}>{t('Preview')}</Button>
    </div>
    <div className="agent-studio-panes">
      <section id={`${id}-configuration-pane`} className={'agent-configuration-pane' + (pane !== 'configuration' ? ' agent-pane--inactive' : '')} aria-label={t('Agent configuration')}>
        <Tabs label={t('Agent configuration workspace')} value={tab} onChange={setTab} items={[
          { value: 'brain', label: t('Brain'), content: <div className="cs-stack">{identity}{configurationArea('brain')}</div> },
          { value: 'integrations', label: t('Integrations'), content: configurationArea('integrations') },
          { value: 'advanced', label: t('Advanced'), content: configurationArea('advanced') },
        ]} />
      </section>
      <div id={`${id}-preview-pane`} className={'agent-preview-pane' + (pane !== 'preview' ? ' agent-pane--inactive' : '')}>{chat}</div>
    </div>
    {publication}
    {pendingSelection && <Dialog title={t('Discard unsaved configuration changes?')} description={t('The selected saved configuration is unchanged. Discarding restores saved settings.')} closeLabel={t('Keep editing')} onRequestClose={() => setPendingSelection(null)} footer={<><Button onClick={() => setPendingSelection(null)}>{t('Keep editing')}</Button><Button variant="danger" onClick={() => { select(pendingSelection.config); setPendingSelection(null) }}>{t('Discard and restore')}</Button></>} />}
  </div>
}
