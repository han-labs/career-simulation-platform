// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../../shared/api/httpClient.js'
import AgentUsagePage from '../agentPlatform/AgentUsagePage.jsx'
import AdminStudioLayout from '../agentPlatform/AdminStudioLayout.jsx'
import { emptyUsageFixture, usageFixture } from './usageFixture.js'
import { initialFilters, usageRequest, usageUrl } from './usageDashboard.js'
import { readFileSync } from 'node:fs'

vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn(), getAccountGeneration: () => 0 }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear(); apiRequest.mockResolvedValue(usageFixture()) })
afterEach(cleanup)
function page(locale = 'en', shell = false) {
  return render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={['/admin/usage']}>{shell ? <Routes><Route path="admin" element={<AdminStudioLayout />}><Route path="usage" element={<AgentUsagePage />} /><Route path="knowledge" element={<h1>Existing Knowledge Bases</h1>} /><Route path="agents" element={<h1>Existing AI Agents</h1>} /></Route></Routes> : <AgentUsagePage />}</MemoryRouter></I18nProvider>)
}
const loaded = () => screen.findByRole('heading', { name: 'Recent AI executions' })
const metric = name => screen.getByRole('heading', { name, exact: true }).parentElement
const query = (index = -1) => new URL(apiRequest.mock.calls.at(index)[0], 'http://localhost').searchParams
it('loads once on entry with a bounded server request and a clear loading state', async () => {
  let deliver; apiRequest.mockImplementation(() => new Promise(resolve => { deliver = resolve }))
  page(); expect(screen.getByRole('status').textContent).toContain('Loading AI usage...')
  expect(query().get('limit')).toBe('20'); expect(screen.queryByRole('table')).toBeNull()
  await act(async () => deliver(usageFixture())); await loaded(); expect(apiRequest).toHaveBeenCalledTimes(1)
})
it('distinguishes empty from error and avoids a fake zero activity chart', async () => {
  apiRequest.mockResolvedValue(emptyUsageFixture()); page()
  await screen.findByRole('heading', { name: 'No AI usage records are available for this period.' })
  expect(metric('Requests').textContent).toContain('0'); expect(screen.queryByRole('img')).toBeNull()
  expect(screen.queryByRole('alert')).toBeNull(); expect(metric('Estimated Cost').textContent).toContain('Not available')
})
it('shows safe API error and Retry rather than presenting an empty dataset', async () => {
  apiRequest.mockRejectedValueOnce(new Error('PRIVATE_PROVIDER_ERROR')).mockResolvedValue(usageFixture()); page()
  expect((await screen.findByRole('alert')).textContent).toContain('AI usage data could not be loaded.')
  expect(screen.queryByText('PRIVATE_PROVIDER_ERROR')).toBeNull(); expect(screen.queryByText('No AI usage records are available for this period.')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Retry' })); await loaded(); expect(apiRequest).toHaveBeenCalledTimes(2)
})
it('renders the server request count without deriving it from a paged list', async () => {
  page(); await loaded(); expect(metric('Requests').querySelector('.usage-kpi-value').textContent).toBe('4')
  expect(within(screen.getByRole('table', { name: 'Recent executions, newest first (UTC)' })).getAllByRole('row')).toHaveLength(3)
})
it('labels recorded estimated cost and partial/unavailable coverage honestly', async () => {
  page(); await loaded(); const card = metric('Estimated Cost'); expect(card.textContent).toContain('$1.27')
  for (const text of ['not provider billing', '1 unavailable', '1 partial', '1 not applicable']) expect(card.textContent).toContain(text)
})
it('uses canonical measured execution latency in milliseconds with coverage', async () => {
  page(); await loaded(); expect(metric('Average Latency').textContent).toContain('240 ms')
  expect(metric('Average Latency').textContent).toContain('3 measured finished executions')
  expect(metric('Average Latency').textContent).toContain('1 missing measurements')
})
it('distinguishes failed and refused with separate counts and status labels', async () => {
  page(); await loaded(); expect(metric('Failed / Refused').querySelector('.usage-kpi-value').textContent).toBe('1 / 1')
  const rows = within(screen.getByRole('table', { name: 'Recent executions, newest first (UTC)' })).getAllByRole('row')
  expect(rows[1].textContent).toContain('Failed'); expect(rows[2].textContent).toContain('Refused')
  expect(within(metric('Outcome health')).getByText('Cancelled')).toBeTruthy()
})
it.each([['24h', 1], ['7d', 7], ['30d', 30], ['366d', 366]])('applies time preset %s to server ISO bounds', async (range, days) => {
  page(); await loaded(); fireEvent.change(screen.getByLabelText('Time range'), { target: { value: range } })
  fireEvent.click(screen.getByRole('button', { name: 'Apply filters' })); await loaded()
  expect((Date.parse(query().get('to')) - Date.parse(query().get('from'))) / 86400000).toBe(days)
})
it.each([['Agent', 'agent-a', 'agent'], ['Provider', 'gemini', 'provider'], ['Model', 'openai/synthetic', 'model'], ['Outcome', 'REFUSED', 'outcome']])('sends %s filtering to the server only after Apply', async (label, value, key) => {
  page(); await loaded(); fireEvent.change(screen.getByLabelText(label, { exact: true }), { target: { value } })
  expect(apiRequest).toHaveBeenCalledTimes(1); expect(screen.getByText('Apply filters to update results.')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Apply filters' })); await loaded(); expect(query().get(key)).toBe(value)
})
it('clears an incompatible model when provider changes', async () => {
  page(); await loaded(); fireEvent.change(screen.getByLabelText('Model', { exact: true }), { target: { value: 'openai/synthetic' } })
  fireEvent.change(screen.getByLabelText('Provider', { exact: true }), { target: { value: 'gemini' } })
  expect(screen.getByLabelText('Model', { exact: true }).value).toBe('')
  expect(within(screen.getByLabelText('Model', { exact: true })).queryByText('openai/synthetic')).toBeNull()
})
it('renders only actual trend DTO buckets and an accessible exact-value equivalent', async () => {
  page(); await loaded(); expect(screen.getByRole('img', { name: /Request volume chart/ }).children).toHaveLength(2)
  fireEvent.click(screen.getByText('View trend values'))
  const values = screen.getByRole('table', { name: 'Requests per UTC bucket' })
  expect(within(values).getAllByRole('row')).toHaveLength(3); expect(within(values).getByText('3')).toBeTruthy()
})
it('shows tools without arguments, prompt fingerprint and safe historical/KB identity', async () => {
  page(); await loaded(); const table = screen.getByRole('table', { name: 'Recent executions, newest first (UTC)' }); expect(table.textContent).toContain('Career guide')
  expect(table.textContent).toContain('Knowledge Base test'); expect(table.textContent).toContain('kb.search · simulation.explore')
  fireEvent.click(within(table).getAllByText('Details')[0]); expect(within(table).getByText('safe-prompt-fingerprint')).toBeTruthy()
  expect(within(table).getByText('PROVIDER_UNAVAILABLE')).toBeTruthy()
})
it('keeps the same interval while using bounded next/previous cursors and resets on Apply', async () => {
  apiRequest.mockResolvedValueOnce(usageFixture({ nextCursor: 'cursor-a' })).mockResolvedValue(usageFixture())
  page(); await loaded(); const first = query(); expect(screen.getByRole('button', { name: 'Previous page' }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Next page' })); await loaded()
  expect(query().get('cursor')).toBe('cursor-a'); expect(query().get('from')).toBe(first.get('from')); expect(query().get('to')).toBe(first.get('to'))
  fireEvent.click(screen.getByRole('button', { name: 'Previous page' })); await loaded(); expect(query().has('cursor')).toBe(false)
  fireEvent.change(screen.getByLabelText('Outcome'), { target: { value: 'FAILED' } }); fireEvent.click(screen.getByRole('button', { name: 'Apply filters' })); await loaded()
  expect(query().has('cursor')).toBe(false); expect(screen.getByRole('button', { name: 'Previous page' }).disabled).toBe(true)
})
it('does not convert unavailable metrics to zero', async () => {
  const data = usageFixture(); data.summary.recordedEstimatedCostUsd = null; data.summary.averageLatencyMs = null
  apiRequest.mockResolvedValue(data); page(); await loaded()
  for (const label of ['Estimated Cost', 'Average Latency']) expect(metric(label).querySelector('.usage-kpi-value').textContent).toBe('Not available')
})
it('does not render private fields accidentally added to an API object', async () => {
  const data = usageFixture(); Object.assign(data.executions[0], { inputSnapshot: { query: 'PRIVATE_STUDENT_TEXT' }, outputSnapshot: { text: 'PRIVATE_REASONING' }, pricingSnapshot: { key: 'PRIVATE_KEY' }, initiatedByUserId: 'PRIVATE_STUDENT_ID', promptTemplate: 'PRIVATE_PROMPT', toolStepsAndSources: [{ arguments: 'PRIVATE_ARGUMENTS' }] })
  data.executions[0].failureCategory = 'PRIVATE_FAILURE'; data.executions[0].tools.push('PRIVATE_TOOL')
  apiRequest.mockResolvedValue(data); const view = page(); await loaded(); expect(view.container.textContent).not.toContain('PRIVATE_')
})
it('uses feature-scoped responsive stacked-row CSS and semantic column labels', async () => {
  const view = page(); await loaded(); expect(view.container.querySelector('.ai-usage .usage-kpis').children).toHaveLength(4)
  expect(view.container.querySelector('td[data-label="Latency"]')).toBeTruthy()
  const css = readFileSync('src/features/admin/usage/usageDashboard.css', 'utf8')
  expect(css).toContain('@media(max-width:760px)'); expect(css).toContain('content:attr(data-label)')
  expect(css).not.toMatch(/^\s*(body|button|input|table)\s*[{,]/m)
})
it.each([['Knowledge Bases', 'Existing Knowledge Bases', '/admin/knowledge'], ['AI Agents', 'Existing AI Agents', '/admin/agents']])('retains the Admin Studio sibling %s route and active Usage navigation', async (label, heading, href) => {
  page('en', true); await loaded(); const nav = screen.getByRole('navigation', { name: 'Admin Studio navigation' })
  expect(within(nav).getByRole('link', { name: 'AI Usage' }).getAttribute('aria-current')).toBe('page')
  expect(within(nav).getAllByRole('link').map(link => link.getAttribute('href'))).toEqual(['/admin/knowledge', '/admin/agents', '/admin/usage'])
  expect(within(nav).getByRole('link', { name: label }).getAttribute('href')).toBe(href)
  fireEvent.click(within(nav).getByRole('link', { name: label })); await screen.findByRole('heading', { name: heading })
})
it('ignores stale responses after filters change or the page unmounts', async () => {
  let deliver; apiRequest.mockImplementationOnce(() => new Promise(resolve => { deliver = resolve })).mockResolvedValue(emptyUsageFixture())
  const view = page(); fireEvent.change(screen.getByLabelText('Time range'), { target: { value: '24h' } })
  fireEvent.submit(screen.getByRole('form', { name: 'AI usage filters' })); await screen.findByText('No AI usage records are available for this period.')
  await act(async () => deliver(usageFixture())); expect(screen.queryByRole('heading', { name: 'Recent AI executions' })).toBeNull()
  view.unmount(); expect(apiRequest.mock.calls.every(([path]) => !path.includes('interactions') && !path.includes('agent-executions/'))).toBe(true)
})
it('renders Vietnamese dashboard labels without changing metric semantics', async () => {
  page('vi'); await screen.findByRole('heading', { name: 'Lượt thực thi AI gần đây' })
  expect(screen.getByRole('heading', { name: 'Độ trễ trung bình' }).parentElement.textContent).toContain('240 ms')
  expect(screen.getByRole('button', { name: 'Áp dụng bộ lọc' })).toBeTruthy()
})
it('preserves UTC elapsed windows across local timezone/daylight changes', () => {
  const now = new Date('2026-03-08T07:30:00Z'), request = usageRequest(initialFilters, now)
  expect(request.to).toBe(now.toISOString()); expect(Date.parse(request.to) - Date.parse(request.from)).toBe(7 * 86400000)
  expect(usageUrl(request, null)).not.toContain('undefined')
})
