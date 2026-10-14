import { Link } from 'react-router-dom'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { riasecDescriptions } from '../data/mockQuestions.js'

const dimensionOrder = ['R', 'I', 'A', 'S', 'E', 'C']
const maximumDimensionScore = 35

function AssessmentResult({ result, onRestart }) {
  const { t } = useI18n()
  const scores = Array.isArray(result?.scores) ? result.scores : []
  const validScores = scores.filter(
    ({ dimension, score }) => riasecDescriptions[dimension] && Number.isFinite(score),
  )
  const hasSixUniqueDimensions =
    validScores.length === dimensionOrder.length &&
    new Set(validScores.map(({ dimension }) => dimension)).size === dimensionOrder.length

  if (!hasSixUniqueDimensions) {
    return (
      <section className="assessment-result assessment-result--empty" aria-labelledby="assessment-result-title">
        <h1 id="assessment-result-title">{t('Your results are unavailable')}</h1>
        <p>{t('The result is incomplete. You can return and take the assessment again.')}</p>
        <button className="button button--secondary" type="button" onClick={onRestart}>{t('Retake assessment')}</button>
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
        <h1 id="assessment-result-title">
          {t(strongestDimensions.length > 1 ? 'Your strongest interest signals' : 'Your strongest interest signal')}
        </h1>
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
            <p>{t('Each score is the sum of your answers in that dimension, from 7 to 35.')}</p>
          </div>
          <Link className="button button--secondary" to="/simulations">{t('Explore IT simulations')}</Link>
        </div>
        <ul className="assessment-score-list">
          {orderedScores.map(({ dimension, score }) => {
            const info = riasecDescriptions[dimension]
            const percentage = Math.max(0, Math.min(100, (score / maximumDimensionScore) * 100))
            return (
              <li className="assessment-score" key={dimension}>
                <div className="assessment-score__label">
                  <span>{t(info.name)}</span>
                  <strong>{score} / {maximumDimensionScore}</strong>
                </div>
                <div className="assessment-score__track" role="progressbar" aria-label={`${t(info.name)}: ${score} out of ${maximumDimensionScore}`} aria-valuemin={0} aria-valuemax={maximumDimensionScore} aria-valuenow={score}>
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
        <button className="button button--primary" type="button" onClick={onRestart}>{t('Retake assessment')}</button>
      </div>
    </section>
  )
}

export default AssessmentResult
