const quizLabels = {
  INTERESTS: {
    title: 'Interests Quiz',
    statement: 'Your highest ratings show work activities you currently find most interesting.',
    categories: {
      ANALYTICAL: ['Analytical', 'Investigating information, patterns, and system behavior.'],
      CREATIVE: ['Creative', 'Designing, expressing ideas, and experimenting with alternatives.'],
      COLLABORATIVE: ['Collaborative', 'Helping others, gathering feedback, and solving problems together.'],
      ORGANIZING: ['Organizing', 'Planning steps, maintaining information, and following clear processes.'],
    },
  },
  ABILITIES: {
    title: 'Abilities Quiz',
    statement: 'Your highest ratings show work activities where you currently feel more confident.',
    categories: {
      PROBLEM_SOLVING: ['Problem solving', 'Breaking down questions, comparing options, and investigating outcomes.'],
      COMMUNICATION: ['Communication', 'Explaining ideas, asking questions, and sharing progress.'],
      ORGANIZATION: ['Organization', 'Tracking work, following steps, and keeping information orderly.'],
      TECHNICAL_LEARNING: ['Technical learning', 'Learning tools, using examples, and applying feedback.'],
    },
  },
  WORK_ACTIVITIES: {
    title: 'Work Activities Test',
    statement: 'Your highest ratings show the kinds of work activities you currently prefer.',
    categories: {
      INFORMATION: ['Information', 'Examining facts, solving questions, and organizing or creating with information.'],
      PEOPLE: ['People', 'Helping, communicating, and working with others toward a shared outcome.'],
      THINGS: ['Things', 'Making, arranging, operating, or improving practical objects and tools.'],
    },
  },
}

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
