

export function scoreAssessment(answers, questions) {
  const scores = { R: 0, I: 0, A: 0, S: 0, E: 0, C: 0 }

  questions.forEach((q) => {
    const val = answers[q.id]
    if (val !== undefined) {
      scores[q.dimension] += val
    }
  })

  // Find top dimension
  let topDimension = 'R'
  let maxScore = -1

  for (const [dim, score] of Object.entries(scores)) {
    if (score > maxScore) {
      maxScore = score
      topDimension = dim
    }
  }

  return {
    scores,
    topDimension
  }
}
