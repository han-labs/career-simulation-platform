import { useI18n } from '../../../shared/i18n/useI18n.js'

const riasecProfiles = [
  { id: 'backend', label: 'Demo Backend Profile' },
  { id: 'frontend', label: 'Demo Frontend Profile' },
  { id: 'data', label: 'Demo Data Profile' },
]

function AssessmentDemoControls({ onDemo, profiles = riasecProfiles }) {
  const { t } = useI18n()

  return (
    <div className="assessment-demo-controls" aria-label={t('Quick demo profiles')}>
      {profiles.map((profile) => (
        <button key={profile.id} type="button" onClick={() => onDemo(profile.id)} className="button button--small button--secondary">
          {t(profile.label)}
        </button>
      ))}
    </div>
  )
}

export default AssessmentDemoControls
