import { Route, Routes, useLocation } from 'react-router-dom'
import { useEffect, useSyncExternalStore } from 'react'
import { getAccountGeneration, subscribeAccount } from '../shared/api/httpClient.js'
import AppShell from '../shared/layout/AppShell.jsx'
import AssessmentPage from '../features/assessment/pages/AssessmentPage.jsx'
import SynWidget from '../features/guidance/components/SynWidget.jsx'
import GuidancePage from '../features/guidance/pages/GuidancePage.jsx'
import LandingPage from '../features/landing/pages/LandingPage.jsx'
import SimulationAttemptPage from '../features/simulations/pages/SimulationAttemptPage.jsx'
import SimulationCatalogPage from '../features/simulations/pages/SimulationCatalogPage.jsx'
import SimulationDetailPage from '../features/simulations/pages/SimulationDetailPage.jsx'
import { coreEnabled } from '../shared/api/coreApi.js'
import AccountPage from '../features/identity/AccountPage.jsx'
import VersionedAssessmentPage from '../features/assessment/pages/VersionedAssessmentPage.jsx'
import CoreSimulationPage from '../features/simulations/pages/CoreSimulationPage.jsx'
import CoreDashboardPage from '../features/guidance/pages/CoreDashboardPage.jsx'
import CoreCatalogPage from '../features/simulations/pages/CoreCatalogPage.jsx'
import EnterpriseWorkspace from '../features/enterprise/EnterpriseWorkspace.jsx'
import AssessmentAdministration from '../features/admin/AssessmentAdministration.jsx'

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
  const generation = useSyncExternalStore(subscribeAccount, getAccountGeneration)
  return (
    <>
      <ScrollToTop />
      <AppShell key={generation} />
      <SynWidget />
    </>
  )
}

function App() {
  return (
    <Routes>
      <Route element={<ApplicationLayout />}>
        <Route index element={<LandingPage />} />
        <Route path="account" element={<AccountPage />} />
        <Route path="assessment" element={coreEnabled ? <VersionedAssessmentPage /> : <AssessmentPage />} />
        <Route path="assessment/:assessmentType" element={coreEnabled ? <VersionedAssessmentPage /> : <AssessmentPage />} />
        <Route path="simulations" element={coreEnabled ? <CoreCatalogPage /> : <SimulationCatalogPage />} />
        {coreEnabled && <Route path="enterprise" element={<EnterpriseWorkspace />} />}
        {coreEnabled && <Route path="admin/assessments" element={<AssessmentAdministration />} />}
        <Route path="simulations/:slug" element={coreEnabled ? <CoreSimulationPage /> : <SimulationDetailPage />} />
        <Route
          path="simulations/:slug/attempt"
          element={coreEnabled ? <CoreSimulationPage /> : <SimulationAttemptPage />}
        />
        <Route path="dashboard" element={coreEnabled ? <CoreDashboardPage /> : <GuidancePage />} />
        <Route path="guidance" element={<GuidancePage />} />
      </Route>
    </Routes>
  )
}

export default App
