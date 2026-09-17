// Loads the simulation catalog from the API with a local mock fallback.
import { ArrowLeft } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import SimulationCatalog from '../components/SimulationCatalog.jsx'
import { backendDeveloperSimulation } from '../data/backendDeveloperSimulation.js'
import { fetchSimulations } from '../api/simulationApi.js'

async function loadCatalog(setState) {
  try {
    const simulations = await fetchSimulations()
    setState({ status: 'ready', simulations })
  } catch (error) {
    console.warn('Simulation catalog API unavailable; using mock data.', error)
    setState({ status: 'ready', simulations: [backendDeveloperSimulation] })
  }
}

function SimulationCatalogPage() {
  const [state, setState] = useState({ status: 'loading', simulations: [] })

  useEffect(() => {
    loadCatalog(setState)
  }, [])

  const retry = () => {
    setState({ status: 'loading', simulations: [] })
    loadCatalog(setState)
  }

  return (
    <main className="catalog-page us03-page section-shell">
      <Link className="back-link" to="/">
        <ArrowLeft size={17} aria-hidden="true" /> Back to overview
      </Link>
      <header className="dashboard-heading">
        <div>
          <p className="eyebrow">Explore by doing</p>
          <h1>Career simulations</h1>
          <p>Explore IT career paths through representative tasks</p>
        </div>
      </header>
      <SimulationCatalog
        status={state.status}
        simulations={state.simulations}
        onRetry={retry}
      />
    </main>
  )
}

export default SimulationCatalogPage
