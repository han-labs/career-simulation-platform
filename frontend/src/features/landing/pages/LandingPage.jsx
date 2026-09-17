import {
  ArrowRight,
  BrainCircuit,
  FlaskConical,
  Sparkles,
  Target,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getPublishedSimulations } from '../../simulations/api/simulationCatalogApi.js'
import SimulationCard from '../../simulations/components/SimulationCard.jsx'
import { useAssessment } from '../../assessment/hooks/useAssessment.js'
import { riasecDescriptions } from '../../assessment/data/mockQuestions.js'

const fallbackSimulations = [
  {
    id: 'fallback-1',
    slug: 'backend-api-triage',
    title: 'Backend API Triage',
    careerTrack: 'BACKEND_DEVELOPMENT',
    summary:
      'Inspect an API response, choose a safe query, and identify the likely source of a server error.',
    difficulty: 'INTRODUCTORY',
    estimatedMinutes: 35,
  },
  {
    id: 'fallback-2',
    slug: 'frontend-accessibility-review',
    title: 'Frontend Accessibility Review',
    careerTrack: 'FRONTEND_DEVELOPMENT',
    summary:
      'Make evidence-based decisions about semantics, keyboard use, and interface feedback states.',
    difficulty: 'INTRODUCTORY',
    estimatedMinutes: 30,
  },
  {
    id: 'fallback-3',
    slug: 'data-quality-investigation',
    title: 'Data Quality Investigation',
    careerTrack: 'DATA_ANALYSIS',
    summary:
      'Find inconsistencies in a small dataset and select reproducible cleaning and validation steps.',
    difficulty: 'INTERMEDIATE',
    estimatedMinutes: 45,
  },
]

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

function LandingPage() {
  const [simulations, setSimulations] = useState(fallbackSimulations)
  const [catalogStatus, setCatalogStatus] = useState('loading')
  const { status, result } = useAssessment()

  useEffect(() => {
    let active = true

    getPublishedSimulations()
      .then((data) => {
        if (active && data.length > 0) {
          setSimulations(data.slice(0, 3))
        }
        if (active) setCatalogStatus('ready')
      })
      .catch(() => {
        if (active) setCatalogStatus('fallback')
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <>
      <section className="hero section-shell">
        <div className="hero__copy">
          <span className="eyebrow">
            <Sparkles size={16} aria-hidden="true" />
            Explore skills through practice
          </span>
          <h1>
            Explore IT work before you choose a <em>direction.</em>
          </h1>
          <p className="hero__lede">
            Discover what interests you, try realistic tasks, and understand what
            your results mean. You stay in charge of the decision.
          </p>
          <div className="hero__actions">
            <Link className="button" to="/assessment">
              {status === 'completed' && result ? (
                <>View your results <ArrowRight size={18} aria-hidden="true" /></>
              ) : (
                <>Start with RIASEC <ArrowRight size={18} aria-hidden="true" /></>
              )}
            </Link>
            <Link className="button button--secondary" to="/simulations">
              Browse simulations
            </Link>
          </div>
        </div>

        <div className="hero-board" aria-label="Career exploration journey preview">
          <div className="hero-board__header">
            <span>Your exploration map</span>
            <span className="pill pill--green">3 steps</span>
          </div>
          <div className="hero-board__path" aria-hidden="true" />
          <div className="hero-board__item hero-board__item--active">
            <span>1</span>
            <div>
              <strong>{status === 'completed' && result ? 'Your top interest' : 'Interest signals'}</strong>
              {status === 'completed' && result && (
                <small>{riasecDescriptions[result.topDimension]?.name || 'RIASEC profile'}</small>
              )}
            </div>
            <span className="mini-chart" aria-hidden="true">
              <i /><i /><i /><i /><i /><i />
            </span>
          </div>
          <div className="hero-board__item">
            <span>2</span>
            <div>
              <strong>Work samples</strong>
            </div>
            <span className="hero-board__score">0 / 3</span>
          </div>
          <div className="hero-board__item">
            <span>3</span>
            <div>
              <strong>Dashboard + Syn</strong>
            </div>
            <BrainCircuit size={24} aria-hidden="true" />
          </div>
        </div>
      </section>

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

      <section className="section section--ink" id="starter-catalog">
        <div className="section-shell">
          <div className="section-heading section-heading--inverse">
            <span className="eyebrow">Starter catalog</span>
            <h2>Try a compact simulation</h2>
          </div>
          {catalogStatus === 'fallback' && (
            <p className="catalog-notice" role="status">
              Demo previews are shown while the backend is unavailable.
            </p>
          )}
          <div className="simulation-grid">
            {simulations.map((simulation) => (
              <SimulationCard key={simulation.id} simulation={simulation} />
            ))}
          </div>
          <div className="section-action">
            <Link className="button button--light" to="/simulations">
              See all simulations <ArrowRight size={18} aria-hidden="true" />
            </Link>
          </div>
        </div>
      </section>

    </>
  )
}

export default LandingPage
