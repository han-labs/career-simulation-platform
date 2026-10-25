import { useI18n } from '../../../shared/i18n/useI18n.js'
import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
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
    <section className="hero hero--campus" aria-labelledby="hero-title">
      <div className="section-shell hero__layout">
        <div className="hero__copy">
          <p className="hero__label">{t('Career Simulation Platform')}</p>
          <h1 id="hero-title" className="hero__title">{t('Explore real careers.')}<br /><span>{t('Build your brighter future.')}</span></h1>
          <p className="hero__lede">{t('Experience real-world tasks, discover your strengths, and get personalized career guidance with the support of AI.')}</p>
          <div className="hero__actions">
            <Link className="button button--primary" to="/simulations">{t('Explore Now')} <ArrowRight size={20} aria-hidden="true" /></Link>
            <Link className="text-link" to="/assessment">{t(isCompleted ? 'View Career Assessment Results' : 'Explore career assessments')}</Link>
          </div>
          {isCompleted && <p className="hero__saved-result">{t('Dominant Trait')}: {t(topDimensionName)}</p>}
        </div>
        <div className="campus-image hero__photo"><img src="/images/hcmute/first%20section.webp" alt={t('HCM-UTE university entrance')} width="474" height="315" fetchPriority="high" /></div>
      </div>
    </section>
  )
}

export default HeroSection
