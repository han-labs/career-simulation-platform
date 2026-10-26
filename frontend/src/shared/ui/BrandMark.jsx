import { useI18n } from '../i18n/useI18n.js'
function BrandMark({ compact = false }) {
  const { t } = useI18n()
  return (
    <span className={`brand-mark ${compact ? 'brand-mark--compact' : ''}`}>
      <img
        src="/images/hcmute/logo.jpg"
        alt={t("Ho Chi Minh City University of Technology and Engineering - HCM-UTE Logo")}
        className="brand-mark__logo"
        width="406"
        height="492"
        loading="eager"
      />
      <span className="brand-mark__info">
        <span className="brand-mark__institution">{t('HCMC University of Technology and Engineering')}</span>
        <span className="brand-mark__title"> {t("Career Simulation Platform")} </span>
      </span>
    </span>
  )
}

export default BrandMark
