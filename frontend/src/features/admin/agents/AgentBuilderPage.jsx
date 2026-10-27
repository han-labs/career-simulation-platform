import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import StatusBadge from '../../../shared/ui/StatusBadge.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import Tabs from '../../../shared/ui/Tabs.jsx'
import AgentPreview from './AgentPreview.jsx'
import { agentPath, platformCommand } from '../agentPlatform/platformApi.js'
import PublishAssignPanel from './PublishAssignPanel.jsx'
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
  const [archiving, setArchiving] = useState(null)
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
    {!id && <header className="cs-page-header"><div><p className="agent-eyebrow">{t('Agent Studio')}</p><h1>{t('AI Agents')}</h1><p>{t('Configure bounded career Agents and their knowledge access.')}</p></div><Button variant="primary" disabled={data?.me.role !== 'ADMIN'} onClick={() => { setCreateError(''); setCreateErrors({}); setCreating(true) }}>{t('+ Create Agent')}</Button></header>}
    {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
    {!data && !error && <FeedbackPanel kind="loading">{t('Loading Agent Builder...')}</FeedbackPanel>}
    {id ? data?.me.role === 'ADMIN' && <><Link className="cs-button cs-button--quiet kb-back" aria-label={t('All Agents')} to="/admin/agents">← {t('All Agents')}</Link><AgentWorkspace key={`${data.me.id}:${id}`} owner={data.me.id} id={id} bases={data.bases} options={data.options} onChanged={refresh} /></> : <div className="agent-collection">
      {data?.agents.map(agent => <article key={agent.id} className="cs-surface studio-collection-card"><Link className="studio-card-main" aria-label={`${agent.name} · ${agent.status}`} to={'?agent=' + encodeURIComponent(agent.id)}><h2>{agent.name}</h2><p className="agent-list-purpose">{agent.purpose}</p><div className="cs-actions"><StatusBadge>{t(agent.status)}</StatusBadge><span className="cs-caption">{t(agent.publishedConfigurationId ? 'Published' : 'Not published')}</span></div></Link><div className="cs-actions"><Link className="cs-button" to={'?agent=' + encodeURIComponent(agent.id)} aria-label={t('Open') + ': ' + agent.name}>{t('Open')}</Link>{agent.status !== 'ARCHIVED' && <><Button variant="quiet" disabled={busy} onClick={() => { setError(''); setArchiving(agent) }} aria-label={t('Archive Agent') + ': ' + agent.name}>{t('Archive')}</Button></>}</div></article>)}
      {data && !data.agents.length && <FeedbackPanel kind="empty">{t('No Agents yet. Create one to configure its first version.')}</FeedbackPanel>}
    </div>}
    {archiving && <Dialog title={t('Archive Agent?')} closeLabel={t('Close dialog')} busy={busy} onRequestClose={() => setArchiving(null)} footer={<><Button disabled={busy} onClick={() => setArchiving(null)}>{t('Cancel')}</Button><Button variant="danger" busy={busy} onClick={async () => { if (pending.current) return; pending.current = true; setBusy(true); try { await platformCommand(agentPath(archiving.id), {}, archiving.revision, 'DELETE'); if (alive.current) { await refresh(); setArchiving(null) } } catch (failure) { if (alive.current) setError(failure.message) } finally { pending.current = false; if (alive.current) setBusy(false) } }}>{t('Confirm archive')}</Button></>}><p>{archiving.name}</p><p>{t('Archive this Agent? Configuration and execution history are retained.')}</p>{error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}</Dialog>}
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
  const { t } = useI18n(), localKey = draftKey(owner, 'agent-config', id)
  const [agent,setAgent]=useState(null), [configs,setConfigs]=useState([]), [selected,setSelected]=useState(null)
  const [draft,setDraft]=useState(()=>emptyConfiguration(options)), [expectedRevision,setExpectedRevision]=useState(null)
  const [error,setError]=useState(''), [notice,setNotice]=useState(''), [errors,setErrors]=useState({}), [busy,setBusy]=useState(false), [conflict,setConflict]=useState(false)
  const [pendingSelection,setPendingSelection]=useState(null), [tab,setTab]=useState('brain'), [pane,setPane]=useState('configuration'), [proof,setProof]=useState(null)
  const alive=useRef(false), inFlight=useRef(false)
  const baseline=selected?configurationDraft(selected,agent):{...emptyConfiguration(options),experienceConfig:{...emptyConfiguration(options).experienceConfig,name:agent?.name ?? '',description:agent?.purpose ?? ''}}
  const hasEdits=JSON.stringify(draft)!==JSON.stringify(baseline)
  const published=configs.find(c=>c.id===agent?.publishedConfigurationId)
  const readWorkspace=useCallback(()=>Promise.all([apiRequest(agentPath(id)),apiRequest(`${agentPath(id)}/configurations`)]),[id])
  useEffect(()=>{
    alive.current=true
    readWorkspace().then(([a,versions])=>{
      if(!alive.current)return
      const local=readDraft(localKey), saved=versions.find(c=>c.id===local?.configurationId) ?? (!local?.dirty?versions[0]:null)
      const initial=saved?configurationDraft(saved,a):{...emptyConfiguration(options),experienceConfig:{...emptyConfiguration(options).experienceConfig,name:a.name,description:a.purpose}}
      // Old repair drafts are retained and upgraded in memory; never overwrite them on load.
      const preserved=local?.dirty?{...initial,...local.draft,experienceConfig:local.draft?.experienceConfig ?? {...initial.experienceConfig,name:local.identity?.name ?? a.name,description:local.identity?.purpose ?? a.purpose}}:initial
      setAgent(a);setConfigs(versions);setSelected(saved ?? null);setDraft(preserved)
      setExpectedRevision(local?.dirty&&Number.isInteger(local.baseRevision)?local.baseRevision:a.revision)
      if(local?.dirty)setNotice('Your preserved local draft has been restored. Save before Preview.')
    }).catch(f=>{if(alive.current)setError(f.message)})
    return()=>{alive.current=false}
  },[localKey,options,readWorkspace])
  function preserve(next,config=selected,revision=expectedRevision,reference=agent){
    const base=config?configurationDraft(config,reference):{...emptyConfiguration(options),experienceConfig:{...emptyConfiguration(options).experienceConfig,name:reference?.name ?? '',description:reference?.purpose ?? ''}}
    writeDraft(localKey,{draft:next,configurationId:config?.id ?? null,baseRevision:revision,dirty:JSON.stringify(next)!==JSON.stringify(base)})
  }
  function edit(next){setDraft(next);preserve(next);setNotice('');setErrors({})}
  function select(config){const next=config?configurationDraft(config,agent):baseline;setSelected(config);setDraft(next);setErrors({});setError('');setNotice('');preserve(next,config)}
  async function refresh(){const [a,versions]=await readWorkspace();if(!alive.current)return;setAgent(a);setConfigs(versions);setSelected(current=>versions.find(c=>c.id===current?.id) ?? current);return a}
  async function action(work){if(inFlight.current)return;inFlight.current=true;setBusy(true);setError('');setNotice('');try{await work()}catch(f){if(alive.current){setError(f.status===409?'This Agent has changed. Reload the latest Agent before saving. Your draft is preserved.':f.message);if(f.status===409)setConflict(true)}}finally{inFlight.current=false;if(alive.current)setBusy(false)}}
  async function save(event){
    event.preventDefault();if(conflict)return
    const invalid=validateConfiguration(draft,options,bases)
    if(!draft.experienceConfig.name.trim()||!draft.experienceConfig.description.trim())invalid.identity='Agent name and purpose are required.'
    setErrors(invalid)
    if(Object.keys(invalid).length){setPane('configuration');setTab(invalid.variables||invalid.variableName?'variables':invalid.knowledgeBaseIds||invalid.tools?'integrations':invalid.identity||invalid.modelKey||invalid.fallbackModelKeys||invalid.promptTemplate||invalid.timezone||invalid.audience||invalid.language||invalid.greeting?'brain':'chat');setError('Complete the required agent settings before saving.');return}
    await action(async()=>{
      const config=await platformCommand(`${agentPath(id)}/configurations`,draft,expectedRevision)
      if(!alive.current)return
      const [a,versions]=await readWorkspace();if(!alive.current)return
      const next=configurationDraft(config,a)
      setAgent(a);setConfigs(versions);setSelected(config);setDraft(next);setExpectedRevision(a.revision);preserve(next,config,a.revision,a)
      setNotice('A new immutable version was saved. Nothing was published or activated; no provider was called.');await onChanged()
    })
  }
  if(!agent)return <FeedbackPanel kind={error?'error':'loading'}>{t(error||'Loading Agent...')}</FeedbackPanel>
  const configurationArea=area=><AgentConfigurationForm area={area} active={tab===area} draft={draft} edit={edit} options={options} bases={bases} busy={busy||conflict} archived={agent.status==='ARCHIVED'} errors={errors} identity={area==='brain'?<><FormField label={t('Name')} required error={t(errors.identity)}><input disabled={busy||conflict} maxLength={180} value={draft.experienceConfig.name} onChange={e=>edit({...draft,experienceConfig:{...draft.experienceConfig,name:e.target.value}})}/></FormField><FormField label={t('Description')} required><textarea disabled={busy||conflict} rows={2} maxLength={2000} value={draft.experienceConfig.description} onChange={e=>edit({...draft,experienceConfig:{...draft.experienceConfig,description:e.target.value}})}/></FormField></>:null}/>
  const reloadLatest=()=>action(async()=>{const a=await refresh();if(a){setExpectedRevision(a.revision);setConflict(false);preserve(draft,selected,a.revision,a);setNotice('Latest Agent revision loaded. Your unsaved input is preserved.')}})
  return <div className="cs-stack agent-workspace">
    <form onSubmit={save} className="cs-surface agent-workspace-header">
      <div className="cs-section-header"><h1>{agent.name}</h1><StatusBadge>{t(agent.status)}</StatusBadge><span className="cs-caption" role="status">{t(hasEdits?'Unsaved changes':'Saved')}</span></div>
      <div className="agent-save-bar cs-actions"><Button disabled={busy||!hasEdits} onClick={()=>setPendingSelection({config:selected})}>{t('Discard unsaved changes')}</Button><Button type="submit" variant="primary" busy={busy} disabled={busy||conflict||agent.status==='ARCHIVED'||(!hasEdits&&Boolean(selected))}>{t('Save changes')}</Button></div>
      <div className="agent-version-controls"><FormField label={t('Selected saved version')}><select disabled={busy} value={selected?.id ?? ''} onChange={e=>{const config=configs.find(c=>c.id===e.target.value);if(hasEdits)setPendingSelection({config});else select(config)}}>{!selected&&<option value="">{t('None selected')}</option>}{configs.map(c=><option key={c.id} value={c.id}>{t('Version')} {c.versionNumber} · {c.status}</option>)}</select></FormField>{agent.publishedConfigurationId&&<p className="cs-caption">{t('Published version')}: {published?`${t('Version')} ${published.versionNumber}`:t('Not available')}</p>}</div>
      <details className="cs-disclosure"><summary>{t('Version details')}</summary><dl className="cs-metadata"><div><dt>{t('Revision')}</dt><dd>{agent.revision}</dd></div><div><dt>{t('Exact configuration hash')}</dt><dd><code>{selected?.contentHash ?? t('Not saved')}</code></dd></div></dl><Button disabled={busy} onClick={reloadLatest}>{t('Reload latest Agent')}</Button></details>
    </form>
    {conflict&&<FeedbackPanel kind="warning" action={<Button disabled={busy} onClick={reloadLatest}>{t('Reload latest Agent')}</Button>}>{t('Review the latest revision before saving again.')}</FeedbackPanel>}
    {error&&<FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}{notice&&<FeedbackPanel kind="success">{t(notice)}</FeedbackPanel>}
    <div className="agent-pane-toggle" role="group" aria-label={t('Agent workspace view')}><Button aria-pressed={pane==='configuration'} aria-controls={`${id}-configuration-pane`} onClick={()=>setPane('configuration')}>{t('Configuration')}</Button><Button aria-pressed={pane==='preview'} aria-controls={`${id}-preview-pane`} onClick={()=>setPane('preview')}>{t('Preview')}</Button></div>
    <div className="agent-studio-panes">
      <section id={`${id}-configuration-pane`} className={'agent-configuration-pane'+(pane!=='configuration'?' agent-pane--inactive':'')} aria-label={t('Agent configuration')}>
        <Tabs label={t('Agent configuration workspace')} value={tab} onChange={setTab} items={[
          {value:'brain',label:t('Brain'),content:configurationArea('brain')},
          {value:'chat',label:t('Chat'),content:configurationArea('chat')},
          {value:'integrations',label:t('Integrations'),content:configurationArea('integrations')},
          {value:'variables',label:t('Variables'),content:configurationArea('variables')},
          {value:'publication',label:t('Publish'),content:<PublishAssignPanel key={id} active={tab==='publication'} agent={agent} selected={selected} published={published} dirty={hasEdits} execution={proof} onChanged={async()=>{const a=await refresh();if(a&&alive.current)setExpectedRevision(a.revision);await onChanged()}}/>},
        ]}/>
      </section>
      <div id={`${id}-preview-pane`} className={'agent-preview-pane'+(pane!=='preview'?' agent-pane--inactive':'')}><AgentPreview owner={owner} agent={agent} selected={selected} hasEdits={hasEdits} onExecution={setProof}/></div>
    </div>
    {pendingSelection&&<Dialog title={t('Discard unsaved configuration changes?')} description={t('The selected saved configuration is unchanged. Discarding restores saved settings.')} closeLabel={t('Keep editing')} onRequestClose={()=>setPendingSelection(null)} footer={<><Button onClick={()=>setPendingSelection(null)}>{t('Keep editing')}</Button><Button variant="danger" onClick={()=>{select(pendingSelection.config);setPendingSelection(null)}}>{t('Discard and restore')}</Button></>}/>}
  </div>
}
