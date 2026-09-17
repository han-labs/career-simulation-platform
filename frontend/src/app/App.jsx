import { Route, Routes, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import AppShell from '../shared/layout/AppShell.jsx'
import AssessmentPage from '../features/assessment/pages/AssessmentPage.jsx'
import SynWidget from '../features/guidance/components/SynWidget.jsx'
import GuidancePage from '../features/guidance/pages/GuidancePage.jsx'
import LandingPage from '../features/landing/pages/LandingPage.jsx'
import SimulationAttemptPage from '../features/simulations/pages/SimulationAttemptPage.jsx'
import SimulationCatalogPage from '../features/simulations/pages/SimulationCatalogPage.jsx'
import SimulationDetailPage from '../features/simulations/pages/SimulationDetailPage.jsx'

function ScrollToTop() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if (!hash) {
      setTimeout(() => {
        window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
      }, 0);
    }
  }, [pathname, hash]);

  return null;
}

function ApplicationLayout() {
  return (
    <>
      <ScrollToTop />
      <AppShell />
      <SynWidget />
    </>
  )
}

function App() {
  return (
    <Routes>
      <Route element={<ApplicationLayout />}>
        <Route index element={<LandingPage />} />
        <Route path="assessment" element={<AssessmentPage />} />
        <Route path="simulations" element={<SimulationCatalogPage />} />
        <Route path="simulations/:slug" element={<SimulationDetailPage />} />
        <Route
          path="simulations/:slug/attempt"
          element={<SimulationAttemptPage />}
        />
        <Route path="dashboard" element={<GuidancePage />} />
        <Route path="guidance" element={<GuidancePage />} />
      </Route>
    </Routes>
  )
}

export default App
