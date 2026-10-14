import { useState, useEffect, useCallback } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'

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

  const setAllAnswers = useCallback((newAnswers) => {
    setState(prev => ({
      ...prev,
      answers: { ...prev.answers, ...newAnswers }
    }))
  }, [])

  const fetchQuestions = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      setQuestions(await apiRequest('/v1/assessments/questions'))
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
      const data = await apiRequest('/v1/assessments/attempts', {
        method: 'POST',
      })
      setState(prev => ({ ...prev, status: 'in_progress', attemptId: data.id }))
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

      const data = await apiRequest(`/v1/assessments/attempts/${state.attemptId}/submit`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ answers: answersPayload })
      })

      setState(prev => ({ ...prev, status: 'completed', result: data }))
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
    setAllAnswers,
    fetchQuestions,
    start,
    submit,
    reset
  }
}
