import { ArrowRight, BookOpen } from 'lucide-react'
import { Link } from 'react-router-dom'
import SimulationCard from '../../simulations/components/SimulationCard.jsx'

function CatalogSection({ simulations, catalogStatus }) {
  return (
    <section className="section section--catalog" id="starter-catalog" aria-labelledby="catalog-heading">
      <div className="section-shell">
        <div className="section-heading">
          <span className="eyebrow">
            <BookOpen size={14} aria-hidden="true" /> Featured Simulation Catalog
          </span>
          <h2 id="catalog-heading">Practice Authentic Tasks in 35 – 45 Minutes</h2>
          <p className="section-heading__sub">
            Pick a simulation and start solving real-world workplace scenarios for your chosen IT role.
          </p>
        </div>

        {catalogStatus === 'fallback' && (
          <p className="catalog-notice" role="status">
            Showing sample simulation data while the backend server is starting up.
          </p>
        )}

        <div className="simulation-grid">
          {simulations.map((simulation) => (
            <SimulationCard key={simulation.id} simulation={simulation} />
          ))}
        </div>

        <div className="section-action">
          <Link className="button button--primary" to="/simulations">
            View All Simulations <ArrowRight size={18} aria-hidden="true" />
          </Link>
        </div>
      </div>
    </section>
  )
}

export default CatalogSection
