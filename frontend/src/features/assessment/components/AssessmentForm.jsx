import { useState } from 'react'
import { Loader2 } from 'lucide-react'

const scaleOptions = [
  { value: 1, label: 'Strongly Dislike' },
  { value: 2, label: 'Dislike' },
  { value: 3, label: 'Neutral' },
  { value: 4, label: 'Like' },
  { value: 5, label: 'Strongly Like' },
]

function AssessmentForm({ questions, answers, setAnswer, onSubmit, isLoading }) {
  const [error, setError] = useState(null)
  const answeredCount = Object.keys(answers).length
  const totalCount = questions.length
  const progressPercent = totalCount > 0 ? (answeredCount / totalCount) * 100 : 0

  const [errorIds, setErrorIds] = useState([])

  const handleSubmit = (e) => {
    e.preventDefault()
    if (answeredCount < totalCount) {
      const missing = questions.filter(q => answers[q.id] === undefined).map(q => q.id)
      setErrorIds(missing)
      setError(`Please answer all questions before submitting. (${totalCount - answeredCount} left)`)
      
      // Find first unanswered question and scroll to it
      if (missing.length > 0) {
        setTimeout(() => {
          const element = document.getElementById(`question-container-${missing[0]}`)
          if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' })
          }
        }, 100)
      }
      return
    }
    setErrorIds([])
    setError(null)
    onSubmit()
  }

  return (
    <form onSubmit={handleSubmit} className="assessment-form">
      {/* Sticky Progress Bar */}
      <div style={{ 
        position: 'sticky', 
        top: '20px', 
        background: 'var(--card)', 
        padding: '16px 24px', 
        borderRadius: '16px', 
        boxShadow: '0 4px 12px rgba(0,0,0,0.08)', 
        zIndex: 10,
        marginBottom: '32px',
        border: '1px solid var(--line)'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '0.9rem', fontWeight: 'bold', color: 'var(--ink)' }}>
          <span>Progress</span>
          <span>{answeredCount} / {totalCount} completed</span>
        </div>
        <div style={{ height: '8px', background: 'var(--line)', borderRadius: '4px', overflow: 'hidden' }}>
          <div style={{ 
            height: '100%', 
            background: 'var(--tech)', 
            width: `${progressPercent}%`,
            transition: 'width 0.3s ease-out'
          }} />
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        {questions.map((q, index) => {
          const isMissing = errorIds.includes(q.id)
          return (
            <div key={q.id} id={`question-container-${q.id}`} className="assessment-question" style={{ 
              padding: '32px', 
              background: isMissing ? '#fcebe6' : 'var(--card)', 
              borderRadius: '16px', 
              border: `1px solid ${isMissing ? 'var(--accent-dark)' : answers[q.id] ? 'var(--tech-soft)' : 'var(--line)'}`,
              transition: 'all 0.3s',
              boxShadow: answers[q.id] ? '0 2px 8px rgba(31, 102, 121, 0.05)' : 'none'
            }}>
            <h3 style={{ fontSize: '1.15rem', margin: '0 0 24px 0', color: 'var(--ink)' }}>
              <span style={{ color: 'var(--muted)', marginRight: '12px' }}>{index + 1}.</span> 
              {q.prompt || q.text}
            </h3>
            
            <div className="assessment-options" style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
              {scaleOptions.map(opt => {
                const isSelected = answers[q.id] === opt.value
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
                      flex: '1 1 auto',
                      minWidth: '140px',
                      justifyContent: 'center',
                      transition: 'all 0.15s ease'
                    }}
                  >
                    <input
                      type="radio"
                      name={`question-${q.id}`}
                      value={opt.value}
                      checked={isSelected}
                      onChange={() => setAnswer(q.id, opt.value)}
                      style={{ margin: 0, accentColor: 'var(--tech)' }}
                    />
                    <span style={{ fontSize: '0.9rem', fontWeight: isSelected ? '600' : '400' }}>{opt.label}</span>
                  </label>
                )
              })}
            </div>
          </div>
          )
        })}
      </div>

      {error && (
        <div style={{ color: 'var(--accent-dark)', marginTop: '32px', fontWeight: 'bold', padding: '16px', background: '#fcebe6', borderRadius: '12px', border: '1px solid var(--accent)' }}>
          {error}
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '48px', paddingTop: '32px', borderTop: '1px solid var(--line)' }}>
        <button 
          type="submit" 
          disabled={isLoading}
          style={{ 
            background: 'var(--ink)', 
            color: 'white', 
            padding: '16px 40px', 
            borderRadius: '12px', 
            fontSize: '1.1rem', 
            fontWeight: 'bold', 
            border: 'none', 
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}
        >
          {isLoading ? <><Loader2 className="animate-spin" size={20} /> Submitting...</> : 'Submit Answers'}
        </button>
      </div>
    </form>
  )
}

export default AssessmentForm
