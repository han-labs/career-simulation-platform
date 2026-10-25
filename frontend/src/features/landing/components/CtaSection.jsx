import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useI18n } from '../../../shared/i18n/useI18n.js'

function CtaSection() {
  const { t } = useI18n()
  return <section className="home-mission section-shell" aria-labelledby="cta-heading">
    <div className="home-mission__copy">
      <h2 id="cta-heading">{t('We believe in a future where you can discover a fitting career path based on your real potential.')}</h2>
      <p>{t('Our mission is to ensure your dedication, skills, and drive are fully recognized and rewarded.')}</p>
      <div className="home-mission__actions">
        <Link className="button button--primary" to="/assessment">{t('Start your journey today')} <ArrowRight size={18} aria-hidden="true" /></Link>
        <Link className="text-link" to="/simulations">{t('Browse IT Career Roles')}</Link>
      </div>
    </div>
    <div className="campus-image home-mission__photo"><img src="/images/hcmute/last%20section.webp" alt={t('HCM-UTE campus from above')} width="474" height="256" loading="lazy" /></div>
  </section>
}
export default CtaSection
