import { useCallback, useEffect, useState } from 'react'

const initialState = () => ({ status: 'intro', attemptId: null, answers: {}, result: null })

export function useCareerQuiz(quizType, demoProfile = null) {
  const storageKey = demoProfile
    ? `careersim_${quizType.toLowerCase()}_demo_${demoProfile}_v1`
    : `careersim_${quizType.toLowerCase()}_quiz_v1`
  const [state, setState] = useState(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(storageKey))
      if (!saved || !['intro', 'in_progress', 'completed'].includes(saved.status)) return initialState()
      if (saved.status !== 'intro' && !saved.attemptId) return initialState()
      return { ...initialState(), ...saved, answers: saved.answers || {} }
    } catch { return initialState() }
  })
  const [questions, setQuestions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => { localStorage.setItem(storageKey, JSON.stringify(state)) }, [state, storageKey])
  useEffect(() => {
    let active = true
    fetch(`/api/v1/assessments/quizzes/${quizType}/questions`)
      .then(async (response) => {
        if (!response.ok) throw new Error('Unable to load quiz questions.')
        const json = await response.json()
        if (active) setQuestions(json.data || [])
      })
      .catch((reason) => { if (active) setError(reason.message) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [quizType])

  useEffect(() => {
    if (state.status !== 'completed' || !state.attemptId) return
    let active = true
    fetch(`/api/v1/assessments/quizzes/attempts/${state.attemptId}/result`)
      .then(async (response) => {
        if (!response.ok) throw new Error('Unable to load the saved result.')
        const json = await response.json()
        if (active) setState((previous) => ({ ...previous, result: json.data }))
      })
      .catch((reason) => { if (active) setError(reason.message) })
    return () => { active = false }
  }, [state.attemptId, state.status])

  const start = useCallback(async () => {
    if (state.status === 'in_progress' && state.attemptId) return true
    setIsLoading(true); setError(null)
    try {
      const response = await fetch(`/api/v1/assessments/quizzes/${quizType}/attempts`, { method: 'POST', headers: { 'X-Student-Id': '1' } })
      if (!response.ok) throw new Error('Unable to start this quiz. Please try again.')
      const json = await response.json()
      setState((previous) => ({ ...previous, status: 'in_progress', attemptId: json.data.id, result: null }))
      return true
    } catch (reason) { setError(reason.message); return false }
    finally { setIsLoading(false) }
  }, [quizType, state.attemptId, state.status])

  const setAnswer = useCallback((questionId, value) => {
    setState((previous) => ({ ...previous, answers: { ...previous.answers, [questionId]: value } }))
  }, [])

  const setAllAnswers = useCallback((answers) => {
    setState((previous) => ({ ...previous, answers: { ...previous.answers, ...answers } }))
  }, [])

  const submit = useCallback(async () => {
    if (!state.attemptId || isLoading) return
    setIsLoading(true); setError(null)
    try {
      const answers = Object.entries(state.answers).map(([questionId, score]) => ({ questionId: Number(questionId), score }))
      const response = await fetch(`/api/v1/assessments/quizzes/attempts/${state.attemptId}/submit`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Student-Id': '1' }, body: JSON.stringify({ answers }),
      })
      if (!response.ok) throw new Error('Unable to submit your answers. Your responses are saved here so you can retry.')
      const json = await response.json()
      setState((previous) => ({ ...previous, status: 'completed', result: json.data }))
    } catch (reason) { setError(reason.message) }
    finally { setIsLoading(false) }
  }, [isLoading, state.answers, state.attemptId])

  const reset = useCallback(() => { setState(initialState()); setError(null) }, [])
  return { ...state, questions, isLoading, error, start, setAnswer, setAllAnswers, submit, reset }
}
