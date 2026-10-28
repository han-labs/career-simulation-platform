// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { useState } from 'react'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../shared/api/httpClient.js'
import { streamExecution } from './studentAgentApi.js'
import StudentAgentChat from './StudentAgentChat.jsx'
import { readFileSync } from 'node:fs'
import { createTranslator } from '../../shared/i18n/translate.js'
import AgentUsagePage from '../admin/agentPlatform/AgentUsagePage.jsx'
import { usageFixture } from '../admin/usage/usageFixture.js'
vi.mock('../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn(), getAccountGeneration: () => 0, invalidateAccount: vi.fn() }))
vi.mock('./studentAgentApi.js', () => ({ sessionPath: id => `/v2/agent-sessions/${id}`, runPath: id => `/v2/agent-executions/${id}`, streamExecution: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() }); afterEach(cleanup)
function Chat({visible=true}) { const [id, setId] = useState('s1'); return <StudentAgentChat owner="7" visible={visible} agent={{id:'a1', name:'Career Explorer'}} id={id} onSessionChange={setId} /> }
function page() { return render(<I18nProvider initialLocale="en"><MemoryRouter><Chat /></MemoryRouter></I18nProvider>) }
function fixture() {
 const plan = { id: 'run-plan', status: 'SUCCEEDED', taskType: 'PLAN_SUGGESTION', planContentHash: 'canonical-hash', output: { text: 'Review this draft.', planSuggestion: { title: 'Explore a task', steps: ['Try one practice task.'] } }, toolStepsAndSources: [] }
 apiRequest.mockImplementation(async (path, request = {}) => {
  if (path === '/v2/me') return { id: '7', role: 'STUDENT' }
  if (path.startsWith('/v2/agent-sessions?')) return [{ id: 's1', title: 'My conversation', configurationVersion: 1 }]
  if (path === '/v2/agent-sessions/s1') return { id: 's1', agentName: 'Career Explorer', title: 'My conversation', configurationVersion: 1 }
  if (path === '/v2/agent-sessions/s1/interactions') return []
  if (path.endsWith('/plan-suggestions')) return plan
  if (path === '/v2/agent-executions/run-plan') return plan
  if (path === '/v2/guidance/dashboard') return { activePlan: { id: 'old-plan', revision: 4 } }
  if (path.endsWith('/confirm-suggestion')) return { id: 'confirmed-plan' }
  if (path === '/v2/insight-reports') return []
  throw new Error(`Unexpected route ${path} ${request.method}`)
 })
 return plan
}
it('keeps a suggested plan a draft until exact source/hash confirmation and preserves the message draft', async () => {
 fixture(); page(); await screen.findByText(/My conversation ·/); fireEvent.change(await screen.findByLabelText('Message to Agent'), { target: { value: 'My original exploration question' } }); fireEvent.click(screen.getByRole('button', { name: 'Suggest draft plan' }))
 await screen.findByText('Explore a task'); expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/confirm-suggestion'))).toBe(false)
 fireEvent.click(screen.getByRole('button', { name: 'Confirm and save this plan' }))
 await waitFor(() => expect(apiRequest.mock.calls.some(([path]) => path.endsWith('/confirm-suggestion'))).toBe(true))
 const body = JSON.parse(apiRequest.mock.calls.find(([path]) => path.endsWith('/confirm-suggestion'))[1].body)
 expect(body).toMatchObject({ sourceExecutionId: 'run-plan', contentHash: 'canonical-hash', expectedActivePlanId: 'old-plan', expectedRevision: 4 }); expect(body.title).toBeUndefined()
 expect(JSON.parse(localStorage.getItem('careersim_core_7_agent-message_s1')).text).toBe('My original exploration question')
})
it('shows real incremental text and reconnects the same execution without re-posting a message', async () => {
 fixture(); const run = { id: 'running', status: 'RUNNING', toolStepsAndSources: [] }
 const original = apiRequest.getMockImplementation(); apiRequest.mockImplementation(async (path, request) => path.endsWith('/messages') || path.endsWith('/running') ? run : original(path, request))
 streamExecution.mockImplementation(async (id, options) => { options.onEvent({ sequenceNumber: 3, type: 'answer.segment', payload: { text: 'First actual segment.' } }); throw new Error('Connection interrupted') })
 page(); await screen.findByText(/My conversation ·/); fireEvent.change(await screen.findByLabelText('Message to Agent'), { target: { value: 'Explore this task' } }); fireEvent.click(screen.getByRole('button', { name: 'Send message' })); await screen.findByText('First actual segment.')
 fireEvent.click(screen.getByRole('button', { name: 'Reconnect saved execution' })); await waitFor(() => expect(streamExecution).toHaveBeenCalledTimes(2))
 expect(streamExecution.mock.calls[1][1].after).toBe(3); expect(apiRequest.mock.calls.filter(([p]) => p.endsWith('/messages'))).toHaveLength(1)
})
it('does not render an earlier account response after the workspace is unmounted', async () => {
 fixture(); let deliver
 const original = apiRequest.getMockImplementation(); apiRequest.mockImplementation((path, request) => path === '/v2/agent-sessions/s1/interactions' ? new Promise(resolve => { deliver = resolve }) : original(path, request))
 const view = page(); await waitFor(() => expect(apiRequest.mock.calls.some(([p]) => p.endsWith('/s1/interactions'))).toBe(true)); view.unmount(); render(<p>Second account workspace</p>); deliver([{ id: 'old', actorType: 'ASSISTANT', content: { text: 'Private previous account narrative' } }]); await Promise.resolve()
 expect(screen.queryByText('Private previous account narrative')).toBeNull()
})
it('renders unknown Admin metrics honestly and only requests the safe dashboard contract', async () => {
 const data = usageFixture(); data.summary.recordedEstimatedCostUsd = null
 apiRequest.mockResolvedValue(data)
 render(<I18nProvider><MemoryRouter><AgentUsagePage /></MemoryRouter></I18nProvider>)
 await screen.findByRole('heading', { name: 'Recent AI executions' })
 expect(screen.getAllByText('Not available').length).toBeGreaterThan(0)
 expect(apiRequest.mock.calls[0][0]).toMatch(/^\/v2\/admin\/agent-usage\?view=dashboard&/)
 expect(apiRequest.mock.calls.some(([p]) => p.includes('insight-reports') || p.includes('interactions'))).toBe(false)
})
it('Student Quick Actions preserves an editable account-scoped draft without auto-send', async()=>{
 fixture();page();await screen.findByText(/My conversation ·/)
 const suggestion='Which simulation could I try next?';fireEvent.click(screen.getByRole('button',{name:suggestion}))
 expect(screen.getByLabelText('Message to Agent').value).toBe(suggestion)
 expect(JSON.parse(localStorage.getItem('careersim_core_7_agent-message_s1')).text).toBe(suggestion)
 expect(apiRequest.mock.calls.filter(([,request])=>request?.method==='POST')).toHaveLength(0)
})
it('shows alternating readable bubbles and permits explicit Student messages without a separate consent form', async()=>{
 fixture();const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation(async(path,options)=>{
  if(path==='/v2/agent-sessions/s1')return {id:'s1',agentName:'Evidence Coach',title:'My conversation',configurationVersion:6,provider:'gemini'}
  if(path.endsWith('/interactions'))return [{id:'q1',actorType:'USER',content:{text:'My question'}},{id:'a1',actorType:'ASSISTANT',content:{text:'My authorized answer',citations:[{referenceId:'K1',quote:'Exact supporting text'}]}}]
  return original(path,options)
 });page();const user=await screen.findByText('My question'),answer=screen.getByText('My authorized answer')
 expect(user.closest('li').className).toContain('student-chat-turn--user');expect(answer.closest('li').className).toContain('student-chat-turn--agent')
 expect(screen.getByRole('region',{name:'Messages'})).toBeTruthy();expect(screen.getAllByText('Evidence Coach').length).toBeGreaterThan(0);expect(screen.queryByText('Agent response')).toBeNull()
 expect(screen.getByText('Exact supporting text').closest('details').open).toBe(false)
 fireEvent.change(screen.getByLabelText('Message to Agent'),{target:{value:'My new question'}})
 expect(screen.getByRole('button',{name:'Send message'}).disabled).toBe(false)
 expect(apiRequest.mock.calls.some(([path])=>path.includes('/consents'))).toBe(false)
 expect(screen.queryByText('Allow AI assistance')).toBeNull()
})

it('sends on Enter once, but not Shift+Enter or Vietnamese IME composition', async () => {
 fixture();let accept;const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation((path,request)=>path.endsWith('/messages')?new Promise(resolve=>{accept=resolve}):original(path,request))
 page();await screen.findByText(/My conversation ·/);const input=screen.getByLabelText('Message to Agent')
 fireEvent.change(input,{target:{value:'Tôi muốn khám phá nghề nghiệp'}})
 fireEvent.keyDown(input,{key:'Enter',shiftKey:true});fireEvent.compositionStart(input);fireEvent.keyDown(input,{key:'Enter'});fireEvent.compositionEnd(input);fireEvent.keyDown(input,{key:'Enter',isComposing:true});fireEvent.keyDown(input,{key:'Enter',keyCode:229})
 expect(apiRequest.mock.calls.filter(([p])=>p.endsWith('/messages'))).toHaveLength(0)
 fireEvent.keyDown(input,{key:'Enter'});fireEvent.keyDown(input,{key:'Enter'})
 expect(apiRequest.mock.calls.filter(([p])=>p.endsWith('/messages'))).toHaveLength(1)
 accept({id:'done',status:'SUCCEEDED',output:{text:'Done'}});await screen.findByText('Done')
})
it('loads Run Details lazily for the correct individual message and hides unauthorized raw fields', async () => {
 fixture();const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation(async(path,request)=>{
  if(path.endsWith('/interactions'))return ['one','two'].map(id=>({id,executionId:id,actorType:'ASSISTANT',content:{text:`Answer ${id}`,citations:[{referenceId:`K-${id}`,quote:`Evidence ${id}`}]}}))
  if(path==='/v2/agent-executions/two')return {id:'two',status:'SUCCEEDED',latencyMs:42,inputTokens:12,outputTokens:8,toolStepsAndSources:[{type:'tool',name:'kb.search',status:'SUCCEEDED',privatePayload:'never render',arguments:{hidden:'never render'}}],rawPrompt:'never render'}
  return original(path,request)
 });page();await screen.findByText('Answer two')
 const first=screen.getByText('Answer one').closest('article'),second=screen.getByText('Answer two').closest('article')
 expect(within(first).getByText('Evidence one').closest('details').open).toBe(false)
 expect(within(second).getByText('View run details').closest('details').open).toBe(false)
 expect(apiRequest.mock.calls.some(([p])=>p.includes('/agent-executions/'))).toBe(false)
 const details=within(second).getByText('View run details').closest('details');details.open=true;fireEvent(details,new Event('toggle'))
 await within(second).findByText('SUCCEEDED');expect(apiRequest.mock.calls.filter(([p])=>p.includes('/agent-executions/')).map(([p])=>p)).toEqual(['/v2/agent-executions/two'])
 expect(within(first).queryByText('SUCCEEDED')).toBeNull();expect(screen.queryByText(/never render/)).toBeNull()
})
it('keeps history hidden, resumes without inheriting drafts and confirms deletion; new chat has no implicit context', async () => {
 fixture();let deleted=false;const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation(async(path,request={})=>{
  if(path.startsWith('/v2/agent-sessions?'))return deleted?[]:[{id:'s1',title:'My conversation',configurationVersion:1},{id:'s2',title:'Second conversation',configurationVersion:1}]
  if(path==='/v2/agent-sessions/s2'&&request.method==='DELETE'){deleted=true;return null}
  if(path==='/v2/agents/a1/sessions')return {id:'new',agentName:'Career Explorer',title:'New conversation',configurationVersion:1}
  if(path==='/v2/agent-sessions/s2'||path==='/v2/agent-sessions/new')return {id:path.split('/').at(-1),agentName:'Career Explorer',title:'Clean conversation',configurationVersion:1}
  if(path.endsWith('/interactions'))return []
  return original(path,request)
 });page();await screen.findByText(/My conversation ·/)
 expect(screen.queryByRole('dialog',{name:'Conversation history'})).toBeNull()
 fireEvent.change(screen.getByLabelText('Message to Agent'),{target:{value:'Only in first conversation'}})
 fireEvent.click(screen.getByRole('button',{name:'History'}));fireEvent.click(await screen.findByRole('button',{name:'Resume · Second conversation'}))
 expect(screen.queryByRole('dialog',{name:'Conversation history'})).toBeNull();await screen.findByText(/Clean conversation/);expect(screen.getByLabelText('Message to Agent').value).toBe('')
 fireEvent.click(screen.getByRole('button',{name:'History'}));fireEvent.click(await screen.findByRole('button',{name:'Delete · Second conversation'}))
 expect(apiRequest.mock.calls.some(([,r])=>r?.method==='DELETE')).toBe(false)
 fireEvent.click(screen.getByRole('button',{name:'Confirm deletion'}));await waitFor(()=>expect(deleted).toBe(true))
 fireEvent.click(screen.getByRole('button',{name:'Close history'}));fireEvent.click(screen.getByRole('button',{name:'New Chat'}));await waitFor(()=>expect(apiRequest.mock.calls.some(([p])=>p==='/v2/agents/a1/sessions')).toBe(true));await screen.findByText(/Clean conversation/)
 const body=JSON.parse(apiRequest.mock.calls.find(([p])=>p==='/v2/agents/a1/sessions')[1].body);expect(body.contextSessionIds).toEqual([]);expect(screen.getByLabelText('Message to Agent').value).toBe('')
})
it('requests a report with its dedicated endpoint and opens the formatted report at its own turn', async () => {
 fixture();const original=apiRequest.getMockImplementation();const report={summary:'Khám phá có minh chứng',hypotheses:[{direction:'Data',reason:'Practice evidence',evidenceRefs:['K1']}],strengthsPracticed:['SQL'],difficulties:['Timing'],limitations:['Limited evidence'],nextSteps:['Practice'],evidenceRefs:['K1']}
 const run={id:'report-run',status:'SUCCEEDED',reportId:'report-1',output:{text:'Report is ready.',report}}
 apiRequest.mockImplementation(async(path,request)=>path==='/v2/insight-reports/report-1'?{id:'report-1',structuredInsights:report}:path.endsWith('/insight-reports')||path.endsWith('/report-run')?run:original(path,request))
 page();await screen.findByText(/My conversation ·/);fireEvent.click(screen.getByRole('button',{name:'Request insight report'}));await screen.findByText('Report is ready.')
 expect(apiRequest.mock.calls.some(([p])=>p.endsWith('/messages'))).toBe(false)
 fireEvent.click(screen.getByRole('button',{name:'Open report'}));const viewer=await screen.findByRole('dialog',{name:'Insight report'})
 expect(within(viewer).getByText('Khám phá có minh chứng')).toBeTruthy();expect(within(viewer).getByRole('heading',{name:'Skills practiced'})).toBeTruthy();expect(within(viewer).queryByText(/schemaVersion/)).toBeNull()
})
it('does not force transcript scrolling when the reader is above the bottom', async () => {
 fixture();const run={id:'stream-scroll',status:'RUNNING'};const original=apiRequest.getMockImplementation();let receive
 apiRequest.mockImplementation(async(path,request)=>path.endsWith('/messages')||path.endsWith('/stream-scroll')?run:original(path,request))
 streamExecution.mockImplementation((_,options)=>{receive=options.onEvent;return new Promise(()=>{})})
 page();await screen.findByText(/My conversation ·/);fireEvent.change(screen.getByLabelText('Message to Agent'),{target:{value:'Question'}});fireEvent.keyDown(screen.getByLabelText('Message to Agent'),{key:'Enter'})
 await waitFor(()=>expect(receive).toBeTypeOf('function'));const transcript=screen.getByRole('region',{name:'Messages'})
 Object.defineProperties(transcript,{scrollHeight:{value:1000,configurable:true},clientHeight:{value:200,configurable:true}});transcript.scrollTop=100;fireEvent.scroll(transcript)
 const {act}=await import('@testing-library/react');await act(()=>receive({sequenceNumber:1,type:'answer.segment',payload:{text:'A streamed answer'}}))
 expect(transcript.scrollTop).toBe(100);fireEvent.click(screen.getByRole('button',{name:'Jump to latest'}));expect(transcript.scrollTop).toBe(1000)
})

it('keeps compact icon controls and hides meaningless pagination for a short conversation', async()=>{
 fixture();page();await screen.findByText(/My conversation ·/)
 const header=document.querySelector('.student-chat-header');expect(within(header).getByRole('button',{name:'History'}).textContent).toBe('');expect(within(header).getByRole('button',{name:'New Chat'}).textContent).toBe('')
 expect(screen.queryByRole('button',{name:'Load more messages'})).toBeNull();expect(screen.queryByRole('button',{name:'Show next messages'})).toBeNull()
 const input=screen.getByLabelText('Message to Agent');expect(input.rows).toBe(1);expect(input.style.height).toBe('40px');expect(screen.getByRole('button',{name:'Send message'}).textContent).toBe('')
 fireEvent.click(screen.getByRole('button',{name:'History'}));const drawer=screen.getByRole('dialog',{name:'Conversation history'});expect(within(drawer).getByRole('button',{name:'Resume · My conversation'}).textContent).toBe('My conversation');expect(within(drawer).getByRole('button',{name:'Delete · My conversation'}).textContent).toBe('')
})
it('shows pagination only for a full server page and hides it when the next page is empty',async()=>{
 fixture();const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation(async(path,request)=>path.endsWith('/interactions')?Array.from({length:100},(_,index)=>({id:`m${index}`,sequenceNumber:index+1,actorType:'USER',content:{text:`Message ${index}`}})):path.endsWith('/interactions?afterSequence=100')?[]:original(path,request))
 page();fireEvent.click(await screen.findByRole('button',{name:'Show next messages'}));await waitFor(()=>expect(screen.queryByRole('button',{name:'Show next messages'})).toBeNull());expect(apiRequest).toHaveBeenCalledWith('/v2/agent-sessions/s1/interactions?afterSequence=100')
})
it('never auto-follows streaming or completion even when reading at the previous bottom',async()=>{
 fixture();let receive,finish;let run={id:'locked',status:'RUNNING'};const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation(async(path,request)=>path.endsWith('/messages')||path.endsWith('/locked')?run:original(path,request))
 streamExecution.mockImplementation((_,options)=>{receive=options.onEvent;return new Promise(resolve=>{finish=resolve})})
 page();await screen.findByText(/My conversation ·/);const input=screen.getByLabelText('Message to Agent');fireEvent.change(input,{target:{value:'Question'}});fireEvent.keyDown(input,{key:'Enter'})
 await waitFor(()=>expect(receive).toBeTypeOf('function'));const transcript=screen.getByRole('region',{name:'Messages'})
 Object.defineProperties(transcript,{scrollHeight:{value:1000,configurable:true},clientHeight:{value:200,configurable:true}});transcript.scrollTop=800;fireEvent.scroll(transcript)
 Object.defineProperty(transcript,'scrollHeight',{value:1600,configurable:true});const {act}=await import('@testing-library/react');await act(()=>receive({sequenceNumber:1,type:'answer.segment',payload:{text:'Long answer'}}));expect(transcript.scrollTop).toBe(800)
 run={...run,status:'SUCCEEDED',output:{text:'Completed answer'}};await act(()=>finish());expect(transcript.scrollTop).toBe(800)
 const jump=screen.getByRole('button',{name:'Jump to latest'});expect(jump.textContent).toBe('');fireEvent.click(jump);expect(transcript.scrollTop).toBe(1600)
})

it('keeps popup proportions compact even when the shared UI stylesheet loads last',async()=>{
 fixture();const style=document.createElement('style');style.textContent=(readFileSync('src/features/agents/studentAgent.css','utf8')+'\n'+readFileSync('src/shared/ui/ui.css','utf8')).replace(/@import[^;]+;/g,'');document.head.appendChild(style)
 try {
  render(<I18nProvider><div className="cs-ui student-agent-widget"><div className="student-agent-popup"><Chat/></div></div></I18nProvider>);await screen.findByText(/My conversation ·/)
  const header=document.querySelector('.student-chat-header'),history=within(header).getByRole('button',{name:'History'}),input=screen.getByLabelText('Message to Agent'),popup=document.querySelector('.student-agent-popup')
  expect(getComputedStyle(header.querySelector('h2')).fontSize).toBe('14px');expect(getComputedStyle(history).minHeight).toBe('32px');expect(getComputedStyle(input).minHeight).toBe('40px');expect(getComputedStyle(popup).width).toBe('460px')
  popup.classList.add('student-agent-popup--expanded');expect(getComputedStyle(popup).width).toBe('780px');expect(getComputedStyle(document.querySelector('.student-transcript-wrap')).flexGrow).toBe('1')
 } finally {style.remove()}
})
it('does not pretend a failed report execution is a downloadable report',async()=>{
 fixture();const original=apiRequest.getMockImplementation();const run={id:'failed-report',taskType:'INSIGHT_REPORT',status:'FAILED',errorCode:'PROVIDER_UNAVAILABLE',output:{text:'Unavailable'}}
 apiRequest.mockImplementation(async(path,request)=>path.endsWith('/insight-reports')||path.endsWith('/failed-report')?run:original(path,request))
 page();await screen.findByText(/My conversation ·/);fireEvent.click(screen.getByRole('button',{name:'Request insight report'}));await screen.findByText('No report was saved for this request. Open saved reports or edit your request to try again.')
 expect(screen.queryByRole('button',{name:'Download report'})).toBeNull();expect(screen.getByRole('button',{name:'Saved reports'})).toBeTruthy()
})

it('preserves the reading position across hidden streaming and reopening without a second message or stream',async()=>{
 fixture();let receive;const run={id:'hidden-stream',status:'RUNNING'};const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation(async(path,request)=>path.endsWith('/messages')||path.endsWith('/hidden-stream')?run:original(path,request));streamExecution.mockImplementation((_,options)=>{receive=options.onEvent;return new Promise(()=>{})})
 const renderChat=visible=><I18nProvider><Chat visible={visible}/></I18nProvider>,view=render(renderChat(true));await screen.findByText(/My conversation ·/)
 fireEvent.change(screen.getByLabelText('Message to Agent'),{target:{value:'Question'}});fireEvent.keyDown(screen.getByLabelText('Message to Agent'),{key:'Enter'});await waitFor(()=>expect(receive).toBeTypeOf('function'))
 const transcript=screen.getByRole('region',{name:'Messages'});Object.defineProperties(transcript,{scrollHeight:{value:1200,configurable:true},clientHeight:{value:200,configurable:true}});transcript.scrollTop=300;fireEvent.scroll(transcript)
 view.rerender(renderChat(false));transcript.scrollTop=0;const {act}=await import('@testing-library/react');await act(()=>receive({sequenceNumber:1,type:'answer.segment',payload:{text:'While closed'}}));view.rerender(renderChat(true));expect(transcript.scrollTop).toBe(300)
 expect(streamExecution).toHaveBeenCalledTimes(1);expect(apiRequest.mock.calls.filter(([path])=>path.endsWith('/messages'))).toHaveLength(1)
})

it.each([['en','insight-reports','Create an evidence-based insight report for me.'],['vi','insight-reports',createTranslator('vi')('Create an evidence-based insight report for me.')],['en','plan-suggestions','Suggest an exploration plan draft for me.'],['vi','plan-suggestions',createTranslator('vi')('Suggest an exploration plan draft for me.')]])('dedicated %s/%s action never submits unrelated composer text',async(locale,task,expected)=>{
 fixture();const original=apiRequest.getMockImplementation();const run={id:'action',status:'SUCCEEDED',output:{text:'Action accepted'}}
 apiRequest.mockImplementation(async(path,request)=>path.endsWith('/'+task)||path.endsWith('/action')?run:original(path,request))
 render(<I18nProvider initialLocale={locale}><Chat/></I18nProvider>);await screen.findByText(/My conversation ·/)
 const input=screen.getByRole('textbox');fireEvent.change(input,{target:{value:'Có simulation nào phù hợp để tôi thử tiếp không?'}})
 // Select by the action's translated accessible name, not by its position in the chip row.
 fireEvent.click(screen.getByRole('button',{name:createTranslator(locale)(task==='insight-reports'?'Request insight report':'Suggest draft plan')}))
 await waitFor(()=>expect(apiRequest.mock.calls.some(([path])=>path.endsWith('/'+task))).toBe(true))
 const body=JSON.parse(apiRequest.mock.calls.find(([path])=>path.endsWith('/'+task))[1].body)
 expect(body.text).toBe(expected);expect(input.value).toBe('Có simulation nào phù hợp để tôi thử tiếp không?');expect(apiRequest.mock.calls.some(([path])=>path.endsWith('/messages'))).toBe(false)
})

it('retries a dedicated action with the exact persisted payload after a locale change',async()=>{
 fixture();const original=apiRequest.getMockImplementation();const pendingId=crypto.randomUUID(),prompt='Create an evidence-based insight report for me.'
 localStorage.setItem('careersim_core_7_agent-message_s1',JSON.stringify({text:'unrelated draft',requestId:pendingId,requestTask:'insight-reports',requestText:prompt}))
 const run={id:'pending-report',status:'SUCCEEDED',output:{text:'Accepted'}};apiRequest.mockImplementation(async(path,request)=>path.endsWith('/insight-reports')||path.endsWith('/pending-report')?run:original(path,request))
 render(<I18nProvider initialLocale="vi"><Chat/></I18nProvider>);await screen.findByText(/My conversation ·/)
 expect(apiRequest.mock.calls.some(([path])=>path.endsWith('/insight-reports'))).toBe(false)
 fireEvent.click(screen.getByRole('button',{name:createTranslator('vi')('Request insight report')}));await waitFor(()=>expect(apiRequest.mock.calls.some(([path])=>path.endsWith('/insight-reports'))).toBe(true))
 expect(JSON.parse(apiRequest.mock.calls.find(([path])=>path.endsWith('/insight-reports'))[1].body)).toEqual({requestId:pendingId,text:prompt})
})

it('renders a report selected by the backend from natural text without a second client action request',async()=>{
 fixture();const original=apiRequest.getMockImplementation(),question='bạn tạo cho tôi insight report để tôi tải về'
 const report={summary:'Báo cáo từ lệnh tự nhiên',hypotheses:[],strengthsPracticed:[],difficulties:[],limitations:['Limited evidence'],nextSteps:['Practice'],evidenceRefs:['K1']}
 const run={id:'natural-report',taskType:'CHAT',status:'SUCCEEDED',reportId:'natural-saved',output:{text:'Báo cáo đã được tạo.',requestedAction:'INSIGHT_REPORT',report}}
 apiRequest.mockImplementation(async(path,request)=>path==='/v2/insight-reports/natural-saved'?{structuredInsights:report}:path.endsWith('/messages')||path.endsWith('/natural-report')?run:original(path,request))
 page();await screen.findByText(/My conversation ·/);fireEvent.change(screen.getByRole('textbox'),{target:{value:question}});fireEvent.keyDown(screen.getByRole('textbox'),{key:'Enter'})
 fireEvent.click(await screen.findByRole('button',{name:'Open report'}));await screen.findByText('Báo cáo từ lệnh tự nhiên')
 expect(JSON.parse(apiRequest.mock.calls.find(([path])=>path.endsWith('/messages'))[1].body).text).toBe(question)
 expect(apiRequest.mock.calls.some(([path,request])=>path.endsWith('/insight-reports')&&request?.method==='POST')).toBe(false)
})
it('does not reuse a legacy malformed action request ID after the payload fix',async()=>{
 fixture();const oldId=crypto.randomUUID();localStorage.setItem('careersim_core_7_agent-message_s1',JSON.stringify({text:'Old simulation prompt',requestId:oldId,requestTask:'insight-reports'}))
 const original=apiRequest.getMockImplementation(),run={id:'fixed-action',status:'SUCCEEDED',output:{text:'Accepted'}}
 apiRequest.mockImplementation(async(path,request)=>path.endsWith('/insight-reports')||path.endsWith('/fixed-action')?run:original(path,request))
 page();await screen.findByText(/My conversation ·/);fireEvent.click(screen.getByRole('button',{name:'Request insight report'}));await waitFor(()=>expect(apiRequest.mock.calls.some(([path])=>path.endsWith('/insight-reports'))).toBe(true))
 const body=JSON.parse(apiRequest.mock.calls.find(([path])=>path.endsWith('/insight-reports'))[1].body);expect(body.requestId).not.toBe(oldId);expect(body.text).toBe('Create an evidence-based insight report for me.')
})


it('clears an accepted message, renders its persisted turn and never restores submitted text on reload', async () => {
 fixture();let sent=false;const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation(async(path,options)=>{
  if(path.endsWith('/messages')){sent=true;return {id:'accepted',status:'SUCCEEDED',output:{text:'Received'}}}
  if(path.endsWith('/interactions'))return sent?[{id:'user',actorType:'USER',executionId:'accepted',content:{text:'Câu hỏi đã gửi'}}]:[]
  if(path.endsWith('/accepted'))return {id:'accepted',status:'SUCCEEDED',output:{text:'Received'}}
  return original(path,options)
 });const view=page();const input=await screen.findByLabelText('Message to Agent');fireEvent.change(input,{target:{value:'Câu hỏi đã gửi'}});fireEvent.keyDown(input,{key:'Enter'})
 await screen.findByText('Câu hỏi đã gửi');expect(input.value).toBe('');expect(JSON.parse(localStorage.getItem('careersim_core_7_agent-message_s1')).text).toBe('')
 fireEvent.change(input,{target:{value:'Bản nháp chưa gửi'}});view.unmount();page()
 expect((await screen.findByLabelText('Message to Agent')).value).toBe('Bản nháp chưa gửi');expect(apiRequest.mock.calls.filter(([p])=>p.endsWith('/messages'))).toHaveLength(1)
})
it('keeps a newer draft typed while an earlier send is awaiting acceptance', async () => {
 fixture();let accept;const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation((path,options)=>path.endsWith('/messages')?new Promise(resolve=>{accept=resolve}):path.endsWith('/accepted')?Promise.resolve({id:'accepted',status:'SUCCEEDED',output:{text:'Accepted'}}):original(path,options))
 page();const input=await screen.findByLabelText('Message to Agent');fireEvent.change(input,{target:{value:'First message'}});fireEvent.keyDown(input,{key:'Enter'});fireEvent.change(input,{target:{value:'Next unsent thought'}})
 accept({id:'accepted',status:'SUCCEEDED',output:{text:'Accepted'}});await screen.findByText('Accepted')
 expect(input.value).toBe('Next unsent thought');expect(JSON.parse(localStorage.getItem('careersim_core_7_agent-message_s1')).text).toBe('Next unsent thought')
})
it('preserves the composer on a rejected send instead of losing unsent input', async () => {
 fixture();const original=apiRequest.getMockImplementation();apiRequest.mockImplementation((path,options)=>path.endsWith('/messages')?Promise.reject(new Error('Network unavailable')):original(path,options))
 page();const input=await screen.findByLabelText('Message to Agent');fireEvent.change(input,{target:{value:'Keep this request'}});fireEvent.keyDown(input,{key:'Enter'});await screen.findByText('Network unavailable');expect(input.value).toBe('Keep this request')
})
function InitiallyUnselectedChat(){const[id,setId]=useState(null);return <StudentAgentChat owner="7" agent={{id:'a1',name:'Career Explorer'}} id={id} onSessionChange={setId}/>}
it('automatically resumes the latest returned conversation without opening history or creating another session', async () => {
 fixture();render(<I18nProvider initialLocale="en"><InitiallyUnselectedChat/></I18nProvider>)
 await screen.findByText(/My conversation ·/);expect(screen.queryByRole('dialog',{name:'Conversation history'})).toBeNull();expect(apiRequest.mock.calls.filter(([,r])=>r?.method==='POST')).toHaveLength(0)
})
it('creates exactly one clean conversation for an empty history and renders its stored greeting', async () => {
 fixture();let created=false;const original=apiRequest.getMockImplementation()
 apiRequest.mockImplementation(async(path,options)=>{
  if(path.startsWith('/v2/agent-sessions?'))return created?[{id:'s1',title:'New conversation'}]:[]
  if(path==='/v2/agents/a1/sessions'){created=true;expect(JSON.parse(options.body).contextSessionIds).toEqual([]);return {id:'s1',agentName:'Career Explorer'}}
  if(path.endsWith('/interactions'))return [{id:'greeting',actorType:'ASSISTANT',executionId:null,content:{text:'Xin chào từ cấu hình đã lưu!'}}]
  return original(path,options)
 });render(<I18nProvider initialLocale="en"><InitiallyUnselectedChat/></I18nProvider>)
 await screen.findByText('Xin chào từ cấu hình đã lưu!');expect(apiRequest.mock.calls.filter(([p])=>p==='/v2/agents/a1/sessions')).toHaveLength(1);expect(apiRequest.mock.calls.some(([p])=>p.includes('/agent-executions/'))).toBe(false)
})

it('restores the last accessible conversation and its unsent draft after leaving the page', async () => {
 fixture();const original=apiRequest.getMockImplementation()
 localStorage.setItem('careersim_core_7_agent-selection_a1',JSON.stringify({id:'s1'}));localStorage.setItem('careersim_core_7_agent-message_s1',JSON.stringify({text:'Draft in an older conversation'}))
 apiRequest.mockImplementation((path,options)=>path.startsWith('/v2/agent-sessions?')?Promise.resolve([{id:'newest',title:'Newest created chat'},{id:'s1',title:'My conversation'}]):original(path,options))
 render(<I18nProvider initialLocale="en"><InitiallyUnselectedChat/></I18nProvider>);expect((await screen.findByLabelText('Message to Agent')).value).toBe('Draft in an older conversation');expect(apiRequest.mock.calls.some(([p])=>p.endsWith('/newest'))).toBe(false)
})
it('ignores a deleted or unauthorized cached selection and resumes the latest accessible history entry', async () => {
 fixture();localStorage.setItem('careersim_core_7_agent-selection_a1',JSON.stringify({id:'no-longer-accessible'}))
 render(<I18nProvider initialLocale="en"><InitiallyUnselectedChat/></I18nProvider>);await screen.findByText(/My conversation ·/);expect(apiRequest.mock.calls.some(([p])=>p.includes('/no-longer-accessible'))).toBe(false)
})

it('anchors the newly submitted User turn near the top immediately, then locks while the answer grows', async()=>{
 fixture();let accept,receive;const original=apiRequest.getMockImplementation();let end=1000
 apiRequest.mockImplementation((path,options)=>path.endsWith('/messages')?new Promise(resolve=>{accept=resolve}):path.endsWith('/anchored')?Promise.resolve({id:'anchored',status:'RUNNING'}):original(path,options))
 streamExecution.mockImplementation((_,options)=>{receive=options.onEvent;return new Promise(()=>{})})
 const rect=Element.prototype.getBoundingClientRect
 vi.spyOn(Element.prototype,'getBoundingClientRect').mockImplementation(function(){const viewport=this.closest('.student-chat-transcript');if(this.classList.contains('student-chat-transcript'))return {top:100};if(viewport&&this.hasAttribute('data-chat-anchor'))return {top:100+900-viewport.scrollTop};if(viewport&&this.classList.contains('chat-viewport-tail'))return {top:100+end-viewport.scrollTop};return rect.call(this)})
 try{
  page();const input=await screen.findByLabelText('Message to Agent'),viewport=screen.getByRole('region',{name:'Messages'});Object.defineProperties(viewport,{clientHeight:{value:300,configurable:true},scrollHeight:{get:()=>end+parseFloat(viewport.querySelector('.chat-viewport-tail').style.height||0)+32,configurable:true}})
  fireEvent.change(input,{target:{value:'Anchor this new question'}});fireEvent.keyDown(input,{key:'Enter'});await within(viewport).findByText('Anchor this new question');expect(viewport.scrollTop).toBe(880);expect(screen.getByText('Sending...')).toBeTruthy()
  accept({id:'anchored',status:'RUNNING'});await waitFor(()=>expect(receive).toBeTypeOf('function'));end=1500;const {act}=await import('@testing-library/react');await act(()=>receive({sequenceNumber:1,type:'answer.segment',payload:{text:'Long growing response'}}));expect(viewport.scrollTop).toBe(880);expect(input.value).toBe('')
 }finally{vi.restoreAllMocks()}
})
