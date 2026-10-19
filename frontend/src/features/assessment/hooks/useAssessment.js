import { useState, useEffect, useCallback, useRef } from 'react'
import { apiRequest, getAccountGeneration } from '../../../shared/api/httpClient.js'

const empty = () => ({ answers: {}, status: 'intro', result: null, attemptId: null })

// Old unowned drafts remain untouched; only an authenticated owner's namespace is read.
export function useAssessment({ storageKey = 'careersim_assessment_state_v2', quizType = null } = {}) {
  const [state, setState] = useState(empty)
  const [owner, setOwner] = useState(null)
  const [questions, setQuestions] = useState([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)
  const submissionInFlight = useRef(false)
  const live = useRef(true)
  const generation = useRef(getAccountGeneration())
  const current = useCallback(() => live.current && generation.current === getAccountGeneration(), [])
  const base = quizType ? `/v1/assessments/quizzes/${quizType}` : '/v1/assessments'
  const attemptBase = quizType ? '/v1/assessments/quizzes' : '/v1/assessments'
  const key = owner ? `${storageKey}:owner:${owner}` : null
  useEffect(() => {
    live.current = true
    apiRequest('/v2/me').then(me => {
      if (!current()) return
      let saved = null
      try { saved = JSON.parse(localStorage.getItem(`${storageKey}:owner:${me.id}`)) } catch { /* Invalid local draft is not adopted. */ }
      setOwner(me.id)
      if (saved && ['intro', 'in_progress', 'completed'].includes(saved.status) && (saved.status === 'intro' || saved.attemptId)) setState({ ...empty(), ...saved })
    }).catch(e => { if (current()) setError(e.message) })
    return () => { live.current = false }
  }, [storageKey, current])
  useEffect(() => { if (key && current()) localStorage.setItem(key, JSON.stringify(state)) }, [key, state, current])
  useEffect(() => {
    if (!owner || state.status !== 'completed' || !state.attemptId) return
    let active = true
    apiRequest(`${attemptBase}/attempts/${state.attemptId}/result`).then(result => { if (active && current()) setState(prev => ({ ...prev, result })) }).catch(e => { if (active && current()) setError(e.message) })
    return () => { active = false }
  }, [owner, state.status, state.attemptId, attemptBase, current])
  const setAnswer = useCallback((id, value) => { if (owner && current()) setState(prev => ({ ...prev, answers: { ...prev.answers, [id]: value } })) }, [owner, current])
  const setAllAnswers = useCallback(answers => { if (owner && current()) setState(prev => ({ ...prev, answers: { ...prev.answers, ...answers } })) }, [owner, current])
  const fetchQuestions = useCallback(async () => {
    setIsLoading(true); setError(null)
    try { const data = await apiRequest(`${base}/questions`); if (current()) setQuestions(data) }
    catch (e) { if (current()) setError(e.message) } finally { if (current()) setIsLoading(false) }
  }, [base, current])
  const start = useCallback(async () => {
    if (!owner) return false
    if (state.status === 'in_progress' && state.attemptId) return true
    setIsLoading(true); setError(null)
    try {
      const data = await apiRequest(`${base}/attempts`, { method: 'POST' })
      if (!current()) return false
      setState(prev => ({ ...prev, status: 'in_progress', attemptId: data.id, result: null })); return true
    } catch (e) { if (current()) setError(e.message); return false }
    finally { if (current()) setIsLoading(false) }
  }, [owner, state.status, state.attemptId, base, current])
  const submit = useCallback(async () => {
    if (!owner || !state.attemptId || submissionInFlight.current) return
    submissionInFlight.current = true; setIsLoading(true); setError(null)
    try {
      const answers = Object.entries(state.answers).map(([questionId, score]) => ({ questionId: Number(questionId), score }))
      const result = await apiRequest(`${attemptBase}/attempts/${state.attemptId}/submit`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ answers }) })
      if (current()) setState(prev => ({ ...prev, status: 'completed', result }))
    } catch (e) { if (current()) setError(e.message) }
    finally { submissionInFlight.current = false; if (current()) setIsLoading(false) }
  }, [owner, state.attemptId, state.answers, attemptBase, current])
  const reset = useCallback(() => { setState(empty()); setError(null) }, [])
  return { ...state, questions, isLoading, error, start, setAnswer, setAllAnswers, submit, reset, fetchQuestions }
}
