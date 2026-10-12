import { Link } from 'react-router-dom'
import { ArrowRight, Sparkles, BrainCircuit, CheckCircle2, Award } from 'lucide-react'
import { riasecDescriptions } from '../../assessment/data/mockQuestions.js'

const riasecNames = {
  R: 'Realistic',
  I: 'Investigative',
  A: 'Artistic',
  S: 'Social',
  E: 'Enterprising',
  C: 'Conventional',
}

function HeroSection({ status, result }) {
  const isCompleted = status === 'completed' && result
  const dimKey = result?.topDimension
    ? (result.topDimension.length === 1 ? result.topDimension.toUpperCase() : result.topDimension.charAt(0).toUpperCase())
    : null
  const topDimensionName = isCompleted
    ? (riasecNames[dimKey] || riasecDescriptions[dimKey]?.name || result.topDimension)
    : null

  return (
    <section className="hero section-shell" aria-labelledby="hero-title">
      <div className="hero__copy">
        <div className="eyebrow eyebrow--hero">
          <Sparkles size={16} aria-hidden="true" />
          <span>Practical IT Career Exploration · HCM-UTE</span>
        </div>

        <h1 id="hero-title" className="hero__title">
          Experience IT careers before choosing your <em>direction.</em>
        </h1>

        <p className="hero__lede">
          Designed for high school students: Discover personal strengths through standardized RIASEC assessment, complete authentic tech tasks, and receive transparent guidance to make informed future choices.
        </p>

        <div className="hero__actions">
          <Link className="button button--primary" to="/assessment">
            {isCompleted ? (
              <>
                View your RIASEC results <ArrowRight size={18} aria-hidden="true" />
              </>
            ) : (
              <>
                Start with RIASEC assessment <ArrowRight size={18} aria-hidden="true" />
              </>
            )}
          </Link>
          <Link className="button button--secondary" to="/simulations">
            Browse IT simulations
          </Link>
        </div>

        <div className="hero__highlights">
          <div className="highlight-pill">
            <CheckCircle2 size={16} aria-hidden="true" />
            <span>Scientific Holland framework</span>
          </div>
          <div className="highlight-pill">
            <CheckCircle2 size={16} aria-hidden="true" />
            <span>5 real-world IT tracks</span>
          </div>
          <div className="highlight-pill">
            <CheckCircle2 size={16} aria-hidden="true" />
            <span>Explainable AI guidance</span>
          </div>
        </div>
      </div>

      <div className="hero-board" aria-label="Career exploration roadmap">
        <div className="hero-board__header">
          <span className="hero-board__tag">
            <Award size={15} aria-hidden="true" /> Your Exploration Roadmap
          </span>
          <span className="pill pill--blue">3-step process</span>
        </div>

        <div className="hero-board__path" aria-hidden="true" />

        <div className={`hero-board__item ${isCompleted ? 'hero-board__item--completed' : 'hero-board__item--active'}`}>
          <span className="hero-board__step-number">1</span>
          <div className="hero-board__item-content">
            <strong>{isCompleted ? 'Dominant Trait' : 'Interest Assessment'}</strong>
            <small>{isCompleted ? topDimensionName : '42-question Holland RIASEC questionnaire'}</small>
          </div>
          <span className="mini-chart" aria-hidden="true" title="6 RIASEC dimensions chart">
            <i /><i /><i /><i /><i /><i />
          </span>
        </div>

        <div className="hero-board__item">
          <span className="hero-board__step-number">2</span>
          <div className="hero-board__item-content">
            <strong>Authentic IT Tasks</strong>
            <small>Experience Frontend, Backend, Data, QA, Security...</small>
          </div>
          <span className="hero-board__score">35 - 45 mins</span>
        </div>

        <div className="hero-board__item">
          <span className="hero-board__step-number">3</span>
          <div className="hero-board__item-content">
            <strong>Dashboard &amp; Syn</strong>
            <small>Synthesize skills &amp; explore recommendations</small>
          </div>
          <BrainCircuit size={22} className="hero-board__icon" aria-hidden="true" />
        </div>
      </div>
    </section>
  )
}

export default HeroSection
