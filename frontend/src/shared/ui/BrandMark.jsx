import { useI18n } from '../i18n/useI18n.js'
function BrandMark({ compact = false }) {
  const { t } = useI18n()
  return (
    <span className={`brand-mark ${compact ? 'brand-mark--compact' : ''}`}>
      <img
        src="/images/hcmute-logo.png"
        alt={t("Ho Chi Minh City University of Technology and Engineering - HCM-UTE Logo")}
        className="brand-mark__logo"
        width={compact ? 36 : 46}
        height={compact ? 46 : 59}
        loading="eager"
      />
      <span className="brand-mark__info">
        <span className="brand-mark__institution">{t("HCM-UTE")}</span>
        <span className="brand-mark__title"> {t("Career Simulation Platform")} </span>
      </span>
    </span>
  )
}

export default BrandMark
