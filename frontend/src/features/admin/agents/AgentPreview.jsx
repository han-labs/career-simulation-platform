import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { ArrowDown, ArrowUp, History, Maximize2, Minimize2, Plus, Square, Trash2, X, Eraser } from 'lucide-react'
import useChatViewport from '../../agents/useChatViewport.js'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import PreviewResult from './PreviewResult.jsx'
import AgentQuickActions from '../../agents/AgentQuickActions.jsx'
import { agentPath, executionPath, platformCommand } from '../agentPlatform/platformApi.js'

const active = execution => execution && ['ACCEPTED','RUNNING'].includes(execution.status)
// Selection and unsent account/chat-scoped drafts are local; authoritative transcripts stay on the server.
export default function AgentPreview({ owner, agent, selected, hasEdits, onExecution }) {
  const { t } = useI18n(), base = `${agentPath(agent.id)}/preview-conversations`, selectionKey = `careersim_preview_chat_${owner}_${agent.id}`
  const [chatId,setChatId]=useState(()=>{try{return sessionStorage.getItem(selectionKey)}catch{return null}})
  const [list,setList]=useState({items:[],nextOffset:null}),[offset,setOffset]=useState(0),[listError,setListError]=useState('')
  const [acceptedRun,setAcceptedRun]=useState(null)
  const [transcript,setTranscript]=useState(null),[loading,setLoading]=useState(false),[error,setError]=useState('')
  const [message,setMessage]=useState(()=>readDraft(draftKey(owner,'admin-preview-message',`${agent.id}:${chatId}`))?.text ?? ''),[busy,setBusy]=useState(false),[sending,setSending]=useState(false),[confirmation,setConfirmation]=useState(null),[expanded,setExpanded]=useState(false)
  const [historyOpen,setHistoryOpen]=useState(false),[sent,setSent]=useState(null)
  const initialResume=useRef(false),listSequence=useRef(0),historyDrawer=useRef(null),historyTrigger=useRef(null),composer=useRef(null),draftRevision=useRef(0)
  const {viewport:messages,tail,following,markSent,observeScroll,jump}=useChatViewport({ready:Boolean(transcript||sent),scope:chatId,content:transcript,pending:sent,layout:expanded})
  const root=useRef(null), toggle=useRef(null), composing=useRef(false), alive=useRef(true), sequence=useRef(0), mutation=useRef(false)
  const pendingMessage=useRef(null), pendingChat=useRef(null), currentChat=useRef(chatId), previousFocus=useRef(null)
  currentChat.current=chatId
  const refreshList=useCallback(async()=>{const token=++listSequence.current;try{const result=await apiRequest(`${base}?limit=20&offset=${offset}`);if(alive.current&&token===listSequence.current){setList(result);setListError('');if(!initialResume.current){initialResume.current=true;if(!currentChat.current&&result.items[0]){const latest=result.items[0].id;setChatId(latest);setMessage(readDraft(draftKey(owner,'admin-preview-message',`${agent.id}:${latest}`))?.text ?? '')}}}}catch(f){if(alive.current&&token===listSequence.current)setListError(f.message)}},[base,offset,owner,agent.id])
  useEffect(()=>{alive.current=true;const fence=sequence;return()=>{alive.current=false;fence.current++}},[])
  useEffect(()=>{Promise.resolve().then(refreshList)},[refreshList])
  useEffect(()=>{try{if(chatId)sessionStorage.setItem(selectionKey,chatId);else sessionStorage.removeItem(selectionKey)}catch{/* Server history remains available. */}},[chatId,selectionKey])
  const load=useCallback(async(before)=>{
    if(!chatId)return
    const token=++sequence.current;setLoading(true)
    try{const data=await apiRequest(`${base}/${chatId}?limit=50${before?`&before=${before}`:''}`);if(alive.current&&token===sequence.current&&currentChat.current===chatId){setTranscript(data);setError('');setAcceptedRun(current=>data.turns?.some(turn=>turn.execution?.id===current?.execution?.id)?null:current);const latest=data.turns?.at(-1)?.execution;if(latest)onExecution?.(latest)}}
    catch(f){if(alive.current&&token===sequence.current){setTranscript(null);setError(f.message)}}finally{if(alive.current&&token===sequence.current)setLoading(false)}
  },[base,chatId,onExecution])
  useEffect(()=>{if(chatId)Promise.resolve().then(()=>load())},[chatId,load])
  const running=transcript?.turns?.map(turn=>turn.execution).find(active) ?? (acceptedRun?.chatId===chatId&&active(acceptedRun.execution)?acceptedRun.execution:null)
  useEffect(()=>{if(!running)return;const timer=setTimeout(()=>load(),800);return()=>clearTimeout(timer)},[running,load])
  useEffect(()=>{const revalidate=()=>{if(chatId)load()};window.addEventListener('focus',revalidate);return()=>window.removeEventListener('focus',revalidate)},[chatId,load])
  useLayoutEffect(()=>{
    const dialog=root.current, scroll=messages.current?.scrollTop ?? 0
    if(expanded){previousFocus.current=document.activeElement;if(dialog.open)dialog.close?.();if(dialog.showModal)dialog.showModal();else dialog.setAttribute('open','');toggle.current?.focus()}
    else{if(dialog.open)dialog.close?.();dialog.setAttribute('open','');if(previousFocus.current?.isConnected){previousFocus.current.focus();previousFocus.current=null}}
    if(messages.current)messages.current.scrollTop=scroll
  },[expanded,messages])
  async function run(work){if(mutation.current)return;mutation.current=true;setBusy(true);setError('');sequence.current++;try{await work()}catch(f){if(alive.current)setError(f.message)}finally{mutation.current=false;if(alive.current){setBusy(false);setSending(false)}}}
  function select(id){if(mutation.current)return;sequence.current++;pendingMessage.current=null;setAcceptedRun(null);setSent(null);setTranscript(null);setMessage(readDraft(draftKey(owner,'admin-preview-message',`${agent.id}:${id}`))?.text ?? '');setChatId(id||null);setError('');setLoading(Boolean(id));setHistoryOpen(false)}
  async function newChat(){if(!selected)return;await run(async()=>{
    if(!pendingChat.current||pendingChat.current.configurationId!==selected.id)pendingChat.current={requestId:crypto.randomUUID(),configurationId:selected.id}
    const created=await platformCommand(base,pendingChat.current);if(!alive.current)return
    pendingChat.current=null;pendingMessage.current=null;setAcceptedRun(null);setMessage('');setSent(null);setTranscript(null);setOffset(0);setChatId(created.id);setHistoryOpen(false);if(currentChat.current===created.id)await load();await refreshList()
  })}
  async function send(event){event.preventDefault();if(!chatId||!transcript?.canSend||running||!message.trim()||mutation.current)return
    const target=chatId, query=message, submittedRevision=draftRevision.current
    await run(async()=>{setSending(true)
      if(!pendingMessage.current||pendingMessage.current.query!==query||pendingMessage.current.chatId!==target)pendingMessage.current={chatId:target,query,requestId:crypto.randomUUID()}
      const requestId=pendingMessage.current.requestId;setSent({key:requestId,query,executionId:null});markSent(requestId)
      let accepted
      try{accepted=await platformCommand(`${base}/${target}/messages`,{query,requestId})}catch(f){if(alive.current)setSent(null);throw f}
      const cleared=draftRevision.current===submittedRevision
      if(cleared)writeDraft(draftKey(owner,'admin-preview-message',`${agent.id}:${target}`),{text:''})
      if(!alive.current||currentChat.current!==target)return
      pendingMessage.current=null;setAcceptedRun({chatId:target,execution:accepted});setSent({key:requestId,query,executionId:accepted.id});if(cleared)setMessage('');setTranscript(current=>current&&({...current,turns:[...current.turns.filter(turn=>turn.execution?.id!==accepted.id),{sequence:current.turns.at(-1)?.sequence+1||1,execution:accepted}]}));onExecution?.(accepted)
      // Acceptance enables Stop immediately; a slow transcript GET must not hold it disabled.
      load();refreshList()
    })
  }
  async function lifecycle(){const operation=confirmation,chat=transcript?.conversation;if(!chat)return
    await run(async()=>{await platformCommand(`${base}/${chat.id}${operation==='clear'?'/clear':''}`,{confirmed:true},chat.revision,operation==='clear'?'POST':'DELETE');if(!alive.current)return
      pendingMessage.current=null;setAcceptedRun(null);setSent(null);setMessage('');writeDraft(draftKey(owner,'admin-preview-message',`${agent.id}:${chat.id}`),{text:''});setConfirmation(null);setHistoryOpen(false);setTranscript(null)
      if(operation==='delete'){setChatId(null);sequence.current++}else await load()
      await refreshList()
    })
  }
  async function stop(){const target=running?.id;if(!target)return;await run(async()=>{await platformCommand(executionPath(target)+'/cancel',{});await load()})}
  function edit(value){draftRevision.current++;setMessage(value);if(chatId)writeDraft(draftKey(owner,'admin-preview-message',`${agent.id}:${chatId}`),{text:value})}
  useLayoutEffect(()=>{const node=composer.current;if(node){node.style.height='0px';node.style.height=`${Math.min(104,Math.max(40,node.scrollHeight))}px`}},[message,expanded])
  useEffect(()=>{if(!historyOpen)return;const before=document.activeElement,trigger=historyTrigger.current;historyDrawer.current?.querySelector('button')?.focus();return()=>{if(trigger?.isConnected)trigger.focus();else if(before?.isConnected)before.focus()}},[historyOpen])
  function historyKeys(event){if(event.key==='Escape'){event.preventDefault();event.stopPropagation();setHistoryOpen(false);return}if(event.key!=='Tab')return;const controls=[...historyDrawer.current.querySelectorAll('button:not([disabled])')],first=controls[0],last=controls.at(-1);if(event.shiftKey&&document.activeElement===first){event.preventDefault();last?.focus()}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first?.focus()}}
  function escape(event){if(event.defaultPrevented)return;if(historyOpen&&event.key==='Escape'){event.preventDefault();setHistoryOpen(false);return}if(!expanded)return;if(event.key==='Escape'){event.preventDefault();setExpanded(false)}}
  const version=transcript?.configurationVersion
  return <dialog ref={root} role={expanded?'dialog':'region'} aria-modal={expanded||undefined} aria-label={t('Admin Preview chat')} onCancel={event=>{event.preventDefault();setExpanded(false)}} onKeyDown={escape} className={'cs-surface agent-chat'+(expanded?' agent-chat--expanded':'')}>
    <header className="agent-chat-header"><p className="agent-eyebrow">{t('PREVIEW · SAVED CONFIGURATION')}</p><div className="agent-chat-title-row"><h2 title={transcript?.agentName ?? agent.name}>{transcript?.agentName ?? agent.name}</h2><div className="agent-chat-header-actions">
      <Button ref={historyTrigger} className="agent-chat-icon" aria-label={t('History')} title={t('History')} aria-expanded={historyOpen} disabled={busy} icon={<History size={18}/>} onClick={()=>{setHistoryOpen(true);refreshList()}}/>
      <Button className="agent-chat-icon" aria-label={t('New chat')} title={t('New chat')} disabled={busy||!selected||agent.status==='ARCHIVED'} icon={<Plus size={19}/>} onClick={newChat}/>
      <Button ref={toggle} className="agent-chat-icon" aria-label={t(expanded?'Restore preview':'Expand preview')} title={t(expanded?'Restore preview':'Expand preview')} icon={expanded?<Minimize2 size={17}/>:<Maximize2 size={17}/>} onClick={()=>setExpanded(value=>!value)}/>
      {expanded&&<Button className="agent-chat-icon" aria-label={t('Close preview')} title={t('Close preview')} icon={<X size={19}/>} onClick={()=>setExpanded(false)}/>}
    </div></div>
      <p className="cs-caption">{t('Saved configuration')} {selected?`v${selected.versionNumber}`:'—'}{version?` · ${t('Chat pinned to')} v${version}`:''}</p>
      {hasEdits&&<p className="cs-caption agent-draft-warning">{t('Unsaved changes are not used in Preview. Save the configuration to test the new settings.')}</p>}
      {transcript?.conversation?.configurationId&&selected&&transcript.conversation.configurationId!==selected.id&&<p className="cs-caption agent-draft-warning" role="status">{t('This chat uses a different saved version. Start a new chat to test the selected configuration and limits.')}</p>}
    </header>
    <div className="agent-chat-viewport">
    <div ref={messages} className="agent-chat-messages" role="log" aria-label={t('Preview messages')} aria-live="polite" aria-relevant="additions text" onScroll={observeScroll}>
      {loading&&!transcript&&<FeedbackPanel kind="loading">{t('Loading conversation...')}</FeedbackPanel>}
      {!chatId&&!error&&<div className="agent-chat-empty"><h3>{t('Try your saved Agent')}</h3><p>{t('Start a new chat or resume a saved conversation.')}</p></div>}
      {transcript&&<><article className="agent-message agent-message--agent agent-configured-greeting" aria-label={t('Configured greeting')}><strong>{transcript.agentName}</strong><p className="cs-prose">{transcript.greeting}</p></article>
      {transcript.nextBefore!=null&&<Button disabled={busy||loading||Boolean(running)} onClick={()=>load(transcript.nextBefore)}>{t('Older turns')}</Button>}
      {transcript.turns.map(turn=>turn.sourceUnavailable?<FeedbackPanel key={turn.sequence} kind="warning">{t('Source unavailable')}</FeedbackPanel>:<div key={turn.execution.id} className="agent-chat-turn" data-execution-id={turn.execution.id}><article data-chat-anchor={sent?.executionId===turn.execution.id?sent?.key:undefined} className="agent-message agent-message--user" aria-label={t('Admin message')}><strong>{t('You')}</strong><p className="cs-prose">{turn.execution.inputSnapshot?.query}</p></article><article className="agent-message agent-message--agent" aria-label={t('Agent response')}><strong>{transcript.agentName}</strong><PreviewResult compactSources id={turn.execution.id} execution={turn.execution} reload={()=>load()} showStop={false} showExcerpts={transcript.chat?.showSourceExcerpts!==false}/></article></div>)}
      {!transcript.canSend&&<FeedbackPanel kind="warning">{t(transcript.unavailableReason)}</FeedbackPanel>}</>}
      {sent&&!transcript?.turns?.some(turn=>turn.execution?.id===sent.executionId)&&<article data-chat-anchor={sent.key} className="agent-message agent-message--user" aria-label={t('Admin message')}><strong>{t('You')}</strong><p className="cs-prose">{sent.query}</p>{!sent.executionId&&<small role="status">{t('Sending...')}</small>}</article>}
      <div ref={tail} className="chat-viewport-tail" aria-hidden="true"/>
    </div>
    {!following&&<Button className="agent-chat-icon agent-chat-jump" aria-label={t('Jump to latest')} title={t('Jump to latest')} icon={<ArrowDown size={17}/>} onClick={jump}/>}
    </div>
    {(error||listError)&&<FeedbackPanel kind="error" action={<Button disabled={busy} onClick={()=>{load();refreshList()}}>{t('Retry')}</Button>}>{t(error||listError)}</FeedbackPanel>}
    <div className="agent-chat-bottom"><AgentQuickActions compact kind="admin" disabled={busy||Boolean(running)} onChoose={value=>{edit(value);composer.current?.focus()}}/>
    <form onSubmit={send} className="agent-chat-composer"><FormField label={t('Message')} required><textarea ref={composer} placeholder={t('Message to Agent')} rows={1} maxLength={2000} disabled={busy||Boolean(running)||!transcript?.canSend} value={message} onChange={e=>edit(e.target.value)} onCompositionStart={()=>{composing.current=true}} onCompositionEnd={()=>{composing.current=false}} onKeyDown={event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.nativeEvent.isComposing&&!composing.current&&event.keyCode!==229){event.preventDefault();event.currentTarget.form?.requestSubmit()}}}/></FormField><div className="cs-actions"><Button className="agent-chat-icon agent-chat-send" type="submit" variant="primary" aria-label={t(pendingMessage.current?'Retry send':'Send')} title={t(pendingMessage.current?'Retry send':'Send')} icon={<ArrowUp size={19}/>} busy={sending} disabled={busy||!message.trim()||!transcript?.canSend||Boolean(running)}/>{running&&<Button className="agent-chat-icon" aria-label={t(running.cancellationRequested?'Stopping...':'Stop')} title={t(running.cancellationRequested?'Stopping...':'Stop')} icon={<Square size={13}/>} disabled={busy||running.cancellationRequested} onClick={stop}/>}</div></form></div>
    {historyOpen&&<div className="agent-chat-history-overlay"><section ref={historyDrawer} className="agent-chat-history-drawer" role="dialog" aria-modal="true" aria-label={t('Preview conversation history')} onKeyDown={historyKeys}>
      <header><h3>{t('History')}</h3><Button className="agent-chat-icon" aria-label={t('Close history')} title={t('Close history')} icon={<X size={18}/>} onClick={()=>setHistoryOpen(false)}/></header>
      {!list.items.length&&<p>{t('No conversations yet.')}</p>}
      <ul>{list.items.map((c,index)=><li key={c.id}><Button className="agent-chat-resume" aria-label={`${t('Resume')} · ${t('Chat')} ${offset+index+1} · ${new Date(c.updatedAt).toLocaleString()}`} aria-pressed={chatId===c.id} disabled={busy} onClick={()=>select(c.id)}><span>{t('Chat')} {offset+index+1} · {new Date(c.updatedAt).toLocaleString()}</span></Button></li>)}</ul>
      {(offset>0||list.nextOffset!=null)&&<div className="cs-actions"><Button disabled={busy||offset===0} onClick={()=>setOffset(Math.max(0,offset-20))}>{t('Previous')}</Button><Button disabled={busy||list.nextOffset==null} onClick={()=>setOffset(list.nextOffset)}>{t('Next')}</Button></div>}
      <footer><Button className="agent-chat-history-action" icon={<Eraser size={15}/>} disabled={busy||!transcript} onClick={()=>setConfirmation('clear')}>{t('Clear chat')}</Button><Button className="agent-chat-history-action" icon={<Trash2 size={15}/>} disabled={busy||!transcript} onClick={()=>setConfirmation('delete')}>{t('Delete chat')}</Button></footer>
    </section></div>}
    {confirmation&&<Dialog title={t(confirmation==='clear'?'Clear conversation?':'Delete conversation?')} closeLabel={t('Close dialog')} busy={busy} onRequestClose={()=>setConfirmation(null)} footer={<><Button disabled={busy} onClick={()=>setConfirmation(null)}>{t('Cancel')}</Button><Button variant="danger" busy={busy} onClick={lifecycle}>{t(confirmation==='clear'?'Confirm clear':'Confirm delete')}</Button></>}><p>{t('This hides the transcript and stops active work. Immutable execution audit and usage records are retained.')}</p>{error&&<FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}</Dialog>}
  </dialog>
}
