import { useI18n } from '../../../shared/i18n/useI18n.js'
import { useLayoutEffect, useState } from 'react'
import { Loader2 } from 'lucide-react'
import AssessmentProgressBar from './AssessmentProgressBar.jsx'
import AssessmentDemoControls from './AssessmentDemoControls.jsx'
import AssessmentQuestionCard from './AssessmentQuestionCard.jsx'
import { createDemoAnswers } from '../data/demoAnswers.js'

const scaleOptions = [
  { value: 1, label: 'Strongly Dislike' },
  { value: 2, label: 'Dislike' },
  { value: 3, label: 'Neutral' },
  { value: 4, label: 'Like' },
  { value: 5, label: 'Strongly Like' },
]

function AssessmentForm({
  questions, answers, setAnswer, setAllAnswers, onSubmit, isLoading,
  showDemo = true,
  scaleOptions: customScaleOptions,
  showDemoControls = true,
  demoProfiles,
}) {
  const { t } = useI18n()
  const [error, setError] = useState(null)
  const options = customScaleOptions || scaleOptions
  const valid = q => options.some(option => option.value === answers[q.id])
  const answeredCount = questions.filter(valid).length
  const totalCount = questions.length
  const progressPercent = totalCount > 0 ? (answeredCount / totalCount) * 100 : 0

  const [errorIds, setErrorIds] = useState([])
  useLayoutEffect(() => {
    if (!errorIds.length) return
    const element = document.getElementById(`question-container-${errorIds[0]}`)
    element?.querySelector('input')?.focus({ preventScroll: true })
    element?.scrollIntoView({ behavior: 'instant', block: 'center' })
  }, [errorIds])

  const handleSubmit = (e) => {
    e.preventDefault()
    const missing = questions.filter(q => q.required !== false && !valid(q)).map(q => q.id)
    if (missing.length) {
      setErrorIds(missing)
      setError('Please answer all required questions before submitting your career quiz.')
      
      return
    }
    setErrorIds([])
    setError(null)
    onSubmit()
  }

  const handleDemo = (type) => {
    const newAnswers = createDemoAnswers(questions, type, demoProfiles)
    if (setAllAnswers) setAllAnswers(newAnswers)
  }

  return (
    <form onSubmit={handleSubmit} className="assessment-form">
      <fieldset disabled={isLoading} style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}>
      <AssessmentProgressBar answeredCount={answeredCount} totalCount={totalCount} progressPercent={progressPercent} />
      
{showDemo && showDemoControls && (
  <AssessmentDemoControls onDemo={handleDemo} profiles={demoProfiles} />
)}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        {questions.map((q, index) => (
          <AssessmentQuestionCard
            key={q.id}
            question={q}
            index={index}
            answer={answers[q.id]}
            setAnswer={setAnswer}
            isMissing={errorIds.includes(q.id) && !valid(q)}
            scaleOptions={options}
          />
        ))}
      </div>

      {error && (
        <div role="alert" style={{ color: 'var(--accent-dark)', marginTop: '32px', fontWeight: 'bold', padding: '16px', background: '#fcebe6', borderRadius: '12px', border: '1px solid var(--accent)' }}>
          {t(error)}
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '48px', paddingTop: '32px', borderTop: '1px solid var(--line)' }}>
        <button 
          type="submit" 
          disabled={isLoading}
          style={{ 
            background: 'var(--assessment-submit, var(--ink))',
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
          {isLoading ? <><Loader2 className="animate-spin" size={20} /> {t("Submitting...")}</> : t('Submit Answers')}
        </button>
      </div>
      </fieldset>
    </form>
  )
}

export default AssessmentForm
