import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useEffect, useSyncExternalStore } from 'react'
import { getAccountGeneration, subscribeAccount } from '../shared/api/httpClient.js'
import AppShell from '../shared/layout/AppShell.jsx'
import AssessmentPage from '../features/assessment/pages/AssessmentPage.jsx'
import GuidancePage from '../features/guidance/pages/GuidancePage.jsx'
import LandingPage from '../features/landing/pages/LandingPage.jsx'
import SimulationAttemptPage from '../features/simulations/pages/SimulationAttemptPage.jsx'
import SimulationCatalogPage from '../features/simulations/pages/SimulationCatalogPage.jsx'
import SimulationDetailPage from '../features/simulations/pages/SimulationDetailPage.jsx'
import { coreEnabled } from '../shared/api/coreApi.js'
import VersionedAssessmentPage from '../features/assessment/pages/VersionedAssessmentPage.jsx'
import CoreSimulationPage from '../features/simulations/pages/CoreSimulationPage.jsx'
import CoreDashboardPage from '../features/guidance/pages/CoreDashboardPage.jsx'
import CoreCatalogPage from '../features/simulations/pages/CoreCatalogPage.jsx'
import EnterpriseWorkspace from '../features/enterprise/EnterpriseWorkspace.jsx'
import { agentPlatformEnabled } from '../features/admin/agentPlatform/platformApi.js'
import KnowledgeStudioPage from '../features/admin/knowledge/KnowledgeStudioPage.jsx'
import AgentBuilderPage from '../features/admin/agents/AgentBuilderPage.jsx'
import AssignedAgentsPage from '../features/agents/AssignedAgentsPage.jsx'
import StudentAgentPage from '../features/agents/StudentAgentPage.jsx'
import AgentUsagePage from '../features/admin/agentPlatform/AgentUsagePage.jsx'
import { useI18n } from '../shared/i18n/useI18n.js'
import AdminStudioLayout from '../features/admin/agentPlatform/AdminStudioLayout.jsx'

function LegacyUsageRedirect() {
  const { search, hash } = useLocation()
  return <Navigate to={{ pathname: '/admin/usage', search, hash }} replace />
}

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
    </>
  )
}

function NotFoundPage() {
  const { t } = useI18n()
  return <section className="section-shell core-flow"><h1>{t('Page not found')}</h1><Link to="/admin">{t('Open Admin Studio')}</Link></section>
}

function App() {
  return (
    <Routes>
      <Route element={<ApplicationLayout />}>
        <Route index element={<LandingPage />} />
        {agentPlatformEnabled && <Route path="admin" element={<AdminStudioLayout />}>
          <Route index element={<Navigate to="/admin/knowledge" replace />} />
          <Route path="knowledge" element={<KnowledgeStudioPage />} />
          <Route path="agents" element={<AgentBuilderPage />} />
          <Route path="usage" element={<AgentUsagePage />} />
          <Route path="ai-usage" element={<LegacyUsageRedirect />} />
        </Route>}
        {agentPlatformEnabled && <Route path="agents" element={<AssignedAgentsPage />} />}
        {agentPlatformEnabled && <Route path="agents/:agentId" element={<StudentAgentPage />} />}
        <Route path="assessment" element={coreEnabled ? <VersionedAssessmentPage /> : <AssessmentPage />} />
        <Route path="assessment/:assessmentType" element={coreEnabled ? <VersionedAssessmentPage /> : <AssessmentPage />} />
        <Route path="simulations" element={coreEnabled ? <CoreCatalogPage /> : <SimulationCatalogPage />} />
        {coreEnabled && <Route path="enterprise" element={<EnterpriseWorkspace />} />}
        <Route path="simulations/:slug" element={coreEnabled ? <CoreSimulationPage /> : <SimulationDetailPage />} />
        <Route
          path="simulations/:slug/attempt"
          element={coreEnabled ? <CoreSimulationPage /> : <SimulationAttemptPage />}
        />
        <Route path="dashboard" element={coreEnabled ? <CoreDashboardPage /> : <GuidancePage />} />
        <Route path="guidance" element={<GuidancePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
