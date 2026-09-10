import {
  ArrowRight,
  BarChart3,
  BrainCircuit,
  CheckCircle2,
  Code2,
  FlaskConical,
  Gauge,
  ShieldCheck,
  Sparkles,
  Target,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getPublishedSimulations } from '../../simulations/api/simulationCatalogApi.js'
import SimulationCard from '../../simulations/components/SimulationCard.jsx'

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
    copy: 'Compare objective results with your interests, then receive explainable guidance and practical next steps.',
  },
]

const tracks = [
  { icon: Code2, title: 'Build systems', tag: 'Backend', color: 'blue' },
  { icon: Gauge, title: 'Shape experiences', tag: 'Frontend', color: 'violet' },
  { icon: BarChart3, title: 'Find signal in data', tag: 'Data', color: 'gold' },
  { icon: ShieldCheck, title: 'Protect what matters', tag: 'Security', color: 'green' },
]

function LandingPage() {
  const [simulations, setSimulations] = useState(fallbackSimulations)
  const [catalogStatus, setCatalogStatus] = useState('loading')

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
              Start with RIASEC <ArrowRight size={18} aria-hidden="true" />
            </Link>
            <Link className="button button--secondary" to="/simulations">
              Browse simulations
            </Link>
          </div>
          <p className="hero__trust">
            <CheckCircle2 size={17} aria-hidden="true" />
            Objective scoring remains deterministic, even when AI is unavailable.
          </p>
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
              <strong>Interest signals</strong>
              <small>RIASEC profile</small>
            </div>
            <span className="mini-chart" aria-hidden="true">
              <i /><i /><i /><i /><i /><i />
            </span>
          </div>
          <div className="hero-board__item">
            <span>2</span>
            <div>
              <strong>Work samples</strong>
              <small>Three focused tasks</small>
            </div>
            <span className="hero-board__score">0 / 3</span>
          </div>
          <div className="hero-board__item">
            <span>3</span>
            <div>
              <strong>Reflection report</strong>
              <small>Strengths and next steps</small>
            </div>
            <BrainCircuit size={24} aria-hidden="true" />
          </div>
          <p className="hero-board__note">
            A recommendation is an invitation to explore, not a final career answer.
          </p>
        </div>
      </section>

      <section className="section section--paper" id="how-it-works">
        <div className="section-shell">
          <div className="section-heading">
            <span className="eyebrow">A practical feedback loop</span>
            <h2>Move from curiosity to informed reflection</h2>
            <p>
              The platform joins interest signals and performance evidence instead of
              relying on a single quiz result.
            </p>
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

      <section className="section section-shell">
        <div className="section-heading section-heading--split">
          <div>
            <span className="eyebrow">IT exploration tracks</span>
            <h2>Start with the kind of problem you want to try</h2>
          </div>
          <p>
            The first release stays intentionally focused on five IT directions so
            each simulation can be short, credible, and testable.
          </p>
        </div>
        <div className="track-grid">
          {tracks.map(({ icon: Icon, title, tag, color }) => (
            <article className={`track-card track-card--${color}`} key={tag}>
              <Icon size={25} aria-hidden="true" />
              <div>
                <span>{tag}</span>
                <h3>{title}</h3>
              </div>
              <ArrowRight size={20} aria-hidden="true" />
            </article>
          ))}
        </div>
      </section>

      <section className="section section--ink">
        <div className="section-shell">
          <div className="section-heading section-heading--inverse">
            <span className="eyebrow">Starter catalog</span>
            <h2>Try a compact simulation</h2>
            <p>
              Original demo scenarios are included for local development. Production
              content requires review and provenance records.
            </p>
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

      <section className="closing section-shell">
        <div>
          <span className="eyebrow">Your next step can stay small</span>
          <h2>Learn something useful about yourself in under an hour.</h2>
        </div>
        <Link className="button" to="/assessment">
          Begin exploration <ArrowRight size={18} aria-hidden="true" />
        </Link>
      </section>
    </>
  )
}

export default LandingPage
