import { useState, useEffect } from 'react'

const STORAGE_KEY = 'careersim_assessment_state'

export function useAssessment() {
  const getInitialState = () => {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      try {
        return JSON.parse(saved)
      } catch (e) {
        console.error('Failed to parse assessment state', e)
      }
    }
    return { answers: {}, status: 'intro', result: null }
  }

  const [answers, setAnswers] = useState(() => getInitialState().answers)
  const [status, setStatus] = useState(() => getInitialState().status) // 'intro', 'in_progress', 'completed'
  const [result, setResult] = useState(() => getInitialState().result)

  // Save to local storage whenever state changes
  useEffect(() => {
    const state = { answers, status, result }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  }, [answers, status, result])

  const setAnswer = (questionId, value) => {
    setAnswers(prev => ({
      ...prev,
      [questionId]: value
    }))
  }

  const start = () => {
    setStatus('in_progress')
  }

  const submit = (calculatedResult) => {
    setResult(calculatedResult)
    setStatus('completed')
  }

  const reset = () => {
    setAnswers({})
    setStatus('intro')
    setResult(null)
  }

  return {
    answers,
    status,
    result,
    setAnswer,
    start,
    submit,
    reset
  }
}
