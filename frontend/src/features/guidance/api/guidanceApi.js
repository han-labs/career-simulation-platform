import { ApiError, apiRequest } from '../../../shared/api/httpClient.js'

export const previewDashboard = {
  source: 'PREVIEW',
  studentName: 'Student',
  progress: {
    completed: 2,
    total: 3,
    label: 'Two exploration steps completed',
  },
  interestSignals: [
    { code: 'I', label: 'Investigative', score: 18 },
    { code: 'C', label: 'Conventional', score: 15 },
    { code: 'R', label: 'Realistic', score: 13 },
  ],
  skills: [
    { name: 'API response analysis', status: 'OBSERVED_STRENGTH', evidence: '2 completed tasks' },
    { name: 'Backend error diagnosis', status: 'PRACTISED', evidence: '1 completed task' },
    { name: 'SQL querying', status: 'NEEDS_MORE_EVIDENCE', evidence: '1 incomplete outcome' },
  ],
  recentResults: [
    {
      id: 'preview-attempt-1',
      title: 'Backend API Triage',
      score: 80,
      completedLabel: 'Latest simulation',
      completedAt: null,
      outcomes: [
        { label: 'API response analysis', status: 'OBSERVED_STRENGTH' },
        { label: 'SQL querying', status: 'NEEDS_MORE_EVIDENCE' },
        { label: 'Backend error diagnosis', status: 'PRACTISED' },
      ],
    },
  ],
  plan: {
    title: 'Explore backend problem solving',
    nextStep: 'Try one more debugging-focused simulation and reflect on the evidence.',
    status: 'DRAFT',
  },
}

// The preview remains usable before US-03 endpoints or authentication are wired.
const fallbackStatuses = new Set([0, 500, 502, 503, 504])

export function normalizeDashboard(data = {}) {
  return {
    source: data.source ?? 'LIVE',
    studentName: data.studentName ?? 'Student',
    progress: data.progress ?? { completed: 0, total: 3, label: 'Start your exploration' },
    interestSignals: Array.isArray(data.interestSignals) ? data.interestSignals : [],
    skills: Array.isArray(data.skills) ? data.skills : [],
    recentResults: Array.isArray(data.recentResults) ? data.recentResults : [],
    plan: data.plan ?? null,
  }
}


function shouldUseStandardMode(error) { return error instanceof ApiError && fallbackStatuses.has(error.status) }

export async function getExplorationDashboard() {
  try {
    return normalizeDashboard(await apiRequest('/v1/guidance/dashboard'))
  } catch (error) {
    if (import.meta.env.VITE_ENABLE_GUIDANCE_PREVIEW !== 'true' || !shouldUseStandardMode(error)) throw error
    return normalizeDashboard(previewDashboard)
  }
}
