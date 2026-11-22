// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiRequest } from '../../shared/api/httpClient.js'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import EnterpriseAnalyticsPage from './EnterpriseAnalyticsPage.jsx'
import EnterpriseWorkspace from './EnterpriseWorkspace.jsx'

vi.mock('../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
const path = '/v2/enterprises/11/analytics'
const member = { enterpriseId: '11', name: 'Example Enterprise', membershipStatus: 'ACTIVE' }
const result = {
  programs: [{ scenarioId: '4', title: 'Example Program' }],
  counts: { enrollments: 6, started: 4, completed: 2 },
  detailStatus: 'AVAILABLE',
  tasks: [
    { taskId: '31', programTitle: 'Example Program', versionNumber: 1, position: 1, taskTitle: 'Review', notSubmitted: 2, drafts: 1, submitted: 1 },
    { taskId: '32', programTitle: 'Example Program', versionNumber: 2, position: 1, taskTitle: 'Review', notSubmitted: 0, drafts: 1, submitted: 1 },
  ],
}

function open(Component = EnterpriseAnalyticsPage, locale = 'en') {
  return render(<MemoryRouter><I18nProvider initialLocale={locale}><Component /></I18nProvider></MemoryRouter>)
}

beforeEach(() => {
  apiRequest.mockReset()
  apiRequest.mockImplementation(url => {
    if (url === '/v2/enterprises/me') return Promise.resolve(member)
    if (url === path) return Promise.resolve(result)
    throw new Error('Unexpected analytics request')
  })
})
afterEach(cleanup)

it('starts at the current enterprise and renders localized totals and separate task versions', async () => {
  open(EnterpriseAnalyticsPage, 'vi')
  expect(await screen.findByText(/^33,3\s?%$/)).toBeTruthy()
  expect(screen.getAllByRole('heading', { name: '1. Review' })).toHaveLength(2)
  expect(screen.getByText(/Phiên bản 1/)).toBeTruthy()
  expect(screen.getByText(/Phiên bản 2/)).toBeTruthy()
  expect(apiRequest.mock.calls.map(([url]) => url)).toEqual(['/v2/enterprises/me', path])
})

it('uses committed filters for both reads and ignores a superseded response', async () => {
  const defaultRead = apiRequest.getMockImplementation()
  let finish
  apiRequest.mockImplementation(url => {
    if (url.includes('/evidence')) return Promise.resolve({ items: [] })
    if (url.includes('status=ENROLLED')) return new Promise(resolve => { finish = resolve })
    if (url.includes('status=COMPLETED')) return Promise.resolve(result)
    return defaultRead(url)
  })
  open()
  await screen.findByText('33.3%')
  fireEvent.change(screen.getByLabelText('Program'), { target: { value: '4' } })
  fireEvent.change(screen.getByLabelText('Through date'), { target: { value: '2026-10-20' } })
  fireEvent.change(screen.getByLabelText('Enrollment status'), { target: { value: 'ENROLLED' } })
  fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
  const oldCall = apiRequest.mock.calls.find(([url]) => url.includes('status=ENROLLED'))
  fireEvent.change(screen.getByLabelText('Enrollment status'), { target: { value: 'COMPLETED' } })
  fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
  expect(oldCall[1].signal.aborted).toBe(true)
  await screen.findByText('33.3%')
  finish({ ...result, counts: { enrollments: 90, started: 0, completed: 0 } })
  fireEvent.click(screen.getByRole('button', { name: 'View shared evidence' }))
  await screen.findByText('Individual participant evidence is not available for this view.')
  expect(apiRequest.mock.calls.some(([url]) => url === path + '/evidence?scenarioId=4&to=2026-10-20&status=COMPLETED')).toBe(true)
  expect(screen.queryByText('90')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))
  await screen.findByText('33.3%')
  expect(apiRequest.mock.calls.at(-1)[0]).toBe(path)
})

it('distinguishes empty and small segments and keeps totals on evidence failure', async () => {
  const defaultRead = apiRequest.getMockImplementation()
  apiRequest.mockImplementation(url => {
    if (url.includes('/evidence')) return Promise.reject(Error('maintenance'))
    if (url.includes('status=ENROLLED')) return Promise.resolve({ ...result, tasks: [], counts: { enrollments: 0, started: 0, completed: 0 }, detailStatus: 'EMPTY' })
    if (url === path) return Promise.resolve({ ...result, tasks: [], detailStatus: 'INSUFFICIENT' })
    return defaultRead(url)
  })
  open()
  expect(await screen.findByText('Insufficient data for the selected segment.')).toBeTruthy()
  expect(screen.getByText('—')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'View shared evidence' }))
  await screen.findByRole('alert')
  expect(screen.getByText('6')).toBeTruthy()
  fireEvent.change(screen.getByLabelText('Enrollment status'), { target: { value: 'ENROLLED' } })
  fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
  expect(await screen.findByText('No activity is available for this selection.')).toBeTruthy()
  expect(screen.queryByText('Insufficient data for the selected segment.')).toBeNull()
})

it('clears stale evidence when reloading after consent revocation', async () => {
  const defaultRead = apiRequest.getMockImplementation()
  let shared = true
  apiRequest.mockImplementation(url => url.includes('/evidence')
    ? Promise.resolve({ items: shared ? [{ reference: '88', programTitle: 'Sample completion', versionNumber: 1, completedAt: '2026-10-11T12:00:00Z' }] : [] })
    : defaultRead(url))
  open()
  fireEvent.click(await screen.findByRole('button', { name: 'View shared evidence' }))
  await screen.findByText('Sample completion')
  shared = false
  fireEvent.click(screen.getByRole('button', { name: 'Refresh evidence' }))
  expect(screen.queryByText('Sample completion')).toBeNull()
  await screen.findByText('Individual participant evidence is not available for this view.')
})

it('stops at membership denial and never requests tenant data', async () => {
  apiRequest.mockResolvedValueOnce({ ...member, membershipStatus: 'SUSPENDED' })
  open()
  expect((await screen.findByRole('alert')).textContent).toBe('You do not have permission to view these analytics.')
  expect(apiRequest).toHaveBeenCalledTimes(1)
})

it('opens the authoring form independently of analytics', async () => {
  apiRequest.mockImplementation(url => Promise.resolve({
    '/v2/me': { id: '7' },
    '/v2/enterprises/me': member,
    '/v2/enterprise/scenario-versions': [],
    '/v2/career-skills': [],
  }[url]))
  open(EnterpriseWorkspace)
  expect(await screen.findByLabelText('Title')).toBeTruthy()
  expect(screen.getByRole('link', { name: 'View analytics' }).getAttribute('href')).toBe('/enterprise/analytics')
  await waitFor(() => expect(apiRequest).toHaveBeenCalledTimes(4))
})
