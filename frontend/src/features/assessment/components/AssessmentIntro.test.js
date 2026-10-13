import React from 'react'
import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import AssessmentIntro from './AssessmentIntro.jsx'

function renderIntro(overrides = {}) {
  return renderToStaticMarkup(
    React.createElement(MemoryRouter, null,
      React.createElement(AssessmentIntro, { status: 'intro', ...overrides }),
    ),
  )
}

describe('Assessment selection', () => {
  it('uses a general heading and presents exactly four active assessment choices', () => {
    const html = renderIntro()

    expect(html).toContain('Career exploration assessments')
    expect(html).toContain('Explore your interests, strengths, and career directions through assessments that fit your goals.')
    expect(html).toContain('RIASEC Test')
    expect(html).toContain('Interests Quiz')
    expect(html).toContain('Abilities Quiz')
    expect(html).toContain('Work Activities Test')
    expect(html).toContain('Information · People · Things')
    expect(html).toContain('9 questions')
    expect(html).toContain('Start Test')
    expect(html.match(/assessment-choice-card--available/g)).toHaveLength(4)
    expect(html).toContain('42 questions')
    expect(html).toContain('Rate how much you like each activity on a 1–5 scale')
    expect(html).toContain('href="/assessment/riasec"')
    expect(html).toContain('href="/assessment/interests"')
    expect(html).toContain('href="/assessment/abilities"')
    expect(html).toContain('href="/assessment/work-activities"')
    expect(html.match(/aria-label="Quick demo:/g)).toHaveLength(4)
    expect(html).toContain('href="/assessment/riasec?demo=backend"')
    expect(html).toContain('href="/assessment/interests?demo=creative"')
    expect(html).toContain('href="/assessment/abilities?demo=problem-solving"')
    expect(html).toContain('href="/assessment/work-activities?demo=information"')
  })

  it('explains how original Interests and Abilities questions are inspired by Job Bank themes', () => {
    const html = renderIntro()

    expect(html).toContain('questions are original to CareerSim')
    expect(html).toContain('not an aptitude test')
    expect(html).not.toContain('Planned')
    expect(html).not.toContain('disabled=""')
  })

  it('offers resume and loading states for the existing assessment', () => {
    expect(renderIntro({ status: 'in_progress' })).toContain('Resume assessment')
  })
})
