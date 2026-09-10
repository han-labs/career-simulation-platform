// Holds the Phase 1 attempt in memory; autosave and resume are intentionally deferred.
import { useState } from 'react'
import { evaluateAttempt } from '../utils/simulationEvaluator.js'

export function useSimulationAttempt(tasks) {
  const [answers, setAnswers] = useState({})
  const [result, setResult] = useState(null)
  const [submitAttempted, setSubmitAttempted] = useState(false)
  const [currentTaskIndex, setCurrentTaskIndex] = useState(0)

  const setAnswer = (taskId, optionId) => {
    if (result) return
    setAnswers((current) => ({ ...current, [taskId]: optionId }))
  }

  const missingTaskIds = tasks
    .filter((task) => !answers[task.id])
    .map((task) => task.id)

  const submit = () => {
    setSubmitAttempted(true)
    if (missingTaskIds.length > 0 || result) {
      if (missingTaskIds.length > 0) {
        setCurrentTaskIndex(tasks.findIndex((task) => task.id === missingTaskIds[0]))
      }
      return false
    }

    setResult(evaluateAttempt(tasks, answers))
    return true
  }

  const restart = () => {
    setAnswers({})
    setResult(null)
    setSubmitAttempted(false)
    setCurrentTaskIndex(0)
  }

  const goPrevious = () => setCurrentTaskIndex((index) => Math.max(0, index - 1))
  const goNext = () => setCurrentTaskIndex((index) => Math.min(tasks.length - 1, index + 1))

  return { answers, result, submitAttempted, missingTaskIds, currentTaskIndex, setAnswer, submit, restart, goPrevious, goNext }
}
