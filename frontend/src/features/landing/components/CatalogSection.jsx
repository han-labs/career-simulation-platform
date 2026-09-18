import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import SimulationCard from '../../simulations/components/SimulationCard.jsx'

function CatalogSection({ simulations, catalogStatus }) {
  return (
    <section className="section section--ink" id="starter-catalog">
      <div className="section-shell">
        <div className="section-heading section-heading--inverse">
          <span className="eyebrow">Starter catalog</span>
          <h2>Try a compact simulation</h2>
        </div>
        {catalogStatus === 'fallback' && (
          <p className="catalog-notice" role="status">
            Demo previews are shown while the backend is unavailable.
          </p>
        )}
        <div className="simulation-grid">
          {simulations.map((simulation) => (
            <SimulationCard key={simulation.id} simulation={simulation} />
          ))}
        </div>
        <div className="section-action">
          <Link className="button button--light" to="/simulations">
            See all simulations <ArrowRight size={18} aria-hidden="true" />
          </Link>
        </div>
      </div>
    </section>
  )
}

export default CatalogSection
