import { useState } from 'react'
import { Loader2 } from 'lucide-react'
import AssessmentProgressBar from './AssessmentProgressBar.jsx'
import AssessmentDemoControls from './AssessmentDemoControls.jsx'
import AssessmentQuestionCard from './AssessmentQuestionCard.jsx'

const scaleOptions = [
  { value: 1, label: 'Strongly Dislike' },
  { value: 2, label: 'Dislike' },
  { value: 3, label: 'Neutral' },
  { value: 4, label: 'Like' },
  { value: 5, label: 'Strongly Like' },
]

function AssessmentForm({ questions, answers, setAnswer, setAllAnswers, onSubmit, isLoading }) {
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

  const handleDemo = (type) => {
    const newAnswers = {}
    questions.forEach((q, index) => {
      if (type === 'backend') {
        newAnswers[q.id] = (index % 6 === 1 || index % 6 === 5) ? 5 : ((index % 6 === 0) ? 4 : 2)
      } else if (type === 'frontend') {
        newAnswers[q.id] = (index % 6 === 2 || index % 6 === 3) ? 5 : ((index % 6 === 4) ? 4 : 2)
      } else {
        newAnswers[q.id] = (index % 6 === 4 || index % 6 === 5) ? 5 : ((index % 6 === 2) ? 4 : 2)
      }
    })
    if (setAllAnswers) setAllAnswers(newAnswers)
  }

  return (
    <form onSubmit={handleSubmit} className="assessment-form">
      <AssessmentProgressBar answeredCount={answeredCount} totalCount={totalCount} progressPercent={progressPercent} />
      
      <AssessmentDemoControls onDemo={handleDemo} />

      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        {questions.map((q, index) => (
          <AssessmentQuestionCard
            key={q.id}
            question={q}
            index={index}
            answer={answers[q.id]}
            setAnswer={setAnswer}
            isMissing={errorIds.includes(q.id)}
            scaleOptions={scaleOptions}
          />
        ))}
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
