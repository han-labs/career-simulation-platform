import { useI18n } from '../../../shared/i18n/useI18n.js'
import { ArrowUpRight, Clock3, Code2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { localized } from '../../../shared/api/coreApi.js'

const trackLabels = {
  BACKEND_DEVELOPMENT: 'Backend Development',
  FRONTEND_DEVELOPMENT: 'Frontend Development',
  DATA_ANALYSIS: 'Data Analysis',
  SOFTWARE_TESTING: 'Software Testing',
  CYBERSECURITY: 'Cybersecurity',
}

const difficultyLabels = {
  INTRODUCTORY: 'Introductory',
  INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced',
}

function SimulationCard({ simulation, compact = false }) {
  const { t, locale } = useI18n()
  const difficultyText = difficultyLabels[simulation.difficulty] ?? simulation.difficulty?.toLowerCase()

  return (
    <article className={`simulation-card${compact ? ' simulation-card--compact' : ''}`}>
      {compact && <div className="simulation-card__preview" aria-hidden="true"><Code2 size={28} />{simulation.media?.cardImageUrl && <img src={simulation.media.cardImageUrl} alt="" width="320" height="160" loading="lazy" onError={event => { event.currentTarget.hidden = true }} />}</div>}
      <div className="simulation-card__topline">
        <span className="pill pill--blue">
          {t(trackLabels[simulation.careerTrack] ?? simulation.careerTrack)}
        </span>
        <span className="simulation-card__duration">
          <Clock3 size={15} aria-hidden="true" />
          {t(simulation.estimatedMinutes)} {t("min")} </span>
      </div>
      <div className="simulation-card__body">
        <p className="simulation-card__level"> {t("Level:")} {t(difficultyText)}
        </p>
        <h3>{t(localized(simulation, locale, 'title'))}</h3>
        <p className="simulation-card__summary">{t(localized(simulation, locale, 'summary'))}</p>
        {compact && <p className="simulation-card__provider">{simulation.enterpriseName || `${t(simulation.estimatedMinutes)} ${t('min')}`}</p>}
      </div>
      <Link className="text-link" aria-label={compact ? `${t('View Simulation')}: ${t(localized(simulation, locale, 'title'))}` : undefined} to={`/simulations/${simulation.slug}`}> <span>{t("View Simulation")}</span> <ArrowUpRight size={17} aria-hidden="true" />
      </Link>
    </article>
  )
}

export default SimulationCard
