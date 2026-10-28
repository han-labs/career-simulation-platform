// @vitest-environment jsdom
import { afterEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import InsightReportView, { SavedInsightReports } from './InsightReportView.jsx'
import { apiRequest } from '../../shared/api/httpClient.js'
import { downloadInsightReport, insightReportHtml } from './insightReportHtml.js'
vi.mock('../../shared/api/httpClient.js',()=>({apiRequest:vi.fn()}))
const report = { structuredInsights: { summary: 'Khám phá nghề nghiệp <script>alert("unsafe")</script>', hypotheses: [{ direction: 'Dữ liệu & phân tích', reason: '<img src=x onerror=alert(1)>', evidenceRefs: ['K1'] }], strengthsPracticed: ['SQL'], difficulties: ['Thời gian'], limitations: ['Limited evidence'], nextSteps: ['Practice'], disclaimer: 'Exploration only', evidenceRefs: ['K1'] } }
afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals(); vi.useRealTimers() })
it('renders readable report sections and treats model text as plain text', () => {
  render(<I18nProvider><InsightReportView report={report} onClose={() => {}} /></I18nProvider>)
  for (const name of ['Summary', 'Directions and evidence', 'Skills practiced', 'Difficulties', 'Limitations', 'Suggested next steps', 'Source references']) expect(screen.getByRole('heading', { name })).toBeTruthy()
  expect(screen.getByText(report.structuredInsights.summary)).toBeTruthy(); expect(document.querySelector('dialog script, dialog img, dialog pre')).toBeNull()
})
it('downloads UTF-8 HTML, escapes every text field, uses no scripts/assets and revokes the URL', () => {
  vi.useFakeTimers()
  const createObjectURL = vi.fn(() => 'blob:report'), revokeObjectURL = vi.fn()
  vi.stubGlobal('URL', class extends URL { static createObjectURL = createObjectURL; static revokeObjectURL = revokeObjectURL })
  const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function () { expect(this.download).toBe('career-insight-report.html'); expect(this.href).toBe('blob:report') })
  const html = insightReportHtml(report)
  expect(html).toContain('charset="utf-8"'); expect(html).toContain('Khám phá nghề nghiệp'); expect(html).toContain('&lt;script&gt;'); expect(html).toContain('&lt;img'); expect(html).toContain('Dữ liệu &amp; phân tích'); expect(html).not.toMatch(/<script|<img|<link|<iframe|https?:/)
  downloadInsightReport(report); expect(createObjectURL.mock.calls[0][0].type).toBe('text/html;charset=utf-8'); expect(click).toHaveBeenCalledOnce(); expect(document.querySelector('a[download]')).toBeNull()
  vi.runAllTimers(); expect(revokeObjectURL).toHaveBeenCalledExactlyOnceWith('blob:report')
})

it('opens and downloads an existing authorized report on demand without requesting generation',async()=>{
 apiRequest.mockReset();apiRequest.mockImplementation(async path=>path==='/v2/insight-reports'?[{id:'saved',...report}]:{id:'saved',...report})
 const click=vi.spyOn(HTMLAnchorElement.prototype,'click').mockImplementation(()=>{}), createObjectURL=vi.fn(()=> 'blob:saved'),revokeObjectURL=vi.fn()
 vi.stubGlobal('URL',class extends URL {static createObjectURL=createObjectURL;static revokeObjectURL=revokeObjectURL})
 render(<I18nProvider><SavedInsightReports onClose={()=>{}}/></I18nProvider>)
 fireEvent.click(await screen.findByRole('button',{name:'Download report'}));await waitFor(()=>expect(click).toHaveBeenCalledOnce());await waitFor(()=>expect(revokeObjectURL).toHaveBeenCalledWith('blob:saved'))
 expect(apiRequest).toHaveBeenCalledWith('/v2/insight-reports/saved');expect(apiRequest.mock.calls.every(([,request])=>!request?.method||request.method==='GET')).toBe(true)
 fireEvent.click(screen.getByRole('button',{name:'Open report'}));await screen.findByRole('heading',{name:'Directions and evidence'});expect(screen.getByText(report.structuredInsights.summary)).toBeTruthy()
})
it('keeps a report download error actionable without fabricating a report or calling AI',async()=>{
 apiRequest.mockReset();apiRequest.mockImplementation(async path=>{if(path==='/v2/insight-reports')return [{id:'saved',...report}];throw new Error('Report unavailable')})
 render(<I18nProvider><SavedInsightReports onClose={()=>{}}/></I18nProvider>)
 fireEvent.click(await screen.findByRole('button',{name:'Open report'}));await screen.findByText('Report unavailable');expect(screen.getByRole('button',{name:'Download report'}).disabled).toBe(false);expect(screen.queryByRole('heading',{name:'Summary'})).toBeNull()
})
