import { useI18n } from '../../../shared/i18n/useI18n.js'
import { ArrowRight, BookOpen } from 'lucide-react'
import { Link } from 'react-router-dom'
import SimulationCard from '../../simulations/components/SimulationCard.jsx'

function CatalogSection({ simulations, catalogStatus }) {
  const { t } = useI18n()
  return (
    <section className="section section--catalog" id="starter-catalog" aria-labelledby="catalog-heading">
      <div className="section-shell">
        <div className="section-heading">
          <span className="eyebrow">
            <BookOpen size={14} aria-hidden="true" /> {t("Featured Simulation Catalog")} </span>
          <h2 id="catalog-heading">{t("Try a career simulation")}</h2>
          <p className="section-heading__sub"> {t("Pick a role and experience a practical task.")} </p>
        </div>

        {catalogStatus === 'fallback' && (
          <p className="catalog-notice" role="status"> {t("Showing sample simulation data while the backend server is starting up.")} </p>
        )}

        <div className="simulation-grid">
          {simulations.map((simulation) => (
            <SimulationCard key={simulation.id} simulation={simulation} />
          ))}
        </div>

        <div className="section-action">
          <Link className="button button--primary" to="/simulations"> {t("View All Simulations")} <ArrowRight size={18} aria-hidden="true" />
          </Link>
        </div>
      </div>
    </section>
  )
}

export default CatalogSection
