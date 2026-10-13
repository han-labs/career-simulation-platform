import { useI18n } from '../../../shared/i18n/useI18n.js'
import {
  Bot,
  BookOpen,
  CircleAlert,
  Check,
  Copy,
  Download,
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
import { safeSourceUrl } from '../utils/synSources.js'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  buildSynResponseMarkdown,
  clearSynSession,
  getExplorationDashboard,
  saveExplorationPlan,
  sendSynMessage,
} from '../api/guidanceApi.js'


const SYN_OPEN_EVENT = 'careersim:syn-open'
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
  localCopy: true,
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
  const { t } = useI18n()
  const aiGenerated = source === 'AI'
  return (
    <span className={`source-badge source-badge--${aiGenerated ? 'ai' : 'standard'}`}>
      {t(aiGenerated ? 'AI-assisted' : 'Standard guidance')}
    </span>
  )
}

function ResultCard({ result }) {
  const { t } = useI18n()
  if (!result) return null

  return (
    <article className="syn-result-card" aria-label={t(`${result.title} result details`)}>
      <div className="syn-result-card__heading">
        <div><span>{t("Objective result")}</span><strong>{t(result.title)}</strong></div>
        <strong className="syn-result-card__score">{t(result.score)}/100</strong>
      </div>
      <ul>
        {(result.outcomes ?? []).map((outcome) => (
          <li
            key={outcome.label}
            className={outcome.status === 'NEEDS_MORE_EVIDENCE' ? 'is-highlighted' : ''}
          >
            <span>{t(outcome.label)}</span><small>{t(statusLabel(outcome.status))}</small>
          </li>
        ))}
      </ul>
      <small className="syn-result-card__note">{t("Syn never changes objective scores.")}</small>
    </article>
  )
}

function ResultCards({ message }) {
  const { t } = useI18n()
  const results = message.resultCards?.length
    ? message.resultCards
    : message.resultCard
      ? [message.resultCard]
      : []
  if (!results.length) return null
  const cards = results.map((result) => <ResultCard key={result.id} result={result} />)
  return ['REVIEW_RESULT', 'REVIEW_LATEST'].includes(message.intent) ? cards : (
    <details className="syn-supporting-details"><summary>{t('View supporting results')}</summary>{cards}</details>
  )
}

function AgentCards({ message }) {
  const { t, locale } = useI18n()
  const vi = locale === 'vi'
  const webStatus = {
    RATE_LIMITED: vi ? 'Chưa tìm được nguồn web: nhà cung cấp đang giới hạn lượt tìm kiếm hoặc quota. Các link nội bộ không phải kết quả tìm web mới.' : 'Live search was rate/quota limited. Local catalog links are not new web search results.',
    ACCESS_DENIED: vi ? 'Nhà cung cấp chưa cho phép truy cập tìm kiếm với cấu hình hiện tại.' : 'The search provider denied access with the current configuration.',
    NO_CONSENT: vi ? 'Tìm web chưa chạy vì bạn chưa bật đồng ý xử lý AI.' : 'Web search requires your AI consent.',
    DISABLED: vi ? 'Tìm web chưa được bật trong cấu hình hệ thống.' : 'Web search is not enabled in this deployment.',
    NEEDS_TOPIC: vi ? 'Hãy nêu rõ hướng nghề hoặc kỹ năng cần tìm.' : 'Name a career path or skill to search.',
  }
  return (
    <>
      {message.comparisonCard?.paths?.length > 0 && (
        <details className="syn-supporting-details">
          <summary>{t(vi ? 'Xem dữ liệu đối chiếu' : 'View comparison evidence')}</summary>
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
        </details>
      )}
      {message.resourceCards?.length > 0 && (
        <article className="syn-agent-card syn-resource-list">
          <strong>{t("Reviewed learning resources")}</strong>
          {message.resourceCards.map((resource) => (
            <a key={resource.url} href={resource.url} target="_blank" rel="noreferrer">
              <span>{resource.title}</span>
              <small>{resource.provider} · {t(resource.difficulty.toLowerCase())} · {t(resource.estimatedMinutes)} {t("min")}</small>
            </a>
          ))}
        </article>
      )}
      {message.webSearch && message.webSearch.status !== 'NOT_REQUESTED' && (
        <article className="syn-agent-card syn-resource-list" aria-label={t("Web learning sources")}>
          <strong>{t("Sources from the web")}</strong>
          <small>{t("Not internally reviewed.")} {t(message.webSearch.retrievedAt ? `Retrieved ${new Date(message.webSearch.retrievedAt).toLocaleString(locale === 'vi' ? 'vi-VN' : 'en')}` : '')}</small>
          {message.webSearch.sources?.filter(source => safeSourceUrl(source.url)).map(source => (
            <a key={source.referenceId} href={safeSourceUrl(source.url)} target="_blank" rel="noreferrer">
              <span>[{t(source.referenceId)}] {source.title}</span><small>{source.excerpt}</small>
            </a>
          ))}
          {!message.webSearch.sources?.length && <p role="status">{t(webStatus[message.webSearch.status] ?? (vi ? 'Tìm web chưa trả được nguồn có thể dẫn chứng. Phần giải thích tổng quát không phải kết quả tìm web đã xác minh.' : 'Live search did not return citable sources. General explanations are not verified web results.'))}</p>}
          {message.webSearch.searchSuggestionsHtml && <iframe title={t("Google Search suggestions")} sandbox="allow-popups allow-popups-to-escape-sandbox" referrerPolicy="no-referrer" srcDoc={message.webSearch.searchSuggestionsHtml} style={{ width: '100%', border: 0, minHeight: 160 }} />}
        </article>
      )}
      {message.simulationCards?.length > 0 && (
        <article className="syn-agent-card syn-simulation-list">
          <strong>{t("Simulations to try")}</strong>
          {message.simulationCards.map((simulation) => (
            <section key={`${simulation.careerTrack}-${simulation.title}`}>
              <b>{t(simulation.title)}</b>
              <small>{t(simulation.careerTrack.replaceAll('_', ' ').toLowerCase())}</small>
            </section>
          ))}
        </article>
      )}
      {message.evidenceReferences?.length > 0 && (
        <div className="syn-evidence-references" aria-label={t("Evidence used")}>
          {message.evidenceReferences.map((reference) => (
            <details key={`${reference.sourceType}-${reference.referenceId ?? reference.label}`}>
              <summary title={t(reference.label)}>{t(reference.referenceId ? `[${reference.referenceId}]` : (vi ? 'Nguồn' : 'Source'))}</summary>
              <small>{reference.label}</small>
              {reference.section && <small>{reference.section}{t(reference.version ? ` · Version ${reference.version}` : '')}</small>}
              {reference.excerpt && <blockquote>{reference.excerpt}</blockquote>}
              {safeSourceUrl(reference.url) && <a href={safeSourceUrl(reference.url)} target="_blank" rel="noreferrer">{t("Open source")}</a>}
            </details>
          ))}
        </div>
      )}
      {message.activity?.length > 0 && (
        <details className="syn-activity">
          <summary>{t(vi ? 'Syn đã kiểm tra gì' : 'What Syn checked')}</summary>
          <ul>{message.activity.map((item) => <li key={item}>{item}</li>)}</ul>
        </details>
      )}
    </>
  )
}

function SynMessage({
  message,
  onSuggestion,
  onPlanReview,
  onCopy,
  onDownload,
  copied,
}) {
  const { t } = useI18n()
  if (message.role === 'user') {
    return <div className="syn-message syn-message--user" data-message-id={message.id}>{message.text}</div>
  }

  return (
    <div className="syn-message syn-message--assistant" data-message-id={message.id}>
      <span className="syn-message__avatar" aria-hidden="true"><Bot size={17} /></span>
      <div className="syn-message__body">
        <p>
          {message.localCopy ? t(message.text) : message.text}
          {message.isRevealing && <span className="syn-typing-cursor" aria-hidden="true" />}
        </p>
        {!message.isRevealing && message.reasonCode && message.provenance !== 'AI' && !['STANDARD_POLICY', 'PENDING_PLAN_CONTEXT'].includes(message.reasonCode) && (
          <small className="syn-fallback-notice">{t(message.reasonCode === 'NO_AI_CONSENT'
            ? 'AI processing requires your consent; this is standard guidance.'
            : 'AI explanation was unavailable; these are the available standard results. You can retry your question.')}</small>
        )}
        {!message.isRevealing && <AgentCards message={message} />}
        {!message.isRevealing && <ResultCards message={message} />}
        {!message.isRevealing && message.planDraft && (
          <article className="syn-plan-draft">
            <span>{t("Draft—not saved")}</span>
            <strong>{message.planDraft.title}</strong>
            <ol>{message.planDraft.steps.map((step) => <li key={step}>{step}</li>)}</ol>
            <button type="button" onClick={() => onPlanReview(message.planDraft)}> {t("Review before saving")} </button>
          </article>
        )}
        {!message.isRevealing && message.suggestions?.length > 0 && (
          <div className="syn-suggestions" aria-label={t("Suggested follow-up questions")}>
            {message.suggestions.map((suggestion) => (
              <button key={suggestion} type="button" onClick={() => onSuggestion(suggestion)}>
                {suggestion}
              </button>
            ))}
          </div>
        )}
        {!message.isRevealing && (
          <div className="syn-response-footer">
            <SourceBadge source={message.provenance} />
            {message.id !== 'syn-welcome' && (
              <div className="syn-response-actions" aria-label={t("Response actions")}>
                <button type="button" onClick={() => onCopy(message)}>
                  {copied ? <Check size={14} /> : <Copy size={14} />}
                  {t(copied ? 'Copied' : 'Copy')}
                </button>
                <button type="button" onClick={() => onDownload(message)}>
                  <Download size={14} /> {t("Download")} </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

function SynWidget() {
  const { t, locale } = useI18n()
  const [mode, setMode] = useState('collapsed')
  const [dashboard, setDashboard] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [messages, setMessages] = useState([initialMessage])
  const [failedRequest, setFailedRequest] = useState(null)
  const [clearError, setClearError] = useState('')
  const [isClearing, setIsClearing] = useState(false)
  const busyRef = useRef(false)
  const [draft, setDraft] = useState('')
  const [isReplying, setIsReplying] = useState(false)
  const [isRevealing, setIsRevealing] = useState(false)
  const [pendingPlan, setPendingPlan] = useState(null)
  const [isSavingPlan, setIsSavingPlan] = useState(false)
  const [planSaveError, setPlanSaveError] = useState('')
  const planSaveRef = useRef(false)
  const [sessionId, setSessionId] = useState(null)
  const [responseLanguage, setResponseLanguage] = useState(
    () => {
      try {
        const stored = localStorage.getItem(SYN_LANGUAGE_KEY)
        return ['AUTO', 'EN', 'VI'].includes(stored) ? stored : 'AUTO'
      } catch { return 'AUTO' }
    },
  )
  const effectiveResponseLanguage = responseLanguage === 'AUTO' ? locale.toUpperCase() : responseLanguage
  const [showJumpToLatest, setShowJumpToLatest] = useState(false)
  const [copiedMessageId, setCopiedMessageId] = useState(null)
  const composerRef = useRef(null)
  const conversationRef = useRef(null)
  const pendingAnchorRef = useRef(null)
  const activeRevealRef = useRef(null)
  const lastPlanDraftRef = useRef(null)

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
    const charactersPerTick = fullText.length > 900 ? 8 : fullText.length > 450 ? 6 : 4
    setIsRevealing(true)
    setMessages((current) => [
      ...current,
      { ...reply, text: '', isRevealing: true },
    ])
    const timer = window.setInterval(() => {
      visibleLength = Math.min(fullText.length, visibleLength + charactersPerTick)
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
    }, 30)
    activeRevealRef.current = { id: reply.id, fullText, timer, resolve }
  }), [finishActiveReveal])

  const askSyn = useCallback(async (
    text,
    actionType = 'FREE_TEXT',
    contextDashboard = dashboard,
    requestContext = null,
    retryId = null,
    retryLanguage = null,
  ) => {
    const message = text.trim()
    if (!message || busyRef.current || isReplying || isClearing || !contextDashboard) return
    busyRef.current = true

    const clientMessageId = retryId ?? crypto.randomUUID()
    const requestLanguage = retryLanguage ?? effectiveResponseLanguage
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
        clientMessageId,
        responseLanguage: requestLanguage,
        dashboard: contextDashboard,
        context: requestContext,
      })
      setFailedRequest(null)
      if (reply.sessionId) {
        setSessionId(reply.sessionId)
      }
      if (reply.planDraft) lastPlanDraftRef.current = reply.planDraft
      await revealReply(reply)
      if (reply.intent === 'CONFIRM_PLAN' && lastPlanDraftRef.current) {
        setPendingPlan(lastPlanDraftRef.current)
      }
    } catch (error) {
      setFailedRequest({ message, actionType, contextDashboard, requestContext, responseLanguage: requestLanguage, clientMessageId: error.code === 'TURN_FINISHED_WITHOUT_RESPONSE' ? crypto.randomUUID() : clientMessageId })
      setMessages((current) => [...current, {
        id: `error-${Date.now()}`,
        role: 'assistant',
        provenance: 'STANDARD',
        localCopy: true,
        text: 'I could not process that safely. Your existing results have not changed.',
      }])
    } finally {
      busyRef.current = false
      setIsReplying(false)
    }
  }, [dashboard, isReplying, isClearing, effectiveResponseLanguage, revealReply, sessionId])

  async function resetConversation() {
    if (busyRef.current) return
    busyRef.current = true
    setIsClearing(true)
    setClearError('')
    finishActiveReveal(false)
    try {
      if (sessionId) await clearSynSession(sessionId)
      setSessionId(null)
      setFailedRequest(null)
      setPendingPlan(null)
      lastPlanDraftRef.current = null
      setMessages([initialMessage])
    } catch {
      setClearError('Memory could not be cleared. Your conversation is still here. Please retry.')
    } finally {
      busyRef.current = false
      setIsClearing(false)
    }
  }

  useEffect(() => {
    async function handleOpen(event) {
      setMode((current) => current === 'expanded' ? current : 'compact')
      const contextDashboard = dashboard ?? await loadDashboard()
      if (contextDashboard) {
        askSyn(
          event.detail.message,
          event.detail.actionType,
          contextDashboard,
          event.detail.context ?? null,
        )
      }
    }
    window.addEventListener(SYN_OPEN_EVENT, handleOpen)
    return () => window.removeEventListener(SYN_OPEN_EVENT, handleOpen)
  }, [askSyn, dashboard, loadDashboard])

  useEffect(() => {
    if (mode !== 'collapsed') composerRef.current?.focus()
  }, [mode])

  useEffect(() => {
    // Remove legacy unscoped caches. Private conversations now stay in this mounted tab only.
    try {
      localStorage.removeItem('careersim_syn_session_v1')
      sessionStorage.removeItem('careersim_syn_transcript_v1')
    } catch { /* Storage may be disabled; private messages stay in React state. */ }
  }, [])

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
    if (!pendingPlan || planSaveRef.current) return
    if (dashboard?.source === 'PREVIEW') {
      setPlanSaveError('Saving needs a signed-in account. Your draft is still available here.')
      return
    }
    planSaveRef.current = true
    setIsSavingPlan(true)
    setPlanSaveError('')
    try {
      const result = await saveExplorationPlan(pendingPlan)
      if (result?.saved !== true) throw new Error('Save not confirmed')
      setPendingPlan(null)
      setMessages((current) => [...current, {
        id: `plan-${Date.now()}`, role: 'assistant', provenance: 'STANDARD',
        localCopy: true,
        text: 'Your exploration plan has been saved. You can revise it as you gather evidence.',
      }])
    } catch {
      setPlanSaveError('Could not confirm the save. Your draft is kept here; please retry.')
    } finally {
      planSaveRef.current = false
      setIsSavingPlan(false)
    }
  }

  if (mode === 'collapsed') {
    return (
      <button className="syn-launcher" type="button" onClick={showWidget} aria-label={t("Open Syn assistant")}>
        <Bot size={26} /><span aria-hidden="true" />
      </button>
    )
  }

  const latestResult = dashboard?.recentResults[0]
  const isNewStudent = dashboard
    && dashboard.interestSignals.length === 0
    && dashboard.recentResults.length === 0
  const availableQuickActions = isNewStudent ? newcomerQuickActions : quickActions

  const quickActionButtons = availableQuickActions.map(({ label, type, icon: Icon }) => (
    <button key={type} type="button" disabled={isReplying} onClick={() => askSyn(t(label), type)}>
      <Icon size={16} /> {t(label)}
    </button>
  ))

  function changeResponseLanguage(event) {
    const language = event.target.value
    try { localStorage.setItem(SYN_LANGUAGE_KEY, language) } catch { /* In-memory override still works. */ }
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

  async function copyResponse(message) {
    try {
      await navigator.clipboard.writeText(message.localCopy ? t(message.text) : message.text)
      setCopiedMessageId(message.id)
      window.setTimeout(() => setCopiedMessageId(null), 1600)
    } catch {
      setCopiedMessageId(null)
    }
  }

  function downloadResponse(message) {
    const blob = new Blob([buildSynResponseMarkdown(message, locale)], {
      type: 'text/markdown;charset=utf-8',
    })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `syn-response-${new Date().toISOString().slice(0, 10)}.md`
    anchor.click()
    URL.revokeObjectURL(url)
  }

  return (
    <aside
      className={`syn-widget syn-widget--${mode}`}
      role="dialog"
      aria-label={t("Syn exploration assistant")}
      aria-modal="false"
    >
      <header className="syn-panel__header">
        <span className="syn-avatar"><Bot size={23} /></span>
        <div><h2>{t("Syn")}</h2><p>{t("Explore your evidence")}</p></div>
        <div className="syn-window-actions">
          <button type="button" onClick={resetConversation} disabled={isReplying || isClearing || Boolean(failedRequest)} aria-label={t("Clear Syn conversation memory")}>
            <Trash2 size={16} />
          </button>
          <button
            type="button"
            onClick={() => setMode(mode === 'expanded' ? 'compact' : 'expanded')}
            aria-label={t(mode === 'expanded' ? 'Use compact Syn window' : 'Expand Syn window')}
          >
            {mode === 'expanded' ? <Minimize2 size={17} /> : <Maximize2 size={17} />}
          </button>
          <button type="button" onClick={() => setMode('collapsed')} aria-label={t("Collapse Syn to a circle")}>
            <Minus size={18} />
          </button>
        </div>
      </header>

      {dashboard && (
        <div className="syn-context-row">
          <span className={`syn-mode-badge ${dashboard.source === 'PREVIEW' ? 'is-preview' : ''}`}>
            {t(dashboard.source === 'PREVIEW' ? 'Preview · Standard' : sessionId ? 'Memory on' : 'Evidence ready')}
          </span>
          {latestResult && <span className="context-chip"><FileSearch size={14} /> {t(latestResult.title)}</span>}
          <label className="syn-language-control">
            <span className="sr-only">{t("Syn reply language")}</span>
            <select value={responseLanguage} onChange={changeResponseLanguage}>
              <option value="AUTO">{t("Use website language")}</option>
              <option value="EN">{t("EN")}</option>
              <option value="VI">{t("VI")}</option>
            </select>
          </label>
        </div>
      )}

      {clearError && <p role="alert">{t(clearError)}</p>}
      {loadError ? (
        <div className="syn-load-error" role="status">
          <CircleAlert size={22} /><p>{t(loadError)}</p>
          <button type="button" onClick={loadDashboard}><RefreshCw size={16} /> {t("Try again")}</button>
        </div>
      ) : !dashboard ? (
        <div className="syn-loading"><LoaderCircle className="spin" size={20} /> {t("Loading your evidence…")}</div>
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
                  onCopy={copyResponse}
                  onDownload={downloadResponse}
                  copied={copiedMessageId === message.id}
                />
              ))}
              {isReplying && !isRevealing && (
                <div className="syn-thinking">
                  <LoaderCircle className="spin" size={17} /> {t("Syn is connecting your context…")} </div>
              )}
            </div>
            {showJumpToLatest && (
              <button className="syn-jump-latest" type="button" onClick={jumpToLatest}> {t("Jump to latest")} </button>
            )}
          </div>

          {failedRequest && <button type="button" disabled={isReplying || isClearing} onClick={() => askSyn(failedRequest.message, failedRequest.actionType, failedRequest.contextDashboard, failedRequest.requestContext, failedRequest.clientMessageId, failedRequest.responseLanguage)}>{t("Retry this message")}</button>}
          {pendingPlan && (
            <section className="plan-confirm" aria-label={t("Confirm exploration plan")}>
              <strong>{t("Save this draft plan?")}</strong>
              <p>{t("Saving never changes an assessment or simulation result.")}</p>
              {planSaveError && <p role="alert">{t(planSaveError)}</p>}
              <div>
                <button type="button" disabled={isSavingPlan} onClick={() => { setPendingPlan(null); setPlanSaveError('') }}>{t("Keep editing")}</button>
                <button type="button" disabled={isSavingPlan} onClick={confirmPlan}>{t(isSavingPlan ? 'Saving…' : 'Confirm and save')}</button>
              </div>
            </section>
          )}

          {messages.length === 1 ? (
            <div className="quick-actions" aria-label={t("Quick actions")}>
              {t(quickActionButtons)}
            </div>
          ) : (
            <details className="quick-actions-disclosure">
              <summary>{t("Quick actions")}</summary>
              <div className="quick-actions" aria-label={t("Quick actions")}>
                {t(quickActionButtons)}
              </div>
            </details>
          )}

          <form className="syn-composer" onSubmit={(event) => { event.preventDefault(); askSyn(draft) }}>
            <label htmlFor="syn-message">{t("Ask Syn")}</label>
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
                placeholder={t("Ask about your results or next step…")}
              />
              <button type="submit" aria-label={t("Send message")} disabled={!draft.trim() || isReplying}>
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
