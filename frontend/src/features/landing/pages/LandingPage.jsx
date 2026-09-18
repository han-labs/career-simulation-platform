import { useEffect, useState } from 'react'
import { getPublishedSimulations } from '../../simulations/api/simulationCatalogApi.js'
import { useAssessment } from '../../assessment/hooks/useAssessment.js'
import HeroSection from '../components/HeroSection.jsx'
import JourneySection from '../components/JourneySection.jsx'
import CatalogSection from '../components/CatalogSection.jsx'

const fallbackSimulations = [
  {
    id: 'fallback-1',
    slug: 'backend-api-triage',
    title: 'Backend API Triage',
    careerTrack: 'BACKEND_DEVELOPMENT',
    summary:
      'Inspect an API response, choose a safe query, and identify the likely source of a server error.',
    difficulty: 'INTRODUCTORY',
    estimatedMinutes: 35,
  },
  {
    id: 'fallback-2',
    slug: 'frontend-accessibility-review',
    title: 'Frontend Accessibility Review',
    careerTrack: 'FRONTEND_DEVELOPMENT',
    summary:
      'Make evidence-based decisions about semantics, keyboard use, and interface feedback states.',
    difficulty: 'INTRODUCTORY',
    estimatedMinutes: 30,
  },
  {
    id: 'fallback-3',
    slug: 'data-quality-investigation',
    title: 'Data Quality Investigation',
    careerTrack: 'DATA_ANALYSIS',
    summary:
      'Find inconsistencies in a small dataset and select reproducible cleaning and validation steps.',
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
    <>
      <HeroSection status={status} result={result} />
      <JourneySection />
      <CatalogSection simulations={simulations} catalogStatus={catalogStatus} />
    </>
  )
}

export default LandingPage
