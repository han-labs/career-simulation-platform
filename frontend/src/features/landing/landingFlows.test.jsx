// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from '../../app/App.jsx'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import { apiRequest, invalidateAccount } from '../../shared/api/httpClient.js'
import CatalogSection from './components/CatalogSection.jsx'
import CompaniesSection from './components/CompaniesSection.jsx'
import SimulationCard from '../simulations/components/SimulationCard.jsx'

vi.mock('../../shared/api/httpClient.js', async original => ({ ...await original(), apiRequest: vi.fn() }))
vi.mock('../../shared/api/coreApi.js', async original => ({ ...await original(), coreEnabled: true }))
vi.mock('../admin/agentPlatform/platformApi.js', async original => ({ ...await original(), agentPlatformEnabled: true }))
const unauthorized = () => Object.assign(new Error('Sign in is required.'), { status: 401 })
const publicSimulation = { id: '17', slug: 'audited-published-simulation', scenarioVersionId: '41', enterpriseName: 'Reviewed API provider',
  metadata: { title: 'Audited API simulation', summary: 'Original published practice.', targetRole: 'IT learner', careerTrack: 'BACKEND_DEVELOPMENT', estimatedMinutes: 30, difficulty: 'INTRODUCTORY', introduction: 'Practice safely.', learningObjectives: 'Reflect on a short task.' }, taskOutline: [], skills: [], media: { cardImageUrl: '/approved-preview.webp', companyLogoUrl: '/approved-provider.png' } }
const normalized = { ...publicSimulation.metadata, id: publicSimulation.id, slug: publicSimulation.slug, enterpriseName: publicSimulation.enterpriseName, media: publicSimulation.media }
let actor, attempt, result
const questions = ['R','I','A','S','E','C'].map((dimension,index) => ({ id: `q${index}`, position: index+1, required: true, dimension, prompt: `Reviewed fixture question ${dimension}` }))
function view(path = '/') { return render(<I18nProvider><MemoryRouter initialEntries={[path]}><App /></MemoryRouter></I18nProvider>) }
function component(children) { return render(<I18nProvider><MemoryRouter>{children}</MemoryRouter></I18nProvider>) }
beforeEach(() => {
  localStorage.clear(); sessionStorage.clear(); vi.clearAllMocks()
  actor = null; attempt = null; result = null
  vi.spyOn(window,'scrollTo').mockImplementation(()=>{})
  HTMLElement.prototype.scrollIntoView = vi.fn()
  apiRequest.mockImplementation(async (path, options={}) => {
    if (path === '/v2/me') { if (!actor) throw unauthorized(); return actor }
    if (path === '/v2/auth/providers') return { google: false }
    if (path === '/v2/auth/login') { actor={id:'student-1',role:'STUDENT',consents:[]}; invalidateAccount(); return actor }
    if (path === '/v2/auth/logout') { actor=null; invalidateAccount(); return {loggedOut:true} }
    if (path === '/v2/scenarios') return [publicSimulation]
    if (path.startsWith('/v2/scenarios?')) return path.includes('paged=true') ? {items:[publicSimulation],nextCursor:null} : [publicSimulation]
    if (path === '/v2/scenarios/audited-published-simulation') return publicSimulation
    if (path === '/v2/assessment-types') return [{id:'type-1',code:'RIASEC',name:'RIASEC',activeVersionId:'version-1'}]
    if (path === '/v2/assessment-attempts') return attempt ? [{id:attempt.id,assessmentTypeCode:'RIASEC',assessmentVersionId:'version-1',status:attempt.status,startedAt:'2026-10-01T00:00:00Z'}] : []
    if (path === '/v2/assessment-versions/version-1/attempts') { attempt={id:'attempt-1',assessmentVersionId:'version-1',status:'IN_PROGRESS',revision:0,questions,responses:[]}; return attempt }
    if (path === '/v2/assessment-attempts/attempt-1') return attempt
    if (path === '/v2/assessment-attempts/attempt-1/responses') { attempt={...attempt,revision:attempt.revision+1,responses:JSON.parse(options.body).items}; return attempt }
    if (path === '/v2/assessment-attempts/attempt-1/submit') {
      attempt={...attempt,status:'SUBMITTED'}
      result={attemptId:attempt.id,assessmentTypeCode:'RIASEC',assessmentVersionId:'version-1',calculatedAt:'2026-10-01T00:01:00Z',resultData:{dimensions:questions.map(q=>({code:q.dimension,raw:3,min:1,max:5})),topDimensions:['R']}}
      return result
    }
    if (path === '/v2/assessment-attempts/attempt-1/result') return result
    if (path === '/v2/guidance/dashboard') { if (!actor) throw unauthorized(); return {assessments:[],enrollments:[],participation:[],completions:[],legacyEvaluations:[],activePlan:null} }
    throw new Error(`Unexpected mocked boundary: ${path}`)
  })
})
afterEach(()=>{cleanup();vi.restoreAllMocks();vi.unstubAllGlobals()})

it('connects the one guest account action to the existing AccountPage form', async()=>{
  view()
  const action=await screen.findByRole('link',{name:'Sign In / Sign Up'})
  expect(screen.getAllByRole('link',{name:'Sign In / Sign Up'})).toHaveLength(1)
  expect(action.getAttribute('href')).toBe('/account')
  fireEvent.click(action)
  await screen.findByRole('heading',{name:'Welcome back'})
  expect(screen.getByLabelText('Email *')).toBeTruthy()
  expect(screen.getByRole('button',{name:'Register',exact:true})).toBeTruthy()
})

it('returns guest Start to the exact simulation after account-generation remount without automatic enrollment',async()=>{
  view()
  fireEvent.click(await screen.findByRole('link',{name:'View Simulation: Audited API simulation'}))
  fireEvent.click(await screen.findByRole('button',{name:'Start free simulation'}))
  await screen.findByRole('heading',{name:'Welcome back'})
  expect(sessionStorage.getItem('careersim_pending_simulation_path')).toBe('/simulations/audited-published-simulation')
  fireEvent.change(screen.getByLabelText('Email *'),{target:{value:'synthetic@example.test'}})
  fireEvent.change(screen.getByLabelText('Password *'),{target:{value:'Synthetic-password-123'}})
  fireEvent.submit(screen.getAllByRole('button',{name:'Sign in',exact:true}).find(button=>button.closest('form')).closest('form'))
  await screen.findByRole('heading',{name:'Audited API simulation',level:1})
  expect(sessionStorage.getItem('careersim_pending_simulation_path')).toBeNull()
  expect(screen.getByRole('button',{name:'Start free simulation'})).toBeTruthy()
  expect(apiRequest.mock.calls.some(([path])=>path.includes('/enrollments'))).toBe(false)
  expect(screen.getByRole('button',{name:'Sign out'})).toBeTruthy()
})

it('retains real assessment selection, required-answer validation, state across VI/EN, server draft and server result flow from the new header',async()=>{
  actor={id:'student-1',role:'STUDENT',consents:[]}
  view()
  fireEvent.click(screen.getByRole('link',{name:'Career Quizzes'}))
  fireEvent.click(await screen.findByRole('button',{name:/Read instructions.*RIASEC Assessment/}))
  fireEvent.click(screen.getByRole('button',{name:'Start or resume'}))
  await screen.findByRole('button',{name:'Submit Answers'})
  fireEvent.click(screen.getByRole('button',{name:'Submit Answers'}))
  expect(apiRequest.mock.calls.some(([path])=>path.endsWith('/submit'))).toBe(false)
  for(const group of screen.getAllByRole('radiogroup')) fireEvent.click(within(group).getByRole('radio',{name:'Neutral',exact:true}))
  fireEvent.change(screen.getByRole('combobox',{name:'Website language'}),{target:{value:'vi'}})
  expect(screen.getAllByRole('radio',{checked:true})).toHaveLength(6)
  fireEvent.change(screen.getByRole('combobox',{name:'Ngôn ngữ website'}),{target:{value:'en'}})
  fireEvent.click(screen.getByRole('button',{name:'Save draft'}))
  await screen.findByText('Saved on the server.')
  expect(result).toBeNull()
  fireEvent.click(screen.getByRole('button',{name:'Submit Answers'}))
  await screen.findByText('Saved result')
  expect(result.attemptId).toBe('attempt-1')
  expect(apiRequest).toHaveBeenCalledWith('/v2/assessment-attempts/attempt-1/submit',expect.objectContaining({method:'POST',headers:expect.objectContaining({'If-Match':'1'})}))
})

it('logs out from Dashboard to anonymous Home without restoring privileged state',async()=>{
  actor={id:'student-1',role:'STUDENT',consents:[]}
  view('/dashboard')
  await screen.findByRole('heading',{name:'Assessment results'})
  fireEvent.click(await screen.findByRole('button',{name:'Sign out'}))
  await screen.findByRole('heading',{name:/Explore real careers/})
  expect(screen.getByRole('link',{name:'Sign In / Sign Up'})).toBeTruthy()
  expect(apiRequest).toHaveBeenCalledWith('/v2/auth/logout',expect.objectContaining({method:'POST',body:'{}'}))
  expect(screen.queryByRole('heading',{name:'Assessment results'})).toBeNull()
})

it('reuses shared error/retry and truthful empty states without fallback simulations',async()=>{
  let offline=true
  const existing=apiRequest.getMockImplementation()
  apiRequest.mockImplementation((path,...args)=>path==='/v2/scenarios' ? offline ? Promise.reject(new Error('offline')) : Promise.resolve([]) : existing(path,...args))
  view()
  await screen.findByRole('heading',{name:'Simulations are unavailable'})
  expect(screen.queryByText('Backend API Triage: Incident Response')).toBeNull()
  offline=false
  fireEvent.click(screen.getByRole('button',{name:'Try again'}))
  await screen.findByText('No simulations are currently available. Please check back later.')
  expect(screen.queryByText('Audited API simulation')).toBeNull()
})

it('shows every published result while retaining the same original card links and metadata',()=>{
  const simulations=Array.from({length:8},(_,index)=>({...normalized,id:String(index),slug:`actual-${index}`,title:`Published ${index}`}))
  const shown=component(<CatalogSection simulations={simulations} catalogStatus="ready" />)
  expect(shown.container.querySelectorAll('.simulation-card')).toHaveLength(8)
  expect(screen.getByRole('link',{name:'View Simulation: Published 7'}).getAttribute('href')).toBe('/simulations/actual-7')
  expect(screen.getAllByText('Backend Development')).toHaveLength(8)
  expect(screen.getAllByText('Introductory',{exact:false})).toHaveLength(8)
})

it.each([false,true])('supports previous/next, keyboard and reduced motion on the existing cards (%s)',reduced=>{
  vi.stubGlobal('matchMedia',vi.fn(()=>({matches:reduced})))
  component(<CatalogSection simulations={[normalized]} catalogStatus="ready" />)
  const region=screen.getByRole('region',{name:'Career simulations'})
  Object.defineProperties(region,{clientWidth:{value:400},scrollWidth:{value:1600}})
  region.scrollBy=vi.fn();region.scrollTo=vi.fn()
  fireEvent.click(screen.getByRole('button',{name:'Next simulations'}))
  expect(region.scrollBy).toHaveBeenLastCalledWith({left:320,behavior:reduced?'instant':'smooth'})
  fireEvent.keyDown(region,{key:'ArrowLeft'})
  expect(region.scrollBy).toHaveBeenLastCalledWith({left:-320,behavior:reduced?'instant':'smooth'})
  fireEvent.keyDown(region,{key:'End'})
  expect(region.scrollTo).toHaveBeenLastCalledWith({left:1600,behavior:'instant'})
  fireEvent.keyDown(region,{key:'Home'})
  expect(region.scrollTo).toHaveBeenLastCalledWith({left:0,behavior:'instant'})
})

it('uses only real provider metadata and handles missing/broken logos without fictional affiliations',()=>{
  const shown=component(<CompaniesSection simulations={[normalized,normalized,{id:'no-provider'}]} />)
  expect(screen.getAllByAltText('Reviewed API provider')).toHaveLength(1)
  fireEvent.error(screen.getByAltText('Reviewed API provider'))
  expect(screen.getByText('Reviewed API provider')).toBeTruthy()
  shown.rerender(<I18nProvider><MemoryRouter><CompaniesSection simulations={[]} /></MemoryRouter></I18nProvider>)
  expect(screen.getByText('Explore the published simulation catalogue.')).toBeTruthy()
  expect(shown.container.querySelector('img')).toBeNull()
})

it('keeps the default main SimulationCard unchanged and makes only compact presentation opt in',()=>{
  const shown=component(<SimulationCard simulation={normalized} />)
  expect(shown.container.querySelector('.simulation-card--compact')).toBeNull()
  expect(screen.getByText('Original published practice.')).toBeTruthy()
  expect(screen.getByRole('link',{name:'View Simulation'}).getAttribute('href')).toBe('/simulations/audited-published-simulation')
})
