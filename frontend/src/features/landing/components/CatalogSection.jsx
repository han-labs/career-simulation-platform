import { useRef } from 'react'
import { ArrowRight, ChevronLeft, ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { coreEnabled } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import SimulationCard from '../../simulations/components/SimulationCard.jsx'
import SimulationCatalog from '../../simulations/components/SimulationCatalog.jsx'

function CatalogSection({ simulations, catalogStatus, onRetry }) {
  const { t } = useI18n()
  const carousel = useRef(null)
  function scroll(direction) {
    const list = carousel.current
    if (!list) return
    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    list.scrollBy({ left: direction * list.clientWidth * 0.8, behavior: reduced ? 'instant' : 'smooth' })
  }
  function navigateCarousel(event) {
    if (event.target !== event.currentTarget) return
    if (['ArrowLeft', 'ArrowRight'].includes(event.key)) {
      event.preventDefault(); scroll(event.key === 'ArrowRight' ? 1 : -1)
    } else if (['Home', 'End'].includes(event.key)) {
      event.preventDefault(); carousel.current.scrollTo({ left: event.key === 'Home' ? 0 : carousel.current.scrollWidth, behavior: 'instant' })
    }
  }
  const ready = catalogStatus === 'ready' && simulations.length > 0
  return <section className="home-catalog section-shell" id="starter-catalog" aria-labelledby="catalog-heading">
    <div className="home-catalog__heading"><h2 id="catalog-heading">{t('Try our free simulations')}</h2><Link className="text-link" to="/simulations">{t('View all simulations')} <ArrowRight size={18} aria-hidden="true" /></Link></div>
    {ready ? <div className="home-carousel">
      <button type="button" className="home-carousel__previous" aria-label={t('Previous simulations')} aria-controls="home-simulation-cards" onClick={() => scroll(-1)}><ChevronLeft size={20} aria-hidden="true" /></button>
      <div ref={carousel} id="home-simulation-cards" className="home-carousel__cards" role="region" aria-label={t('Career simulations')} tabIndex={0} onKeyDown={navigateCarousel}>
        {simulations.map(simulation => <SimulationCard key={simulation.id} simulation={simulation} compact />)}
      </div>
      <button type="button" className="home-carousel__next" aria-label={t('Next simulations')} aria-controls="home-simulation-cards" onClick={() => scroll(1)}><ChevronRight size={20} aria-hidden="true" /></button>
    </div> : <SimulationCatalog status={catalogStatus} simulations={[]} onRetry={onRetry} variant={coreEnabled ? 'core' : 'legacy'} />}
  </section>
}
export default CatalogSection
