function AssessmentQuestionCard({ question, index, answer, setAnswer, isMissing, scaleOptions }) {
  return (
    <div id={`question-container-${question.id}`} className="assessment-question" style={{ 
      padding: '20px', 
      background: isMissing ? '#fcebe6' : 'var(--card)', 
      borderRadius: '16px', 
      border: `1px solid ${isMissing ? 'var(--accent-dark)' : answer ? 'var(--tech-soft)' : 'var(--line)'}`,
      transition: 'all 0.3s',
      boxShadow: answer ? '0 2px 8px rgba(31, 102, 121, 0.05)' : 'none'
    }}>
      <h3 style={{ fontSize: '1.15rem', margin: '0 0 24px 0', color: 'var(--ink)' }}>
        <span style={{ color: 'var(--muted)', marginRight: '12px' }}>{index + 1}.</span> 
        {question.prompt || question.text}
      </h3>
      
      <div className="assessment-options" style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
        {scaleOptions.map(opt => {
          const isSelected = answer === opt.value
          return (
            <label 
              key={opt.value} 
              style={{
                display: 'flex', 
                alignItems: 'center', 
                gap: '8px',
                padding: '14px 16px',
                border: `2px solid ${isSelected ? 'var(--tech)' : 'var(--line)'}`,
                borderRadius: '12px',
                cursor: 'pointer',
                background: isSelected ? 'var(--tech-soft)' : 'transparent',
                color: isSelected ? 'var(--tech-deep)' : 'var(--ink)',
                flex: '1',
                justifyContent: 'center',
                transition: 'all 0.15s ease'
              }}
            >
              <input
                type="radio"
                name={`question-${question.id}`}
                value={opt.value}
                checked={isSelected}
                onChange={() => setAnswer(question.id, opt.value)}
                style={{ margin: 0, accentColor: 'var(--tech)' }}
              />
              <span style={{ fontSize: '0.9rem', fontWeight: isSelected ? '600' : '400' }}>{opt.label}</span>
            </label>
          )
        })}
      </div>
    </div>
  )
}

export default AssessmentQuestionCard
