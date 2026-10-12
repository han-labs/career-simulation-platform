import { Target, Laptop, BrainCircuit, ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'

const journey = [
  {
    icon: Target,
    number: '01',
    title: 'Identify Interests & Personality',
    copy: 'Complete a standardized 42-question assessment based on the Holland (RIASEC) framework to explore 6 career interest dimensions without stereotypes.',
    actionLabel: 'Take Assessment',
    to: '/assessment',
  },
  {
    icon: Laptop,
    number: '02',
    title: 'Experience Authentic IT Tasks',
    copy: 'Step into tech roles and solve focused simulation scenarios (30-45 mins) in Frontend, Backend, Data, QA, or Cybersecurity.',
    actionLabel: 'Browse Roles',
    to: '/simulations',
  },
  {
    icon: BrainCircuit,
    number: '03',
    title: 'Analyze Evidence & Plan Next Steps',
    copy: 'Review objective scores on your Dashboard and chat with assistant Syn to interpret performance evidence and explore relevant learning paths.',
    actionLabel: 'Open Dashboard',
    to: '/dashboard',
  },
]

function JourneySection() {
  return (
    <section className="section section--paper" id="how-it-works" aria-labelledby="journey-heading">
      <div className="section-shell">
        <div className="section-heading">
          <span className="eyebrow">Scientific Exploration Process</span>
          <h2 id="journey-heading">From Natural Interests to Evidence-Based Decisions</h2>
          <p className="section-heading__sub">
            A closed-loop methodology bridging vocational psychology theory with hands-on experiential learning.
          </p>
        </div>

        <div className="journey-grid">
          {journey.map(({ icon: Icon, number, title, copy, actionLabel, to }) => (
            <article className="journey-card" key={number}>
              <div className="journey-card__header">
                <div className="journey-card__icon" aria-hidden="true">
                  <Icon size={24} />
                </div>
                <span className="journey-card__number">{number}</span>
              </div>
              <h3>{title}</h3>
              <p>{copy}</p>
              <Link className="journey-card__link" to={to}>
                {actionLabel} <ArrowRight size={15} aria-hidden="true" />
              </Link>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}

export default JourneySection
