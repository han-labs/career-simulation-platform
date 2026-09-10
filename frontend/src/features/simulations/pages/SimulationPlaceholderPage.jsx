import { ArrowLeft, Boxes } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

function SimulationPlaceholderPage() {
  const { slug } = useParams()

  return (
    <section className="feature-page section-shell">
      <Link className="back-link" to="/">
        <ArrowLeft size={17} aria-hidden="true" /> Back to overview
      </Link>
      <div className="feature-page__panel">
        <span className="feature-page__icon"><Boxes size={28} /></span>
        <span className="eyebrow">US-02 route ready</span>
        <h1>{slug ? 'Simulation workspace' : 'Career simulation catalog'}</h1>
        <p>
          This route is reserved for task instructions, autosaved attempts,
          deterministic evaluation, task-level evidence, and completion states.
        </p>
        {slug && <code className="route-chip">simulation: {slug}</code>}
        <div className="feature-page__contract">
          <strong>Implementation guardrail</strong>
          <span>Do not expose answer keys or evaluation rules in public API DTOs.</span>
        </div>
      </div>
    </section>
  )
}

export default SimulationPlaceholderPage
