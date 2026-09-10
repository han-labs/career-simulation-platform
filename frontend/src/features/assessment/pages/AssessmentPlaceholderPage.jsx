import { ArrowLeft, ClipboardList } from 'lucide-react'
import { Link } from 'react-router-dom'

function AssessmentPlaceholderPage() {
  return (
    <section className="feature-page section-shell">
      <Link className="back-link" to="/">
        <ArrowLeft size={17} aria-hidden="true" /> Back to overview
      </Link>
      <div className="feature-page__panel">
        <span className="feature-page__icon"><ClipboardList size={28} /></span>
        <span className="eyebrow">US-01 route ready</span>
        <h1>RIASEC assessment</h1>
        <p>
          This route is reserved for the deterministic six-dimension assessment,
          progress recovery, score explanation, and initial IT directions.
        </p>
        <div className="feature-page__contract">
          <strong>Implementation guardrail</strong>
          <span>The score is calculated by approved rules, never by an LLM.</span>
        </div>
      </div>
    </section>
  )
}

export default AssessmentPlaceholderPage
