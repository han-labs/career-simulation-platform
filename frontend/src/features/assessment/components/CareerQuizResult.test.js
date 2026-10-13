import React from 'react'
import { describe, expect, it, vi } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import CareerQuizResult from './CareerQuizResult.jsx'

describe('career quiz result presentation', () => {
  it('renders backend Interests scores and explains their scale and limitations', () => {
    const html = renderToStaticMarkup(React.createElement(CareerQuizResult, {
      quizType: 'INTERESTS',
      result: { quizType: 'INTERESTS', scores: [{ category: 'ANALYTICAL', score: 12 }, { category: 'CREATIVE', score: 10 }, { category: 'COLLABORATIVE', score: 8 }, { category: 'ORGANIZING', score: 7 }], topCategories: ['ANALYTICAL'] },
      onRetake: vi.fn(),
    }))
    expect(html).toContain('12 / 15')
    expect(html).toContain('Analytical')
    expect(html).toContain('sum of three responses on a 1–5 scale')
    expect(html).toContain('not a validated aptitude test')
  })

  it('renders Abilities as confidence reflection and protects against invalid result shapes', () => {
    const html = renderToStaticMarkup(React.createElement(CareerQuizResult, {
      quizType: 'ABILITIES',
      result: { quizType: 'ABILITIES', scores: [{ category: 'COMMUNICATION', score: 12 }], topCategories: ['COMMUNICATION'] },
      onRetake: vi.fn(),
    }))
    expect(html).toContain('Your results are unavailable')
    expect(html).not.toContain('12 / 15')
  })

  it('renders backend Work Activities preferences across Information, People, and Things', () => {
    const html = renderToStaticMarkup(React.createElement(CareerQuizResult, {
      quizType: 'WORK_ACTIVITIES',
      result: {
        quizType: 'WORK_ACTIVITIES',
        scores: [
          { category: 'INFORMATION', score: 12 },
          { category: 'PEOPLE', score: 12 },
          { category: 'THINGS', score: 8 },
        ],
        topCategories: ['INFORMATION', 'PEOPLE'],
      },
      onRetake: vi.fn(),
    }))
    expect(html).toContain('Your work activity preference signals')
    expect(html).toContain('Information')
    expect(html).toContain('People')
    expect(html).toContain('Things')
    expect(html.match(/Highest signal/g)).toHaveLength(2)
    expect(html).toContain('12 / 15')
    expect(html).toContain('not a validated aptitude test')
  })
})
