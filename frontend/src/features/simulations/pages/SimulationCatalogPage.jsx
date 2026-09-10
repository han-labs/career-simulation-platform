// Owns the simple local loading lifecycle for the Phase 2 mock catalog.
import { useEffect, useState } from 'react'
import SimulationCatalog from '../components/SimulationCatalog.jsx'
import { backendDeveloperSimulation } from '../data/backendDeveloperSimulation.js'

function SimulationCatalogPage() {
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    const timer = window.setTimeout(() => setStatus('ready'), 250)
    return () => window.clearTimeout(timer)
  }, [status])

  const retry = () => setStatus('loading')

  return (
    <main className="catalog-page section-shell">
      <header className="catalog-page__intro">
        <p className="eyebrow">Explore by doing</p>
        <h1>Career simulations</h1>
        <p>Explore IT career paths through representative tasks</p>
      </header>
      <SimulationCatalog status={status} simulations={[backendDeveloperSimulation]} onRetry={retry} />
    </main>
  )
}

export default SimulationCatalogPage
