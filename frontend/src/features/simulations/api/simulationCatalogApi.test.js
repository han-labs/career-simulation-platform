import { describe, expect, it } from 'vitest'
import { normalizeSimulation } from './simulationCatalogApi.js'

describe('normalizeSimulation', () => {
  it('normalizes the API duration as a number', () => {
    const simulation = normalizeSimulation({
      id: 1,
      slug: 'backend-api-triage',
      title: 'Backend API Triage',
      careerTrack: 'BACKEND_DEVELOPMENT',
      summary: 'Practice backend decisions.',
      difficulty: 'INTRODUCTORY',
      estimatedMinutes: '35',
    })

    expect(simulation.estimatedMinutes).toBe(35)
    expect(simulation.slug).toBe('backend-api-triage')
  })
})
