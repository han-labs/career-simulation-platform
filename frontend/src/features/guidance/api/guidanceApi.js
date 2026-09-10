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
const fallbackStatuses = new Set([0, 401, 403, 404, 500, 502, 503, 504])

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

function shouldUseStandardMode(error) {
  return error instanceof ApiError && fallbackStatuses.has(error.status)
}

export async function getExplorationDashboard() {
  try {
    return normalizeDashboard(await apiRequest('/v1/guidance/dashboard'))
  } catch (error) {
    if (!shouldUseStandardMode(error)) throw error
    return normalizeDashboard(previewDashboard)
  }
}

function standardReply(actionType, message, dashboard) {
  const latestResult = dashboard.recentResults[0]
  const topSignal = dashboard.interestSignals[0]
  const skillNeedingEvidence = dashboard.skills.find(
    (skill) => skill.status === 'NEEDS_MORE_EVIDENCE',
  )

  const replies = {
    EXPLAIN_RIASEC: {
      text: topSignal
        ? `Your strongest current signal is ${topSignal.label}. It is a useful starting point for choosing experiences to test. Treat it as a direction to explore, not a career verdict.`
        : 'There is not enough assessment evidence yet. Completing the RIASEC assessment would give us a clearer starting point.',
    },
    REVIEW_LATEST: {
      text: latestResult
        ? 'Here is the objective evidence from your latest simulation. The score and task outcomes come from deterministic evaluation, not from Syn.'
        : 'There is no completed simulation to review yet. Try a short simulation, then return here to reflect on the result.',
      resultCard: latestResult ?? null,
    },
    NEEDS_EVIDENCE: {
      text: skillNeedingEvidence
        ? `${skillNeedingEvidence.name} currently needs more evidence. One result is not a final judgement; a second focused task would help you compare your experience.`
        : 'Your current evidence does not flag a specific skill gap. Another focused simulation can still help you compare experiences.',
      resultCard: latestResult ?? null,
      suggestions: ['Try another SQL-focused task', 'Review the task evidence', 'Record what felt uncertain'],
    },
    EXPLORE_NEXT: {
      text: 'A useful next step is to gather one more piece of evidence rather than choose a direction immediately. You could try a debugging task, compare it with frontend work, or revisit your reflection notes.',
      suggestions: ['Backend debugging simulation', 'Frontend accessibility review', 'Compare two completed results'],
    },
    DRAFT_PLAN: {
      text: 'I drafted a small exploration plan from the evidence currently available. Review it before deciding whether to save it.',
      planDraft: {
        title: 'Build one more piece of evidence',
        steps: [
          'Complete one debugging-focused simulation.',
          'Note which task felt engaging and which felt difficult.',
          'Compare the new evidence with your Investigative interest signal.',
        ],
      },
    },
  }

  if (replies[actionType]) return replies[actionType]

  const lowerMessage = message.toLowerCase()
  if (lowerMessage.includes('plan')) return replies.DRAFT_PLAN
  if (lowerMessage.includes('result') || lowerMessage.includes('score')) return replies.REVIEW_LATEST
  if (lowerMessage.includes('riasec') || lowerMessage.includes('interest')) return replies.EXPLAIN_RIASEC

  return {
    text: 'Syn live analysis is not connected yet, but Standard guidance is available. Try asking about your RIASEC signals, latest result, missing evidence, or a small exploration plan.',
    suggestions: ['Explain my RIASEC signals', 'Review my latest result', 'Draft an exploration plan'],
  }
}

export function createStandardReply(actionType, message, dashboard = previewDashboard) {
  return {
    id: `standard-${Date.now()}`,
    role: 'assistant',
    provenance: 'STANDARD',
    ...standardReply(actionType, message, normalizeDashboard(dashboard)),
  }
}

export async function sendSynMessage({ message, actionType = 'FREE_TEXT', context, dashboard }) {
  try {
    const data = await apiRequest('/v1/guidance/syn/messages', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, actionType, context }),
    })
    if (typeof data?.text !== 'string' || !data.text.trim()) {
      return createStandardReply(actionType, message, dashboard)
    }
    return {
      id: data.id ?? `live-${Date.now()}`,
      role: 'assistant',
      text: data.text,
      provenance: data.provenance === 'AI' ? 'AI' : 'STANDARD',
      resultCard: data.resultCard ?? null,
      suggestions: Array.isArray(data.suggestions) ? data.suggestions : [],
      planDraft: data.planDraft ?? null,
    }
  } catch (error) {
    if (!shouldUseStandardMode(error)) throw error
    return createStandardReply(actionType, message, dashboard)
  }
}

export async function saveExplorationPlan(planDraft) {
  try {
    return await apiRequest('/v1/guidance/plans', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(planDraft),
    })
  } catch (error) {
    if (!shouldUseStandardMode(error)) throw error
    return { ...planDraft, saved: false, preview: true }
  }
}
