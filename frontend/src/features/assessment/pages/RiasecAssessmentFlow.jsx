import { useEffect, useRef, useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAssessment } from '../hooks/useAssessment.js'
import AssessmentForm from '../components/AssessmentForm.jsx'
import AssessmentResult from '../components/AssessmentResult.jsx'
import { createDemoAnswers } from '../data/demoAnswers.js'

function RiasecAssessmentFlow() {
  const [isTakingTest, setIsTakingTest] = useState(false)
  const [searchParams] = useSearchParams()
  const demoProfile = searchParams.get('demo')
  const demoHandled = useRef(false)
  const storageKey = demoProfile ? `careersim_assessment_demo_${demoProfile}_v1` : undefined
  const { questions, isLoading, error, fetchQuestions, answers, status, result, setAnswer, setAllAnswers, start, submit, reset } = useAssessment(storageKey ? { storageKey } : undefined)

  useEffect(() => { fetchQuestions() }, [fetchQuestions])
  useEffect(() => { window.scrollTo({ top: 0, left: 0, behavior: 'instant' }) }, [status, isTakingTest])

  useEffect(() => {
    if (!demoProfile || !['backend', 'frontend', 'data'].includes(demoProfile) || isLoading || questions.length === 0 || demoHandled.current) return
    demoHandled.current = true
    start().then((started) => {
      if (!started) return
      setAllAnswers(createDemoAnswers(questions, demoProfile))
      setIsTakingTest(true)
    })
  }, [demoProfile, isLoading, questions, start, setAllAnswers])

  const handleStartOrResume = async () => {
    if (status === 'intro' && !(await start())) return
    setIsTakingTest(true)
  }
  const handleReset = () => { reset(); setIsTakingTest(false) }

  return (
    <section className="feature-page section-shell" style={{ backgroundColor: 'var(--page-bg)', minHeight: '100vh', paddingBottom: '48px' }}>
      <Link className="back-link" to="/assessment" style={{ color: 'var(--ink)' }}><ArrowLeft size={17} aria-hidden="true" /> All career tests</Link>
      {error && <div role="alert" className="assessment-api-error"><strong>Something went wrong: </strong>{error}</div>}
      {status !== 'completed' && !isTakingTest && (
        <div className="assessment-discovery">
          <h1>RIASEC Interest Assessment</h1>
          <p>Rate each activity based on how much you like it. You can pause and resume in this browser.</p>
          <button className="button button--primary" type="button" onClick={handleStartOrResume} disabled={isLoading || questions.length === 0}>
            {isLoading ? 'Loading…' : status === 'in_progress' ? 'Resume assessment' : 'Start assessment'}
          </button>
        </div>
      )}
      {status === 'in_progress' && isTakingTest && (
        <div style={{ maxWidth: '800px', margin: '16px auto 0' }}>
          {demoProfile && <p className="assessment-demo-notice" role="status">Quick demo answers are prefilled. Review or change them before submitting for backend-scored results.</p>}
          <h1>Explore your interests</h1>
          <p>Read each activity and choose how much you like it. Answer based on how you feel today.</p>
          <AssessmentForm questions={questions} answers={answers} setAnswer={setAnswer} setAllAnswers={setAllAnswers} onSubmit={submit} isLoading={isLoading} />
        </div>
      )}
      {status === 'completed' && result && <div style={{ maxWidth: '960px', margin: '16px auto 0' }}><AssessmentResult result={result} onRestart={handleReset} /></div>}
    </section>
  )
}

export default RiasecAssessmentFlow
