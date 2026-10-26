import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import PreviewResult from './PreviewResult.jsx'
import { agentPath, executionPath, platformCommand } from '../agentPlatform/platformApi.js'

const active = execution => execution && ['ACCEPTED','RUNNING'].includes(execution.status)
// Only opaque selection is stored locally. Transcripts and pinned settings come from the server.
export default function AgentPreview({ owner, agent, selected, hasEdits, onExecution }) {
  const { t } = useI18n(), base = `${agentPath(agent.id)}/preview-conversations`, selectionKey = `careersim_preview_chat_${owner}_${agent.id}`
  const [chatId,setChatId]=useState(()=>{try{return sessionStorage.getItem(selectionKey)}catch{return null}})
  const [list,setList]=useState({items:[],nextOffset:null}),[offset,setOffset]=useState(0),[listError,setListError]=useState('')
  const [acceptedRun,setAcceptedRun]=useState(null)
  const [transcript,setTranscript]=useState(null),[loading,setLoading]=useState(false),[error,setError]=useState('')
  const [message,setMessage]=useState(''),[busy,setBusy]=useState(false),[sending,setSending]=useState(false),[confirmation,setConfirmation]=useState(null),[expanded,setExpanded]=useState(false)
  const listSequence=useRef(0)
  const root=useRef(null), toggle=useRef(null), messages=useRef(null), composing=useRef(false), alive=useRef(true), sequence=useRef(0), mutation=useRef(false)
  const pendingMessage=useRef(null), pendingChat=useRef(null), currentChat=useRef(chatId), previousFocus=useRef(null)
  currentChat.current=chatId
  const refreshList=useCallback(async()=>{const token=++listSequence.current;try{const result=await apiRequest(`${base}?limit=20&offset=${offset}`);if(alive.current&&token===listSequence.current){setList(result);setListError('')}}catch(f){if(alive.current&&token===listSequence.current)setListError(f.message)}},[base,offset])
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
  },[expanded])
  async function run(work){if(mutation.current)return;mutation.current=true;setBusy(true);setError('');sequence.current++;try{await work()}catch(f){if(alive.current)setError(f.message)}finally{mutation.current=false;if(alive.current){setBusy(false);setSending(false)}}}
  function select(id){if(mutation.current)return;sequence.current++;pendingMessage.current=null;setAcceptedRun(null);setTranscript(null);setChatId(id||null);setError('');setLoading(Boolean(id))}
  async function newChat(){if(!selected)return;await run(async()=>{
    if(!pendingChat.current||pendingChat.current.configurationId!==selected.id)pendingChat.current={requestId:crypto.randomUUID(),configurationId:selected.id}
    const created=await platformCommand(base,pendingChat.current);if(!alive.current)return
    pendingChat.current=null;pendingMessage.current=null;setAcceptedRun(null);setMessage('');setOffset(0);setChatId(created.id);await refreshList()
  })}
  async function send(event){event.preventDefault();if(!chatId||!transcript?.canSend||running||!message.trim()||mutation.current)return
    const target=chatId, query=message
    await run(async()=>{setSending(true)
      if(!pendingMessage.current||pendingMessage.current.query!==query||pendingMessage.current.chatId!==target)pendingMessage.current={chatId:target,query,requestId:crypto.randomUUID()}
      const accepted=await platformCommand(`${base}/${target}/messages`,{query,requestId:pendingMessage.current.requestId})
      if(!alive.current||currentChat.current!==target)return
      pendingMessage.current=null;setAcceptedRun({chatId:target,execution:accepted});setMessage('');setTranscript(current=>current&&({...current,turns:[...current.turns.filter(turn=>turn.execution?.id!==accepted.id),{sequence:current.turns.at(-1)?.sequence+1||1,execution:accepted}]}));onExecution?.(accepted)
      // Acceptance enables Stop immediately; a slow transcript GET must not hold it disabled.
      load();refreshList()
    })
  }
  async function lifecycle(){const operation=confirmation,chat=transcript?.conversation;if(!chat)return
    await run(async()=>{await platformCommand(`${base}/${chat.id}${operation==='clear'?'/clear':''}`,{confirmed:true},chat.revision,operation==='clear'?'POST':'DELETE');if(!alive.current)return
      pendingMessage.current=null;setAcceptedRun(null);setMessage('');setConfirmation(null);setTranscript(null)
      if(operation==='delete'){setChatId(null);sequence.current++}else await load()
      await refreshList()
    })
  }
  async function stop(){const target=running?.id;if(!target)return;await run(async()=>{await platformCommand(executionPath(target)+'/cancel',{});await load()})}
  function escape(event){if(event.defaultPrevented||!expanded)return;if(event.key==='Escape'){event.preventDefault();setExpanded(false)}}
  const version=transcript?.configurationVersion
  return <dialog ref={root} role={expanded?'dialog':'region'} aria-modal={expanded||undefined} aria-label={t('Admin Preview chat')} onCancel={event=>{event.preventDefault();setExpanded(false)}} onKeyDown={escape} className={'cs-surface agent-chat'+(expanded?' agent-chat--expanded':'')}>
    <header className="agent-chat-header"><p className="agent-eyebrow">{t('PREVIEW · SAVED CONFIGURATION')}</p><div className="cs-section-header"><h2>{t('Chat preview')}</h2><div className="cs-actions"><Button ref={toggle} variant="quiet" onClick={()=>setExpanded(value=>!value)}>{t(expanded?'Restore preview':'Expand preview')}</Button><Button disabled={busy||!selected||agent.status==='ARCHIVED'} onClick={newChat}>{t('New chat')}</Button></div></div>
      <p className="cs-caption">{t('Saved configuration')} {selected?`v${selected.versionNumber}`:'—'}{version?` · ${t('Chat pinned to')} v${version}`:''}</p>
      <FormField label={t('Resume conversation')}><select disabled={busy} value={chatId ?? ''} onChange={e=>select(e.target.value)}><option value="">{t('Select a conversation')}</option>{chatId&&!list.items.some(c=>c.id===chatId)&&<option value={chatId}>{t('Selected conversation')}</option>}{list.items.map((c,index)=><option key={c.id} value={c.id}>{t('Chat')} {offset+index+1} · {new Date(c.updatedAt).toLocaleString()}</option>)}</select></FormField>
      {(offset>0||list.nextOffset!=null)&&<div className="cs-actions"><Button disabled={busy||offset===0} onClick={()=>setOffset(Math.max(0,offset-20))}>{t('Previous')}</Button><Button disabled={busy||list.nextOffset==null} onClick={()=>setOffset(list.nextOffset)}>{t('Next')}</Button></div>}
      <div className="cs-actions"><Button disabled={busy||!transcript} onClick={()=>setConfirmation('clear')}>{t('Clear chat')}</Button><Button variant="quiet" disabled={busy||!transcript} onClick={()=>setConfirmation('delete')}>{t('Delete chat')}</Button></div>
      {hasEdits&&<p className="cs-caption agent-draft-warning">{t('Unsaved changes are not used in Preview. Save the configuration to test the new settings.')}</p>}
    </header>
    <div ref={messages} className="agent-chat-messages" role="log" aria-label={t('Preview messages')} aria-live="polite" aria-relevant="additions text">
      {loading&&!transcript&&<FeedbackPanel kind="loading">{t('Loading conversation...')}</FeedbackPanel>}
      {!chatId&&!error&&<div className="agent-chat-empty"><h3>{t('Try your saved Agent')}</h3><p>{t('Start a new chat or resume a saved conversation.')}</p></div>}
      {transcript&&<><article className="agent-message agent-message--agent agent-configured-greeting" aria-label={t('Configured greeting')}><small>{t('Configured greeting')}</small><p className="cs-prose">{transcript.greeting}</p></article>
      {transcript.nextBefore!=null&&<Button disabled={busy||loading||Boolean(running)} onClick={()=>load(transcript.nextBefore)}>{t('Older turns')}</Button>}
      {transcript.turns.map(turn=>turn.sourceUnavailable?<FeedbackPanel key={turn.sequence} kind="warning">{t('Source unavailable')}</FeedbackPanel>:<div key={turn.execution.id} className="agent-chat-turn" data-execution-id={turn.execution.id}><article className="agent-message agent-message--user" aria-label={t('Admin message')}><strong>{t('You')}</strong><p className="cs-prose">{turn.execution.inputSnapshot?.query}</p></article><article className="agent-message agent-message--agent" aria-label={t('Agent response')}><strong>{transcript.agentName}</strong><PreviewResult id={turn.execution.id} execution={turn.execution} reload={()=>load()} showStop={false} showExcerpts={transcript.chat?.showSourceExcerpts!==false}/></article></div>)}
      {!transcript.canSend&&<FeedbackPanel kind="warning">{t(transcript.unavailableReason)}</FeedbackPanel>}</>}
    </div>
    {(error||listError)&&<FeedbackPanel kind="error" action={<Button disabled={busy} onClick={()=>{load();refreshList()}}>{t('Retry')}</Button>}>{t(error||listError)}</FeedbackPanel>}
    <form onSubmit={send} className="agent-chat-composer"><FormField label={t('Message')} required><textarea rows={3} maxLength={2000} disabled={busy||Boolean(running)||!transcript?.canSend} value={message} onChange={e=>setMessage(e.target.value)} onCompositionStart={()=>{composing.current=true}} onCompositionEnd={()=>{composing.current=false}} onKeyDown={event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.nativeEvent.isComposing&&!composing.current&&event.keyCode!==229){event.preventDefault();event.currentTarget.form?.requestSubmit()}}}/></FormField><div className="cs-actions"><Button type="submit" variant="primary" busy={sending} disabled={busy||!message.trim()||!transcript?.canSend||Boolean(running)}>{t(pendingMessage.current?'Retry send':'Send')}</Button>{running&&<Button disabled={busy||running.cancellationRequested} onClick={stop}>{t(running.cancellationRequested?'Stopping...':'Stop')}</Button>}</div></form>
    {confirmation&&<Dialog title={t(confirmation==='clear'?'Clear conversation?':'Delete conversation?')} closeLabel={t('Close dialog')} busy={busy} onRequestClose={()=>setConfirmation(null)} footer={<><Button disabled={busy} onClick={()=>setConfirmation(null)}>{t('Cancel')}</Button><Button variant="danger" busy={busy} onClick={lifecycle}>{t(confirmation==='clear'?'Confirm clear':'Confirm delete')}</Button></>}><p>{t('This hides the transcript and stops active work. Immutable execution audit and usage records are retained.')}</p>{error&&<FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}</Dialog>}
  </dialog>
}
