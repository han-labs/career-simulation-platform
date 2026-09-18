import { Link } from 'react-router-dom'
import { ArrowRight, Sparkles, BrainCircuit } from 'lucide-react'
import { riasecDescriptions } from '../../assessment/data/mockQuestions.js'

function HeroSection({ status, result }) {
  return (
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
  )
}

export default HeroSection
