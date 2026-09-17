import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  clearSynSession,
  createStandardReply,
  getExplorationDashboard,
  normalizeDashboard,
  previewDashboard,
  sendSynMessage,
} from './guidanceApi.js'

describe('US-03 guidance adapter', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('normalizes partial dashboard data into safe empty collections', () => {
    expect(normalizeDashboard({ studentName: 'Mai' })).toMatchObject({
      source: 'LIVE',
      studentName: 'Mai',
      interestSignals: [],
      skills: [],
      recentResults: [],
      plan: null,
    })
  })

  it('uses explicit preview data before the protected dashboard is connected', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: false, code: 'NOT_FOUND' }),
    }))

    const dashboard = await getExplorationDashboard()

    expect(dashboard.source).toBe('PREVIEW')
    expect(dashboard.recentResults[0].score).toBe(80)
  })

  it('keeps objective scores unchanged in Standard guidance', () => {
    const reply = createStandardReply('REVIEW_LATEST', '', previewDashboard)

    expect(reply.provenance).toBe('STANDARD')
    expect(reply.resultCard.score).toBe(previewDashboard.recentResults[0].score)
  })

  it('provides Vietnamese Standard onboarding without inventing personal evidence', () => {
    const emptyDashboard = {
      ...previewDashboard,
      interestSignals: [],
      skills: [],
      recentResults: [],
      plan: null,
    }

    const reply = createStandardReply('GET_STARTED', '', emptyDashboard, 'VI')

    expect(reply.responseLanguage).toBe('VI')
    expect(reply.text).toContain('chưa cần')
    expect(reply.resultCard).toBeUndefined()
  })

  it('grounds missing-evidence guidance in the supplied dashboard', () => {
    const reply = createStandardReply('NEEDS_EVIDENCE', '', {
      ...previewDashboard,
      skills: [{ name: 'Accessibility review', status: 'NEEDS_MORE_EVIDENCE' }],
    })

    expect(reply.text).toContain('Accessibility review')
  })

  it('normalizes backend provenance and structured response fields', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({
        success: true,
        data: {
          id: 'message-1',
          sessionId: '7f2d4023-8506-44f5-a685-e7afdb74d43c',
          intent: 'COMPARE_PATHS',
          responseLanguage: 'VI',
          explorationStage: 'PRACTISING',
          text: 'Evidence-grounded response.',
          provenance: 'AI',
          activity: ['Reviewed available evidence'],
          evidenceReferences: [{ label: 'Backend result', sourceType: 'SIMULATION' }],
          comparisonCard: { title: 'Compare', paths: [] },
          resourceCards: [],
          suggestions: ['Review evidence'],
        },
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    const reply = await sendSynMessage({
      message: 'What did I practise?',
      responseLanguage: 'VI',
      context: { attemptId: 'attempt-1' },
      dashboard: previewDashboard,
    })

    expect(reply).toMatchObject({
      id: 'message-1',
      sessionId: '7f2d4023-8506-44f5-a685-e7afdb74d43c',
      provenance: 'AI',
      intent: 'COMPARE_PATHS',
      responseLanguage: 'VI',
      explorationStage: 'PRACTISING',
      activity: ['Reviewed available evidence'],
      suggestions: ['Review evidence'],
    })
    expect(JSON.parse(fetchMock.mock.calls[0][1].body).responseLanguage).toBe('VI')
  })

  it('maps a malformed provider response to Standard guidance', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true, data: { provenance: 'AI' } }),
    }))

    const reply = await sendSynMessage({
      message: 'Review my latest result',
      actionType: 'REVIEW_LATEST',
      dashboard: previewDashboard,
    })

    expect(reply.provenance).toBe('STANDARD')
    expect(reply.resultCard.score).toBe(80)
  })

  it('keeps quick actions available when the provider cannot be reached', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('timeout')))

    const reply = await sendSynMessage({
      message: 'Draft an exploration plan',
      actionType: 'DRAFT_PLAN',
      dashboard: previewDashboard,
    })

    expect(reply.provenance).toBe('STANDARD')
    expect(reply.planDraft.steps).toHaveLength(3)
  })

  it('clears server-owned session memory through its bounded endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true, data: true }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(clearSynSession('7f2d4023-8506-44f5-a685-e7afdb74d43c')).resolves.toBe(true)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/v1/guidance/syn/sessions/7f2d4023-8506-44f5-a685-e7afdb74d43c'),
      expect.objectContaining({ method: 'DELETE' }),
    )
  })
})
