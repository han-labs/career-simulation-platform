import { ArrowRight, ClipboardList, Clock3, Play, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { assessmentCatalog } from '../data/assessmentCatalog.js'

function AssessmentIntro({ status = 'intro' }) {
  const { t } = useI18n()

  return (
    <div className="assessment-discovery">
      <header className="assessment-discovery__header">
        <span className="eyebrow">{t('CareerSim · Self-exploration')}</span>
        <h1>{t('Career exploration assessments')}</h1>
        <p>{t('Explore your interests, strengths, and career directions through assessments that fit your goals.')}</p>
        <p className="assessment-discovery__note">
          {t('Results offer ideas for further exploration. They are not a diagnosis or a decision about your future.')}
        </p>
      </header>

      <div className="assessment-choice-grid" aria-label={t('Career assessments')}>
        {assessmentCatalog.map((assessment) => {
          const isAvailable = assessment.status === 'available'

          return (
            <article
              className={`assessment-choice-card${isAvailable ? ' assessment-choice-card--available' : ''}`}
              key={assessment.id}
              aria-labelledby={`assessment-title-${assessment.id}`}
            >
              <div className="assessment-choice-card__topline">
                <span className="assessment-choice-card__icon" aria-hidden="true">
                  {assessment.id === 'riasec' ? <ClipboardList size={22} /> : <Sparkles size={22} />}
                </span>
                <span className={`assessment-status${isAvailable ? ' assessment-status--available' : ''}`}>
                  {t(assessment.statusLabel)}
                </span>
              </div>

              <h2 id={`assessment-title-${assessment.id}`}>{t(assessment.title)}</h2>
              <p className="assessment-choice-card__description">{t(assessment.description)}</p>

              <dl className="assessment-choice-card__details">
                <div><dt>{t('Explores')}</dt><dd>{t(assessment.dimensions)}</dd></div>
                <div><dt>{t('Time')}</dt><dd><Clock3 size={15} aria-hidden="true" /> {t(assessment.duration)}</dd></div>
                <div><dt>{t('Questions')}</dt><dd>{t(assessment.questionCount)}</dd></div>
                <div><dt>{t('Format')}</dt><dd>{t(assessment.format)}</dd></div>
              </dl>

              {assessment.inspiration && (
                <p className="assessment-choice-card__inspiration">{t(assessment.inspiration)}</p>
              )}

              {isAvailable ? (
                <div className="assessment-choice-card__actions">
                  <Link
                    className="button button--primary assessment-choice-card__action"
                    to={`/assessment/${assessment.route}`}
                    aria-describedby={`assessment-local-progress-${assessment.id}`}
                  >
                    {t(assessment.id === 'riasec' && status === 'in_progress' ? 'Resume assessment' : assessment.actionLabel)}
                    <ArrowRight size={17} aria-hidden="true" />
                  </Link>
                  <Link
                    className="button button--secondary assessment-choice-card__demo"
                    to={`/assessment/${assessment.route}?demo=${assessment.demoProfile}`}
                    aria-label={`${t('Quick demo')}: ${t(assessment.title)}`}
                  >
                    <Play size={15} aria-hidden="true" /> {t('Quick demo')}
                  </Link>
                  <p id={`assessment-local-progress-${assessment.id}`} className="assessment-choice-card__hint">
                    {t('Your progress is saved in this browser.')}
                  </p>
                </div>
              ) : (
                <div className="assessment-choice-card__unavailable">
                  <button className="button button--secondary assessment-choice-card__action" type="button" disabled>
                    {t('Coming soon')}
                  </button>
                  <p>{t(assessment.unavailableReason)}</p>
                </div>
              )}
            </article>
          )
        })}
      </div>
    </div>
  )
}

export default AssessmentIntro
