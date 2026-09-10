import { ArrowUpRight, Clock3 } from 'lucide-react'
import { Link } from 'react-router-dom'

const trackLabels = {
  BACKEND_DEVELOPMENT: 'Backend development',
  FRONTEND_DEVELOPMENT: 'Frontend development',
  DATA_ANALYSIS: 'Data analysis',
  SOFTWARE_TESTING: 'Software testing',
  CYBERSECURITY: 'Cybersecurity',
}

function SimulationCard({ simulation }) {
  return (
    <article className="simulation-card">
      <div className="simulation-card__topline">
        <span className="pill pill--blue">
          {trackLabels[simulation.careerTrack] ?? simulation.careerTrack}
        </span>
        <span className="simulation-card__duration">
          <Clock3 size={15} aria-hidden="true" />
          {simulation.estimatedMinutes} min
        </span>
      </div>
      <div>
        <p className="simulation-card__level">
          {simulation.difficulty.toLowerCase()}
        </p>
        <h3>{simulation.title}</h3>
        <p>{simulation.summary}</p>
      </div>
      <Link className="text-link" to={`/simulations/${simulation.slug}`}>
        View simulation <ArrowUpRight size={17} aria-hidden="true" />
      </Link>
    </article>
  )
}

export default SimulationCard
