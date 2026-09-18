import {
  Bot,
  BookOpen,
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
  Trash2,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  clearSynSession,
  getExplorationDashboard,
  saveExplorationPlan,
  sendSynMessage,
} from '../api/guidanceApi.js'

const SYN_OPEN_EVENT = 'careersim:syn-open'
const SYN_SESSION_KEY = 'careersim_syn_session_v1'
const SYN_LANGUAGE_KEY = 'careersim_syn_language_v1'

const quickActions = [
  { label: 'Understand my interests', type: 'EXPLAIN_RIASEC', icon: Sparkles },
  { label: 'Compare backend and frontend', type: 'COMPARE_PATHS', icon: FileSearch },
  { label: 'Find evidence gaps', type: 'NEEDS_EVIDENCE', icon: Lightbulb },
  { label: 'Suggest a learning resource', type: 'LEARNING_RESOURCES', icon: BookOpen },
]

const newcomerQuickActions = [
  { label: 'Help me get started', type: 'GET_STARTED', icon: Sparkles },
  { label: 'Understand RIASEC', type: 'EXPLAIN_RIASEC', icon: Lightbulb },
  { label: 'Compare backend and frontend', type: 'COMPARE_PATHS', icon: FileSearch },
  { label: 'Show a learning resource', type: 'LEARNING_RESOURCES', icon: BookOpen },
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

function AgentCards({ message }) {
  return (
    <>
      {message.comparisonCard?.paths?.length > 0 && (
        <article className="syn-agent-card">
          <strong>{message.comparisonCard.title}</strong>
          {message.comparisonCard.paths.map((path) => (
            <section key={path.code}>
              <b>{path.title}</b>
              <p>{path.evidenceSummary}</p>
              <small>{path.nextExperiment}</small>
            </section>
          ))}
        </article>
      )}
      {message.resourceCards?.length > 0 && (
        <article className="syn-agent-card syn-resource-list">
          <strong>Reviewed learning resources</strong>
          {message.resourceCards.map((resource) => (
            <a key={resource.url} href={resource.url} target="_blank" rel="noreferrer">
              <span>{resource.title}</span>
              <small>{resource.provider} · {resource.difficulty.toLowerCase()} · {resource.estimatedMinutes} min</small>
            </a>
          ))}
        </article>
      )}
      {message.evidenceReferences?.length > 0 && (
        <div className="syn-evidence-references" aria-label="Evidence used">
          {message.evidenceReferences.map((reference) => (
            <span key={`${reference.sourceType}-${reference.label}`}>{reference.label}</span>
          ))}
        </div>
      )}
      {message.activity?.length > 0 && (
        <details className="syn-activity">
          <summary>What Syn checked</summary>
          <ul>{message.activity.map((item) => <li key={item}>{item}</li>)}</ul>
        </details>
      )}
    </>
  )
}

function SynMessage({ message, onSuggestion, onPlanReview }) {
  if (message.role === 'user') {
    return <div className="syn-message syn-message--user" data-message-id={message.id}>{message.text}</div>
  }

  return (
    <div className="syn-message syn-message--assistant" data-message-id={message.id}>
      <span className="syn-message__avatar" aria-hidden="true"><Bot size={17} /></span>
      <div className="syn-message__body">
        <p>
          {message.text}
          {message.isRevealing && <span className="syn-typing-cursor" aria-hidden="true" />}
        </p>
        {!message.isRevealing && <AgentCards message={message} />}
        {!message.isRevealing && <ResultCard result={message.resultCard} />}
        {!message.isRevealing && message.planDraft && (
          <article className="syn-plan-draft">
            <span>Draft—not saved</span>
            <strong>{message.planDraft.title}</strong>
            <ol>{message.planDraft.steps.map((step) => <li key={step}>{step}</li>)}</ol>
            <button type="button" onClick={() => onPlanReview(message.planDraft)}>
              Review before saving
            </button>
          </article>
        )}
        {!message.isRevealing && message.suggestions?.length > 0 && (
          <div className="syn-suggestions" aria-label="Suggested follow-up questions">
            {message.suggestions.map((suggestion) => (
              <button key={suggestion} type="button" onClick={() => onSuggestion(suggestion)}>
                {suggestion}
              </button>
            ))}
          </div>
        )}
        {!message.isRevealing && <SourceBadge source={message.provenance} />}
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
  const [isRevealing, setIsRevealing] = useState(false)
  const [pendingPlan, setPendingPlan] = useState(null)
  const [sessionId, setSessionId] = useState(() => localStorage.getItem(SYN_SESSION_KEY))
  const [responseLanguage, setResponseLanguage] = useState(
    () => localStorage.getItem(SYN_LANGUAGE_KEY) ?? 'AUTO',
  )
  const [showJumpToLatest, setShowJumpToLatest] = useState(false)
  const composerRef = useRef(null)
  const conversationRef = useRef(null)
  const pendingAnchorRef = useRef(null)
  const activeRevealRef = useRef(null)

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

  const finishActiveReveal = useCallback((showFullText = true) => {
    const active = activeRevealRef.current
    if (!active) return
    window.clearInterval(active.timer)
    if (showFullText) {
      setMessages((current) => current.map((item) => (
        item.id === active.id
          ? { ...item, text: active.fullText, isRevealing: false }
          : item
      )))
    }
    setIsRevealing(false)
    activeRevealRef.current = null
    active.resolve()
  }, [])

  const revealReply = useCallback((reply) => new Promise((resolve) => {
    const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (reducedMotion || reply.text.length < 24) {
      setMessages((current) => [...current, reply])
      setIsRevealing(false)
      resolve()
      return
    }

    const fullText = reply.text
    let visibleLength = 0
    setIsRevealing(true)
    setMessages((current) => [
      ...current,
      { ...reply, text: '', isRevealing: true },
    ])
    const timer = window.setInterval(() => {
      visibleLength = Math.min(fullText.length, visibleLength + 12)
      setMessages((current) => current.map((item) => (
        item.id === reply.id
          ? {
            ...item,
            text: fullText.slice(0, visibleLength),
            isRevealing: visibleLength < fullText.length,
          }
          : item
      )))
      if (visibleLength >= fullText.length) finishActiveReveal(true)
    }, 24)
    activeRevealRef.current = { id: reply.id, fullText, timer, resolve }
  }), [finishActiveReveal])

  const askSyn = useCallback(async (text, actionType = 'FREE_TEXT', contextDashboard = dashboard) => {
    const message = text.trim()
    if (!message || isReplying || !contextDashboard) return

    const userMessageId = `user-${Date.now()}`
    pendingAnchorRef.current = userMessageId
    setMessages((current) => [
      ...current,
      { id: userMessageId, role: 'user', text: message },
    ])
    setDraft('')
    setIsReplying(true)
    try {
      const reply = await sendSynMessage({
        message,
        actionType,
        sessionId,
        responseLanguage,
        dashboard: contextDashboard,
        context: { attemptId: contextDashboard.recentResults[0]?.id ?? null },
      })
      if (reply.sessionId) {
        localStorage.setItem(SYN_SESSION_KEY, reply.sessionId)
        setSessionId(reply.sessionId)
      }
      await revealReply(reply)
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
  }, [dashboard, isReplying, responseLanguage, revealReply, sessionId])

  async function resetConversation() {
    finishActiveReveal(false)
    await clearSynSession(sessionId)
    localStorage.removeItem(SYN_SESSION_KEY)
    setSessionId(null)
    setPendingPlan(null)
    setMessages([initialMessage])
  }

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
    const container = conversationRef.current
    const anchorId = pendingAnchorRef.current
    if (!container || !anchorId) return undefined
    const frame = window.requestAnimationFrame(() => {
      const anchor = container.querySelector(`[data-message-id="${anchorId}"]`)
      if (anchor) {
        container.scrollTo({ top: Math.max(0, anchor.offsetTop - 12), behavior: 'smooth' })
        setShowJumpToLatest(false)
      }
      pendingAnchorRef.current = null
    })
    return () => window.cancelAnimationFrame(frame)
  }, [messages])

  useEffect(() => {
    const container = conversationRef.current
    if (!container || pendingAnchorRef.current) return undefined
    const frame = window.requestAnimationFrame(() => {
      const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight
      setShowJumpToLatest(distanceFromBottom > 56)
    })
    return () => window.cancelAnimationFrame(frame)
  }, [messages, isReplying])

  useEffect(() => () => finishActiveReveal(false), [finishActiveReveal])

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
  const isNewStudent = dashboard
    && dashboard.interestSignals.length === 0
    && dashboard.recentResults.length === 0
  const availableQuickActions = isNewStudent ? newcomerQuickActions : quickActions

  function changeResponseLanguage(event) {
    const language = event.target.value
    localStorage.setItem(SYN_LANGUAGE_KEY, language)
    setResponseLanguage(language)
  }

  function updateConversationPosition() {
    const container = conversationRef.current
    if (!container) return
    const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight
    if (distanceFromBottom <= 32) setShowJumpToLatest(false)
  }

  function jumpToLatest() {
    conversationRef.current?.scrollTo({
      top: conversationRef.current.scrollHeight,
      behavior: 'smooth',
    })
    setShowJumpToLatest(false)
  }

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
          <button type="button" onClick={resetConversation} aria-label="Clear Syn conversation memory">
            <Trash2 size={16} />
          </button>
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
            {dashboard.source === 'PREVIEW' ? 'Preview · Standard' : sessionId ? 'Memory on' : 'Evidence ready'}
          </span>
          {latestResult && <span className="context-chip"><FileSearch size={14} /> {latestResult.title}</span>}
          <label className="syn-language-control">
            <span className="sr-only">Syn reply language</span>
            <select value={responseLanguage} onChange={changeResponseLanguage}>
              <option value="AUTO">Auto</option>
              <option value="EN">EN</option>
              <option value="VI">VI</option>
            </select>
          </label>
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
          <div className="syn-conversation-wrap">
            <div
              className="syn-conversation"
              ref={conversationRef}
              aria-live="polite"
              onScroll={updateConversationPosition}
            >
              {messages.map((message) => (
                <SynMessage
                  key={message.id}
                  message={message}
                  onSuggestion={askSyn}
                  onPlanReview={setPendingPlan}
                />
              ))}
              {isReplying && !isRevealing && (
                <div className="syn-thinking">
                  <LoaderCircle className="spin" size={17} /> Syn is connecting your context…
                </div>
              )}
            </div>
            {showJumpToLatest && (
              <button className="syn-jump-latest" type="button" onClick={jumpToLatest}>
                Jump to latest
              </button>
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
            {availableQuickActions.map(({ label, type, icon: Icon }) => (
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
                onKeyDown={(event) => {
                  if (event.key === 'Enter' && !event.shiftKey) {
                    event.preventDefault()
                    event.currentTarget.form?.requestSubmit()
                  }
                }}
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
