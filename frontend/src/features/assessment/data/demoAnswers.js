export function createDemoAnswers(questions, profile, profiles) {
  if (profiles) {
    const preferredIndex = profiles.find((option) => option.id === profile)?.preferredIndex ?? 0
    const groupSize = questions.length / profiles.length
    return Object.fromEntries(questions.map((question, index) => [
      question.id,
      Math.floor(index / groupSize) === preferredIndex ? 5 : 2,
    ]))
  }

  return Object.fromEntries(questions.map((question, index) => {
    let answer
    if (profile === 'backend') {
      answer = (index % 6 === 1 || index % 6 === 5) ? 5 : ((index % 6 === 0) ? 4 : 2)
    } else if (profile === 'frontend') {
      answer = (index % 6 === 2 || index % 6 === 3) ? 5 : ((index % 6 === 4) ? 4 : 2)
    } else {
      answer = (index % 6 === 4 || index % 6 === 5) ? 5 : ((index % 6 === 2) ? 4 : 2)
    }
    return [question.id, answer]
  }))
}
