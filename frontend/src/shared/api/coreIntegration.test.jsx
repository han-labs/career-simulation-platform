// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import I18nProvider from '../i18n/I18nProvider.jsx'
import { apiRequest } from './httpClient.js'
import { draftKey } from './coreApi.js'
import CoreSimulationPage from '../../features/simulations/pages/CoreSimulationPage.jsx'
import VersionedAssessmentPage from '../../features/assessment/pages/VersionedAssessmentPage.jsx'
import AccountPage from '../../features/identity/AccountPage.jsx'
vi.mock('./httpClient.js', () => ({ apiRequest: vi.fn(), API_BASE_URL: '/api' }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(cleanup)
function page(component, path = '/test', locale = 'en') { return render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={[path]}><Routes><Route path="*" element={component} /></Routes></MemoryRouter></I18nProvider>) }
const definition = { id: '9', name: 'Response', format: 'TEXT', required: true, validationConfig: { maxLength: 1000 } }
const task = { id: '4', title: 'Fixture task', instructions: 'Write actual work', resources: [], definitions: [definition], translations: {} }
const enrollment = { id: '2', scenarioVersionId: '3', requiredTasks: 1, finalRequiredTasks: 0, metadata: { title: 'Fixture', translations: {} }, tasks: [task] }
it('keeps a scoped draft after failed persistence and never displays a fabricated final result', async () => {
  apiRequest.mockImplementation(path => path === '/v2/me' ? Promise.resolve({ id: '7' }) : path === '/v2/enrollments/2' ? Promise.resolve(enrollment) : path.endsWith('/submission') ? Promise.resolve({ status: 'DRAFT', revision: 0, items: [] }) : Promise.reject(new Error('Unavailable')))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  const input = await screen.findByRole('textbox', { name: 'Response *' })
  fireEvent.change(input, { target: { value: 'Keep my work' } })
  apiRequest.mockRejectedValueOnce(new Error('Synthetic save failure'))
  fireEvent.click(screen.getByRole('button', { name: 'Save draft' }))
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(input.value).toBe('Keep my work')
  expect(JSON.parse(localStorage.getItem(draftKey('7', 'task', '2_4'))).values['9'].contentText).toBe('Keep my work')
  expect(screen.queryByText('Simulation completed')).toBeNull()
  expect(screen.queryByRole('button', { name: 'View model answer' })).toBeNull()
})
it('renders immutable final work without merging a later local draft', async () => {
  localStorage.setItem(draftKey('7', 'task', '2_4'), JSON.stringify({ values: { 9: { contentText: 'Unaccepted local text' } } }))
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/me' ? { id: '7' } : path === '/v2/enrollments/2' ? enrollment : { status: 'FINAL', revision: 2, items: [{ definitionId: '9', contentText: 'Accepted work' }] }))
  page(<CoreSimulationPage />, '/test?enrollment=2')
  const input = await screen.findByRole('textbox', { name: 'Response *' })
  expect(input.value).toBe('Accepted work')
  expect(input.closest('fieldset').disabled).toBe(true)
  expect(screen.getByRole('button', { name: 'View model answer' })).toBeTruthy()
})
it('isolates owner drafts and exposes an empty assessment catalog truthfully in Vietnamese', async () => {
  localStorage.setItem(draftKey('8', 'assessment', '1'), JSON.stringify({ answers: { 1: 5 } }))
  apiRequest.mockImplementation(path => Promise.resolve(path === '/v2/me' ? { id: '7' } : []))
  page(<VersionedAssessmentPage />, '/assessment', 'vi')
  expect(await screen.findByText('Chưa có phiên bản đánh giá đã rà soát được công bố.')).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Bắt đầu hoặc tiếp tục' }).disabled).toBe(true)
  expect(localStorage.getItem(draftKey('8', 'assessment', '1'))).toContain('5')
})
it('requires explicit unchecked consent and leaves the form available after denial', async () => {
  apiRequest.mockResolvedValueOnce({ id: '7', displayName: 'Synthetic', role: 'STUDENT', consents: [] })
  page(<AccountPage />)
  const check = await screen.findByRole('checkbox')
  expect(check.checked).toBe(false); expect(check.required).toBe(true)
  fireEvent.click(check)
  apiRequest.mockRejectedValueOnce(new Error('Synthetic consent rejection'))
  fireEvent.submit(check.closest('form'))
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(check.checked).toBe(true)
  await waitFor(() => expect(apiRequest).toHaveBeenCalledWith('/v2/me/consents', expect.objectContaining({ method: 'POST', body: expect.stringContaining('"confirmed":true') })))
})
