// Loads the simulation catalog from the API with a local mock fallback.
import { useEffect, useState } from 'react'
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
    <main className="catalog-page section-shell">
      <header className="catalog-page__intro">
        <p className="eyebrow">Explore by doing</p>
        <h1>Career simulations</h1>
        <p>Explore IT career paths through representative tasks</p>
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
