import { useCallback, useEffect, useEffectEvent, useRef, useState } from 'react'
import { assessmentApi } from '../api/assessmentApi.js'
import { draftKey, readDraft } from '../../../shared/api/coreApi.js'

const unavailable = 'This career quiz is temporarily unavailable. Please select another quiz.'
const saveFailure = 'Your career quiz result could not be saved. Please try again.'
const storageFailure = 'Browser storage is unavailable. Keep this page open and save your draft on the server.'
const normalize = value => value.toLowerCase().replaceAll('_', '-')

export function useVersionedAssessment(assessmentType, requestedAttemptId = null) {
  const [owner, setOwner] = useState(null)
  const [types, setTypes] = useState([]), [selected, setSelected] = useState('')
  const [history, setHistory] = useState([]), [historyError, setHistoryError] = useState('')
  const [attempt, setAttempt] = useState(null), [answers, setAnswers] = useState({}), [result, setResult] = useState(null)
  const [error, setError] = useState(''), [notice, setNotice] = useState(''), [storageError, setStorageError] = useState('')
  const [loading, setLoading] = useState(true), [busy, setBusy] = useState(false), [conflict, setConflict] = useState(false)
  const inFlight = useRef(false), live = useRef(false), starts = useRef({})
  const pendingNavigation = useRef(null)
  const previousTarget = useRef(requestedAttemptId)

  const persist = useCallback((key, value) => {
    try { localStorage.setItem(key, JSON.stringify(value)); return true }
    catch { setStorageError(storageFailure); return false }
  }, [])
  const refreshHistory = useCallback(async () => {
    try { const rows = await assessmentApi.history(); if (live.current) { setHistory(rows); setHistoryError('') } }
    catch { if (live.current) setHistoryError('Your assessment history could not be loaded. Please try again.') }
  }, [])
  const initialize = useCallback(() =>
    Promise.all([assessmentApi.actor(), assessmentApi.catalog()]).then(async ([me, list]) => {
      if (!live.current) return
      setError('')
      setOwner(me.id); setTypes(list)
      const type = assessmentType ? list.find(row => normalize(row.code) === normalize(assessmentType)) : list[0]
      setSelected(type?.activeVersionId ?? '')
      await refreshHistory()
    }).catch(e => { if (live.current) setError(e.message) })
      .finally(() => { if (live.current) setLoading(false) }), [assessmentType, refreshHistory])
  useEffect(() => { live.current = true; initialize(); return () => { live.current = false } }, [initialize])

  // The URL selects an existing owner-authorized attempt; it never grants access.
  const syncView = useEffectEvent(id => {
    const action = () => id ? load(id) : selection()
    // Browser history may change while a read is pending. Apply its latest target
    // after that request finishes rather than dropping the navigation.
    if (inFlight.current) { pendingNavigation.current = action; return }
    if (id && id !== attempt?.id) run(action)
    else if (!id && attempt) selection()
  })
  useEffect(() => {
    if (owner === null) return
    const changed = previousTarget.current !== requestedAttemptId
    previousTarget.current = requestedAttemptId
    if (requestedAttemptId || changed) syncView(requestedAttemptId)
  }, [owner, requestedAttemptId])

  async function run(action) {
    if (inFlight.current) return
    inFlight.current = true; setBusy(true); setError('')
    try { return await action() }
    catch (e) { if (live.current) setError(e.message) }
    finally {
      inFlight.current = false
      if (live.current) {
        setBusy(false)
        const navigation = pendingNavigation.current
        pendingNavigation.current = null
        if (navigation) run(navigation)
      }
    }
  }
  async function load(id) {
    const a = await assessmentApi.attempt(id)
    if (!live.current) return
    const server = Object.fromEntries(a.responses.map(r => [r.questionId, r.answerData.value]))
    const local = readDraft(draftKey(owner, 'assessment', id))
    const hasLocal = a.status === 'IN_PROGRESS' && local?.answers && typeof local.answers === 'object' && !Array.isArray(local.answers)
    const questionIds = new Set(a.questions.map(q => q.id))
    const localAnswers = hasLocal ? Object.fromEntries(Object.entries(local.answers).filter(([id]) => questionIds.has(id))) : {}
    setAttempt(a); setResult(null); setNotice('')
    setAnswers(hasLocal ? { ...server, ...localAnswers } : server)
    setConflict(Boolean(hasLocal && local.revision !== a.revision))
    if (a.status === 'SUBMITTED') {
      const stored = await assessmentApi.result(id)
      if (live.current) setResult(stored)
    }
  }
  function start() {
    return run(async () => {
      const key = draftKey(owner, 'assessment-start', selected)
      const pendingId = starts.current[selected] ?? readDraft(key)?.requestId
      const requestId = typeof pendingId === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(pendingId)
        ? pendingId : crypto.randomUUID()
      starts.current[selected] = requestId
      persist(key, { requestId })
      try { const a = await assessmentApi.start(selected, requestId); await load(a.id); await refreshHistory(); return a.id }
      catch (e) {
        if (e.status === 404) { setAttempt(null); setSelected(''); setError(unavailable) }
        else throw e
      }
    })
  }
  function update(next) {
    setAnswers(next); persist(draftKey(owner, 'assessment', attempt.id), { revision: attempt.revision, answers: next })
    setNotice('Local draft. Save to keep it on the server.')
  }
  function save(final) {
    if (conflict || attempt?.status !== 'IN_PROGRESS') return
    return run(async () => {
      const items = Object.entries(answers).map(([questionId, value]) => ({ questionId, answerData: { schemaVersion: 1, value } }))
      try {
        const saved = await assessmentApi.save(attempt, items, final)
        if (!live.current) return
        if (final) { setResult(saved); setAttempt({ ...attempt, status: 'SUBMITTED' }) }
        else { setAttempt(saved); persist(draftKey(owner, 'assessment', attempt.id), { revision: saved.revision, answers }) }
        setNotice('Saved on the server.'); await refreshHistory()
      } catch (e) {
        if (e.status === 404) { setAttempt(null); setResult(null); setSelected(''); setError(unavailable) }
        else {
          setError(e.status === 409 ? 'Your draft is preserved. Reload the attempt before retrying a conflict.' : final ? saveFailure : e.message)
          if (e.status === 409) setConflict(true)
        }
      }
    })
  }
  function resolveConflict(keepLocal) {
    return run(async () => {
      const fresh = await assessmentApi.attempt(attempt.id)
      if (fresh.status !== 'IN_PROGRESS') { await load(fresh.id); return }
      const next = keepLocal ? answers : Object.fromEntries(fresh.responses.map(r => [r.questionId, r.answerData.value]))
      setAttempt(fresh); setAnswers(next); setConflict(false); setNotice('Local draft. Save to keep it on the server.')
      persist(draftKey(owner, 'assessment', fresh.id), { revision: fresh.revision, answers: next })
    })
  }
  function selection(retake = false) {
    if (retake && attempt) {
      const typeCode = result?.assessmentTypeCode ?? history.find(row => row.id === attempt.id)?.assessmentTypeCode
      const version = types.find(row => row.code === typeCode)?.activeVersionId ?? ''
      setSelected(version)
      if (version) {
        starts.current[version] = crypto.randomUUID()
        persist(draftKey(owner, 'assessment-start', version), { requestId: starts.current[version] })
      }
    }
    setAttempt(null); setResult(null); setAnswers({}); setError(''); setNotice(''); setConflict(false)
  }
  return { types, selected, setSelected, attempt, answers, result, history, historyError, error, notice, storageError,
    ready: owner !== null, loading, busy, conflict, initialize: () => { setLoading(true); setError(''); return initialize() }, refreshHistory, start, update, save, resolveConflict, selection,
    open: id => run(() => load(id)) }
}
