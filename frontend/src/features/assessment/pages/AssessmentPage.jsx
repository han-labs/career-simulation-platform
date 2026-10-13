import { useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useAssessment } from '../hooks/useAssessment.js'
import AssessmentIntro from '../components/AssessmentIntro.jsx'
import RiasecAssessmentFlow from './RiasecAssessmentFlow.jsx'
import CareerQuizPage from './CareerQuizPage.jsx'

function AssessmentSelectionPage() {
  const { status } = useAssessment()
  return (
    <section className="feature-page section-shell" style={{ backgroundColor: 'var(--page-bg)', minHeight: '100vh', paddingBottom: '48px' }}>
      <AssessmentIntro status={status} />
    </section>
  )
}

function AssessmentPage() {
  const { assessmentType } = useParams()

  useEffect(() => { window.scrollTo({ top: 0, left: 0, behavior: 'instant' }) }, [assessmentType])

  if (!assessmentType) return <AssessmentSelectionPage />
  if (assessmentType === 'riasec') return <RiasecAssessmentFlow />
  if (assessmentType === 'interests' || assessmentType === 'abilities' || assessmentType === 'work-activities') {
    const quizType = assessmentType === 'work-activities' ? 'WORK_ACTIVITIES' : assessmentType.toUpperCase()
    return <CareerQuizPage key={assessmentType} quizType={quizType} />
  }
  return <AssessmentSelectionPage />
}

export default AssessmentPage
