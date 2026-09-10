// Deterministic mock scorer for Phase 1 only; production scoring belongs on the backend.
export function evaluateAttempt(tasks, answers) {
  const outcomes = tasks.map((task) => {
    const selectedOption = answers[task.id] ?? null
    const isCorrect = selectedOption === task.correctAnswer

    return {
      taskId: task.id,
      title: task.title,
      selectedOption,
      isCorrect,
      explanation: task.explanation,
    }
  })

  const correctCount = outcomes.filter((outcome) => outcome.isCorrect).length

  return {
    correctCount,
    totalTasks: tasks.length,
    percentage: Math.round((correctCount / tasks.length) * 100),
    outcomes,
  }
}
