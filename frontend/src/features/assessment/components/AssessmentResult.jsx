import { riasecDescriptions } from '../data/mockQuestions.js'

function AssessmentResult({ result, onRestart }) {
  const { scores, topDimension } = result
  const topInfo = riasecDescriptions[topDimension]

  // Sort dimensions by score descending
  const sortedDimensions = Object.entries(scores)
    .sort((a, b) => b[1] - a[1])
    .map(([dim, score]) => ({
      dim,
      score,
      info: riasecDescriptions[dim]
    }))

  const maxPossibleScore = 10 // Since 2 questions per dimension * 5 max points

  return (
    <div className="assessment-result">
      <div className="hero-board" style={{ marginBottom: '48px' }}>
        <div className="hero-board__header">
          Your Strongest Interest Area
          <span className="pill pill--blue">{topInfo.name}</span>
        </div>
        
        <h2 style={{ fontSize: '2rem', marginBottom: '12px', marginTop: 0 }}>{topInfo.name}</h2>
        <p style={{ color: 'var(--muted)', fontSize: '1.05rem', lineHeight: '1.6', maxWidth: '600px' }}>
          {topInfo.description}
        </p>

        <h3 style={{ fontSize: '1rem', marginTop: '32px', marginBottom: '16px' }}>Exploration Directions</h3>
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          {topInfo.directions.map(dir => (
            <span key={dir} className="pill" style={{ background: topInfo.bgColor, color: topInfo.color }}>
              {dir}
            </span>
          ))}
        </div>
      </div>

      <h3 style={{ fontSize: '1.25rem', marginBottom: '24px' }}>Full Dimension Breakdown</h3>
      <div style={{ display: 'grid', gap: '16px', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))' }}>
        {sortedDimensions.map(({ dim, score, info }) => (
          <div key={dim} style={{ padding: '20px', background: 'var(--card)', borderRadius: 'var(--radius-md)', border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', fontWeight: 'bold' }}>
              <span style={{ color: info.color }}>{info.name}</span>
              <span>{score} / {maxPossibleScore}</span>
            </div>
            {/* Progress bar */}
            <div style={{ height: '8px', background: 'var(--line)', borderRadius: '4px', overflow: 'hidden' }}>
              <div 
                style={{ 
                  height: '100%', 
                  background: info.color, 
                  width: `${(score / maxPossibleScore) * 100}%`,
                  transition: 'width 0.5s ease-out'
                }} 
              />
            </div>
          </div>
        ))}
      </div>

      <div style={{ marginTop: '48px', paddingTop: '24px', borderTop: '1px solid var(--line)', textAlign: 'center' }}>
        <p style={{ color: 'var(--muted)', marginBottom: '24px', fontSize: '0.9rem' }}>
          These results provide exploration options, not final career predictions.
        </p>
        <button className="button button--secondary" onClick={onRestart}>
          Take Assessment Again
        </button>
      </div>
    </div>
  )
}

export default AssessmentResult
