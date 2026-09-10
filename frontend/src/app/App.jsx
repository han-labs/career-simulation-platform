import { Route, Routes } from 'react-router-dom'
import AppShell from '../shared/layout/AppShell.jsx'
import AssessmentPage from '../features/assessment/pages/AssessmentPage.jsx'
import GuidancePlaceholderPage from '../features/guidance/pages/GuidancePlaceholderPage.jsx'
import LandingPage from '../features/landing/pages/LandingPage.jsx'
import SimulationPlaceholderPage from '../features/simulations/pages/SimulationPlaceholderPage.jsx'

function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<LandingPage />} />
        <Route path="assessment" element={<AssessmentPage />} />
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
