import { useI18n } from '../../../shared/i18n/useI18n.js'
import { Target, Laptop, BrainCircuit, ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'

const journey = [
  {
    icon: Target,
    number: '01',
    title: 'Explore Interests & Preferences',
    copy: 'Use a 42-question RIASEC self-reflection to review six interest dimensions and choose what you want to explore next.',
    actionLabel: 'Take Assessment',
    to: '/assessment',
  },
  {
    icon: Laptop,
    number: '02',
    title: 'Experience Authentic IT Tasks',
    copy: 'Try a focused task in Frontend, Backend, Data, QA or Cybersecurity.',
    actionLabel: 'Browse Roles',
    to: '/simulations',
  },
  {
    icon: BrainCircuit,
    number: '03',
    title: 'Analyze Evidence & Plan Next Steps',
    copy: 'Review your results and choose a next step to explore.',
    actionLabel: 'Open Dashboard',
    to: '/dashboard',
  },
]

function JourneySection() {
  const { t } = useI18n()
  return (
    <section className="section section--paper" id="how-it-works" aria-labelledby="journey-heading">
      <div className="section-shell">
        <div className="section-heading">
          <span className="eyebrow">{t('A student-led exploration process')}</span>
          <h2 id="journey-heading">{t('From Interests to First-hand Experience')}</h2>
          <p className="section-heading__sub">
            {t('Combine self-reflection with hands-on activities and decide for yourself what to explore further.')}
          </p>
        </div>

        <div className="journey-grid">
          {journey.map(({ icon: Icon, number, title, copy, actionLabel, to }) => (
            <article className="journey-card" key={number}>
              <div className="journey-card__header">
                <div className="journey-card__icon" aria-hidden="true">
                  <Icon size={24} />
                </div>
                <span className="journey-card__number">{t(number)}</span>
              </div>
              <h3>{t(title)}</h3>
              <p>{t(copy)}</p>
              <Link className="journey-card__link" to={to}>
                {t(actionLabel)} <ArrowRight size={15} aria-hidden="true" />
              </Link>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}

export default JourneySection
