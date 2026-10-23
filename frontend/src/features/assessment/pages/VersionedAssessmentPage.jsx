import { Link, useParams, useSearchParams } from 'react-router-dom'
import { Compass, Heart, Lightbulb, Layers, ArrowRight } from 'lucide-react'
import { useState } from 'react'
import { localized } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { useVersionedAssessment } from '../hooks/useVersionedAssessment.js'
import { useAssessmentScroll } from '../hooks/useAssessmentScroll.js'
import AssessmentForm from '../components/AssessmentForm.jsx'
import AssessmentResult from '../components/AssessmentResult.jsx'
import ProfileAssessmentResult from '../components/ProfileAssessmentResult.jsx'
import { assessmentInstructions, assessmentTitle } from '../data/assessmentInstructions.js'
import '../assessment.css'

const icons = { RIASEC: Compass, INTERESTS: Heart, ABILITIES: Lightbulb, WORK_ACTIVITIES: Layers }

export default function VersionedAssessmentPage() {
  const { assessmentType } = useParams()
  return <AssessmentWorkflow key={assessmentType ?? ''} assessmentType={assessmentType} />
}

function AssessmentWorkflow({ assessmentType }) {
  const { t, locale } = useI18n()
  const [introduction, setIntroduction] = useState(false)
  const [query, setQuery] = useSearchParams()
  const requestedId = query.get('attempt')
  const quiz = useVersionedAssessment(assessmentType, requestedId)
  const { attempt, answers, result, busy } = quiz
  const type = quiz.types.find(row => row.activeVersionId === (attempt?.assessmentVersionId ?? quiz.selected))
  const code = result?.assessmentTypeCode ?? quiz.history.find(row => row.id === attempt?.id)?.assessmentTypeCode ?? type?.code
  const instructions = assessmentInstructions(code)
  const pendingView = requestedId && requestedId !== attempt?.id
  const view = quiz.loading || pendingView ? 'loading' : result ? `result:${result.attemptId}` : attempt ? `questions:${attempt.id}:${attempt.status}` : 'selection'
  const heading = useAssessmentScroll(view)
  const projected = result?.resultData.dimensions && {
    attemptId: result.attemptId,
    scores: result.resultData.dimensions.map(d => ({ dimension: d.code, score: d.raw, min: d.min, max: d.max })),
    topDimensions: result.resultData.topDimensions,
  }
  function showAttempt(id) {
    const next = new URLSearchParams(query)
    next.set('attempt', id)
    setQuery(next)
  }
  async function start() {
    const id = await quiz.start()
    if (id) showAttempt(id)
  }
  function selection(retake = false) {
    setIntroduction(false)
    quiz.selection(retake)
    const next = new URLSearchParams(query)
    next.delete('attempt')
    setQuery(next)
  }
  return <section className="feature-page section-shell assessment-workflow" aria-busy={quiz.loading || busy}>
    <header className="assessment-workflow__header">
      <span className="eyebrow">CareerSim · {t('Self-exploration')}</span>
      <h1 ref={heading} tabIndex={-1}>{t(attempt ? assessmentTitle(code) : 'Career Assessments')}</h1>
      <p>{t('Choose an optional quiz. Your results stay separate for each attempt and content version.')}</p>
      <Link className="button button--secondary" to="/simulations">{t('Explore IT simulations')}</Link>
    </header>
    {quiz.error && <p className="assessment-api-error" role="alert">{t(quiz.error)}</p>}
    {quiz.storageError && <p role="alert">{t(quiz.storageError)}</p>}
    {quiz.notice && <p className="assessment-workflow__notice" role="status">{t(quiz.notice)}</p>}
    {query.has('demo') && <p role="status">{t('Quick demo profiles belong to the legacy quizzes. Published version questions require your own reviewed answers.')}</p>}
    {quiz.loading || pendingView ? quiz.error && pendingView ? <div className="assessment-workflow__actions">
      <button disabled={busy} onClick={() => quiz.open(requestedId)}>{t('Try again')}</button>
      <button disabled={busy} onClick={() => selection()}>{t('Back to assessment selection')}</button>
    </div> : <p role="status">{t('Loading assessments…')}</p> : <>
      {quiz.error && !attempt && <button type="button" disabled={busy} onClick={requestedId ? () => quiz.open(requestedId) : quiz.initialize}>{t('Try again')}</button>}
      {quiz.ready && <>
      {!attempt && <>
        <h2>{t('Choose your assessment')}</h2>
        <div className="assessment-choice-grid">
          {quiz.types.map(row => {
            const Icon = icons[row.code] ?? Compass
            return <article key={row.id} className={`assessment-choice-card assessment-choice-card--available${introduction && row.activeVersionId === quiz.selected ? ' assessment-choice-card--selected' : ''}`}>
              <div className="assessment-choice-card__topline"><span className="assessment-choice-card__icon"><Icon size={24} aria-hidden="true" /></span><span className="assessment-status assessment-status--available">{t('Available')}</span></div>
              <h3>{t(assessmentTitle(row.code, row.name))}</h3>
              <p>{t(assessmentInstructions(row.code).description)}</p>
              <button className="button button--secondary" disabled={busy} aria-expanded={introduction && row.activeVersionId === quiz.selected} aria-controls="assessment-instructions-panel" onClick={() => { quiz.setSelected(row.activeVersionId); setIntroduction(true) }}>
                {t('Read instructions')}<ArrowRight size={18} aria-hidden="true" /><span className="sr-only"> · {t(assessmentTitle(row.code, row.name))}</span>
              </button>
            </article>
          })}
        </div>
        {introduction && <article id="assessment-instructions-panel" className="assessment-workflow__panel" aria-labelledby="assessment-instructions-title">
          <h2 id="assessment-instructions-title">{t('Instructions')} · {t(assessmentTitle(code))}</h2>
          <p>{t(instructions.description)}</p>
          <p>{t('Read the questions in the fixed version before answering. You can save a draft or postpone without creating a result.')}</p>
          <button className="button button--primary" disabled={!quiz.selected || busy} onClick={start}>{t(busy ? 'Opening assessment…' : 'Start or resume')}</button>
          <button className="button button--secondary" disabled={busy} onClick={() => setIntroduction(false)}>{t('Back to assessment selection')}</button>
        </article>}
        {!quiz.types.length && <p>{t('No reviewed assessment version is published yet.')}</p>}
        {quiz.types.length > 0 && assessmentType && !quiz.selected && <p role="status">{t('The requested assessment type has no published version.')}</p>}
      </>}
      {attempt && <div className="assessment-workflow__meta">
        <p><span className="assessment-status">{t(attempt.status === 'SUBMITTED' ? 'Submitted' : 'In progress')}</span> · {t('Version')} {attempt.assessmentVersionId}</p>
        <button disabled={busy} onClick={() => quiz.open(attempt.id)}>{t('Reload saved work')}</button>
        <button disabled={busy} onClick={() => selection()}>{t('Back to assessment selection')}</button>
      </div>}
      {attempt?.status === 'IN_PROGRESS' && <>
        <div className="assessment-workflow__instructions"><h2>{t('Instructions')}</h2><p>{t(instructions.description)}</p><p>{t('Choose one response for every required question. You can change your answers before submitting.')}</p></div>
        {quiz.conflict && <div className="assessment-workflow__panel" role="alert">
          <p>{t('The server has a different draft revision. Review your answers and choose which draft to keep before saving.')}</p>
          <div className="assessment-workflow__actions">
            <button disabled={busy} onClick={() => quiz.resolveConflict(false)}>{t('Use saved server answers')}</button>
            <button disabled={busy} onClick={() => quiz.resolveConflict(true)}>{t('Keep my local answers')}</button>
          </div>
        </div>}
        <AssessmentForm key={attempt.id} showDemo={false} questions={attempt.questions.map(q => ({ ...q, prompt: localized(q, locale, 'prompt') }))}
          answers={answers} setAnswer={(id, value) => quiz.update({ ...answers, [id]: value })}
          onSubmit={() => quiz.save(true)} isLoading={busy || quiz.conflict} scaleOptions={instructions.options} />
        <div className="assessment-workflow__actions">
          <button disabled={busy || quiz.conflict} onClick={() => quiz.save(false)}>{t('Save draft')}</button>
          <button disabled={busy} onClick={() => selection()}>{t('Postpone assessment')}</button>
        </div>
      </>}
      {attempt?.status === 'SUBMITTED' && !result && <p role="status">{t('Your saved result could not be loaded. Use Reload saved work to try again.')}</p>}
      {result && <p className="assessment-workflow__saved"><span>{t('Saved result')}</span> · <time dateTime={result.calculatedAt}>{new Date(result.calculatedAt).toLocaleString(locale)}</time></p>}
      {projected && <AssessmentResult result={projected} disabled={busy} titleAs="h2" onRestart={() => selection(true)} />}
      {result?.resultData.categories && <ProfileAssessmentResult result={result} disabled={busy} onRestart={() => selection(true)} />}
      <section className="assessment-workflow__history" aria-labelledby="assessment-history-title">
        <h2 id="assessment-history-title">{t('Assessment history')}</h2>
        <p>{t('Your latest 100 attempts, including drafts. Results from different types and versions are kept separately.')}</p>
        {quiz.historyError && <p role="alert">{t(quiz.historyError)}</p>}
        <button disabled={busy} onClick={quiz.refreshHistory}>{t('Refresh history')}</button>
        {!quiz.history.length && !quiz.historyError && <p>{t('No assessment attempts yet.')}</p>}
        <ul>{quiz.history.map(row => <li key={row.id}>
          <div><strong>{t(assessmentTitle(row.assessmentTypeCode))}</strong><p>{t('Version')} {row.assessmentVersionId} · <time dateTime={row.submittedAt ?? row.startedAt}>{new Date(row.submittedAt ?? row.startedAt).toLocaleString(locale)}</time></p><span className="assessment-status">{t(row.status === 'SUBMITTED' ? 'Submitted' : 'In progress')}</span></div>
          <button disabled={busy} onClick={() => showAttempt(row.id)}>{t(row.status === 'SUBMITTED' ? 'View saved result' : 'Resume draft')} · #{row.id}</button>
        </li>)}</ul>
      </section>
      </>}
    </>}
  </section>
}
