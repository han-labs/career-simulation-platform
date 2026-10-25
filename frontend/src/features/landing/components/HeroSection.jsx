import { useI18n } from '../../../shared/i18n/useI18n.js'
import { Link } from 'react-router-dom'
import { ArrowRight, Sparkles, BrainCircuit, Award } from 'lucide-react'
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
  const { t } = useI18n()
  const isCompleted = status === 'completed' && result
  const firstTopDimension = result?.topDimensions?.[0] || result?.topDimension
  const dimKey = firstTopDimension
    ? (firstTopDimension.length === 1 ? firstTopDimension.toUpperCase() : firstTopDimension.charAt(0).toUpperCase())
    : null
  const topDimensionName = isCompleted
    ? (riasecNames[dimKey] || riasecDescriptions[dimKey]?.name || result.topDimension)
    : null

  return (
    <section className="hero section-shell" aria-labelledby="hero-title">
      <div className="hero__copy">
        <div className="eyebrow eyebrow--hero">
          <Sparkles size={16} aria-hidden="true" />
          <span>{t("Practical Career Exploration")}</span>
        </div>

        <h1 id="hero-title" className="hero__title"> {t("Experience IT careers before choosing your")} <em>{t("direction.")}</em>
        </h1>

        <p className="hero__lede">
          {t('Designed for high school students: Explore your interests, complete authentic tech tasks, and use transparent guidance to reflect on possible next steps.')}
        </p>

        <div className="hero__actions">
          <Link className="button button--primary" to="/assessment">
            {isCompleted ? (
              <> {t("View Career Assessment Results")} <ArrowRight size={18} aria-hidden="true" />
              </>
            ) : (
              <> {t('Explore career assessments')} <ArrowRight size={18} aria-hidden="true" />
              </>
            )}
          </Link>
          <Link className="button button--secondary" to="/simulations"> {t("Browse IT simulations")} </Link>
        </div>

      </div>

      <div className="hero-board" aria-label={t("Career exploration roadmap")}>
        <div className="hero-board__header">
          <span className="hero-board__tag">
            <Award size={15} aria-hidden="true" /> {t("Your Exploration Roadmap")} </span>
          <span className="pill pill--blue">{t("3-step process")}</span>
        </div>

        <div className="hero-board__path" aria-hidden="true" />

        <div className={`hero-board__item ${isCompleted ? 'hero-board__item--completed' : 'hero-board__item--active'}`}>
          <span className="hero-board__step-number">1</span>
          <div className="hero-board__item-content">
            <strong>{t(isCompleted ? 'Dominant Trait' : 'Interest Assessment')}</strong>
            <small>{t(isCompleted ? topDimensionName : '42 questions to explore your interests')}</small>
          </div>
          <span className="mini-chart" aria-hidden="true" title={t("6 RIASEC dimensions chart")}>
            <i /><i /><i /><i /><i /><i />
          </span>
        </div>

        <div className="hero-board__item">
          <span className="hero-board__step-number">2</span>
          <div className="hero-board__item-content">
            <strong>{t("Authentic IT Tasks")}</strong>
            <small>{t("Try a practical task in a tech role")}</small>
          </div>
        </div>

        <div className="hero-board__item">
          <span className="hero-board__step-number">3</span>
          <div className="hero-board__item-content">
            <strong>{t("Dashboard")}</strong>
            <small>{t("Reflect on results and plan your next step")}</small>
          </div>
          <BrainCircuit size={22} className="hero-board__icon" aria-hidden="true" />
        </div>
      </div>
    </section>
  )
}

export default HeroSection
