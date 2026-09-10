import { Route, Routes } from 'react-router-dom'
import AppShell from '../shared/layout/AppShell.jsx'
import AssessmentPage from '../features/assessment/pages/AssessmentPage.jsx'
import GuidancePage from '../features/guidance/pages/GuidancePage.jsx'
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
        <Route path="dashboard" element={<GuidancePage />} />
        <Route path="guidance" element={<GuidancePage />} />
      </Route>
    </Routes>
  )
}

export default App
