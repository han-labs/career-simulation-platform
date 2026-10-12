import { useEffect, useState } from 'react'
import { getPublishedSimulations } from '../../simulations/api/simulationCatalogApi.js'
import { useAssessment } from '../../assessment/hooks/useAssessment.js'
import HeroSection from '../components/HeroSection.jsx'
import FeaturesSection from '../components/FeaturesSection.jsx'
import JourneySection from '../components/JourneySection.jsx'
import TracksSection from '../components/TracksSection.jsx'
import CatalogSection from '../components/CatalogSection.jsx'
import CtaSection from '../components/CtaSection.jsx'

const fallbackSimulations = [
  {
    id: 'fallback-1',
    slug: 'backend-api-triage',
    title: 'Backend API Triage: Incident Response',
    careerTrack: 'BACKEND_DEVELOPMENT',
    summary:
      'Inspect API responses, select safe SQL statements, and identify the root cause of an unexpected 500 server error.',
    difficulty: 'INTRODUCTORY',
    estimatedMinutes: 35,
  },
  {
    id: 'fallback-2',
    slug: 'frontend-accessibility-review',
    title: 'Frontend Accessibility: UI Audit',
    careerTrack: 'FRONTEND_DEVELOPMENT',
    summary:
      'Make design and code decisions on semantic HTML, keyboard navigation, and responsive accessibility.',
    difficulty: 'INTRODUCTORY',
    estimatedMinutes: 30,
  },
  {
    id: 'fallback-3',
    slug: 'data-quality-investigation',
    title: 'Data Quality Investigation: Anomaly Detection',
    careerTrack: 'DATA_ANALYSIS',
    summary:
      'Investigate discrepancies across student dataset records and identify proper data cleansing workflows.',
    difficulty: 'INTERMEDIATE',
    estimatedMinutes: 45,
  },
]

function LandingPage() {
  const [simulations, setSimulations] = useState(fallbackSimulations)
  const [catalogStatus, setCatalogStatus] = useState('loading')
  const { status, result } = useAssessment()

  useEffect(() => {
    let active = true

    getPublishedSimulations()
      .then((data) => {
        if (active && data.length > 0) {
          setSimulations(data.slice(0, 3))
        }
        if (active) setCatalogStatus('ready')
      })
      .catch(() => {
        if (active) setCatalogStatus('fallback')
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <div className="landing-page-container">
      <HeroSection status={status} result={result} />
      <FeaturesSection />
      <JourneySection />
      <TracksSection />
      <CatalogSection simulations={simulations} catalogStatus={catalogStatus} />
      <CtaSection />
    </div>
  )
}

export default LandingPage
