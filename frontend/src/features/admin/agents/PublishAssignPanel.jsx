import { useEffect, useRef, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import { agentPath, platformCommand } from '../agentPlatform/platformApi.js'

const roleLabel={STUDENT:'Students',ENTERPRISE:'Enterprise representatives'}
export default function PublishPanel({active,agent,selected,published,dirty,execution,onChanged}){
 const {t}=useI18n(),root=agentPath(agent.id),alive=useRef(true),pending=useRef(false)
 const [refreshKey,setRefreshKey]=useState(0),[roleDraft,setRoleDraft]=useState(null),[confirmation,setConfirmation]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState('')
 useEffect(()=>{alive.current=true;return()=>{alive.current=false}},[])
 const readiness=useAdminRead(`${root}/configurations/${selected?.id}/publication-readiness`,active&&Boolean(selected),`${agent.revision}:${selected?.contentHash}:${refreshKey}:${execution?.id}:${execution?.status}`)
 const roleState=useAdminRead('/v2/admin/agent-role-bindings',active,`${agent.revision}:${refreshKey}`)
 const publishedReady=useAdminRead(`${root}/configurations/${agent.publishedConfigurationId}/publication-readiness`,active&&Boolean(agent.publishedConfigurationId),`${agent.revision}:${refreshKey}:${execution?.id}:${execution?.status}`)
 const ready=readiness.value,bindings=roleState.value
 const canPublish=Boolean(!dirty&&selected&&ready?.publishable&&ready.configurationId===selected.id&&ready.configurationHash===selected.contentHash&&ready.agentRevision===agent.revision)
 const roles=roleDraft??bindings?.filter(b=>b.agentId===agent.id).map(b=>b.role)??[]
 const roleChanged=Boolean(bindings?.some(b=>(b.agentId===agent.id)!==roles.includes(b.role)))
 const canActivate=Boolean(bindings&&publishedReady.value?.publishable&&agent.status==='ACTIVE'&&!dirty&&roleChanged)
 const refresh=()=>setRefreshKey(v=>v+1)
 async function confirm(){
  if(pending.current||!confirmation)return
  const operation=confirmation
  if(operation.agentRevision!==agent.revision||operation.kind==='publish'&&(!canPublish||operation.config!==selected.id||operation.hash!==selected.contentHash)||operation.kind==='activate'&&(!canActivate||JSON.stringify(bindings)!==operation.bindingSnapshot)){setConfirmation(null);setError('Publication or role state changed. Refresh before continuing.');return}
  pending.current=true;setBusy(true);setError('');setNotice('')
  try{
   if(operation.kind==='publish')await platformCommand(`${root}/configurations/${operation.config}/publish`,{confirmed:true},operation.agentRevision)
   else await platformCommand(`${root}/roles`,{roles:operation.roles,expectedRoleRevisions:Object.fromEntries(bindings.map(b=>[b.role,b.revision])),confirmed:true},operation.agentRevision,'PUT')
   if(!alive.current)return
   setConfirmation(null);setRoleDraft(null);setNotice(operation.kind==='publish'?'Version published. Role activation is a separate action.':'Role availability updated atomically.');refresh();await onChanged()
  }catch(f){if(alive.current){setConfirmation(null);setError(f.message);refresh()}}
  finally{pending.current=false;if(alive.current)setBusy(false)}
 }
 return <div className="cs-stack agent-publication-panel">
  <section className="cs-surface cs-stack" aria-label={t('Publication')}><div className="cs-section-header"><h3>{t('Publication')}</h3><Button disabled={busy} onClick={refresh}>{t('Refresh status')}</Button></div>
   <dl className="cs-metadata"><div><dt>{t('Selected saved version')}</dt><dd>{selected?`v${selected.versionNumber}`:t('Not saved')}</dd></div><div><dt>{t('Published version')}</dt><dd>{published?`v${published.versionNumber}`:agent.publishedConfigurationId?t('Published version unavailable'):t('Not published')}</dd></div></dl>
   {dirty&&<FeedbackPanel kind="warning">{t('Unsaved changes must be saved and previewed before publication.')}</FeedbackPanel>}
   {selected&&!ready&&!readiness.error&&<FeedbackPanel kind="loading">{t('Checking publication readiness...')}</FeedbackPanel>}
   {readiness.error&&<FeedbackPanel kind="error">{t(readiness.error)}</FeedbackPanel>}
   {ready&&<p role="status">{t(canPublish?'Ready to publish':'Publication needs attention')}</p>}
   {ready&&!ready.previewSucceeded&&<FeedbackPanel kind="warning">{t('Successful Preview of this exact saved configuration')}</FeedbackPanel>}
   {ready&&!ready.knowledgeAvailable&&<FeedbackPanel kind="warning">{t('Knowledge grants available')} · {t('Blocked')}</FeedbackPanel>}
   {agent.status==='ARCHIVED'&&<FeedbackPanel kind="warning">{t('Agent is archived.')}</FeedbackPanel>}
   <Button variant="primary" disabled={busy||!canPublish} onClick={()=>setConfirmation({kind:'publish',config:selected.id,hash:selected.contentHash,agentRevision:agent.revision,version:selected.versionNumber})}>{t('Publish version')}{selected?` ${selected.versionNumber}`:''}</Button>
   <details className="cs-disclosure"><summary>{t('Publication details')}</summary><dl className="cs-metadata"><div><dt>{t('Exact configuration hash')}</dt><dd><code>{selected?.contentHash}</code></dd></div><div><dt>{t('Revision')}</dt><dd>{agent.revision}</dd></div></dl></details>
  </section>
  <section className="cs-surface cs-stack" aria-label={t('Available to roles')}><h3>{t('Available to roles')}</h3><p className="cs-caption">{t('Role activation uses the published version. It does not grant private-data permissions.')}</p>
   {!agent.publishedConfigurationId&&<FeedbackPanel kind="info">{t('Publish this Agent before role activation.')}</FeedbackPanel>}
   {!bindings&&!roleState.error&&<FeedbackPanel kind="loading">{t('Loading role availability...')}</FeedbackPanel>}
   {roleState.error&&<FeedbackPanel kind="error" action={<Button onClick={refresh}>{t('Retry')}</Button>}>{t(roleState.error)}</FeedbackPanel>}
   {bindings?.map(binding=><div key={binding.role}><label className="agent-builder-choice"><input type="checkbox" checked={roles.includes(binding.role)} disabled={busy||agent.status!=='ACTIVE'||!agent.publishedConfigurationId} onChange={e=>setRoleDraft(e.target.checked?[...roles,binding.role]:roles.filter(r=>r!==binding.role))}/><span>{t(roleLabel[binding.role])}</span></label><p className="cs-caption">{t(roleLabel[binding.role])} · {t('Currently use')}: {binding.agentName??t('No active Agent')}</p></div>)}
   <Button variant="primary" disabled={busy||!canActivate} onClick={()=>setConfirmation({kind:'activate',roles:[...roles],bindingSnapshot:JSON.stringify(bindings),agentRevision:agent.revision})}>{t('Activate')}</Button>
  </section>
  {error&&<FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}{notice&&<FeedbackPanel kind="success">{t(notice)}</FeedbackPanel>}
  {confirmation&&<Dialog title={t(confirmation.kind==='publish'?'Confirm publication':'Confirm role activation')} closeLabel={t('Close dialog')} busy={busy} onRequestClose={()=>setConfirmation(null)} footer={<><Button disabled={busy} onClick={()=>setConfirmation(null)}>{t('Cancel')}</Button><Button variant="primary" busy={busy} onClick={confirm}>{t(confirmation.kind==='publish'?'Confirm publish':'Activate')}</Button></>}>
   {confirmation.kind==='publish'?<p>{agent.name} · v{confirmation.version}. {t('Publish this exact saved version? Role availability is unchanged.')}</p>:<div className="cs-stack">{bindings?.filter(b=>confirmation.roles.includes(b.role)||b.agentId===agent.id).map(b=><p key={b.role}>{t(roleLabel[b.role])} · {t('Currently use')}: <strong>{b.agentName??t('No active Agent')}</strong>. {confirmation.roles.includes(b.role)?<>{t('Activate instead')}: <strong>{agent.name}</strong>?</>:t('Remove this Agent from the role?')}</p>)}</div>}
  </Dialog>}
 </div>
}

// Key every read to its Agent/configuration/revision. Obsolete responses never qualify readiness.
function useAdminRead(path, enabled, revision) {
  const [snapshot, setSnapshot] = useState(null)
  const key = `${path}:${revision}`
  useEffect(() => {
    if (!enabled) return undefined
    let current = true
    apiRequest(path).then(value => { if (current) setSnapshot({ key, value, error: '' }) })
      .catch(failure => { if (current) setSnapshot({ key, value: null, error: failure.message }) })
    return () => { current = false }
  }, [path, enabled, key])
  return snapshot?.key === key ? snapshot : { value: null, error: '' }
}
