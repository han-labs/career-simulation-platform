import { useI18n } from '../../../shared/i18n/useI18n.js'
import { ArrowRight, Sparkles, GraduationCap } from 'lucide-react'
import { Link } from 'react-router-dom'

function CtaSection() {
  const { t } = useI18n()
  return (
    <section className="section section--cta" aria-labelledby="cta-heading">
      <div className="section-shell">
        <div className="cta-card">
          <div className="cta-card__content">
            <span className="eyebrow eyebrow--light">
              <GraduationCap size={16} aria-hidden="true" /> {t('Ready to Start Your Career Exploration?')}
            </span>
            <h2 id="cta-heading" className="cta-card__title">{t('Begin Discovering Your IT Potential Today')}</h2>
            <p className="cta-card__text">
              {t('Start with a 42-question RIASEC self-reflection, then explore possible IT directions at your own pace.')}
            </p>
            <div className="cta-card__actions">
              <Link className="button button--cta-primary" to="/assessment">
                <Sparkles size={17} aria-hidden="true" /> {t('Explore career assessments')}
              </Link>
              <Link className="button button--cta-secondary" to="/simulations">
                {t('Browse IT Career Roles')} <ArrowRight size={17} aria-hidden="true" />
              </Link>
            </div>
          </div>
          <div className="cta-card__badge-corner" aria-hidden="true">
            <div className="cta-pulse" />
          </div>
        </div>
      </div>
    </section>
  )
}

export default CtaSection
