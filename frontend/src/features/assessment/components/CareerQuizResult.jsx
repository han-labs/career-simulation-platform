import { quizLabels } from '../data/profileMetadata.js'

function CareerQuizResult({ quizType, result, onRetake }) {
  const quiz = quizLabels[quizType]
  const expectedCategories = quiz ? Object.keys(quiz.categories).sort().join('|') : ''
  const receivedCategories = Array.isArray(result?.scores) ? result.scores.map((item) => item?.category).sort().join('|') : ''
  const validScores = Array.isArray(result?.scores) && result.scores.every((item) => Number.isInteger(item?.score) && item.score >= 3 && item.score <= 15)
  if (!quiz || !result || result.quizType !== quizType || !validScores || receivedCategories !== expectedCategories) {
    return <section className="assessment-result assessment-result--empty"><h1>Your results are unavailable</h1><p>The saved result could not be read. You can take this quiz again.</p><button className="button button--primary" type="button" onClick={onRetake}>Retake quiz</button></section>
  }
  return (
    <section className="assessment-result" aria-labelledby="career-quiz-result-title">
      <div className="assessment-result__summary">
        <span className="eyebrow">{quiz.title} results</span>
        <h1 id="career-quiz-result-title">Your {quizType === 'INTERESTS' ? 'interest' : quizType === 'WORK_ACTIVITIES' ? 'work activity preference' : 'self-reported strength'} signals</h1>
        <p className="assessment-result__interpretation">{quiz.statement}</p>
        <div className="assessment-result__scores">
          <p>Each category score is the sum of three responses on a 1–5 scale (3–15). Equal highest scores are shared signals.</p>
          <ul className="assessment-score-list">
            {result.scores.map(({ category, score }) => {
              const details = quiz.categories[category]
              if (!details || !Number.isInteger(score)) return null
              const isTop = result.topCategories?.includes(category)
              return <li className="assessment-score" key={category}>
                <div className="assessment-score__label"><span>{details[0]}{isTop ? ' · Highest signal' : ''}</span><strong>{score} / 15</strong></div>
                <p>{details[1]}</p>
                <div className="assessment-score__track" role="progressbar" aria-label={`${details[0]}: ${score} out of 15`} aria-valuemin={0} aria-valuemax={15} aria-valuenow={score}><span style={{ width: `${Math.max(0, Math.min(100, score / 15 * 100))}%` }} /></div>
              </li>
            })}
          </ul>
        </div>
      </div>
      <div className="assessment-result__limitations">
        <h2>Use this as a starting point</h2>
        <p>This CareerSim self-reflection quiz is not a validated aptitude test, certification, or prediction of career success. Ratings reflect your answers today. Try a related task and decide what you want to explore further.</p>
      </div>
      <div className="assessment-result__actions"><button className="button button--primary" type="button" onClick={onRetake}>Retake quiz</button></div>
    </section>
  )
}

export default CareerQuizResult
