// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { useSyncExternalStore } from 'react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import I18nProvider from '../i18n/I18nProvider.jsx'
import AppShell from './AppShell.jsx'
import AssignedAgentsPage from '../../features/agents/AssignedAgentsPage.jsx'
import StudentAgentPage from '../../features/agents/StudentAgentPage.jsx'
import { streamExecution } from '../../features/agents/studentAgentApi.js'
import { apiRequest, getAccountGeneration, invalidateAccount, subscribeAccount } from '../api/httpClient.js'

vi.mock('../../features/admin/agentPlatform/platformApi.js',()=>({agentPlatformEnabled:true}))
vi.mock('../../features/agents/studentAgentApi.js',async original=>({...await original(),streamExecution:vi.fn()}))
vi.mock('../api/httpClient.js', async original => ({ ...await original(), apiRequest: vi.fn() }))
beforeEach(() => { vi.clearAllMocks(); localStorage.clear() })
afterEach(cleanup)
function Shell() {
  const generation = useSyncExternalStore(subscribeAccount, getAccountGeneration)
  return <AppShell key={generation} />
}
function Destination() { return <h1>{useLocation().pathname}</h1> }
function page(path = '/assessment') {
  return render(<I18nProvider><MemoryRouter initialEntries={[path]}><Routes><Route element={<Shell />}>
    <Route path="/agents" element={<AssignedAgentsPage/>}/><Route path="/agents/:agentId" element={<StudentAgentPage/>}/>{['/', '/assessment', '/simulations', '/simulations/demo', '/simulations/demo/results', '/guidance', '/enterprise', '/admin', '/account'].map(route => <Route key={route} path={route} element={<Destination />} />)}
  </Route></Routes></MemoryRouter></I18nProvider>)
}
it.each(['/admin','/account','/simulations'])('always returns Home through the institutional logo from %s', async path => {
  apiRequest.mockResolvedValue({ id:'7', role:'ADMIN' })
  page(path)
  const logo=screen.getByRole('link',{name:'HCM-UTE Career Simulation Platform Home'})
  expect(logo.getAttribute('href')).toBe('/')
  fireEvent.click(logo)
  expect(await screen.findByRole('heading',{name:'/'})).toBeTruthy()
})
it.each(['STUDENT', 'ENTERPRISE', 'ADMIN'])('uses the server role for navigation and offers shared logout for %s', async role => {
  apiRequest.mockResolvedValue({ id: '7', role })
  page()
  await screen.findByRole('button', { name: 'Sign out' })
  expect(Boolean(screen.queryByRole('link', { name: 'Admin Studio' }))).toBe(role === 'ADMIN')
  for (const name of ['Home', 'Career Quizzes', 'Career Simulations']) expect(within(document.querySelector('header')).getByRole('link', { name })).toBeTruthy()
  expect(Boolean(within(document.querySelector('header')).queryByRole('link', { name: 'Dashboard', exact: true }))).toBe(role === 'STUDENT')
  if (role === 'STUDENT') expect(within(document.querySelector('header')).getByRole('link', { name: 'Dashboard', exact: true }).getAttribute('href')).toBe('/guidance')
  expect(document.querySelector('header a[href="/dashboard"]')).toBeNull()
  expect(document.querySelector('a[href="/admin/assessments"], .syn-widget, .syn-launcher')).toBeNull()
})
it('never exposes privileged navigation while anonymous or when session lookup fails', async () => {
  apiRequest.mockRejectedValue(Object.assign(new Error('Sign in is required.'), { status: 401 }))
  page()
  await waitFor(() => expect(apiRequest).toHaveBeenCalledTimes(2))
  expect(screen.queryByRole('link', { name: 'Admin Studio' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Sign out' })).toBeNull()
  expect(within(document.querySelector('header')).queryByRole('link', { name: 'Dashboard', exact: true })).toBeNull()
})
it.each(['/assessment', '/enterprise', '/admin'])('logs out through the existing API, isolates account state and returns to Home from %s', async path => {
  let signedIn = true
  apiRequest.mockImplementation(async (url, options) => {
    if (url === '/v2/auth/logout') {
      expect(options.method).toBe('POST')
      signedIn = false; invalidateAccount(); return { loggedOut: true }
    }
    if (!signedIn) throw Object.assign(new Error('Unauthorized'), { status: 401 })
    return { id: '7', role: path === '/admin' ? 'ADMIN' : path === '/enterprise' ? 'ENTERPRISE' : 'STUDENT' }
  })
  const before = getAccountGeneration()
  page(path)
  fireEvent.click(await screen.findByRole('button', { name: 'Sign out' }))
  await screen.findByRole('heading', { name: '/' })
  expect(getAccountGeneration()).toBeGreaterThan(before)
  expect(screen.queryByRole('button', { name: 'Sign out' })).toBeNull()
  expect(apiRequest).toHaveBeenCalledWith('/v2/auth/logout', expect.objectContaining({ method: 'POST', body: '{}' }))
})
it('blocks duplicate logout and preserves the session on failure so retry remains possible', async () => {
  let fail
  apiRequest.mockImplementation(url => url === '/v2/auth/logout' ? new Promise((_, reject) => { fail = reject }) : Promise.resolve({ id: '7', role: 'STUDENT' }))
  page()
  const button = await screen.findByRole('button', { name: 'Sign out' })
  fireEvent.click(button); fireEvent.click(button)
  expect(apiRequest.mock.calls.filter(([url]) => url === '/v2/auth/logout')).toHaveLength(1)
  expect(button.disabled).toBe(true)
  fail(new Error('The platform is not reachable. Please try again shortly.'))
  await screen.findByRole('alert')
  expect(button.disabled).toBe(false)
  expect(screen.getByRole('heading', { name: '/assessment' })).toBeTruthy()
})
it('preserves simulation catalogue/detail/results navigation without privileged links', async () => {
  apiRequest.mockResolvedValue({ id: '7', role: 'STUDENT' })
  page('/simulations/demo/results')
  await screen.findByRole('button', { name: 'Sign out' })
  fireEvent.click(screen.getByRole('link', { name: 'Career Simulations' }))
  expect(await screen.findByRole('heading', { name: '/simulations' })).toBeTruthy()
  expect(screen.queryByRole('link', { name: 'Admin Studio' })).toBeNull()
})

it('labels local workspace and never offers misleading logout or posts logout',async()=>{
 apiRequest.mockImplementation(path=>Promise.resolve(path==='/v2/auth/providers'?{google:false,localWorkspace:true}:{id:'7',role:'ADMIN'}))
 page('/admin');await screen.findByText('Local demo');expect(screen.queryByRole('button',{name:'Sign out'})).toBeNull();expect(screen.getByText('Local demo').getAttribute('title')).toContain('LOCAL_DEMO_MODE');expect(apiRequest.mock.calls.some(([path])=>path==='/v2/auth/logout')).toBe(false)
})
it('keeps mobile navigation controls and returns focus to its trigger on Escape', async () => {
  apiRequest.mockRejectedValue(Object.assign(new Error('Sign in is required.'), { status: 401 }))
  page('/')
  const trigger = screen.getByRole('button', { name: 'Open menu' })
  fireEvent.click(trigger)
  expect(trigger.getAttribute('aria-expanded')).toBe('true')
  expect(document.getElementById(trigger.getAttribute('aria-controls')).classList.contains('site-nav--open')).toBe(true)
  const guidance = within(document.querySelector('header')).getByRole('link', { name: 'Home' })
  guidance.focus()
  fireEvent.keyDown(guidance, { key: 'Escape' })
  expect(trigger.getAttribute('aria-expanded')).toBe('false')
  expect(document.activeElement).toBe(trigger)
  fireEvent.click(trigger)
  fireEvent.click(guidance)
  await screen.findByRole('heading', { name: '/' })
  expect(trigger.getAttribute('aria-expanded')).toBe('false')
})

it.each(['STUDENT','ADMIN','ENTERPRISE',null])('renders the global Agent launcher only for authenticated STUDENT, role=%s', async role=>{
 apiRequest.mockImplementation(async path=>{
  if(path==='/v2/auth/providers')return {localWorkspace:false}
  if(path==='/v2/me'){if(!role)throw new Error('Unauthorized');return {id:'7',role}}
  if(path==='/v2/agents')return [{id:'a1',name:'Career Explorer',runtimeAvailable:true}]
  throw new Error('Unexpected route')
 });page('/');await waitFor(()=>expect(apiRequest.mock.calls.some(([p])=>p==='/v2/me')).toBe(true))
 if(role==='STUDENT'){const launcher=await screen.findByRole('button',{name:'Career Agent'});expect(launcher.textContent).toBe('');expect(launcher.querySelectorAll('svg')).toHaveLength(2)}
 else expect(screen.queryByRole('button',{name:'Career Agent'})).toBeNull()
})
it('safely hides the launcher when no published Agent is active',async()=>{
 apiRequest.mockImplementation(async path=>path==='/v2/agents'?[]:path==='/v2/auth/providers'?{localWorkspace:false}:{id:'7',role:'STUDENT'})
 page('/');await waitFor(()=>expect(apiRequest.mock.calls.some(([p])=>p==='/v2/agents')).toBe(true));expect(screen.queryByRole('button',{name:'Career Agent'})).toBeNull()
})
it('shares one live popup with the Agent route; closing, locale change and reopening never repost or reconnect twice',async()=>{
 let release;const run={id:'active-run',status:'RUNNING'}
 apiRequest.mockImplementation(async(path,options={})=>{
  if(path==='/v2/auth/providers')return {localWorkspace:false}
  if(path==='/v2/me')return {id:'7',role:'STUDENT'}
  if(path==='/v2/agents')return [{id:'a1',name:'Career Explorer',runtimeAvailable:true}]
  if(path.startsWith('/v2/agent-sessions?'))return [{id:'s1',title:'Existing conversation',configurationVersion:1}]
  if(path==='/v2/agent-sessions/s1')return {id:'s1',agentName:'Configured Coach',title:'Existing conversation',configurationVersion:1}
  if(path.endsWith('/interactions'))return []
  if(path.endsWith('/messages')){expect(options.method).toBe('POST');return run}
  if(path.endsWith('/active-run'))return run
  throw new Error('Unexpected route '+path)
 })
 streamExecution.mockImplementation(()=>new Promise(resolve=>{release=resolve}))
 page('/agents/a1?session=s1');await screen.findByRole('heading',{name:'Configured Coach'})
 expect(screen.getAllByRole('dialog',{name:'Career Agent'})).toHaveLength(1)
 fireEvent.change(screen.getByLabelText('Message to Agent'),{target:{value:'My question'}});fireEvent.keyDown(screen.getByLabelText('Message to Agent'),{key:'Enter'})
 await waitFor(()=>expect(streamExecution).toHaveBeenCalledTimes(1))
 const signal=streamExecution.mock.calls[0][1].signal
 fireEvent.click(screen.getByRole('button',{name:'Expand'}));expect(document.querySelector('.student-agent-popup--expanded')).toBeTruthy()
 fireEvent.click(screen.getByRole('button',{name:'Close',exact:true}));expect(signal.aborted).toBe(false);expect(screen.queryByRole('dialog',{name:'Career Agent'})).toBeNull()
 fireEvent.click(screen.getByRole('button',{name:'Career Agent'}));expect(screen.getByLabelText('Message to Agent').value).toBe('')
 fireEvent.change(screen.getByRole('combobox',{name:'Website language'}),{target:{value:'vi'}})
 await screen.findByRole('button',{name:'Lịch sử'});expect(streamExecution).toHaveBeenCalledTimes(1)
 expect(apiRequest.mock.calls.filter(([p])=>p.endsWith('/messages'))).toHaveLength(1);expect(apiRequest.mock.calls.some(([p])=>p.endsWith('/cancel'))).toBe(false)
 release()
})
