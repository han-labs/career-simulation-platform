export const reportSections = { strengthsPracticed: 'Skills practiced', difficulties: 'Difficulties', limitations: 'Limitations', nextSteps: 'Suggested next steps' }
const escape = value => String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character])
export function insightReportHtml(report, t = value => value) {
  const data = report.structuredInsights
  const list = rows => `<ul>${(rows ?? []).filter(row => typeof row === 'string').map(row => `<li>${escape(row)}</li>`).join('')}</ul>`
  return `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escape(t('Insight report'))}</title><style>body{font-family:Segoe UI,sans-serif;max-width:860px;margin:32px auto;padding:16px;line-height:1.6;color:#172b4d}h1,h2,h3{line-height:1.3}p,li{white-space:pre-wrap;overflow-wrap:anywhere}</style></head><body><h1>${escape(t('Insight report'))}</h1><h2>${escape(t('Summary'))}</h2><p>${escape(data.summary)}</p><h2>${escape(t('Directions and evidence'))}</h2>${(data.hypotheses ?? []).map(item => `<section><h3>${escape(item.direction)}</h3><p>${escape(item.reason)}</p>${list(item.evidenceRefs)}</section>`).join('')}${Object.entries(reportSections).map(([key, label]) => `<h2>${escape(t(label))}</h2>${list(data[key])}`).join('')}<p>${escape(data.disclaimer)}</p><h2>${escape(t('Source references'))}</h2>${list(data.evidenceRefs)}</body></html>`
}
export function downloadInsightReport(report, t) {
  const url = URL.createObjectURL(new Blob([insightReportHtml(report, t)], { type: 'text/html;charset=utf-8' }))
  const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'career-insight-report.html'
  try { document.body.appendChild(anchor); anchor.click() } finally { anchor.remove(); setTimeout(() => URL.revokeObjectURL(url), 0) }
}
