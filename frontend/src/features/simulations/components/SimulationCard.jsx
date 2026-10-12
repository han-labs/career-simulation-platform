import { ArrowUpRight, Clock3 } from 'lucide-react'
import { Link } from 'react-router-dom'

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
  const difficultyText = difficultyLabels[simulation.difficulty] ?? simulation.difficulty?.toLowerCase()

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
          Level: {difficultyText}
        </p>
        <h3>{simulation.title}</h3>
        <p>{simulation.summary}</p>
      </div>
      <Link className="text-link" to={`/simulations/${simulation.slug}`}>
        View Simulation <ArrowUpRight size={17} aria-hidden="true" />
      </Link>
    </article>
  )
}

export default SimulationCard
