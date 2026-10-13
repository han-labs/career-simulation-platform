import { useI18n } from '../../../shared/i18n/useI18n.js'
import {
  ArrowRight,
  Check,
  ChevronDown,
  ChevronUp,
  CircleAlert,
  Clock3,
  LoaderCircle,
  RefreshCw,
  Sparkles,
  Target,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getExplorationDashboard } from '../api/guidanceApi.js'

function openSyn(message, actionType = 'FREE_TEXT', context = null) {
  window.dispatchEvent(new CustomEvent('careersim:syn-open', {
    detail: { message, actionType, context },
  }))
}

function completedLabel(result, locale = 'en') {
  if (!result.completedAt) return result.completedLabel
  return new Intl.DateTimeFormat(locale === 'vi' ? 'vi-VN' : 'en', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date(result.completedAt))
}

function statusClass(status = '') {
  return status.toLowerCase().replaceAll('_', '-')
}

function statusLabel(status = '') {
  const labels = {
    OBSERVED_STRENGTH: 'Observed strength',
    PRACTISED: 'Practised',
    NEEDS_MORE_EVIDENCE: 'Needs more evidence',
  }
  return labels[status] ?? status
}

function EmptyState({ children }) {
  const { t } = useI18n()
  return <p className="dashboard-empty">{t(children)}</p>
}

function GuidancePage() {
  const { t, locale } = useI18n()
  const [dashboard, setDashboard] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [showAllResults, setShowAllResults] = useState(false)

  const loadDashboard = useCallback(async () => {
    setLoadError('')
    try {
      setDashboard(await getExplorationDashboard())
    } catch {
      setLoadError('We could not load your exploration data. Please try again.')
    }
  }, [])

  useEffect(() => {
    let isCurrent = true
    getExplorationDashboard()
      .then((data) => { if (isCurrent) setDashboard(data) })
      .catch(() => {
        if (isCurrent) setLoadError('We could not load your exploration data. Please try again.')
      })
    return () => { isCurrent = false }
  }, [])

  if (loadError) {
    return (
      <section className="section-shell dashboard-error">
        <CircleAlert size={28} />
        <h1>{t("Dashboard unavailable")}</h1>
        <p>{t(loadError)}</p>
        <button className="button" type="button" onClick={loadDashboard}>
          <RefreshCw size={17} /> {t("Try again")} </button>
      </section>
    )
  }

  if (!dashboard) {
    return (
      <div className="section-shell dashboard-loading">
        <LoaderCircle className="spin" /> {t("Loading your dashboard…")} </div>
    )
  }

  const progressTotal = Math.max(dashboard.progress.total, 1)
  const progressPercent = Math.min(
    100,
    Math.round((dashboard.progress.completed / progressTotal) * 100),
  )
  const visibleResults = showAllResults
    ? dashboard.recentResults
    : dashboard.recentResults.slice(0, 3)

  return (
    <section className="section-shell us03-page">
      <header className="dashboard-heading">
        <div>
          <h1>{t("Dashboard")}</h1>
          <p>{t("Your progress, interests, and recent practice at a glance.")}</p>
        </div>
        <Link className="button button--secondary" to="/simulations"> {t("Browse simulations")} <ArrowRight size={17} />
        </Link>
      </header>

      {dashboard.source === 'PREVIEW' && (
        <div className="preview-notice" role="status">
          <CircleAlert size={17} /> {t("Preview data — sign-in integration is still pending.")} </div>
      )}

      <div className="dashboard-grid">
        <article className="dashboard-card dashboard-card--progress">
          <div className="dashboard-card__heading">
            <h2>{t("Exploration progress")}</h2>
            <strong>{t(dashboard.progress.completed)}/{t(dashboard.progress.total)}</strong>
          </div>
          <div
            className="progress-track"
            role="progressbar"
            aria-valuenow={dashboard.progress.completed}
            aria-valuemin="0"
            aria-valuemax={dashboard.progress.total}
          >
            <span style={{ width: `${progressPercent}%` }} />
          </div>
        </article>

        <article className="dashboard-card">
          <div className="dashboard-card__heading">
            <h2>{t("Top interest signals")}</h2><Sparkles size={20} />
          </div>
          {dashboard.interestSignals.length ? (
            <ul className="signal-list">
              {dashboard.interestSignals.map((signal) => (
                <li key={signal.code}>
                  <strong>{t(signal.code)}</strong><span>{t(signal.label)}</span>
                  <span className="signal-meter" aria-label={t(`${signal.score} points`)}>
                    <i style={{ width: `${Math.min(signal.score * 5, 100)}%` }} />
                  </span>
                  <small>{t(signal.score)}</small>
                </li>
              ))}
            </ul>
          ) : <EmptyState>{t("Complete the RIASEC assessment to see your signals.")}</EmptyState>}
          <Link className="text-button" to="/assessment"> {t("View assessment")} <ArrowRight size={15} />
          </Link>
        </article>

        <article className="dashboard-card">
          <div className="dashboard-card__heading">
            <h2>{t("Skills practised")}</h2><Check size={20} />
          </div>
          {dashboard.skills.length ? (
            <ul className="skill-list">
              {dashboard.skills.map((skill) => (
                <li key={skill.name}>
                  <strong>{t(skill.name)}</strong>
                  <span className={`skill-status skill-status--${statusClass(skill.status)}`}>
                    {t(statusLabel(skill.status))}
                  </span>
                </li>
              ))}
            </ul>
          ) : <EmptyState>{t("Completed simulations will build your evidence here.")}</EmptyState>}
        </article>

        <article className="dashboard-card dashboard-card--history">
          <div className="dashboard-card__heading">
            <h2>{t("Simulation history")}</h2><Clock3 size={20} />
          </div>
          {visibleResults.length ? (
            <div className="simulation-history">
              {visibleResults.map((result, index) => (
                <article className="simulation-history__item" key={result.id}>
                  <div>
                    <span>{t(index === 0 ? 'Latest' : completedLabel(result, locale))}</span>
                    <strong>{t(result.title)}</strong>
                    {index === 0 && <small>{t(completedLabel(result, locale))}</small>}
                  </div>
                  <strong className="simulation-history__score">
                    {t(result.score)}<small>/100</small>
                  </strong>
                  <button
                    className="text-button"
                    type="button"
                    onClick={() => openSyn(
                      t(`Review my ${result.title} result`),
                      'REVIEW_RESULT',
                      { attemptId: result.id },
                    )}
                  > {t("View with Syn")} <ArrowRight size={15} />
                  </button>
                </article>
              ))}
              {dashboard.recentResults.length > 3 && (
                <button
                  className="history-toggle"
                  type="button"
                  onClick={() => setShowAllResults((current) => !current)}
                >
                  {showAllResults ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
                  {t(showAllResults ? 'Show less' : `Show all ${dashboard.recentResults.length}`)}
                </button>
              )}
            </div>
          ) : <EmptyState>{t("Your completed simulations will appear here.")}</EmptyState>}
        </article>

        <article className="dashboard-card">
          <div className="dashboard-card__heading">
            <h2>{t("Current plan")}</h2><Target size={20} />
          </div>
          {dashboard.plan ? (
            <div className="current-plan">
              <strong>{t(dashboard.plan.title)}</strong><p>{t(dashboard.plan.nextStep)}</p>
            </div>
          ) : <EmptyState>{t("Ask Syn to draft a small, editable exploration plan.")}</EmptyState>}
          <button
            className="text-button"
            type="button"
            onClick={() => openSyn(t('Draft an exploration plan'), 'DRAFT_PLAN')}
          > {t("Draft with Syn")} <ArrowRight size={15} />
          </button>
        </article>
      </div>
    </section>
  )
}

export default GuidancePage
