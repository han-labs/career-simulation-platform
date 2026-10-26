import { ArrowRight, BriefcaseBusiness, ChartNoAxesColumnIncreasing, Trophy, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useI18n } from '../../../shared/i18n/useI18n.js'

const journey = [
  { icon: UserRound, title: 'Get to know yourself', copy: 'Take career quizzes to discover your interests, strengths, and values.', to: '/assessment', actionLabel: 'Take Assessment' },
  { icon: BriefcaseBusiness, title: 'Explore the career simulation', copy: 'Experience real-world tasks in various career fields.', to: '/simulations', actionLabel: 'Browse Roles' },
  { icon: ChartNoAxesColumnIncreasing, title: 'Analyze your results & Plan for education/training', copy: 'Receive detailed feedback and personalized learning suggestions.', to: '/dashboard', actionLabel: 'Open Dashboard' },
  { icon: Trophy, title: 'Stand out to employers', copy: 'Build your skills and showcase your potential through practical experience.' },
]

function JourneySection() {
  const { t } = useI18n()
  return <section className="home-journey section-shell" id="how-it-works" aria-labelledby="journey-heading">
    <h2 id="journey-heading">{t('How It Works')}</h2>
    <p className="home-journey__intro">{t('Our platform brings you closer to employers through real-world experiences and self-discovery assessments, helping you gradually identify career paths that suit you. Throughout this journey, an AI assistant is available to help you understand your strengths and plan your future development.')}</p>
    <ol className="home-journey__steps">{journey.map(({ icon: Icon, title, copy, to, actionLabel }, index) => <li key={title}>
      <span className="home-journey__icon"><Icon size={28} aria-hidden="true" /></span>
      <h3>{t(title)}</h3><p>{t(copy)}</p>
      {to && <Link className="text-link" to={to}>{t(actionLabel)} <ArrowRight size={14} aria-hidden="true" /></Link>}
      {index < 3 && <ArrowRight className="home-journey__arrow" size={26} aria-hidden="true" />}
    </li>)}</ol>
  </section>
}
export default JourneySection
