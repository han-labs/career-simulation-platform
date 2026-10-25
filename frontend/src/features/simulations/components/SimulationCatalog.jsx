import { useI18n } from '../../../shared/i18n/useI18n.js'
// Displays the shared legacy and API-backed Core catalogue lifecycle states.
import { ArrowRight, BriefcaseBusiness, Clock3, Code2, Gauge, ListChecks, TriangleAlert } from 'lucide-react'
import { Link } from 'react-router-dom'
import { localized } from '../../../shared/api/coreApi.js'

function SimulationCatalog({ status, simulations, onRetry, onClear, emptyState = 'empty', variant = 'legacy' }) {
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
      <p>{t(variant === 'core' ? 'The simulation catalogue could not be loaded. Please try again.' : 'Try loading the local catalog again.')}</p>
      <button className="button button--secondary" type="button" onClick={onRetry}>{t("Try again")}</button>
    </section>
  }

  if (simulations.length === 0) {
    return <section className="catalog-state">
      <ListChecks size={28} aria-hidden="true" />
      <h2>{t(emptyState === 'no-match' ? 'No simulations match your search. Try changing the filters.' : variant === 'core' ? 'No simulations are currently available. Please check back later.' : 'No simulations yet')}</h2>
      {variant !== 'core' && <p>{t('New representative IT tasks will appear here when they are ready.')}</p>}
      {emptyState === 'no-match' && <button className="button button--secondary" type="button" onClick={onClear}>{t('Clear filters')}</button>}
    </section>
  }

  return <div className="catalog-grid">
    {simulations.map((simulation) => variant === 'core' ? (
      <Link className="catalog-card catalog-card--link" key={simulation.id} to={`/simulations/${simulation.id}`}>
        <div className="catalog-card__media" aria-hidden="true">
          {simulation.media?.cardImageUrl
            ? <img src={simulation.media.cardImageUrl} alt="" loading="lazy" />
            : <span className="catalog-card__media-fallback"><Code2 size={28} /></span>}
        </div>
        <div className="catalog-card__topline">
          <span className="catalog-card__company">
            {simulation.media?.companyLogoUrl && <img src={simulation.media.companyLogoUrl} alt="" loading="lazy" />}
            {simulation.enterpriseName || t('CareerSim partner')}
          </span>
          <span>{t(simulation.industryCode || 'IT')}</span>
        </div>
        <h2>{localized(simulation, locale, 'title')}</h2>
        <p className="catalog-card__summary">{localized({ ...simulation, summary: simulation.description }, locale, 'summary')}</p>
        <div className="catalog-meta" aria-label={t('Simulation information')}>
          <span aria-label={`${t('Role')}: ${localized(simulation, locale, 'targetRole')}`}><BriefcaseBusiness size={14} aria-hidden="true" /> {localized(simulation, locale, 'targetRole')}</span>
          <span aria-label={`${t('Duration')}: ${t(`${simulation.estimatedMinutes} minutes`)}`}><Clock3 size={14} aria-hidden="true" /> {t(`${simulation.estimatedMinutes} minutes`)}</span>
          <span aria-label={`${t('Difficulty')}: ${t(simulation.difficulty)}`}><Gauge size={14} aria-hidden="true" /> {t(simulation.difficulty)}</span>
        </div>
      </Link>
    ) : (
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
