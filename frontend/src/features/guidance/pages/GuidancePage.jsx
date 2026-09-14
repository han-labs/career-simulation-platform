import {
  ArrowRight,
  Check,
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

function openSyn(message, actionType = 'FREE_TEXT') {
  window.dispatchEvent(new CustomEvent('careersim:syn-open', {
    detail: { message, actionType },
  }))
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
  return <p className="dashboard-empty">{children}</p>
}

function GuidancePage() {
  const [dashboard, setDashboard] = useState(null)
  const [loadError, setLoadError] = useState('')

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
        <h1>Dashboard unavailable</h1>
        <p>{loadError}</p>
        <button className="button" type="button" onClick={loadDashboard}>
          <RefreshCw size={17} /> Try again
        </button>
      </section>
    )
  }

  if (!dashboard) {
    return (
      <div className="section-shell dashboard-loading">
        <LoaderCircle className="spin" /> Loading your dashboard…
      </div>
    )
  }

  const progressTotal = Math.max(dashboard.progress.total, 1)
  const progressPercent = Math.min(
    100,
    Math.round((dashboard.progress.completed / progressTotal) * 100),
  )
  const latestResult = dashboard.recentResults[0]

  return (
    <section className="section-shell us03-page">
      <header className="dashboard-heading">
        <div>
          <h1>Dashboard</h1>
          <p>Your progress, interests, and recent practice at a glance.</p>
        </div>
        <Link className="button button--secondary" to="/simulations">
          Browse simulations <ArrowRight size={17} />
        </Link>
      </header>

      {dashboard.source === 'PREVIEW' && (
        <div className="preview-notice" role="status">
          <CircleAlert size={17} /> Preview data — sign-in integration is still pending.
        </div>
      )}

      <div className="dashboard-grid">
        <article className="dashboard-card dashboard-card--progress">
          <div className="dashboard-card__heading">
            <h2>Exploration progress</h2>
            <strong>{dashboard.progress.completed}/{dashboard.progress.total}</strong>
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
            <h2>Top interest signals</h2><Sparkles size={20} />
          </div>
          {dashboard.interestSignals.length ? (
            <ul className="signal-list">
              {dashboard.interestSignals.map((signal) => (
                <li key={signal.code}>
                  <strong>{signal.code}</strong><span>{signal.label}</span>
                  <span className="signal-meter" aria-label={`${signal.score} points`}>
                    <i style={{ width: `${Math.min(signal.score * 5, 100)}%` }} />
                  </span>
                  <small>{signal.score}</small>
                </li>
              ))}
            </ul>
          ) : <EmptyState>Complete the RIASEC assessment to see your signals.</EmptyState>}
          <Link className="text-button" to="/assessment">
            View assessment <ArrowRight size={15} />
          </Link>
        </article>

        <article className="dashboard-card">
          <div className="dashboard-card__heading">
            <h2>Skills practised</h2><Check size={20} />
          </div>
          {dashboard.skills.length ? (
            <ul className="skill-list">
              {dashboard.skills.map((skill) => (
                <li key={skill.name}>
                  <strong>{skill.name}</strong>
                  <span className={`skill-status skill-status--${statusClass(skill.status)}`}>
                    {statusLabel(skill.status)}
                  </span>
                </li>
              ))}
            </ul>
          ) : <EmptyState>Completed simulations will build your evidence here.</EmptyState>}
        </article>

        <article className="dashboard-card">
          <div className="dashboard-card__heading">
            <h2>Latest result</h2><Clock3 size={20} />
          </div>
          {latestResult ? (
            <div className="latest-result">
              <strong className="latest-result__title">{latestResult.title}</strong>
              <strong>{latestResult.score}<small>/100</small></strong>
              <button
                className="text-button"
                type="button"
                onClick={() => openSyn('Review my latest result', 'REVIEW_LATEST')}
              >
                View details with Syn <ArrowRight size={15} />
              </button>
            </div>
          ) : <EmptyState>Your latest completed simulation will appear here.</EmptyState>}
        </article>

        <article className="dashboard-card">
          <div className="dashboard-card__heading">
            <h2>Current plan</h2><Target size={20} />
          </div>
          {dashboard.plan ? (
            <div className="current-plan">
              <strong>{dashboard.plan.title}</strong><p>{dashboard.plan.nextStep}</p>
            </div>
          ) : <EmptyState>Ask Syn to draft a small, editable exploration plan.</EmptyState>}
          <button
            className="text-button"
            type="button"
            onClick={() => openSyn('Draft an exploration plan', 'DRAFT_PLAN')}
          >
            Draft with Syn <ArrowRight size={15} />
          </button>
        </article>
      </div>
    </section>
  )
}

export default GuidancePage
