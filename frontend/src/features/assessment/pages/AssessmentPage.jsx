import { ArrowLeft, ClipboardList } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAssessment } from '../hooks/useAssessment.js'
import { mockQuestions } from '../data/mockQuestions.js'
import { scoreAssessment } from '../utils/riasecScorer.js'
import AssessmentForm from '../components/AssessmentForm.jsx'
import AssessmentResult from '../components/AssessmentResult.jsx'

function AssessmentPage() {
  const { answers, status, result, setAnswer, start, submit, reset } = useAssessment()

  const handleSubmit = () => {
    const calculated = scoreAssessment(answers, mockQuestions)
    submit(calculated)
  }

  return (
    <section className="feature-page section-shell">
      <Link className="back-link" to="/">
        <ArrowLeft size={17} aria-hidden="true" /> Back to overview
      </Link>
      
      {status === 'intro' && (
        <div className="feature-page__panel">
          <span className="feature-page__icon"><ClipboardList size={28} /></span>
          <span className="eyebrow">RIASEC Assessment</span>
          <h1>Discover Your Path</h1>
          <p>
            This assessment helps you explore your interests across six dimensions: 
            Realistic, Investigative, Artistic, Social, Enterprising, and Conventional.
            Your score will provide directions to explore further.
          </p>
          <div className="feature-page__contract" style={{ marginBottom: '32px' }}>
            <strong>Implementation guardrail</strong>
            <span>The score is calculated by approved deterministic rules, not by AI.</span>
          </div>
          <button className="button" onClick={start}>
            Start Assessment
          </button>
        </div>
      )}

      {status === 'in_progress' && (
        <div style={{ maxWidth: '800px', margin: '48px auto 0' }}>
          <h2 style={{ fontSize: '2rem', marginBottom: '8px', fontFamily: 'Georgia, serif' }}>Assessment Questions</h2>
          <p style={{ color: 'var(--muted)', marginBottom: '32px' }}>
            Read each statement and select how much you like or dislike the activity.
            Your progress is autosaved locally.
          </p>
          <AssessmentForm 
            questions={mockQuestions}
            answers={answers}
            setAnswer={setAnswer}
            onSubmit={handleSubmit}
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
