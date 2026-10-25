import { Link } from 'react-router-dom'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { quizLabels } from '../data/profileMetadata.js'

export default function ProfileAssessmentResult({ result, onRestart, disabled = false }) {
  const { t } = useI18n()
  const quiz = quizLabels[result?.assessmentTypeCode]
  const data = result?.resultData
  const scores = data?.categories
  const expected = quiz && Object.keys(quiz.categories)
  const valid = expected && Array.isArray(scores) && scores.length === expected.length &&
    new Set(scores.map(s => s.code)).size === expected.length &&
    scores.every(s => expected.includes(s.code) && [s.raw, s.min, s.max].every(Number.isFinite) &&
      s.max > s.min && s.raw >= s.min && s.raw <= s.max) &&
    Array.isArray(data.topCategories) && data.topCategories.length > 0 &&
    data.topCategories.every(code => expected.includes(code))

  if (!valid) return <section className="assessment-result assessment-result--empty">
    <h2>{t('Your results are unavailable')}</h2>
    <p>{t('The result is incomplete. You can return and take the assessment again.')}</p>
    <button className="button button--secondary" disabled={disabled} onClick={onRestart}>{t('Start another attempt')}</button>
  </section>

  const ordered = expected.map(code => scores.find(s => s.code === code))
  return <section className="assessment-result assessment-profile" aria-labelledby="profile-result-title">
    <div className="assessment-result__summary">
      <span className="eyebrow">{t(quiz.title)} · {t('Saved result')}</span>
      <h2 id="profile-result-title" className="assessment-profile__title">{t('Your self-exploration summary')}</h2>
      <p className="assessment-result__interpretation">{t(quiz.statement)}</p>
      <h3>{t(data.topCategories.length > 1 ? 'Shared highest signals' : 'Highest signal')}</h3>
      <div className="assessment-result__strongest" aria-label={t('Highest-scoring categories')}>
        {expected.filter(code => data.topCategories.includes(code)).map(code => <span key={code} className="assessment-result__badge">{t(quiz.categories[code][0])}</span>)}
      </div>
      {data.topCategories.length > 1 && <p>{t('These categories share the highest saved score. Each is an equal signal to reflect on.')}</p>}
      {result.assessmentTypeCode === 'ABILITIES' && <p className="assessment-profile__confidence">{t('These ratings describe your current confidence, not verified professional skills.')}</p>}
    </div>
    <div className="assessment-result__scores">
      <h2>{t('Your category breakdown')}</h2>
      <p>{t('Each score is the sum of your answers. Ranges depend on the questions in this version.')}</p>
      <ul className="assessment-score-list">
        {ordered.map(({ code, raw, min, max }) => {
          const [label, description] = quiz.categories[code]
          const top = data.topCategories.includes(code)
          return <li key={code} className={`assessment-score assessment-profile__card${top ? ' assessment-profile__card--top' : ''}`}>
            <div className="assessment-score__label"><h3>{t(label)}</h3><strong>{raw} / {max}</strong></div>
            {top && <span className="assessment-profile__signal">{t('Highest signal')}</span>}
            <p>{t(description)}</p>
            <div className="assessment-score__track" role="progressbar" aria-label={t(label)} aria-valuemin={min} aria-valuemax={max} aria-valuenow={raw}>
              <span style={{ width: `${(raw - min) / (max - min) * 100}%` }} />
            </div>
            <small>{t('Score range')}: {min}–{max}</small>
          </li>
        })}
      </ul>
    </div>
    <div className="assessment-result__limitations">
      <h2>{t('Use this as a starting point')}</h2>
      <p>{t('This CareerSim self-reflection quiz is not a validated aptitude test, certification, or prediction of career success. Ratings reflect your answers today. Try a related task and decide what you want to explore further.')}</p>
    </div>
    <div className="assessment-result__actions">
      <Link className="button button--secondary" to="/simulations">{t('Explore IT simulations')}</Link>
      <button className="button button--primary" disabled={disabled} onClick={onRestart}>{t('Start another attempt')}</button>
    </div>
  </section>
}
