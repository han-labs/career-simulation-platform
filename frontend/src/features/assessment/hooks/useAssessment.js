import { useState, useEffect, useCallback } from 'react'

const STORAGE_KEY = 'careersim_assessment_state_v2'

export function useAssessment() {
  const getInitialState = () => {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      try {
        const parsed = JSON.parse(saved)
        if (parsed.status === 'in_progress' && !parsed.attemptId) {
          console.warn('Old state version detected, resetting...')
          return { answers: {}, status: 'intro', result: null, attemptId: null }
        }
        return parsed
      } catch (e) {
        console.error('Failed to parse assessment state', e)
      }
    }
    return { answers: {}, status: 'intro', result: null, attemptId: null }
  }

  const [state, setState] = useState(getInitialState)
  const [questions, setQuestions] = useState([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)

  // Save to local storage whenever state changes
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  }, [state])

  const setAnswer = useCallback((questionId, value) => {
    setState(prev => ({
      ...prev,
      answers: { ...prev.answers, [questionId]: value }
    }))
  }, [])

  const fetchQuestions = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const res = await fetch('/api/v1/assessments/questions')
      if (!res.ok) throw new Error('Failed to load questions')
      const json = await res.json()
      setQuestions(json.data)
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const start = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const res = await fetch('/api/v1/assessments/attempts', {
        method: 'POST',
        headers: {
          'X-Student-Id': '1' // Backend mock requirement
        }
      })
      if (!res.ok) throw new Error('Failed to start assessment attempt')
      const json = await res.json()
      setState(prev => ({ ...prev, status: 'in_progress', attemptId: json.data.id }))
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const submit = useCallback(async () => {
    if (!state.attemptId) {
      setError('No active attempt found.')
      return
    }
    
    setIsLoading(true)
    setError(null)
    try {
      const answersPayload = Object.entries(state.answers).map(([qId, score]) => ({
        questionId: Number(qId),
        score: score
      }))

      const res = await fetch(`/api/v1/assessments/attempts/${state.attemptId}/submit`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Student-Id': '1'
        },
        body: JSON.stringify({ answers: answersPayload })
      })

      if (!res.ok) throw new Error('Failed to submit assessment')
      const json = await res.json()
      
      setState(prev => ({ ...prev, status: 'completed', result: json.data }))
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }, [state.answers, state.attemptId])

  const reset = useCallback(() => {
    setState({ answers: {}, status: 'intro', result: null, attemptId: null })
  }, [])

  return {
    ...state,
    questions,
    isLoading,
    error,
    setAnswer,
    fetchQuestions,
    start,
    submit,
    reset
  }
}
