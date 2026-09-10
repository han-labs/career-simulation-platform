import {
  ArrowRight,
  Bot,
  Check,
  CircleAlert,
  Clock3,
  FileSearch,
  Lightbulb,
  LoaderCircle,
  RefreshCw,
  Send,
  Sparkles,
  Target,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getExplorationDashboard,
  saveExplorationPlan,
  sendSynMessage,
} from '../api/guidanceApi.js'

const quickActions = [
  { label: 'Understand my interests', type: 'EXPLAIN_RIASEC', icon: Sparkles },
  { label: 'Review latest result', type: 'REVIEW_LATEST', icon: FileSearch },
  { label: 'Find evidence gaps', type: 'NEEDS_EVIDENCE', icon: Lightbulb },
  { label: 'Draft a next-step plan', type: 'DRAFT_PLAN', icon: Target },
]

const initialMessage = {
  id: 'syn-welcome',
  role: 'assistant',
  provenance: 'STANDARD',
  text: "Hi, I'm Syn. I can help you understand your results and plan what to explore next. The choice stays yours.",
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

function SourceBadge({ source }) {
  const aiGenerated = source === 'AI'
  return (
    <span className={`source-badge source-badge--${aiGenerated ? 'ai' : 'standard'}`}>
      {aiGenerated ? 'AI-assisted' : 'Standard guidance'}
    </span>
  )
}

function ResultCard({ result }) {
  if (!result) return null

  return (
    <article className="syn-result-card" aria-label={`${result.title} result details`}>
      <div className="syn-result-card__heading">
        <div>
          <span>Objective simulation result</span>
          <strong>{result.title}</strong>
        </div>
        <strong className="syn-result-card__score">{result.score}/100</strong>
      </div>
      <ul>
        {(result.outcomes ?? []).map((outcome) => (
          <li key={outcome.label} className={outcome.status === 'NEEDS_MORE_EVIDENCE' ? 'is-highlighted' : ''}>
            <span>{outcome.label}</span>
            <small>{statusLabel(outcome.status)}</small>
          </li>
        ))}
      </ul>
      <small className="syn-result-card__note">Objective result — Syn does not change this score.</small>
    </article>
  )
}

function SynMessage({ message, onSuggestion, onPlanReview }) {
  if (message.role === 'user') {
    return <div className="syn-message syn-message--user">{message.text}</div>
  }

  return (
    <div className="syn-message syn-message--assistant">
      <span className="syn-message__avatar" aria-hidden="true"><Bot size={17} /></span>
      <div className="syn-message__body">
        <p>{message.text}</p>
        <ResultCard result={message.resultCard} />
        {message.planDraft && (
          <article className="syn-plan-draft">
            <span>Draft—not saved</span>
            <strong>{message.planDraft.title}</strong>
            <ol>{message.planDraft.steps.map((step) => <li key={step}>{step}</li>)}</ol>
            <button type="button" onClick={() => onPlanReview(message.planDraft)}>Review before saving</button>
          </article>
        )}
        {message.suggestions?.length > 0 && (
          <div className="syn-suggestions" aria-label="Suggested follow-up questions">
            {message.suggestions.map((suggestion) => (
              <button key={suggestion} type="button" onClick={() => onSuggestion(suggestion)}>{suggestion}</button>
            ))}
          </div>
        )}
        <SourceBadge source={message.provenance} />
      </div>
    </div>
  )
}

function EmptyState({ children }) {
  return <p className="dashboard-empty">{children}</p>
}

function GuidancePage() {
  const [dashboard, setDashboard] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [messages, setMessages] = useState([initialMessage])
  const [draft, setDraft] = useState('')
  const [isReplying, setIsReplying] = useState(false)
  const [pendingPlan, setPendingPlan] = useState(null)

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
      .catch(() => { if (isCurrent) setLoadError('We could not load your exploration data. Please try again.') })

    return () => { isCurrent = false }
  }, [])

  async function askSyn(text, actionType = 'FREE_TEXT') {
    const message = text.trim()
    if (!message || isReplying || !dashboard) return

    setMessages((current) => [...current, { id: `user-${Date.now()}`, role: 'user', text: message }])
    setDraft('')
    setIsReplying(true)
    try {
      const reply = await sendSynMessage({
        message,
        actionType,
        dashboard,
        context: { attemptId: dashboard.recentResults[0]?.id ?? null },
      })
      setMessages((current) => [...current, reply])
    } catch {
      setMessages((current) => [...current, {
        id: `error-${Date.now()}`,
        role: 'assistant',
        provenance: 'STANDARD',
        text: 'I could not process that request safely. Your existing results have not changed. Please try again.',
      }])
    } finally {
      setIsReplying(false)
    }
  }

  async function confirmPlan() {
    const result = await saveExplorationPlan(pendingPlan)
    setPendingPlan(null)
    setMessages((current) => [...current, {
      id: `plan-${Date.now()}`,
      role: 'assistant',
      provenance: 'STANDARD',
      text: result.preview
        ? 'The plan is ready, but saving is disabled in preview mode. Nothing was written to your account.'
        : 'Your exploration plan has been saved. You can revise it whenever you gather new evidence.',
    }])
  }

  if (loadError) {
    return (
      <section className="section-shell dashboard-error">
        <CircleAlert size={28} />
        <h1>Dashboard unavailable</h1>
        <p>{loadError}</p>
        <button className="button" type="button" onClick={loadDashboard}><RefreshCw size={17} /> Try again</button>
      </section>
    )
  }

  if (!dashboard) {
    return <div className="section-shell dashboard-loading"><LoaderCircle className="spin" /> Loading your dashboard…</div>
  }

  const progressTotal = Math.max(dashboard.progress.total, 1)
  const progressPercent = Math.min(100, Math.round((dashboard.progress.completed / progressTotal) * 100))
  const latestResult = dashboard.recentResults[0]

  return (
    <section className="section-shell us03-page">
      <header className="dashboard-heading">
        <div>
          <h1>Dashboard</h1>
          <p>Your progress, interests, and recent practice at a glance.</p>
        </div>
        <Link className="button button--secondary" to="/simulations">Browse simulations <ArrowRight size={17} /></Link>
      </header>

      {dashboard.source === 'PREVIEW' && (
        <div className="preview-notice" role="status">
          <CircleAlert size={17} /> Preview data — live student data is not connected yet.
        </div>
      )}

      <div className="us03-layout">
        <div className="dashboard-grid">
          <article className="dashboard-card dashboard-card--progress">
            <div className="dashboard-card__heading">
              <h2>Exploration progress</h2>
              <strong>{dashboard.progress.completed}/{dashboard.progress.total}</strong>
            </div>
            <div className="progress-track" role="progressbar" aria-valuenow={dashboard.progress.completed} aria-valuemin="0" aria-valuemax={dashboard.progress.total}>
              <span style={{ width: `${progressPercent}%` }} />
            </div>
          </article>

          <article className="dashboard-card">
            <div className="dashboard-card__heading"><h2>Top interest signals</h2><Sparkles size={20} /></div>
            {dashboard.interestSignals.length ? (
              <ul className="signal-list">
                {dashboard.interestSignals.map((signal) => (
                  <li key={signal.code}>
                    <strong>{signal.code}</strong><span>{signal.label}</span>
                    <span className="signal-meter" aria-label={`${signal.score} points`}><i style={{ width: `${Math.min(signal.score * 5, 100)}%` }} /></span>
                    <small>{signal.score}</small>
                  </li>
                ))}
              </ul>
            ) : <EmptyState>Complete the RIASEC assessment to see your signals.</EmptyState>}
            <Link className="text-button" to="/assessment">View assessment <ArrowRight size={15} /></Link>
          </article>

          <article className="dashboard-card">
            <div className="dashboard-card__heading"><h2>Skills practised</h2><Check size={20} /></div>
            {dashboard.skills.length ? (
              <ul className="skill-list">
                {dashboard.skills.map((skill) => (
                  <li key={skill.name}>
                    <strong>{skill.name}</strong>
                    <span className={`skill-status skill-status--${statusClass(skill.status)}`}>{statusLabel(skill.status)}</span>
                  </li>
                ))}
              </ul>
            ) : <EmptyState>Completed simulations will build your evidence here.</EmptyState>}
          </article>

          <article className="dashboard-card">
            <div className="dashboard-card__heading"><h2>Latest result</h2><Clock3 size={20} /></div>
            {latestResult ? (
              <div className="latest-result">
                <strong className="latest-result__title">{latestResult.title}</strong>
                <strong>{latestResult.score}<small>/100</small></strong>
                <button className="text-button" type="button" onClick={() => askSyn('Review my latest result', 'REVIEW_LATEST')}>View details with Syn <ArrowRight size={15} /></button>
              </div>
            ) : <EmptyState>Your latest completed simulation will appear here.</EmptyState>}
          </article>

          <article className="dashboard-card">
            <div className="dashboard-card__heading"><h2>Current plan</h2><Target size={20} /></div>
            {dashboard.plan ? (
              <div className="current-plan"><strong>{dashboard.plan.title}</strong><p>{dashboard.plan.nextStep}</p></div>
            ) : <EmptyState>Ask Syn to draft a small, editable exploration plan.</EmptyState>}
            <button className="text-button" type="button" onClick={() => askSyn('Draft an exploration plan', 'DRAFT_PLAN')}>Draft with Syn <ArrowRight size={15} /></button>
          </article>
        </div>

        <aside className="syn-panel" aria-label="Syn exploration assistant">
          <header className="syn-panel__header">
            <span className="syn-avatar"><Bot size={23} /></span>
            <div><h2>Syn</h2><p>Your CareerSim exploration assistant</p></div>
            <span className="syn-state"><i /> {dashboard.source === 'PREVIEW' ? 'Standard mode' : 'Available'}</span>
          </header>

          {latestResult && <div className="context-chip"><FileSearch size={15} /><span>Discussing <strong>{latestResult.title}</strong></span></div>}

          <div className="syn-conversation" aria-live="polite">
            {messages.map((message) => (
              <SynMessage key={message.id} message={message} onSuggestion={askSyn} onPlanReview={setPendingPlan} />
            ))}
            {isReplying && <div className="syn-thinking"><LoaderCircle className="spin" size={17} /> Syn is connecting your evidence…</div>}
          </div>

          {pendingPlan && (
            <section className="plan-confirm" aria-label="Confirm exploration plan">
              <strong>Save this draft plan?</strong>
              <p>Review the steps above. Saving does not change any assessment or simulation result.</p>
              <div><button type="button" onClick={() => setPendingPlan(null)}>Keep editing</button><button type="button" onClick={confirmPlan}>Confirm and save</button></div>
            </section>
          )}

          <div className="quick-actions" aria-label="Quick actions">
            {quickActions.map(({ label, type, icon: Icon }) => (
              <button key={type} type="button" disabled={isReplying} onClick={() => askSyn(label, type)}><Icon size={16} /> {label}</button>
            ))}
          </div>

          <form className="syn-composer" onSubmit={(event) => { event.preventDefault(); askSyn(draft) }}>
            <label htmlFor="syn-message">Ask Syn about your exploration</label>
            <div><textarea id="syn-message" rows="2" maxLength="600" value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="Ask about your results, evidence, or next step…" /><button type="submit" aria-label="Send message" disabled={!draft.trim() || isReplying}><Send size={18} /></button></div>
          </form>
        </aside>
      </div>
    </section>
  )
}

export default GuidancePage
