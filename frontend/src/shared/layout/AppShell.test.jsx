// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { useSyncExternalStore } from 'react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import I18nProvider from '../i18n/I18nProvider.jsx'
import AppShell from './AppShell.jsx'
import { apiRequest, getAccountGeneration, invalidateAccount, subscribeAccount } from '../api/httpClient.js'

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
    {['/', '/assessment', '/simulations', '/simulations/demo', '/simulations/demo/results', '/guidance', '/enterprise', '/admin', '/account'].map(route => <Route key={route} path={route} element={<Destination />} />)}
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
