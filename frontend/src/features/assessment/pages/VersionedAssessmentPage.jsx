import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { command, draftKey, localized, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import AssessmentForm from '../components/AssessmentForm.jsx'
import AssessmentResult from '../components/AssessmentResult.jsx'

export default function VersionedAssessmentPage() {
  const { assessmentType } = useParams()
  return <AssessmentWorkflow key={assessmentType ?? ''} assessmentType={assessmentType} />
}

function AssessmentWorkflow({ assessmentType }) {
  const { t, locale } = useI18n()
  const [owner, setOwner] = useState(null), [types, setTypes] = useState([]), [selected, setSelected] = useState('')
  const [attempt, setAttempt] = useState(null), [answers, setAnswers] = useState({}), [result, setResult] = useState(null)
  const [error, setError] = useState(''), [busy, setBusy] = useState(false), [notice, setNotice] = useState('')
  useEffect(() => { let active = true; Promise.all([apiRequest('/v2/me'), apiRequest('/v2/assessment-types')]).then(([me, list]) => { if (active) { const normalize = value => value.toLowerCase().replaceAll('_', '-'); const type = assessmentType ? list.find(type => normalize(type.code) === normalize(assessmentType)) : list[0]; setOwner(me.id); setTypes(list); setSelected(type?.activeVersionId ?? '') } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [assessmentType])
  const load = useCallback(async (id, user) => {
    const a = await apiRequest(`/v2/assessment-attempts/${id}`)
    const local = readDraft(draftKey(user, 'assessment', id))
    setAttempt(a); setAnswers({ ...Object.fromEntries(a.responses.map(r => [r.questionId, r.answerData.value])), ...(a.status === 'IN_PROGRESS' ? local?.answers : {}) })
    if (a.status === 'SUBMITTED') setResult(await apiRequest(`/v2/assessment-attempts/${id}/result`))
  }, [])
  async function start() { setBusy(true); setError(''); try {
    const key = draftKey(owner, 'assessment-start', selected), pending = readDraft(key)
    const requestId = pending?.requestId ?? crypto.randomUUID(); writeDraft(key, { requestId })
    const a = await command(`/v2/assessment-versions/${selected}/attempts`, { requestId }); setResult(null); await load(a.id, owner)
  } catch (e) { setError(e.message) } finally { setBusy(false) } }
  const update = (next) => { setAnswers(next); if (attempt) writeDraft(draftKey(owner, 'assessment', attempt.id), { answers: next }); setNotice('Local draft. Save to keep it on the server.') }
  async function save(final) { setBusy(true); setError(''); try {
    const items = Object.entries(answers).map(([questionId, value]) => ({ questionId, answerData: { schemaVersion: 1, value } }))
    if (final) { const r = await command(`/v2/assessment-attempts/${attempt.id}/submit`, { items }, attempt.revision); setResult(r); setAttempt({ ...attempt, status: 'SUBMITTED' }); setNotice('Saved on the server.') }
    else { setAttempt(await command(`/v2/assessment-attempts/${attempt.id}/responses`, { items }, attempt.revision, 'PUT')); setNotice('Saved on the server.') }
  } catch (e) { setError(e.message); setNotice('Your draft is preserved. Reload the attempt before retrying a conflict.') } finally { setBusy(false) } }
  function retake() { if (selected) writeDraft(draftKey(owner, 'assessment-start', selected), { requestId: crypto.randomUUID() }); setAttempt(null); setResult(null); setAnswers({}) }
  const projected = result?.resultData.dimensions && { attemptId: result.attemptId, scores: result.resultData.dimensions.map(d => ({ dimension: d.code, score: d.raw, max: d.max })), topDimensions: result.resultData.topDimensions }
  return <section className="feature-page section-shell core-flow"><h1>{t('Career Assessment')}</h1>
    {error && <p role="alert">{t(error)}</p>}{notice && <p role="status">{t(notice)}</p>}
    {!attempt && <><p>{t('Your older browser draft is preserved separately. New attempts use a fixed content version.')}</p><label>{t('Assessment type')}<select value={selected} onChange={e => setSelected(e.target.value)}><option value="">{t('Select an assessment type')}</option>{types.map(type => <option key={type.id} value={type.activeVersionId}>{type.name}</option>)}</select></label><button className="button" disabled={!owner || !selected || busy} onClick={start}>{t('Start or resume')}</button>{!types.length && <p>{t('No reviewed assessment version is published yet.')}</p>}{types.length > 0 && assessmentType && !selected && <p role="status">{t('The requested assessment type has no published version.')}</p>}</>}
    {attempt && <><p>{t('Version')} {attempt.assessmentVersionId} · {attempt.status}</p><button disabled={busy} onClick={() => { setBusy(true); load(attempt.id, owner).catch(e => setError(e.message)).finally(() => setBusy(false)) }}>{t('Reload saved work')}</button></>}
    {attempt?.status === 'IN_PROGRESS' && <><AssessmentForm showDemo={false} questions={attempt.questions.map(q => ({ ...q, prompt: localized(q, locale, 'prompt') }))} answers={answers} setAnswer={(id, value) => update({ ...answers, [id]: value })} setAllAnswers={next => update({ ...answers, ...next })} onSubmit={() => save(true)} isLoading={busy} /><button disabled={busy} onClick={() => save(false)}>{t('Save draft')}</button></>}
    {projected && <AssessmentResult result={projected} onRestart={retake} />}
    {result?.resultData.categories && <article><h2>{result.assessmentTypeCode}</h2><p>{t('Self-reported exploration results do not certify proficiency.')}</p><ul>{result.resultData.categories.map(c => <li key={c.code}>{c.code}: {c.raw}/{c.max}</li>)}</ul><button onClick={retake}>{t('Start another attempt')}</button></article>}
  </section>
}
