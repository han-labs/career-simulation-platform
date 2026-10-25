import { useEffect, useState } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { apiRequest, API_BASE_URL } from '../../../shared/api/httpClient.js'
import { localized } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'

export default function CoreSimulationResultsPage() {
  const { t, locale } = useI18n()
  const { slug } = useParams()
  const [search] = useSearchParams()
  const enrollmentId = search.get('enrollment')
  const [state, setState] = useState({ status: 'loading', enrollment: null, scenario: null, results: null })
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let active = true
    if (!enrollmentId) return () => { active = false }
    Promise.all([
      apiRequest('/v2/me'),
      apiRequest(`/v2/enrollments/${enrollmentId}`),
      apiRequest(`/v2/enrollments/${enrollmentId}/results`),
      apiRequest(`/v2/scenarios/${encodeURIComponent(slug)}`),
    ]).then(([, enrollment, results, scenario]) => {
      if (active) setState({ status: 'ready', enrollment, results, scenario })
    }).catch(error => {
      if (active) setState({ status: [401, 403, 404].includes(error.status) ? 'denied' : 'error', enrollment: null, scenario: null, results: null })
    })
    return () => { active = false }
  }, [enrollmentId, slug, retry])

  const { enrollment, scenario, results } = state
  const status = enrollmentId ? state.status : 'error'
  const completion = results?.completion
  return <main className="feature-page section-shell core-flow core-completion-page">
    <Link className="back-link" to="/simulations">{t('Back to catalog')}</Link>
    {status === 'loading' && <p role="status">{t('Loading simulation results…')}</p>}
    {status === 'error' && <><p role="alert">{t('Simulation results could not be loaded. Please try again.')}</p>{enrollmentId && <button className="button button--secondary" type="button" onClick={() => { setState({ status: 'loading', enrollment: null, scenario: null, results: null }); setRetry(value => value + 1) }}>{t('Retry results')}</button>}</>}
    {status === 'denied' && <><p role="alert">{t('You do not have permission to view these simulation results.')}</p><Link to="/dashboard">{t('Return to my simulation history')}</Link></>}
    {status === 'ready' && <>
      <header className="core-completion-page__hero">
        <p className="core-eyebrow">{scenario.enterpriseName}</p>
        <h1>{localized(enrollment.metadata, locale, 'title')}</h1>
        <p>{t('Simulation completed')}</p>
      </header>
      <section className="core-results" aria-labelledby="simulation-results-title">
        <p className="core-eyebrow">{t('Your results')}</p>
        <h2 id="simulation-results-title">{t('Simulation results')}</h2>
        <div className="core-results__grid">{results.tasks?.map(result => { const task = enrollment.tasks?.find(item => item.id === result.taskId); return <article key={result.taskId}><CheckCircle2 aria-hidden="true" /><div><h3>{localized(task, locale, 'title')}</h3><p>{t(result.status === 'FINAL' ? 'Final submission' : result.status === 'DRAFT' ? 'Draft' : 'Not started')}{result.quizResult && <span> · {t('Quiz score')}: {result.quizResult.correct}/{result.quizResult.total}</span>}</p></div></article> })}</div>
        <section className="core-results__skills"><h3>{t('Practiced skills')}</h3>{results.practicedSkills?.length ? <ul>{results.practicedSkills.map(skill => <li key={skill}>{skill}</li>)}</ul> : <p>{t('No practiced skills recorded yet.')}</p>}</section>
      </section>
      {completion && <article className="core-results__artifact"><h2>{t('Completion record')}</h2><p>{t('This records simulation participation, not professional competence.')}</p>{completion.certificateCode && <a className="button" href={`${API_BASE_URL}/v2/completions/${completion.id}/certificate`} target="_blank" rel="noreferrer">{t('View certificate')}</a>}</article>}
    </>}
  </main>
}
