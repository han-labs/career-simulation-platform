import { ArrowLeft, MessageSquareText } from 'lucide-react'
import { Link } from 'react-router-dom'

function GuidancePlaceholderPage() {
  return (
    <section className="feature-page section-shell">
      <Link className="back-link" to="/">
        <ArrowLeft size={17} aria-hidden="true" /> Back to overview
      </Link>
      <div className="feature-page__panel">
        <span className="feature-page__icon"><MessageSquareText size={28} /></span>
        <span className="eyebrow">US-03 route ready</span>
        <h1>Reflection and guidance</h1>
        <p>
          This route is reserved for strengths, difficulties, evidence citations,
          possible next activities, AI provenance, and transparent fallback states.
        </p>
        <div className="feature-page__contract">
          <strong>Implementation guardrail</strong>
          <span>AI guidance is advisory and must degrade to reviewed templates.</span>
        </div>
      </div>
    </section>
  )
}

export default GuidancePlaceholderPage
