// Keeps legacy answers locally until the API confirms a persisted result.
import { useState } from 'react'
import { submitAttempt } from '../api/simulationApi.js'

export function useSimulationAttempt(tasks, simulationSlug) {
  const [answers, setAnswers] = useState({})
  const [result, setResult] = useState(null)
  const [submitAttempted, setSubmitAttempted] = useState(false)
  const [currentTaskIndex, setCurrentTaskIndex] = useState(0)
  const [submitStatus, setSubmitStatus] = useState('idle')
  const [submitError, setSubmitError] = useState(null)

  const setAnswer = (taskId, optionId) => {
    if (result) return
    setAnswers((current) => ({ ...current, [taskId]: optionId }))
  }

  const missingTaskIds = tasks
    .filter((task) => !answers[task.id])
    .map((task) => task.id)

  const submit = async () => {
    setSubmitAttempted(true)
    if (missingTaskIds.length > 0 || result) {
      if (missingTaskIds.length > 0) {
        setCurrentTaskIndex(tasks.findIndex((task) => task.id === missingTaskIds[0]))
      }
      return false
    }

    setSubmitStatus('submitting')
    setSubmitError(null)
    try {
      const apiResult = await submitAttempt(simulationSlug, answers)
      setResult(apiResult)
      setSubmitStatus('idle')
      return true
    } catch (error) {
      setSubmitStatus('error')
      setSubmitError(error.message)
      return false
    }
  }

  const restart = () => {
    setAnswers({})
    setResult(null)
    setSubmitAttempted(false)
    setCurrentTaskIndex(0)
  }

  const goPrevious = () => setCurrentTaskIndex((index) => Math.max(0, index - 1))
  const goNext = () => setCurrentTaskIndex((index) => Math.min(tasks.length - 1, index + 1))

  return {
    answers,
    result,
    submitAttempted,
    missingTaskIds,
    currentTaskIndex,
    submitStatus,
    submitError,
    setAnswer,
    submit,
    restart,
    goPrevious,
    goNext,
  }
}
