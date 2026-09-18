import { useEffect, useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAssessment } from '../hooks/useAssessment.js'
import AssessmentForm from '../components/AssessmentForm.jsx'
import AssessmentResult from '../components/AssessmentResult.jsx'
import AssessmentIntro from '../components/AssessmentIntro.jsx'

function AssessmentPage() {
  const [isTakingTest, setIsTakingTest] = useState(false);
  const { 
    questions, isLoading, error, fetchQuestions, 
    answers, status, result, setAnswer, setAllAnswers, start, submit, reset 
  } = useAssessment()

  useEffect(() => {
    fetchQuestions()
  }, [fetchQuestions])

  useEffect(() => {
    setTimeout(() => {
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
    }, 0);
  }, [status, isTakingTest]);

  const handleSubmit = () => {
    submit()
  }

  const handleStartOrResume = () => {
    if (status === 'intro') {
      start();
    }
    setIsTakingTest(true);
  }

  const handleReset = () => {
    reset();
    setIsTakingTest(false);
  }

  return (
    <section className="feature-page section-shell" style={{ backgroundColor: 'var(--page-bg)', minHeight: '100vh', paddingBottom: '48px' }}>
      <Link className="back-link" to="/" style={{ color: 'var(--ink)' }}>
        <ArrowLeft size={17} aria-hidden="true" /> Back to overview
      </Link>
      
      {error && (
        <div style={{ background: '#fcebe6', color: 'var(--accent-dark)', padding: '16px', borderRadius: '8px', marginBottom: '24px', border: '1px solid var(--accent)' }}>
          <strong>Error: </strong> {error}
        </div>
      )}

      {(!isTakingTest || status === 'intro') && status !== 'completed' && (
        <AssessmentIntro 
          onStartOrResume={handleStartOrResume} 
          isLoading={isLoading} 
          isReady={questions.length > 0} 
          status={status} 
        />
      )}

      {status === 'in_progress' && isTakingTest && (
        <div style={{ maxWidth: '800px', margin: '16px auto 0' }}>
          <div style={{ marginBottom: '16px' }}>
            <h2 style={{ fontSize: '2.5rem', margin: '0 0 8px 0', fontFamily: 'Georgia, serif', color: 'var(--ink)' }}>Assessment Questions</h2>
            <p style={{ color: 'var(--muted)', fontSize: '1.1rem' }}>
              Read each statement and select how much you like or dislike the activity.
              Don't overthink it, just go with your first instinct.
            </p>
          </div>
          <AssessmentForm 
            questions={questions}
            answers={answers}
            setAnswer={setAnswer}
            setAllAnswers={setAllAnswers}
            onSubmit={handleSubmit}
            isLoading={isLoading}
          />
        </div>
      )}

      {status === 'completed' && result && (
        <div style={{ maxWidth: '960px', margin: '16px auto 0' }}>
          <AssessmentResult result={result} onRestart={handleReset} />
        </div>
      )}
    </section>
  )
}

export default AssessmentPage
