import { afterEach, describe, expect, it, vi } from 'vitest'
import {
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

  it('grounds missing-evidence guidance in the supplied dashboard', () => {
    const reply = createStandardReply('NEEDS_EVIDENCE', '', {
      ...previewDashboard,
      skills: [{ name: 'Accessibility review', status: 'NEEDS_MORE_EVIDENCE' }],
    })

    expect(reply.text).toContain('Accessibility review')
  })

  it('normalizes backend provenance and structured response fields', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({
        success: true,
        data: {
          id: 'message-1',
          text: 'Evidence-grounded response.',
          provenance: 'AI',
          suggestions: ['Review evidence'],
        },
      }),
    }))

    const reply = await sendSynMessage({
      message: 'What did I practise?',
      context: { attemptId: 'attempt-1' },
      dashboard: previewDashboard,
    })

    expect(reply).toMatchObject({
      id: 'message-1',
      provenance: 'AI',
      suggestions: ['Review evidence'],
    })
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
})
