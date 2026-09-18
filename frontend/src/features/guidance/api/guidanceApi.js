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
const fallbackStatuses = new Set([0, 401, 403, 404, 429, 500, 502, 503, 504])

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

function fallbackLanguage(responseLanguage, message = '') {
  if (responseLanguage === 'VI' || responseLanguage === 'EN') return responseLanguage
  const normalized = message.toLocaleLowerCase('vi')
  if (/[ăâđêôơưà-ỹ]/i.test(normalized)) return 'VI'
  if (/\b(ko|khong|mình|minh|em|ntn|so sanh|bat dau|tai lieu|nghe)\b/i.test(normalized)) {
    return 'VI'
  }
  return 'EN'
}

export async function getExplorationDashboard() {
  try {
    return normalizeDashboard(await apiRequest('/v1/guidance/dashboard'))
  } catch (error) {
    if (!shouldUseStandardMode(error)) throw error
    return normalizeDashboard(previewDashboard)
  }
}

function standardReply(actionType, message, dashboard, responseLanguage = 'EN') {
  const vi = responseLanguage === 'VI'
  const latestResult = dashboard.recentResults[0]
  const topSignal = dashboard.interestSignals[0]
  const skillNeedingEvidence = dashboard.skills.find(
    (skill) => skill.status === 'NEEDS_MORE_EVIDENCE',
  )

  const replies = {
    GET_STARTED: {
      text: vi
        ? 'Bạn chưa cần biết ngay mình hợp nghề nào. Hãy bắt đầu bằng bài RIASEC ngắn hoặc một mô phỏng công việc IT để có trải nghiệm đầu tiên; không lựa chọn nào quyết định nghề nghiệp thay bạn.'
        : 'You do not need to know your best-fit career yet. Start with a short RIASEC assessment or one IT work simulation to gather a first experience; neither option decides your career.',
      suggestions: vi
        ? ['Bắt đầu bài RIASEC', 'Khám phá một mô phỏng ngắn']
        : ['Start the RIASEC assessment', 'Explore a short simulation'],
    },
    EXPLAIN_RIASEC: {
      text: topSignal
        ? vi
          ? `Tín hiệu nổi bật hiện tại của bạn là ${topSignal.label}. Hãy dùng nó để chọn trải nghiệm muốn thử, không phải như một kết luận nghề nghiệp.`
          : `Your strongest current signal is ${topSignal.label}. It is a useful starting point for choosing experiences to test. Treat it as a direction to explore, not a career verdict.`
        : vi
          ? 'Chưa có đủ dữ liệu đánh giá. Hoàn thành bài RIASEC sẽ tạo một điểm bắt đầu rõ hơn.'
          : 'There is not enough assessment evidence yet. Completing the RIASEC assessment would give us a clearer starting point.',
    },
    REVIEW_LATEST: {
      text: latestResult
        ? vi
          ? 'Đây là bằng chứng khách quan từ mô phỏng gần nhất. Điểm và kết quả nhiệm vụ được chấm theo quy tắc xác định, không phải do Syn tạo ra.'
          : 'Here is the objective evidence from your latest simulation. The score and task outcomes come from deterministic evaluation, not from Syn.'
        : vi
          ? 'Bạn chưa có mô phỏng hoàn thành để xem lại. Hãy thử một mô phỏng ngắn rồi quay lại nhìn lại kết quả.'
          : 'There is no completed simulation to review yet. Try a short simulation, then return here to reflect on the result.',
      resultCard: latestResult ?? null,
    },
    NEEDS_EVIDENCE: {
      text: skillNeedingEvidence
        ? vi
          ? `${skillNeedingEvidence.name} hiện cần thêm bằng chứng. Một kết quả chưa phải đánh giá cuối cùng; một nhiệm vụ có trọng tâm khác sẽ giúp bạn so sánh trải nghiệm.`
          : `${skillNeedingEvidence.name} currently needs more evidence. One result is not a final judgement; a second focused task would help you compare your experience.`
        : vi
          ? 'Dữ liệu hiện tại chưa chỉ ra khoảng trống kỹ năng cụ thể. Một mô phỏng khác vẫn có thể giúp bạn so sánh trải nghiệm.'
          : 'Your current evidence does not flag a specific skill gap. Another focused simulation can still help you compare experiences.',
      resultCard: latestResult ?? null,
      suggestions: vi
        ? ['Thử thêm nhiệm vụ có trọng tâm', 'Xem lại bằng chứng', 'Ghi lại điều còn chưa chắc']
        : ['Try another SQL-focused task', 'Review the task evidence', 'Record what felt uncertain'],
    },
    EXPLORE_NEXT: {
      text: vi
        ? 'Bước tiếp theo hữu ích là thu thập thêm một trải nghiệm thay vì chọn hướng ngay. Bạn có thể thử nhiệm vụ khác hoặc ghi lại điều khiến mình hứng thú và thấy khó.'
        : 'A useful next step is to gather one more piece of evidence rather than choose a direction immediately. You could try a debugging task, compare it with frontend work, or revisit your reflection notes.',
      suggestions: vi
        ? ['Thử mô phỏng backend', 'Thử accessibility frontend', 'So sánh hai kết quả']
        : ['Backend debugging simulation', 'Frontend accessibility review', 'Compare two completed results'],
    },
    DRAFT_PLAN: {
      text: vi
        ? 'Syn đã phác thảo một kế hoạch khám phá nhỏ từ dữ liệu hiện có. Hãy xem lại trước khi quyết định lưu.'
        : 'I drafted a small exploration plan from the evidence currently available. Review it before deciding whether to save it.',
      planDraft: {
        title: vi ? 'Thu thập thêm một bằng chứng' : 'Build one more piece of evidence',
        steps: vi
          ? [
            'Hoàn thành một mô phỏng có trọng tâm.',
            'Ghi lại nhiệm vụ khiến bạn hứng thú và nhiệm vụ gây khó khăn.',
            'So sánh trải nghiệm mới với tín hiệu sở thích hiện tại.',
          ]
          : [
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
    text: vi
      ? 'Phân tích trực tiếp của Syn chưa kết nối, nhưng Standard guidance vẫn hoạt động. Bạn có thể hỏi về RIASEC, kết quả gần nhất, phần cần thêm bằng chứng hoặc kế hoạch khám phá nhỏ.'
      : 'Syn live analysis is not connected yet, but Standard guidance is available. Try asking about your RIASEC signals, latest result, missing evidence, or a small exploration plan.',
    suggestions: vi
      ? ['Giải thích RIASEC', 'Xem kết quả gần nhất', 'Phác thảo kế hoạch']
      : ['Explain my RIASEC signals', 'Review my latest result', 'Draft an exploration plan'],
  }
}

export function createStandardReply(
  actionType,
  message,
  dashboard = previewDashboard,
  responseLanguage = 'EN',
) {
  return {
    id: `standard-${Date.now()}`,
    role: 'assistant',
    provenance: 'STANDARD',
    responseLanguage,
    ...standardReply(actionType, message, normalizeDashboard(dashboard), responseLanguage),
  }
}

export async function sendSynMessage({
  message,
  actionType = 'FREE_TEXT',
  sessionId,
  responseLanguage = 'AUTO',
  context,
  dashboard,
}) {
  try {
    const data = await apiRequest('/v1/guidance/syn/messages', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, actionType, sessionId, responseLanguage, context }),
    })
    if (typeof data?.text !== 'string' || !data.text.trim()) {
      return createStandardReply(
        actionType,
        message,
        dashboard,
        fallbackLanguage(responseLanguage, message),
      )
    }
    return {
      id: data.id ?? `live-${Date.now()}`,
      sessionId: data.sessionId ?? sessionId ?? null,
      role: 'assistant',
      text: data.text,
      provenance: data.provenance === 'AI' ? 'AI' : 'STANDARD',
      intent: data.intent ?? actionType,
      responseLanguage: data.responseLanguage ?? fallbackLanguage(responseLanguage, message),
      explorationStage: data.explorationStage ?? null,
      activity: Array.isArray(data.activity) ? data.activity : [],
      evidenceReferences: Array.isArray(data.evidenceReferences) ? data.evidenceReferences : [],
      comparisonCard: data.comparisonCard ?? null,
      resourceCards: Array.isArray(data.resourceCards) ? data.resourceCards : [],
      resultCard: data.resultCard ?? null,
      suggestions: Array.isArray(data.suggestions) ? data.suggestions : [],
      planDraft: data.planDraft ?? null,
    }
  } catch (error) {
    if (!shouldUseStandardMode(error)) throw error
    return createStandardReply(
      actionType,
      message,
      dashboard,
      fallbackLanguage(responseLanguage, message),
    )
  }
}

export async function clearSynSession(sessionId) {
  if (!sessionId) return false
  try {
    return await apiRequest(`/v1/guidance/syn/sessions/${sessionId}`, { method: 'DELETE' })
  } catch (error) {
    if (!shouldUseStandardMode(error)) throw error
    return false
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
