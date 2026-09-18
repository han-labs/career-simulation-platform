import { Target, FlaskConical, BrainCircuit } from 'lucide-react'

const journey = [
  {
    icon: Target,
    number: '01',
    title: 'Notice what draws you in',
    copy: 'A short RIASEC assessment highlights directions worth exploring without treating the result as a verdict.',
  },
  {
    icon: FlaskConical,
    number: '02',
    title: 'Try representative work',
    copy: 'Complete compact tasks that reflect how an IT role reasons, communicates, and solves problems.',
  },
  {
    icon: BrainCircuit,
    number: '03',
    title: 'Reflect on evidence',
    copy: 'Bring your results together on the Dashboard, then explore the evidence and practical next steps with Syn.',
  },
]

function JourneySection() {
  return (
    <section className="section section--paper" id="how-it-works">
      <div className="section-shell">
        <div className="section-heading">
          <span className="eyebrow">A practical feedback loop</span>
          <h2>Move from curiosity to informed reflection</h2>
        </div>
        <div className="journey-grid">
          {journey.map(({ icon: Icon, number, title, copy }) => (
            <article className="journey-card" key={number}>
              <div className="journey-card__icon">
                <Icon size={24} aria-hidden="true" />
              </div>
              <span className="journey-card__number">{number}</span>
              <h3>{title}</h3>
              <p>{copy}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}

export default JourneySection
