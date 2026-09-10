// Verifies the deterministic Phase 1 evaluator independently from the React UI.
import { describe, expect, it } from 'vitest'
import { backendDeveloperSimulation } from '../data/backendDeveloperSimulation.js'
import { evaluateAttempt } from './simulationEvaluator.js'

describe('evaluateAttempt', () => {
  it('returns task outcomes and an objective total from selected options', () => {
    const result = evaluateAttempt(backendDeveloperSimulation.tasks, {
      'api-response': 'b',
      'sql-query': 'd',
      'backend-error': 'b',
    })

    expect(result).toMatchObject({ correctCount: 2, totalTasks: 3, percentage: 67 })
    expect(result.outcomes.map((outcome) => outcome.isCorrect)).toEqual([true, false, true])
  })
})
