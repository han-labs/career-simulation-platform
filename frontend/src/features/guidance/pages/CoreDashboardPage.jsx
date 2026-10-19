import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { command, localized } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'

export default function CoreDashboardPage() {
  const { t, locale } = useI18n()
  const [data, setData] = useState(null), [error, setError] = useState(''), [busy, setBusy] = useState(false)
  useEffect(() => { let active = true; apiRequest('/v2/guidance/dashboard').then(d => { if (active) setData(d) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [])
  async function plan(event) { event.preventDefault(); setBusy(true); setError(''); const form = new FormData(event.currentTarget); try { await command('/v2/plans/confirm', { expectedActivePlanId: data.activePlan?.id ?? null, expectedRevision: data.activePlan?.revision ?? null, title: form.get('title'), steps: form.get('steps').split('\n').filter(s => s.trim()) }); setData(await apiRequest('/v2/guidance/dashboard')) } catch (e) { setError(e.message) } finally { setBusy(false) } }
  return <section className="feature-page section-shell core-flow"><h1>{t('Dashboard')}</h1><Link to="/account">{t('Account and privacy')}</Link>{error && <p role="alert">{t(error)}</p>}
    {!data && !error && <p>{t('Loading your dashboard…')}</p>}
    {data && <><h2>{t('Assessment results')}</h2><ul>{data.assessments.map(r => <li key={r.attemptId}>{r.assessmentTypeCode} · {t('Version')} {r.assessmentVersionId}: {(r.resultData.dimensions ?? r.resultData.categories ?? []).map(d => `${d.code} ${d.raw}/${d.max}`).join(', ')}</li>)}</ul>
      <h2>{t('Enrollments')}</h2><ul>{data.enrollments.map(e => <li key={e.id}><Link to={`/simulations/enrolled/attempt?enrollment=${e.id}`}>{localized(e.metadata, locale, 'title')} · {t('Version')} {e.scenarioVersionId}</Link> · {e.finalRequiredTasks}/{e.requiredTasks} · {e.status}</li>)}</ul>
      <h2>{t('Practiced skills')}</h2><ul>{data.participation.map(p => <li key={`${p.submissionId}_${p.skillCode}`}>{p.skillNameSnapshot} · {t('Task')} {p.taskId} · {t('Version')} {p.scenarioVersionId}</li>)}</ul>
      <h2>{t('Completions')}</h2><ul>{data.completions.map(c => <li key={c.id}>{c.certificateSnapshot.scenarioTitle} · {new Date(c.completedAt).toLocaleDateString(locale)}<p>{c.resumeSnippetText}</p></li>)}</ul>
      <h2>{t('Legacy evaluations (historical scores)')}</h2><ul>{data.legacyEvaluations.map(({ data: e }) => <li key={e.attemptId}>{e.title} · {e.score}/100 · {t('Content version unknown')}</li>)}</ul>
      <h2>{t('Exploration plan')}</h2>{data.activePlan && <><p>{data.activePlan.title}</p><ol>{data.activePlan.steps.map((s, i) => <li key={i}>{s}</li>)}</ol></>}
      <form onSubmit={plan}><label>{t('Plan title')}<input name="title" required maxLength={120} /></label><label>{t('One to five steps, one per line')}<textarea name="steps" required /></label><button disabled={busy}>{t('Confirm and save plan')}</button></form>
    </>}
  </section>
}
