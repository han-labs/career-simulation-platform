import { useI18n } from '../../../shared/i18n/useI18n.js'
import { ArrowUpRight, Clock3 } from 'lucide-react'
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

function SimulationCard({ simulation }) {
  const { t, locale } = useI18n()
  const difficultyText = difficultyLabels[simulation.difficulty] ?? simulation.difficulty?.toLowerCase()

  return (
    <article className="simulation-card">
      <div className="simulation-card__topline">
        <span className="pill pill--blue">
          {t(trackLabels[simulation.careerTrack] ?? simulation.careerTrack)}
        </span>
        <span className="simulation-card__duration">
          <Clock3 size={15} aria-hidden="true" />
          {t(simulation.estimatedMinutes)} {t("min")} </span>
      </div>
      <div>
        <p className="simulation-card__level"> {t("Level:")} {t(difficultyText)}
        </p>
        <h3>{t(localized(simulation, locale, 'title'))}</h3>
        <p>{t(localized(simulation, locale, 'summary'))}</p>
      </div>
      <Link className="text-link" to={`/simulations/${simulation.slug}`}> {t("View Simulation")} <ArrowUpRight size={17} aria-hidden="true" />
      </Link>
    </article>
  )
}

export default SimulationCard
