import { useI18n } from '../../../shared/i18n/useI18n.js'
function AssessmentProgressBar({ answeredCount, totalCount, progressPercent }) {
  const { t } = useI18n()
  return (
    <div style={{ 
      position: 'sticky', 
      top: '20px', 
      background: 'var(--card)', 
      padding: '16px 24px', 
      borderRadius: '16px', 
      boxShadow: '0 4px 12px rgba(0,0,0,0.08)', 
      zIndex: 10,
      marginBottom: '32px',
      border: '1px solid var(--line)'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '0.9rem', fontWeight: 'bold', color: 'var(--ink)' }}>
        <span>{t("Progress")}</span>
        <span>{t(answeredCount)} / {t(totalCount)} {t("completed")}</span>
      </div>
      <div
        role="progressbar"
        aria-label={t('Assessment progress')}
        aria-valuemin={0}
        aria-valuemax={totalCount}
        aria-valuenow={answeredCount}
        aria-valuetext={`${answeredCount} / ${totalCount} ${t('completed')}`}
        style={{ height: '8px', background: 'var(--line)', borderRadius: '4px', overflow: 'hidden' }}
      >
        <div style={{ 
          height: '100%', 
          background: 'var(--tech)', 
          width: `${progressPercent}%`,
          transition: 'width 0.3s ease-out'
        }} />
      </div>
    </div>
  )
}

export default AssessmentProgressBar
