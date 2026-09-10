// Resolves a local mock simulation by route slug and renders its public detail view.
import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import SimulationDetail from '../components/SimulationDetail.jsx'
import { backendDeveloperSimulation } from '../data/backendDeveloperSimulation.js'

function SimulationDetailPage() {
  const { slug } = useParams()
  const simulation = slug === backendDeveloperSimulation.id ? backendDeveloperSimulation : null

  if (!simulation) {
    return <main className="catalog-page section-shell">
      <section className="catalog-state"><h1>Simulation not found</h1><Link className="button" to="/simulations"><ArrowLeft size={17} aria-hidden="true" /> Back to catalog</Link></section>
    </main>
  }

  return <main className="detail-page section-shell"><SimulationDetail simulation={simulation} /></main>
}

export default SimulationDetailPage
