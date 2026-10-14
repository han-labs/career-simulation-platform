import { useI18n } from '../../../shared/i18n/useI18n.js'
// Displays catalog lifecycle states using local mock data only in Phase 2.
import { ArrowRight, Clock3, Code2, ListChecks, TriangleAlert } from 'lucide-react'
import { Link } from 'react-router-dom'
import { localized } from '../../../shared/api/coreApi.js'

function SimulationCatalog({ status, simulations, onRetry }) {
  const { t, locale } = useI18n()
  if (status === 'loading') {
    return <div className="catalog-grid" aria-label={t("Loading simulations")}>
      {[1, 2, 3].map((item) => <div className="catalog-skeleton" key={item} aria-hidden="true" />)}
    </div>
  }

  if (status === 'error') {
    return <section className="catalog-state" role="alert">
      <TriangleAlert size={28} aria-hidden="true" />
      <h2>{t("Simulations are unavailable")}</h2>
      <p>{t("Try loading the local catalog again.")}</p>
      <button className="button button--secondary" type="button" onClick={onRetry}>{t("Try again")}</button>
    </section>
  }

  if (simulations.length === 0) {
    return <section className="catalog-state">
      <ListChecks size={28} aria-hidden="true" />
      <h2>{t("No simulations yet")}</h2>
      <p>{t("New representative IT tasks will appear here when they are ready.")}</p>
    </section>
  }

  return <div className="catalog-grid">
    {simulations.map((simulation) => (
      <article className="catalog-card" key={simulation.id}>
        <span className="catalog-card__icon"><Code2 size={25} aria-hidden="true" /></span>
        <p className="eyebrow">{t(simulation.difficulty)}</p>
        <h2>{t(localized(simulation, locale, 'title'))}</h2>
        <p>{t(localized({ ...simulation, summary: simulation.description }, locale, 'summary'))}</p>
        <dl className="catalog-meta">
          <div><dt><Clock3 size={16} aria-hidden="true" /> {t("Duration")}</dt><dd>{t(simulation.duration)}</dd></div>
          {simulation.taskCount !== undefined && <div><dt><ListChecks size={16} aria-hidden="true" /> {t("Tasks")}</dt><dd>{t(simulation.taskCount)}</dd></div>}
        </dl>
        <Link className="button" to={`/simulations/${simulation.id}`}> {t("Start")} <ArrowRight size={17} aria-hidden="true" />
        </Link>
      </article>
    ))}
  </div>
}

export default SimulationCatalog
