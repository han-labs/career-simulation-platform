import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../../shared/api/httpClient.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import Button from '../../shared/ui/Button.jsx'
import '../../shared/ui/ui.css'
import './enterpriseAnalytics.css'

const EMPTY_FILTER = { scenarioId: '', from: '', to: '', status: '' }
const TIME_ZONE = 'Asia/Ho_Chi_Minh'
const STATUS = {
  ENROLLED: 'Enrolled',
  IN_PROGRESS: 'Work in progress',
  COMPLETED: 'Completed',
}

function useRead(path) {
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState(null)
  useEffect(() => {
    if (!path) return
    const controller = new AbortController()
    apiRequest(path, { signal: controller.signal }).then(
      data => { if (!controller.signal.aborted) setResult({ path, revision, data }) },
      error => { if (!controller.signal.aborted) setResult({ path, revision, error }) },
    )
    return () => controller.abort()
  }, [path, revision])
  const current = result?.path === path && result.revision === revision ? result : null
  return { ...current, loading: Boolean(path && !current), refresh: () => setRevision(value => value + 1) }
}

function ReadError({ error, refresh }) {
  const { t } = useI18n()
  if ([401, 403, 404].includes(error.status)) {
    return <div role="alert">
      <p>{t('You do not have permission to view these analytics.')}</p>
      {error.status === 401 && <Link to="/account">{t('Sign in')}</Link>}
    </div>
  }
  return <div role="alert">
    <p>{t('Analytics could not be loaded. Please refresh the page or try again later.')}</p>
    <Button onClick={refresh}>{t('Try again')}</Button>
  </div>
}

function TaskProgress({ tasks, number }) {
  const { t } = useI18n()
  const labels = [t('Not submitted'), t('Draft'), t('Final submitted')]
  return <section className="cs-surface" aria-labelledby="analytics-task-heading">
    <h2 id="analytics-task-heading">{t('Progress by task')}</h2>
    <div className="enterprise-task-list" role="list">
      {tasks.map(task => <article role="listitem" key={task.taskId} className="enterprise-task">
        <div>
          <h3>{task.position}. {task.taskTitle}</h3>
          <p className="cs-caption">{task.programTitle} · {t('Version')} {task.versionNumber}</p>
        </div>
        <dl>{[task.notSubmitted, task.drafts, task.submitted].map((value, index) =>
          <div key={labels[index]}><dt>{labels[index]}</dt><dd>{number.format(value)}</dd></div>)}</dl>
      </article>)}
    </div>
  </section>
}

function Evidence({ path, query }) {
  const { t, locale } = useI18n()
  const [cursors, setCursors] = useState([])
  const cursor = cursors.at(-1)
  const params = new URLSearchParams(query)
  if (cursor) params.set('before', cursor)
  const result = useRead(cursors.length ? `${path}/evidence?${params}` : null)
  const date = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeZone: TIME_ZONE })
  return <section className="cs-surface cs-stack" aria-labelledby="analytics-evidence-heading">
    <h2 id="analytics-evidence-heading">{t('Shared completion evidence')}</h2>
    <div className="cs-actions">
      {!cursors.length
        ? <Button onClick={() => { result.refresh(); setCursors([null]) }}>{t('View shared evidence')}</Button>
        : <Button onClick={() => setCursors([])}>{t('Hide evidence')}</Button>}
      {!!cursors.length && <Button onClick={result.refresh} disabled={result.loading}>{t('Refresh evidence')}</Button>}
    </div>
    {result.loading && <p role="status">{t('Loading shared evidence…')}</p>}
    {result.error && <ReadError error={result.error} refresh={result.refresh} />}
    {result.data && <>
      {result.data.items.length === 0
        ? <p role="status">{t('Individual participant evidence is not available for this view.')}</p>
        : <ul className="enterprise-evidence-list">{result.data.items.map(item => <li key={item.reference}>
          <strong>{item.programTitle}</strong>
          <span>{t('Version')} {item.versionNumber} · {t('Reference')} {item.reference}</span>
          <time dateTime={item.completedAt}>{date.format(new Date(item.completedAt))}</time>
        </li>)}</ul>}
      {(cursors.length > 1 || result.data.nextBefore) && <div className="cs-actions">
        <Button disabled={cursors.length <= 1} onClick={() => {
          result.refresh()
          setCursors(value => value.slice(0, -1))
        }}>{t('Previous page')}</Button>
        <Button disabled={!result.data.nextBefore} onClick={() => setCursors(value => [...value, result.data.nextBefore])}>{t('Next page')}</Button>
      </div>}
    </>}
  </section>
}

function Analytics({ member }) {
  const { t, locale } = useI18n()
  const [draft, setDraft] = useState(EMPTY_FILTER)
  const [selected, setSelected] = useState(EMPTY_FILTER)
  const [programs, setPrograms] = useState([])
  const [validation, setValidation] = useState(false)
  const [evidenceEpoch, setEvidenceEpoch] = useState(0)
  const path = `/v2/enterprises/${member.enterpriseId}/analytics`
  const query = new URLSearchParams(Object.entries(selected).filter(([, value]) => value)).toString()
  const result = useRead(query ? `${path}?${query}` : path)
  const data = result.data
  // Preserve the authorized options even when a filtered request fails.
  if (data && programs !== data.programs) setPrograms(data.programs)
  const number = new Intl.NumberFormat(locale)
  const percent = new Intl.NumberFormat(locale, { style: 'percent', maximumFractionDigits: 1 })
  function edit(event) {
    setDraft(current => ({ ...current, [event.target.name]: event.target.value }))
    setValidation(false)
  }
  function apply(next) {
    if (next.from && next.to && next.from > next.to) {
      setValidation(true)
      return
    }
    setValidation(false)
    setSelected(next)
    setEvidenceEpoch(value => value + 1)
    result.refresh()
  }
  return <>
    <p className="cs-caption">{member.name}</p>
    <form className="cs-surface cs-stack" onSubmit={event => { event.preventDefault(); apply(draft) }}>
      <div className="enterprise-analytics-filters">
        <label className="cs-field">{t('Program')}<select name="scenarioId" value={draft.scenarioId} onChange={edit}>
          <option value="">{t('All programs')}</option>
          {programs.map(program => <option key={program.scenarioId} value={program.scenarioId}>{program.title}</option>)}
        </select></label>
        <fieldset>
          <legend>{t('Registration date (Vietnam time)')}</legend>
          <div className="enterprise-analytics-dates">
            <label className="cs-field">{t('From date')}<input type="date" name="from" value={draft.from} onChange={edit} /></label>
            <label className="cs-field">{t('Through date')}<input type="date" name="to" value={draft.to} onChange={edit} /></label>
          </div>
        </fieldset>
        <label className="cs-field">{t('Enrollment status')}<select name="status" value={draft.status} onChange={edit}>
          <option value="">{t('All statuses')}</option>
          {Object.entries(STATUS).map(([value, label]) => <option key={value} value={value}>{t(label)}</option>)}
        </select></label>
      </div>
      {validation && <p role="alert">{t('The registration start date must be on or before the end date.')}</p>}
      <div className="cs-actions">
        <Button variant="primary" type="submit">{t('Apply filters')}</Button>
        <Button onClick={() => { setDraft(EMPTY_FILTER); apply(EMPTY_FILTER) }}>{t('Clear filters')}</Button>
      </div>
    </form>
    {result.loading && <p role="status">{t('Loading analytics…')}</p>}
    {result.error && <ReadError error={result.error} refresh={result.refresh} />}
    {data && <>
      <dl className="enterprise-analytics-kpis">
        <div className="cs-surface"><dt>{t('Enrollment count')}</dt><dd>{number.format(data.counts.enrollments)}</dd></div>
        <div className="cs-surface"><dt>{t('Started')}</dt><dd>{number.format(data.counts.started)}</dd></div>
        <div className="cs-surface"><dt>{t('Completed enrollments')}</dt><dd>{number.format(data.counts.completed)}</dd></div>
        <div className="cs-surface"><dt>{t('Completion rate')}</dt><dd>{data.detailStatus === 'AVAILABLE'
          ? percent.format(data.counts.completed / data.counts.enrollments) : '—'}</dd></div>
      </dl>
      {data.detailStatus === 'EMPTY' && <p role="status">{t('No activity is available for this selection.')}</p>}
      {data.detailStatus === 'INSUFFICIENT' && <p role="status">{t('Insufficient data for the selected segment.')}</p>}
      {data.detailStatus === 'AVAILABLE' && <TaskProgress tasks={data.tasks} number={number} />}
      <Evidence key={evidenceEpoch} path={path} query={query} />
    </>}
  </>
}

export default function EnterpriseAnalyticsPage() {
  const { t } = useI18n()
  const membership = useRead('/v2/enterprises/me')
  return <section className="section-shell cs-ui cs-stack enterprise-analytics">
    <header>
      <Link to="/enterprise" className="text-link">{t('Back to enterprise workspace')}</Link>
      <h1>{t('Participation analytics')}</h1>
    </header>
    {membership.loading && <p role="status">{t('Loading analytics…')}</p>}
    {membership.error && <ReadError error={membership.error} refresh={membership.refresh} />}
    {membership.data?.membershipStatus !== 'ACTIVE' && membership.data &&
      <ReadError error={{ status: 403 }} refresh={membership.refresh} />}
    {membership.data?.membershipStatus === 'ACTIVE' && <Analytics key={membership.data.enterpriseId} member={membership.data} />}
  </section>
}
