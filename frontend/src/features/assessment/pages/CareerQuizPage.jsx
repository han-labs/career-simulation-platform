import { ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'
import AssessmentForm from '../components/AssessmentForm.jsx'
import CareerQuizResult from '../components/CareerQuizResult.jsx'
import { useCareerQuiz } from '../hooks/useCareerQuiz.js'
import { useSearchParams } from 'react-router-dom'
import { useEffect, useRef } from 'react'
import { createDemoAnswers } from '../data/demoAnswers.js'
import { quizDemoProfiles } from '../data/demoProfiles.js'

const interestOptions = [
  { value: 1, label: 'Not interested' }, { value: 2, label: 'Slightly interested' },
  { value: 3, label: 'Moderately interested' }, { value: 4, label: 'Very interested' },
  { value: 5, label: 'Extremely interested' },
]
const abilityOptions = [
  { value: 1, label: 'Not confident yet' }, { value: 2, label: 'A little confident' },
  { value: 3, label: 'Somewhat confident' }, { value: 4, label: 'Very confident' },
  { value: 5, label: 'Highly confident' },
]
const workActivityOptions = [
  { value: 1, label: 'Strongly prefer to avoid' }, { value: 2, label: 'Prefer to avoid' },
  { value: 3, label: 'No strong preference' }, { value: 4, label: 'Prefer this' },
  { value: 5, label: 'Strongly prefer this' },
]

function CareerQuizPage({ quizType }) {
  const [searchParams] = useSearchParams()
  const demoProfile = searchParams.get('demo')
  const quiz = useCareerQuiz(quizType, demoProfile)
  const { start: startQuiz, setAllAnswers } = quiz
  const demoHandled = useRef(false)
  const demoProfiles = quizDemoProfiles[quizType]
  const isInterests = quizType === 'INTERESTS'
  const isAbilities = quizType === 'ABILITIES'
  const isWorkActivities = quizType === 'WORK_ACTIVITIES'
  const title = isInterests ? 'Interests Quiz' : isAbilities ? 'Abilities Quiz' : 'Work Activities Test'
  const questionCount = isWorkActivities ? 9 : 12
  const description = isInterests
    ? 'Rate how much you would enjoy each activity.'
    : isAbilities
      ? 'Rate how confident you feel doing each activity today.'
      : 'Rate how much you would prefer each kind of work activity.'
  const intro = isInterests
    ? 'Explore the work activities you find engaging across analytical, creative, collaborative, and organizing tasks.'
    : isAbilities
      ? 'Reflect on your current confidence across problem solving, communication, organization, and technical learning.'
      : 'Explore whether you prefer working with information, people, or practical things and tasks.'
  const handleRetake = async () => { quiz.reset(); await quiz.start() }

  useEffect(() => {
    if (!demoProfile || quiz.isLoading || quiz.questions.length !== questionCount || demoHandled.current) return
    const profile = demoProfiles.find((option) => option.id === demoProfile)
    if (!profile) return
    demoHandled.current = true
    startQuiz().then((started) => {
      if (!started) return
      setAllAnswers(createDemoAnswers(quiz.questions, demoProfile, demoProfiles))
    })
  }, [demoProfile, quiz.isLoading, quiz.questions, demoProfiles, startQuiz, setAllAnswers, questionCount])

  return (
    <section className="feature-page section-shell" style={{ backgroundColor: 'var(--page-bg)', minHeight: '100vh', paddingBottom: '48px' }}>
      <Link className="back-link" to="/assessment" style={{ color: 'var(--ink)' }}><ArrowLeft size={17} aria-hidden="true" /> All career tests</Link>
      {quiz.error && <div role="alert" className="assessment-api-error"><strong>Something went wrong: </strong>{quiz.error}</div>}
      {quiz.status === 'completed' ? (
        <div style={{ maxWidth: '960px', margin: '16px auto 0' }}>
          {quiz.result ? <CareerQuizResult quizType={quizType} result={quiz.result} onRetake={handleRetake} /> : quiz.error ? <div role="alert"><p>Your saved result could not be loaded.</p><button className="button button--primary" type="button" onClick={handleRetake}>Retake quiz</button></div> : <p role="status">Loading your saved result…</p>}
        </div>
      ) : quiz.status === 'in_progress' ? (
        <div style={{ maxWidth: '800px', margin: '16px auto 0' }}>
          {demoProfile && <p className="assessment-demo-notice" role="status">Quick demo answers are prefilled. Review or change them before submitting for backend-scored results.</p>}
          <h1>{title}</h1>
          <p>{description} These self-reflection questions are original to CareerSim.</p>
          <AssessmentForm questions={quiz.questions} answers={quiz.answers} setAnswer={quiz.setAnswer} setAllAnswers={quiz.setAllAnswers} onSubmit={quiz.submit} isLoading={quiz.isLoading} scaleOptions={isInterests ? interestOptions : isAbilities ? abilityOptions : workActivityOptions} demoProfiles={demoProfiles} />
        </div>
      ) : (
        <div className="assessment-discovery">
          <span className="eyebrow">CareerSim · Self-exploration</span>
          <h1>{title}</h1>
          <p>{intro}</p>
          <p className="assessment-discovery__note">{questionCount} questions · About {isWorkActivities ? '3' : '5'} minutes · Results are exploratory and are not a validated measurement.</p>
          <button className="button button--primary" type="button" onClick={quiz.start} disabled={quiz.isLoading || quiz.questions.length !== questionCount}>
            {quiz.isLoading ? 'Loading…' : quiz.questions.length !== questionCount ? 'Questions unavailable' : isWorkActivities ? 'Start Test' : 'Start quiz'}
          </button>
        </div>
      )}
    </section>
  )
}

export default CareerQuizPage
