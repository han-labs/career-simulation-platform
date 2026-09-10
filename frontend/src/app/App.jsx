import { Route, Routes } from 'react-router-dom'
import AppShell from '../shared/layout/AppShell.jsx'
import AssessmentPlaceholderPage from '../features/assessment/pages/AssessmentPlaceholderPage.jsx'
import GuidancePlaceholderPage from '../features/guidance/pages/GuidancePlaceholderPage.jsx'
import LandingPage from '../features/landing/pages/LandingPage.jsx'
import SimulationPlaceholderPage from '../features/simulations/pages/SimulationPlaceholderPage.jsx'

function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<LandingPage />} />
        <Route path="assessment" element={<AssessmentPlaceholderPage />} />
        <Route
          path="simulations/:slug?"
          element={<SimulationPlaceholderPage />}
        />
        <Route path="guidance" element={<GuidancePlaceholderPage />} />
      </Route>
    </Routes>
  )
}

export default App
