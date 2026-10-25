import { useEffect, useState } from 'react'
import { getPublishedSimulations } from '../../simulations/api/simulationCatalogApi.js'
import { useAssessment } from '../../assessment/hooks/useAssessment.js'
import HeroSection from '../components/HeroSection.jsx'
import CompaniesSection from '../components/CompaniesSection.jsx'
import JourneySection from '../components/JourneySection.jsx'
import CatalogSection from '../components/CatalogSection.jsx'
import CtaSection from '../components/CtaSection.jsx'

function LandingPage() {
  const [simulations, setSimulations] = useState([])
  const [catalogStatus, setCatalogStatus] = useState('loading')
  const { status, result } = useAssessment()
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true

    getPublishedSimulations()
      .then((data) => {
        if (active) {
          setSimulations(data)
        }
        if (active) setCatalogStatus('ready')
      })
      .catch(() => {
        if (active) setCatalogStatus('error')
      })

    return () => {
      active = false
    }
  }, [reload])

  return (
    <div className="landing-page-container hcmute-home">
      <HeroSection status={status} result={result} />
      <CompaniesSection simulations={catalogStatus === 'ready' ? simulations : []} />
      <CatalogSection simulations={simulations} catalogStatus={catalogStatus} onRetry={() => { setCatalogStatus('loading'); setReload(value => value + 1) }} />
      <JourneySection />
      <CtaSection />
    </div>
  )
}

export default LandingPage
