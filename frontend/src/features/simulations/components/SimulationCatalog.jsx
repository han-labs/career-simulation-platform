// Displays catalog lifecycle states using local mock data only in Phase 2.
import { ArrowRight, Clock3, Code2, ListChecks, TriangleAlert } from 'lucide-react'
import { Link } from 'react-router-dom'

function SimulationCatalog({ status, simulations, onRetry }) {
  if (status === 'loading') {
    return <div className="catalog-grid" aria-label="Loading simulations">
      {[1, 2, 3].map((item) => <div className="catalog-skeleton" key={item} aria-hidden="true" />)}
    </div>
  }

  if (status === 'error') {
    return <section className="catalog-state" role="alert">
      <TriangleAlert size={28} aria-hidden="true" />
      <h2>Simulations are unavailable</h2>
      <p>Try loading the local catalog again.</p>
      <button className="button button--secondary" type="button" onClick={onRetry}>Try again</button>
    </section>
  }

  if (simulations.length === 0) {
    return <section className="catalog-state">
      <ListChecks size={28} aria-hidden="true" />
      <h2>No simulations yet</h2>
      <p>New representative IT tasks will appear here when they are ready.</p>
    </section>
  }

  return <div className="catalog-grid">
    {simulations.map((simulation) => (
      <article className="catalog-card" key={simulation.id}>
        <span className="catalog-card__icon"><Code2 size={25} aria-hidden="true" /></span>
        <p className="eyebrow">{simulation.difficulty}</p>
        <h2>{simulation.title}</h2>
        <p>{simulation.description}</p>
        <dl className="catalog-meta">
          <div><dt><Clock3 size={16} aria-hidden="true" /> Duration</dt><dd>{simulation.duration}</dd></div>
          <div><dt><ListChecks size={16} aria-hidden="true" /> Tasks</dt><dd>{simulation.taskCount}</dd></div>
        </dl>
        <Link className="button" to={`/simulations/${simulation.id}`}>
          Start <ArrowRight size={17} aria-hidden="true" />
        </Link>
      </article>
    ))}
  </div>
}

export default SimulationCatalog
