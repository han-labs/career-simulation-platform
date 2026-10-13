import { describe, expect, it } from 'vitest'
import { createDemoAnswers } from './demoAnswers.js'
import { quizDemoProfiles } from './demoProfiles.js'

describe('assessment quick demo answers', () => {
  it('fills all quiz questions with a profile-specific pattern', () => {
    for (const [quizType, profiles] of Object.entries(quizDemoProfiles)) {
      const questions = Array.from({ length: quizType === 'WORK_ACTIVITIES' ? 9 : 12 }, (_, index) => ({ id: index + 1 }))
      const profile = profiles[1]
      const answers = createDemoAnswers(questions, profile.id, profiles)

      expect(Object.keys(answers)).toHaveLength(questions.length)
      expect(questions.slice(profile.preferredIndex * 3, profile.preferredIndex * 3 + 3).every(({ id }) => answers[id] === 5)).toBe(true)
      expect(questions.filter(({ id }) => answers[id] === 2)).toHaveLength(questions.length - 3)
    }
  })

  it('preserves the existing RIASEC demo profiles', () => {
    const questions = Array.from({ length: 42 }, (_, index) => ({ id: index + 1 }))
    const answers = createDemoAnswers(questions, 'backend')

    expect(Object.keys(answers)).toHaveLength(42)
    expect(answers[2]).toBe(5)
    expect(answers[1]).toBe(4)
  })
})
