import { useEffect, useRef, useState } from 'react'
import { apiRequest, getAccountGeneration } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import UsageTrend from '../usage/UsageTrend.jsx'
import UsageExecutions from '../usage/UsageExecutions.jsx'
import { money, number } from '../usage/usageFormat.js'
import { initialFilters, usageRequest, usageUrl } from '../usage/usageDashboard.js'
import '../usage/usageDashboard.css'

const outcomes = { SUCCEEDED: 'Succeeded', FAILED: 'Failed', REFUSED: 'Refused', CANCELLED: 'Cancelled', ACCEPTED: 'Accepted', RUNNING: 'Running' }

export default function AgentUsagePage() {
  const { t, locale } = useI18n()
  const [filters, setFilters] = useState(initialFilters)
  const [request, setRequest] = useState(() => usageRequest(initialFilters))
  const [history, setHistory] = useState([null]), [reload, setReload] = useState(0)
  const [data, setData] = useState(null), [options, setOptions] = useState({ agents: [], models: [] })
  const [busy, setBusy] = useState(true), [error, setError] = useState('')
  const sequence = useRef(0)
  const cursor = history.at(-1)
  useEffect(() => {
    const id = ++sequence.current, generation = getAccountGeneration(), controller = new AbortController()
    let active = true
    apiRequest(usageUrl(request, cursor), { signal: controller.signal }).then(result => {
      if (active && id === sequence.current && generation === getAccountGeneration()) { setData(result); setOptions(result.options); setError('') }
    }).catch(failure => {
      if (active && id === sequence.current && generation === getAccountGeneration()) setError([401, 403].includes(failure.status) ? 'Administrator access is required. Sign in with an authorized account.' : 'AI usage data could not be loaded. Please try again.')
    }).finally(() => { if (active && id === sequence.current && generation === getAccountGeneration()) setBusy(false) })
    return () => { active = false; controller.abort() }
  }, [request, cursor, reload])
  function start() { setBusy(true); setError(''); setData(null) }
  function apply(event) { event.preventDefault(); start(); setHistory([null]); setRequest(usageRequest(filters)) }
  function edit(key, value) { setFilters(current => ({ ...current, [key]: value, ...(key === 'provider' ? { model: '' } : {}) })) }
  function page(next) { start(); setHistory(current => next ? [...current, data.nextCursor] : current.slice(0, -1)) }
  const n = value => number(value, locale)
  const summary = data?.summary
  const providers = [...new Set(options.models.map(model => model.provider).filter(Boolean))]
  const dirty = Object.keys(initialFilters).some(key => filters[key] !== request[key])
  const date = value => new Intl.DateTimeFormat(locale === 'vi' ? 'vi-VN' : 'en-GB', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'UTC' }).format(new Date(value))
  return <section className="cs-ui ai-usage core-flow">
    <header className="cs-page-header"><div><h1>{t('AI Usage')}</h1><p>{t('Monitor AI request volume, runtime health, latency and cost.')}</p></div></header>
    <form className="cs-surface usage-filters" onSubmit={apply} aria-label={t('AI usage filters')}>
      <FormField label={t('Time range')}><select value={filters.range} onChange={e => edit('range', e.target.value)}>
        <option value="24h">{t('Last 24 hours')}</option><option value="7d">{t('Last 7 days')}</option><option value="30d">{t('Last 30 days')}</option><option value="366d">{t('Last 366 days')}</option>
      </select></FormField>
      <FormField label={t('Agent')}><select value={filters.agent} onChange={e => edit('agent', e.target.value)}><option value="">{t('All Agents')}</option>{options.agents.map(agent => <option value={agent.id} key={agent.id}>{agent.name}</option>)}</select></FormField>
      <FormField label={t('Provider')}><select value={filters.provider} onChange={e => edit('provider', e.target.value)}><option value="">{t('All providers')}</option>{providers.map(provider => <option key={provider}>{provider}</option>)}</select></FormField>
      <FormField label={t('Model')}><select value={filters.model} onChange={e => edit('model', e.target.value)}><option value="">{t('All models')}</option>{options.models.filter(model => !filters.provider || model.provider === filters.provider).map(model => <option value={model.model} key={model.model}>{model.model}</option>)}</select></FormField>
      <FormField label={t('Outcome')}><select value={filters.outcome} onChange={e => edit('outcome', e.target.value)}><option value="">{t('All outcomes')}</option>{Object.entries(outcomes).map(([value, label]) => <option key={value} value={value}>{t(label)}</option>)}</select></FormField>
      <Button type="submit" variant="primary" busy={busy}>{t('Apply filters')}</Button>
      {dirty && <p className="cs-caption usage-filter-note">{t('Apply filters to update results.')}</p>}
      {options.truncated && <p className="cs-caption usage-filter-note">{t('Filter choices are limited to 100. Narrow the time range for other choices.')}</p>}
    </form>
    <p className="cs-caption usage-scope">{t('Agent Platform executions only: Student Chat, Admin Preview and KB Test. Legacy Syn and ingestion are excluded.')}</p>
    {busy && <FeedbackPanel kind="loading">{t('Loading AI usage...')}</FeedbackPanel>}
    {error && <FeedbackPanel kind="error" title={t('AI usage unavailable')} action={<Button onClick={() => { start(); setReload(value => value + 1) }}>{t('Retry')}</Button>}>{t(error)}</FeedbackPanel>}
    {!busy && !error && data && <div className="cs-stack">
      <p className="cs-caption" aria-label={t('Applied reporting interval')}>{date(data.from)} — {date(data.to)} · UTC{request.agent && <> · {options.agents.find(a => a.id === request.agent)?.name ?? t('Historical Agent')}</>}{request.provider && <> · {request.provider}</>}{request.model && <> · {request.model}</>}{request.outcome && <> · {t(outcomes[request.outcome])}</>}</p>
      <div className="usage-kpis" aria-label={t('AI usage key metrics')}>
        <section className="cs-surface usage-kpi"><h2>{t('Requests')}</h2><p className="usage-kpi-value">{n(summary.requests)}</p><p className="cs-caption">{t('One persisted execution per request, including no-model requests.')}</p></section>
        <section className="cs-surface usage-kpi"><h2>{t('Estimated Cost')}</h2><p className="usage-kpi-value">{money(summary.recordedEstimatedCostUsd, locale, t)}</p><p className="cs-caption">{t('Recorded generation subtotal in USD; not provider billing.')}</p><p className="cs-caption">{n(summary.estimatedCostExecutions)} {t('estimated')} · {n(summary.unknownCostExecutions)} {t('unavailable')} · {n(summary.partialCostExecutions)} {t('partial')} · {n(summary.notApplicableCostExecutions)} {t('not applicable')}</p></section>
        <section className="cs-surface usage-kpi"><h2>{t('Average Latency')}</h2><p className="usage-kpi-value">{summary.averageLatencyMs == null ? t('Not available') : n(summary.averageLatencyMs) + ' ms'}</p><p className="cs-caption">{n(summary.latencySamples)} {t('measured finished executions')} · {n(summary.missingLatencyExecutions)} {t('missing measurements')}</p></section>
        <section className="cs-surface usage-kpi"><h2>{t('Failed / Refused')}</h2><p className="usage-kpi-value">{n(summary.failed)} / {n(summary.refused)}</p><p className="cs-caption">{t('Failed executions / refused executions. Refusal is not an infrastructure failure.')}</p></section>
      </div>
      {!summary.requests ? <FeedbackPanel kind="empty" title={t('No AI usage records are available for this period.')}>{t('No executions match the selected filters. Change the filters or reporting interval.')}</FeedbackPanel> : <>
        <div className="usage-overview"><UsageTrend data={data.trend} unit={data.bucketUnit} />
          <section className="cs-surface"><h2>{t('Outcome health')}</h2><p className="cs-caption">{t('Execution outcomes retain their original meaning.')}</p><dl className="usage-outcomes">{[['succeeded', 'Succeeded'], ['failed', 'Failed'], ['refused', 'Refused'], ['cancelled', 'Cancelled'], ['pending', 'In progress']].map(([key, label]) => <div key={key} className={'usage-outcome usage-outcome--' + key}><dt>{t(label)}</dt><dd>{n(summary[key])}</dd></div>)}</dl></section>
        </div>
        <UsageExecutions rows={data.executions} />
        <nav className="cs-actions usage-pagination" aria-label={t('Execution pagination')}><Button disabled={history.length === 1} onClick={() => page(false)}>{t('Previous page')}</Button><span>{t('Page')} {history.length}</span><Button disabled={!data.nextCursor} onClick={() => page(true)}>{t('Next page')}</Button></nav>
      </>}
      <p className="cs-caption">{t('Latency is full execution latency in milliseconds, measured for finished executions only. Unknown cost is never zero; partial cost is not a complete total. Embedding costs are excluded.')}</p>
      <p className="cs-caption">{t('Model identifies the selected model. Recorded model attempts describe actual calls; a selection alone does not imply a provider call.')}</p>
    </div>}
  </section>
}
