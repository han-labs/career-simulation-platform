import {
  Bot,
  CircleAlert,
  FileSearch,
  Lightbulb,
  LoaderCircle,
  Maximize2,
  Minimize2,
  Minus,
  RefreshCw,
  Send,
  Sparkles,
  Target,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getExplorationDashboard,
  saveExplorationPlan,
  sendSynMessage,
} from '../api/guidanceApi.js'

const SYN_OPEN_EVENT = 'careersim:syn-open'

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
  text: "Hi, I'm Syn. I connect your interests and practice evidence so you can decide what to explore next.",
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
        <div><span>Objective result</span><strong>{result.title}</strong></div>
        <strong className="syn-result-card__score">{result.score}/100</strong>
      </div>
      <ul>
        {(result.outcomes ?? []).map((outcome) => (
          <li
            key={outcome.label}
            className={outcome.status === 'NEEDS_MORE_EVIDENCE' ? 'is-highlighted' : ''}
          >
            <span>{outcome.label}</span><small>{statusLabel(outcome.status)}</small>
          </li>
        ))}
      </ul>
      <small className="syn-result-card__note">Syn never changes objective scores.</small>
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
            <button type="button" onClick={() => onPlanReview(message.planDraft)}>
              Review before saving
            </button>
          </article>
        )}
        {message.suggestions?.length > 0 && (
          <div className="syn-suggestions" aria-label="Suggested follow-up questions">
            {message.suggestions.map((suggestion) => (
              <button key={suggestion} type="button" onClick={() => onSuggestion(suggestion)}>
                {suggestion}
              </button>
            ))}
          </div>
        )}
        <SourceBadge source={message.provenance} />
      </div>
    </div>
  )
}

function SynWidget() {
  const [mode, setMode] = useState('collapsed')
  const [dashboard, setDashboard] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [messages, setMessages] = useState([initialMessage])
  const [draft, setDraft] = useState('')
  const [isReplying, setIsReplying] = useState(false)
  const [pendingPlan, setPendingPlan] = useState(null)
  const composerRef = useRef(null)
  const conversationRef = useRef(null)

  const loadDashboard = useCallback(async () => {
    setLoadError('')
    try {
      const data = await getExplorationDashboard()
      setDashboard(data)
      return data
    } catch {
      setLoadError('Syn could not load your exploration data.')
      return null
    }
  }, [])

  const showWidget = useCallback(() => {
    setMode((current) => current === 'collapsed' ? 'compact' : current)
    if (!dashboard && !loadError) loadDashboard()
  }, [dashboard, loadDashboard, loadError])

  const askSyn = useCallback(async (text, actionType = 'FREE_TEXT', contextDashboard = dashboard) => {
    const message = text.trim()
    if (!message || isReplying || !contextDashboard) return

    setMessages((current) => [
      ...current,
      { id: `user-${Date.now()}`, role: 'user', text: message },
    ])
    setDraft('')
    setIsReplying(true)
    try {
      const reply = await sendSynMessage({
        message,
        actionType,
        dashboard: contextDashboard,
        context: { attemptId: contextDashboard.recentResults[0]?.id ?? null },
      })
      setMessages((current) => [...current, reply])
    } catch {
      setMessages((current) => [...current, {
        id: `error-${Date.now()}`,
        role: 'assistant',
        provenance: 'STANDARD',
        text: 'I could not process that safely. Your existing results have not changed.',
      }])
    } finally {
      setIsReplying(false)
    }
  }, [dashboard, isReplying])

  useEffect(() => {
    async function handleOpen(event) {
      setMode((current) => current === 'expanded' ? current : 'compact')
      const contextDashboard = dashboard ?? await loadDashboard()
      if (contextDashboard) {
        askSyn(event.detail.message, event.detail.actionType, contextDashboard)
      }
    }
    window.addEventListener(SYN_OPEN_EVENT, handleOpen)
    return () => window.removeEventListener(SYN_OPEN_EVENT, handleOpen)
  }, [askSyn, dashboard, loadDashboard])

  useEffect(() => {
    if (mode !== 'collapsed') composerRef.current?.focus()
  }, [mode])

  useEffect(() => {
    conversationRef.current?.scrollTo({
      top: conversationRef.current.scrollHeight,
      behavior: 'smooth',
    })
  }, [messages, isReplying])

  useEffect(() => {
    function handleEscape(event) {
      if (event.key === 'Escape') setMode('collapsed')
    }
    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [])

  async function confirmPlan() {
    const result = await saveExplorationPlan(pendingPlan)
    setPendingPlan(null)
    setMessages((current) => [...current, {
      id: `plan-${Date.now()}`,
      role: 'assistant',
      provenance: 'STANDARD',
      text: result.preview
        ? 'The plan is ready, but saving needs a signed-in account. Nothing was changed.'
        : 'Your exploration plan has been saved. You can revise it as you gather evidence.',
    }])
  }

  if (mode === 'collapsed') {
    return (
      <button className="syn-launcher" type="button" onClick={showWidget} aria-label="Open Syn assistant">
        <Bot size={26} /><span aria-hidden="true" />
      </button>
    )
  }

  const latestResult = dashboard?.recentResults[0]

  return (
    <aside
      className={`syn-widget syn-widget--${mode}`}
      role="dialog"
      aria-label="Syn exploration assistant"
      aria-modal="false"
    >
      <header className="syn-panel__header">
        <span className="syn-avatar"><Bot size={23} /></span>
        <div><h2>Syn</h2><p>Explore your evidence</p></div>
        <div className="syn-window-actions">
          <button
            type="button"
            onClick={() => setMode(mode === 'expanded' ? 'compact' : 'expanded')}
            aria-label={mode === 'expanded' ? 'Use compact Syn window' : 'Expand Syn window'}
          >
            {mode === 'expanded' ? <Minimize2 size={17} /> : <Maximize2 size={17} />}
          </button>
          <button type="button" onClick={() => setMode('collapsed')} aria-label="Collapse Syn to a circle">
            <Minus size={18} />
          </button>
        </div>
      </header>

      {dashboard && (
        <div className="syn-context-row">
          <span className={`syn-mode-badge ${dashboard.source === 'PREVIEW' ? 'is-preview' : ''}`}>
            {dashboard.source === 'PREVIEW' ? 'Preview · Standard' : 'Evidence ready'}
          </span>
          {latestResult && <span className="context-chip"><FileSearch size={14} /> {latestResult.title}</span>}
        </div>
      )}

      {loadError ? (
        <div className="syn-load-error" role="status">
          <CircleAlert size={22} /><p>{loadError}</p>
          <button type="button" onClick={loadDashboard}><RefreshCw size={16} /> Try again</button>
        </div>
      ) : !dashboard ? (
        <div className="syn-loading"><LoaderCircle className="spin" size={20} /> Loading your evidence…</div>
      ) : (
        <>
          <div className="syn-conversation" ref={conversationRef} aria-live="polite">
            {messages.map((message) => (
              <SynMessage
                key={message.id}
                message={message}
                onSuggestion={askSyn}
                onPlanReview={setPendingPlan}
              />
            ))}
            {isReplying && (
              <div className="syn-thinking">
                <LoaderCircle className="spin" size={17} /> Syn is connecting your evidence…
              </div>
            )}
          </div>

          {pendingPlan && (
            <section className="plan-confirm" aria-label="Confirm exploration plan">
              <strong>Save this draft plan?</strong>
              <p>Saving never changes an assessment or simulation result.</p>
              <div>
                <button type="button" onClick={() => setPendingPlan(null)}>Keep editing</button>
                <button type="button" onClick={confirmPlan}>Confirm and save</button>
              </div>
            </section>
          )}

          <div className="quick-actions" aria-label="Quick actions">
            {quickActions.map(({ label, type, icon: Icon }) => (
              <button key={type} type="button" disabled={isReplying} onClick={() => askSyn(label, type)}>
                <Icon size={16} /> {label}
              </button>
            ))}
          </div>

          <form className="syn-composer" onSubmit={(event) => { event.preventDefault(); askSyn(draft) }}>
            <label htmlFor="syn-message">Ask Syn</label>
            <div>
              <textarea
                ref={composerRef}
                id="syn-message"
                rows="2"
                maxLength="600"
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Ask about your results or next step…"
              />
              <button type="submit" aria-label="Send message" disabled={!draft.trim() || isReplying}>
                <Send size={18} />
              </button>
            </div>
          </form>
        </>
      )}
    </aside>
  )
}

export default SynWidget
