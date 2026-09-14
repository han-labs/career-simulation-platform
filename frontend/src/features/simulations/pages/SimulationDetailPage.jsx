// Loads public simulation details from the API with a local mock fallback.
import { ArrowLeft, LoaderCircle, TriangleAlert } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import SimulationDetail from '../components/SimulationDetail.jsx'
import { backendDeveloperSimulation } from '../data/backendDeveloperSimulation.js'
import { fetchSimulationDetail } from '../api/simulationApi.js'

function SimulationDetailPage() {
  const { slug } = useParams()
  const [state, setState] = useState({ status: 'loading', simulation: null })

  useEffect(() => {
    let active = true
    fetchSimulationDetail(slug)
      .then((simulation) => {
        if (active) setState({ status: 'ready', simulation })
      })
      .catch((error) => {
        console.warn('Simulation detail API unavailable; using mock data.', error)
        if (!active) return
        const fallback = slug === backendDeveloperSimulation.id
          ? backendDeveloperSimulation
          : null
        setState({ status: 'ready', simulation: fallback })
      })
    return () => {
      active = false
    }
  }, [slug])

  if (state.status === 'loading') {
    return <main className="catalog-page section-shell"><section className="catalog-state"><LoaderCircle className="spin" aria-hidden="true" /><p>Loading simulation...</p></section></main>
  }

  if (!state.simulation) {
    return <main className="catalog-page section-shell">
      <section className="catalog-state" role="alert"><TriangleAlert size={28} aria-hidden="true" /><h1>Simulation not found</h1><Link className="button" to="/simulations"><ArrowLeft size={17} aria-hidden="true" /> Back to catalog</Link></section>
    </main>
  }

  return <main className="detail-page section-shell"><SimulationDetail simulation={state.simulation} /></main>
}

export default SimulationDetailPage
