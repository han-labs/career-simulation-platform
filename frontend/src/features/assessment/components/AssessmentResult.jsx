import { Link } from 'react-router-dom'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { riasecDescriptions } from '../data/mockQuestions.js'

const dimensionOrder = ['R', 'I', 'A', 'S', 'E', 'C']

function AssessmentResult({ result, onRestart, titleAs: Title = 'h1', disabled = false }) {
  const { t } = useI18n()
  const scores = Array.isArray(result?.scores) ? result.scores : []
  // Legacy questionnaires have seven items per dimension; v2 supplies pinned ranges.
  const validScores = scores.map(s => ({ ...s, min: s.min ?? 7, max: s.max ?? 35 })).filter(
    ({ dimension, score, min, max }) => riasecDescriptions[dimension] &&
      [score, min, max].every(Number.isFinite) && max > min && score >= min && score <= max,
  )
  const hasSixUniqueDimensions =
    validScores.length === dimensionOrder.length &&
    new Set(validScores.map(({ dimension }) => dimension)).size === dimensionOrder.length

  if (!hasSixUniqueDimensions) {
    return (
      <section className="assessment-result assessment-result--empty" aria-labelledby="assessment-result-title">
        <Title id="assessment-result-title">{t('Your results are unavailable')}</Title>
        <p>{t('The result is incomplete. You can return and take the assessment again.')}</p>
        <button className="button button--secondary" type="button" disabled={disabled} onClick={onRestart}>{t('Retake assessment')}</button>
      </section>
    )
  }

  const orderedScores = [...validScores].sort(
    (left, right) => dimensionOrder.indexOf(left.dimension) - dimensionOrder.indexOf(right.dimension),
  )
  const strongestScore = Math.max(...orderedScores.map(({ score }) => score))
  const strongestDimensions = orderedScores.filter(({ score }) => score === strongestScore)
  const explorationDirections = [...new Set(
    strongestDimensions.flatMap(({ dimension }) => riasecDescriptions[dimension].directions),
  )]

  return (
    <section className="assessment-result" aria-labelledby="assessment-result-title">
      <div className="assessment-result__summary">
        <span className="eyebrow">{t('RIASEC results')}</span>
        <Title id="assessment-result-title" className="assessment-profile__title">
          {t(strongestDimensions.length > 1 ? 'Your strongest interest signals' : 'Your strongest interest signal')}
        </Title>
        <div className="assessment-result__strongest" aria-label={t('Highest-scoring interest dimensions')}>
          {strongestDimensions.map(({ dimension }) => {
            const info = riasecDescriptions[dimension]
            return <span className="assessment-result__badge" key={dimension} style={{ color: info.color, backgroundColor: info.bgColor }}>{t(info.name)}</span>
          })}
        </div>
        <p className="assessment-result__interpretation">
          {strongestDimensions.map(({ dimension }) => t(riasecDescriptions[dimension].description)).join(' ')}
        </p>
        <h2>{t('Directions to explore')}</h2>
        <ul className="assessment-result__directions">
          {explorationDirections.map((direction) => <li key={direction}>{t(direction)}</li>)}
        </ul>
        <p className="assessment-result__next-step">
          {t('Try an IT simulation to experience a sample task, then reflect on how it felt to you.')}
        </p>
      </div>

      <div className="assessment-result__scores">
        <div className="assessment-result__scores-heading">
          <div>
            <h2>{t('Your six interest dimensions')}</h2>
            <p>{t('Each score is the sum of your answers. Ranges depend on the questions in this version.')}</p>
          </div>
          <Link className="button button--secondary" to="/simulations">{t('Explore IT simulations')}</Link>
        </div>
        <ul className="assessment-score-list">
          {orderedScores.map(({ dimension, score, min, max }) => {
            const info = riasecDescriptions[dimension]
            const percentage = ((score - min) / (max - min)) * 100
            return (
              <li className="assessment-score" key={dimension}>
                <div className="assessment-score__label">
                  <span>{t(info.name)}</span>
                  <strong>{score} / {max} <small>({min}–{max})</small></strong>
                </div>
                <div className="assessment-score__track" role="progressbar" aria-label={t(info.name)} aria-valuemin={min} aria-valuemax={max} aria-valuenow={score}>
                  <span style={{ width: `${percentage}%`, backgroundColor: info.color }} />
                </div>
              </li>
            )
          })}
        </ul>
      </div>

      <div className="assessment-result__limitations">
        <h2>{t('Keep your results in perspective')}</h2>
        <p>{t('This is a self-reflection tool, not a psychological diagnosis or a measure of ability. The scores do not determine whether a career is right for you or predict your future. Your interests may change as you gain experience.')}</p>
      </div>

      <div className="assessment-result__actions">
        <button className="button button--primary" type="button" disabled={disabled} onClick={onRestart}>{t('Retake assessment')}</button>
      </div>
    </section>
  )
}

export default AssessmentResult
