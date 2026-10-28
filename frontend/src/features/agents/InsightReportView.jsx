import { useEffect, useRef, useState } from 'react'
import { FileText, Download } from 'lucide-react'
import { apiRequest } from '../../shared/api/httpClient.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import Dialog from '../../shared/ui/Dialog.jsx'
import Button from '../../shared/ui/Button.jsx'
import FeedbackPanel from '../../shared/ui/FeedbackPanel.jsx'
import { downloadInsightReport, reportSections } from './insightReportHtml.js'

export default function InsightReportView({ report, onClose }) {
  const { t } = useI18n(), data = report.structuredInsights
  return <Dialog title={t('Insight report')} closeLabel={t('Close dialog')} onRequestClose={onClose} footer={<><Button onClick={() => downloadInsightReport(report, t)}>{t('Download report')}</Button><Button onClick={onClose}>{t('Close')}</Button></>}>
    <article className="student-insight-report cs-stack"><section><h3>{t('Summary')}</h3><p>{data.summary}</p></section><section><h3>{t('Directions and evidence')}</h3>{data.hypotheses?.map((item, index) => <section key={index}><h4>{item.direction}</h4><p>{item.reason}</p><p>{item.evidenceRefs?.join(', ')}</p></section>)}</section>
      {Object.entries(reportSections).map(([key, label]) => <section key={key}><h3>{t(label)}</h3><ul>{data[key]?.map((item, index) => <li key={index}>{item}</li>)}</ul></section>)}<p>{data.disclaimer}</p><section><h3>{t('Source references')}</h3><ul>{data.evidenceRefs?.map((item, index) => <li key={index}>{item}</li>)}</ul></section>
    </article>
  </Dialog>
}

// Load only on the user's explicit request; these are existing owner-scoped reports.
export function SavedInsightReports({ onClose }) {
  const { t } = useI18n()
  const [rows, setRows] = useState(null), [opened, setOpened] = useState(null), [error, setError] = useState(''), [busy, setBusy] = useState(false), [attempt, setAttempt] = useState(0)
  const alive = useRef(false), pending = useRef(false)
  useEffect(() => {
    alive.current = true
    apiRequest('/v2/insight-reports').then(value => { if (alive.current) setRows(value) }).catch(failure => { if (alive.current) setError(failure.message) })
    return () => { alive.current = false }
  }, [attempt])
  async function show(row, download) {
    if (pending.current) return
    pending.current = true; setBusy(true); setError('')
    try {
      const report = await apiRequest(`/v2/insight-reports/${encodeURIComponent(row.id)}`)
      if (!report?.structuredInsights?.summary) throw new Error('The report is not available. Please try again.')
      if (!alive.current) return
      if (download) downloadInsightReport(report, t)
      else setOpened(report)
    } catch (failure) { if (alive.current) setError(failure.message) }
    finally { pending.current = false; if (alive.current) setBusy(false) }
  }
  if (opened) return <InsightReportView report={opened} onClose={() => setOpened(null)} />
  return <Dialog title={t('Saved reports')} closeLabel={t('Close dialog')} onRequestClose={onClose}>
    {error && <><FeedbackPanel kind="error">{t(error)}</FeedbackPanel><Button onClick={() => { setError(''); setRows(null); setAttempt(value => value + 1) }}>{t('Reload reports')}</Button></>}
    {!rows && !error && <p role="status">{t('Loading reports...')}</p>}
    {rows?.length === 0 && <p>{t('No saved reports yet. Request an insight report from the conversation.')}</p>}
    <ul className="student-saved-reports">{rows?.map(row => <li key={row.id}><p>{row.structuredInsights?.summary}</p><div className="cs-actions"><Button icon={<FileText size={15} />} disabled={busy} onClick={() => show(row, false)}>{t('Open report')}</Button><Button icon={<Download size={15} />} disabled={busy} onClick={() => show(row, true)}>{t('Download report')}</Button></div></li>)}</ul>
  </Dialog>
}
