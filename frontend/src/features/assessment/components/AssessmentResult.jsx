import { riasecDescriptions } from '../data/mockQuestions.js'

function AssessmentResult({ result, onRestart }) {
  const { scores, topDimensions } = result
  
  // Create a sorted list based on the scores returned from backend
  const sortedDimensions = scores
    .sort((a, b) => b.score - a.score)
    .map(dimScore => ({
      dim: dimScore.dimension,
      score: dimScore.score,
      info: riasecDescriptions[dimScore.dimension]
    }))

  const topDimension = topDimensions[0]
  const topInfo = riasecDescriptions[topDimension]

  const maxPossibleScore = 35 // 7 questions per dimension * 5 max points

  return (
    <div className="assessment-result">
      <div className="hero-board" style={{ 
        marginBottom: '32px', 
        padding: '32px', 
        background: 'var(--card)', 
        borderRadius: '24px', 
        border: '1px solid var(--line)',
        boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '24px' }}>
          <span style={{ color: 'var(--muted)', textTransform: 'uppercase', fontSize: '0.85rem', fontWeight: 'bold', letterSpacing: '1px' }}>
            Your Strongest Interest Area
          </span>
          <span className="pill" style={{ background: topInfo.bgColor, color: topInfo.color, padding: '4px 12px', borderRadius: '100px', fontSize: '0.85rem', fontWeight: 'bold' }}>
            {topInfo.name}
          </span>
        </div>
        
        <h2 style={{ fontSize: '3.5rem', margin: '0 0 16px 0', fontFamily: 'Georgia, serif', color: 'var(--ink)' }}>{topInfo.name}</h2>
        <p style={{ color: 'var(--muted)', fontSize: '1.15rem', lineHeight: '1.7', maxWidth: '640px' }}>
          {topInfo.description}
        </p>

        <h3 style={{ fontSize: '1.1rem', marginTop: '24px', marginBottom: '12px', color: 'var(--ink)' }}>Exploration Directions</h3>
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          {topInfo.directions.map(dir => (
            <span key={dir} style={{ background: 'var(--page-bg)', color: 'var(--ink)', padding: '10px 16px', borderRadius: '12px', fontSize: '0.95rem', border: '1px solid var(--line)' }}>
              {dir}
            </span>
          ))}
        </div>
      </div>

      <h3 style={{ fontSize: '1.5rem', marginBottom: '20px', fontFamily: 'Georgia, serif', color: 'var(--ink)' }}>Full Dimension Breakdown</h3>
      <div style={{ display: 'grid', gap: '16px', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))' }}>
        {sortedDimensions.map(({ dim, score, info }) => (
          <div key={dim} style={{ padding: '16px', background: 'var(--card)', borderRadius: '16px', border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', alignItems: 'center' }}>
              <span style={{ color: info.color, fontWeight: 'bold', fontSize: '1.1rem' }}>{info.name}</span>
              <span style={{ color: 'var(--muted)', fontSize: '0.95rem' }}>{score} / {maxPossibleScore}</span>
            </div>
            {/* Progress bar */}
            <div style={{ height: '10px', background: 'var(--page-bg)', borderRadius: '5px', overflow: 'hidden' }}>
              <div 
                style={{ 
                  height: '100%', 
                  background: info.color, 
                  width: `${(score / maxPossibleScore) * 100}%`,
                  transition: 'width 1s cubic-bezier(0.22, 1, 0.36, 1)'
                }} 
              />
            </div>
          </div>
        ))}
      </div>

      <div style={{ marginTop: '40px', paddingTop: '24px', borderTop: '1px solid var(--line)', textAlign: 'center' }}>
        <p style={{ color: 'var(--muted)', marginBottom: '24px', fontSize: '0.95rem', maxWidth: '600px', margin: '0 auto 24px' }}>
          <strong>Implementation guardrail:</strong> These results are calculated by deterministic scoring rules and provide exploration options, not final career predictions. No AI was used to calculate this score.
        </p>
        <button 
          onClick={onRestart}
          style={{ 
            background: 'transparent', 
            color: 'var(--ink)', 
            padding: '14px 28px', 
            borderRadius: '12px', 
            fontSize: '1rem', 
            fontWeight: '600', 
            border: '2px solid var(--line)', 
            cursor: 'pointer' 
          }}
        >
          Retake Assessment
        </button>
      </div>
    </div>
  )
}

export default AssessmentResult
