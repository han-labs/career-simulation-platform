import { ArrowRight, Sparkles, GraduationCap } from 'lucide-react'
import { Link } from 'react-router-dom'

function CtaSection() {
  return (
    <section className="section section--cta" aria-labelledby="cta-heading">
      <div className="section-shell">
        <div className="cta-card">
          <div className="cta-card__content">
            <span className="eyebrow eyebrow--light">
              <GraduationCap size={16} aria-hidden="true" /> Ready to Start Your Career Exploration?
            </span>
            <h2 id="cta-heading" className="cta-card__title">
              Begin Discovering Your IT Potential Today
            </h2>
            <p className="cta-card__text">
              It takes just 10–15 minutes to complete the 42-question Holland (RIASEC) questionnaire and receive tailored IT pathway suggestions that match your personality and strengths.
            </p>
            <div className="cta-card__actions">
              <Link className="button button--cta-primary" to="/assessment">
                <Sparkles size={17} aria-hidden="true" />
                Take the RIASEC Assessment
              </Link>
              <Link className="button button--cta-secondary" to="/simulations">
                Browse IT Career Roles <ArrowRight size={17} aria-hidden="true" />
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
