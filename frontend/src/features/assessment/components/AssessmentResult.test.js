import React from 'react'
import { describe, expect, it, vi } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import AssessmentResult from './AssessmentResult.jsx'

const result = {
  attemptId: 12,
  scores: [
    { dimension: 'C', score: 20 },
    { dimension: 'A', score: 28 },
    { dimension: 'R', score: 16 },
    { dimension: 'E', score: 21 },
    { dimension: 'I', score: 28 },
    { dimension: 'S', score: 18 },
  ],
  topDimensions: ['A', 'I', 'E'],
}

function renderResult(resultData = result) {
  return renderToStaticMarkup(
    React.createElement(
      MemoryRouter,
      null,
      React.createElement(AssessmentResult, { result: resultData, onRestart: vi.fn() }),
    ),
  )
}

describe('RIASEC results', () => {
  it('displays dimension scores in stable RIASEC order and treats ties as shared signals', () => {
    const html = renderResult()
    const labels = ['Realistic', 'Investigative', 'Artistic', 'Social', 'Enterprising', 'Conventional']
    const positions = labels.map((label) => html.indexOf(`<span>${label}</span>`))

    expect(positions.every((position) => position >= 0)).toBe(true)
    expect(positions).toEqual([...positions].sort((left, right) => left - right))
    expect(html.match(/assessment-result__badge/g)).toHaveLength(2)
    expect(html).toContain('Your strongest interest signals')
    expect(html).toContain('28 / 35')
    expect(html).toContain('Explore IT simulations')
  })

  it('explains that results are exploratory and do not diagnose or decide a career', () => {
    const html = renderResult()

    expect(html).toContain('not a psychological diagnosis')
    expect(html).toContain('do not determine whether a career is right for you or predict your future')
    expect(html).toContain('Try an IT simulation')
  })

  it('shows a safe empty state if the API result lacks all six dimensions', () => {
    const html = renderResult({ attemptId: 12, scores: [], topDimensions: [] })

    expect(html).toContain('Your results are unavailable')
    expect(html).toContain('take the assessment again')
    expect(html).not.toContain('NaN')
  })
})
