import { useEffect } from 'react'
import { ArrowLeft, ClipboardList, Loader2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAssessment } from '../hooks/useAssessment.js'
import AssessmentForm from '../components/AssessmentForm.jsx'
import AssessmentResult from '../components/AssessmentResult.jsx'

function AssessmentPage() {
  const { 
    questions, isLoading, error, fetchQuestions, 
    answers, status, result, setAnswer, start, submit, reset 
  } = useAssessment()

  useEffect(() => {
    fetchQuestions()
  }, [fetchQuestions])

  const handleSubmit = () => {
    submit()
  }

  return (
    <section className="feature-page section-shell" style={{ backgroundColor: 'var(--page-bg)', minHeight: '100vh', paddingBottom: '96px' }}>
      <Link className="back-link" to="/" style={{ color: 'var(--ink)' }}>
        <ArrowLeft size={17} aria-hidden="true" /> Back to overview
      </Link>
      
      {error && (
        <div style={{ background: '#fcebe6', color: 'var(--accent-dark)', padding: '16px', borderRadius: '8px', marginBottom: '24px', border: '1px solid var(--accent)' }}>
          <strong>Error: </strong> {error}
        </div>
      )}

      {status === 'intro' && (
        <div className="feature-page__panel" style={{ background: 'var(--card)', borderRadius: '28px', padding: '48px', maxWidth: '720px', margin: '48px auto', border: '1px solid var(--line)', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
          <span className="feature-page__icon" style={{ background: 'var(--sage)', color: 'var(--sage-ink)', display: 'inline-flex', padding: '16px', borderRadius: '16px', marginBottom: '24px' }}>
            <ClipboardList size={32} />
          </span>
          <span className="eyebrow" style={{ color: 'var(--muted)', textTransform: 'uppercase', fontSize: '12px', fontWeight: 'bold', letterSpacing: '1px', display: 'block', marginBottom: '8px' }}>RIASEC Assessment</span>
          <h1 style={{ fontFamily: 'Georgia, serif', fontSize: '3rem', margin: '0 0 16px 0', color: 'var(--ink)' }}>Discover Your Path</h1>
          <p style={{ color: 'var(--muted)', fontSize: '1.1rem', lineHeight: '1.7', marginBottom: '32px' }}>
            This 42-question assessment helps you explore your interests across six dimensions: 
            Realistic, Investigative, Artistic, Social, Enterprising, and Conventional.
            Taking roughly 5-10 minutes, your score will provide objective directions to explore further in tech careers.
          </p>
          <div className="feature-page__contract" style={{ background: 'var(--page-bg)', padding: '16px 20px', borderRadius: '12px', marginBottom: '32px', border: '1px solid var(--line)' }}>
            <strong style={{ display: 'block', marginBottom: '4px', color: 'var(--ink)' }}>Implementation guardrail</strong>
            <span style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>The score is calculated by approved deterministic rules, not by AI.</span>
          </div>
          <button 
            className="button" 
            onClick={start}
            disabled={isLoading || questions.length === 0}
            style={{ background: 'var(--ink)', color: 'white', padding: '16px 32px', borderRadius: '12px', fontSize: '1.1rem', fontWeight: 'bold', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px' }}
          >
            {isLoading ? <><Loader2 className="animate-spin" size={20} /> Loading...</> : 'Start Assessment'}
          </button>
        </div>
      )}

      {status === 'in_progress' && (
        <div style={{ maxWidth: '800px', margin: '48px auto 0' }}>
          <div style={{ marginBottom: '48px' }}>
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
            onSubmit={handleSubmit}
            isLoading={isLoading}
          />
        </div>
      )}

      {status === 'completed' && result && (
        <div style={{ maxWidth: '960px', margin: '48px auto 0' }}>
          <AssessmentResult result={result} onRestart={reset} />
        </div>
      )}
    </section>
  )
}

export default AssessmentPage
