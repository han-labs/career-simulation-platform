import { useState } from 'react'

const scaleOptions = [
  { value: 1, label: 'Strongly Dislike' },
  { value: 2, label: 'Dislike' },
  { value: 3, label: 'Neutral' },
  { value: 4, label: 'Like' },
  { value: 5, label: 'Strongly Like' },
]

function AssessmentForm({ questions, answers, setAnswer, onSubmit }) {
  const [error, setError] = useState(null)

  const handleSubmit = (e) => {
    e.preventDefault()
    
    // Validate all questions answered
    const unanswered = questions.filter(q => answers[q.id] === undefined)
    if (unanswered.length > 0) {
      setError(`Please answer all questions before submitting. (${unanswered.length} left)`)
      return
    }

    setError(null)
    onSubmit()
  }

  return (
    <form onSubmit={handleSubmit} className="assessment-form">
      {questions.map((q, index) => (
        <div key={q.id} className="assessment-question" style={{ marginBottom: '32px', padding: '24px', background: 'var(--card)', borderRadius: 'var(--radius-md)', border: '1px solid var(--line)' }}>
          <h3 style={{ fontSize: '1.1rem', marginTop: 0, marginBottom: '20px' }}>
            {index + 1}. {q.text}
          </h3>
          
          <div className="assessment-options" style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
            {scaleOptions.map(opt => (
              <label 
                key={opt.value} 
                style={{
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '8px',
                  padding: '12px 16px',
                  border: `1px solid ${answers[q.id] === opt.value ? 'var(--tech)' : 'var(--line)'}`,
                  borderRadius: '12px',
                  cursor: 'pointer',
                  background: answers[q.id] === opt.value ? 'var(--tech-soft)' : 'transparent',
                  flex: '1 1 auto',
                  minWidth: '140px',
                  justifyContent: 'center'
                }}
              >
                <input
                  type="radio"
                  name={`question-${q.id}`}
                  value={opt.value}
                  checked={answers[q.id] === opt.value}
                  onChange={() => setAnswer(q.id, opt.value)}
                  style={{ margin: 0 }}
                />
                <span style={{ fontSize: '0.88rem', fontWeight: answers[q.id] === opt.value ? '600' : '400' }}>{opt.label}</span>
              </label>
            ))}
          </div>
        </div>
      ))}

      {error && (
        <div style={{ color: 'var(--accent-dark)', marginBottom: '16px', fontWeight: 'bold', padding: '12px', background: '#fcebe6', borderRadius: '8px' }}>
          {error}
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '24px' }}>
        <span style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>
          {Object.keys(answers).length} of {questions.length} answered
        </span>
        <button type="submit" className="button">
          Submit answers
        </button>
      </div>
    </form>
  )
}

export default AssessmentForm
