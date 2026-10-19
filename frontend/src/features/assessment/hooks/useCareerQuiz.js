import { useEffect } from 'react'
import { useAssessment } from './useAssessment.js'

export function useCareerQuiz(quizType, demoProfile = null) {
  const quiz = useAssessment({ quizType, storageKey: `careersim_${quizType.toLowerCase()}_${demoProfile ? `demo_${demoProfile}` : 'quiz'}_v1` })
  const { fetchQuestions } = quiz
  useEffect(() => { fetchQuestions() }, [fetchQuestions])
  return quiz
}
