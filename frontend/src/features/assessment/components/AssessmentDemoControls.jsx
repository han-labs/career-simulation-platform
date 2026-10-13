import { useI18n } from '../../../shared/i18n/useI18n.js'
function AssessmentDemoControls({ onDemo }) {
  const { t } = useI18n()
  return (
    <div style={{ display: 'flex', gap: '12px', marginBottom: '24px', justifyContent: 'flex-end' }}>
      <button type="button" onClick={() => onDemo('backend')} className="button button--small button--secondary">{t("Demo Backend Profile")}</button>
      <button type="button" onClick={() => onDemo('frontend')} className="button button--small button--secondary">{t("Demo Frontend Profile")}</button>
      <button type="button" onClick={() => onDemo('data')} className="button button--small button--secondary">{t("Demo Data Profile")}</button>
    </div>
  )
}

export default AssessmentDemoControls
