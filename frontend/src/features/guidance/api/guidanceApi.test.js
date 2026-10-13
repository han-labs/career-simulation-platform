import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  buildSynResponseMarkdown,
  clearSynSession,
  createStandardReply,
  getExplorationDashboard,
  normalizeDashboard,
  previewDashboard,
  sendSynMessage,
  saveExplorationPlan,
} from './guidanceApi.js'

describe('US-03 guidance adapter', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('does not turn a failed plan save into a preview success', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))
    await expect(saveExplorationPlan({ title: 'Draft', steps: ['Try SQL'] })).rejects.toBeTruthy()
  })

  it('requires an explicit saved flag from the server', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true, status: 200, headers: { get: () => 'application/json' },
      json: async () => ({ success: true, data: { saved: false } }),
    }))
    await expect(saveExplorationPlan({ title: 'Draft', steps: ['Try SQL'] })).rejects.toThrow('did not confirm')
  })

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

  it('does not disguise authorization failure as Preview', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: false, code: 'NOT_FOUND' }),
    }))

    await expect(getExplorationDashboard()).rejects.toMatchObject({ status: 403 })
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
          simulationCards: [{ title: 'Frontend Review', careerTrack: 'FRONTEND_DEVELOPMENT' }],
          resultCards: [{ id: 42, title: 'Backend API Triage', score: 80, outcomes: [] }],
          suggestions: ['Review evidence'],
        },
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    const reply = await sendSynMessage({
      message: 'What did I practise?',
      responseLanguage: 'VI',
      context: { attemptId: 'attempt-1' },
      dashboard: { ...previewDashboard, source: 'LIVE' },
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
      simulationCards: [{ title: 'Frontend Review', careerTrack: 'FRONTEND_DEVELOPMENT' }],
    })
    const requestBody = JSON.parse(fetchMock.mock.calls[0][1].body)
    expect(requestBody.responseLanguage).toBe('VI')
    expect(requestBody.context).toEqual({ attemptId: 'attempt-1' })
    expect(reply.resultCards[0].id).toBe(42)
  })

  it('exports one Syn response with its selected evidence and recommendations', () => {
    const markdown = buildSynResponseMarkdown({
      text: 'Compare these observations.',
      provenance: 'AI',
      resultCards: [{
        title: 'Frontend Review',
        score: 90,
        outcomes: [{ label: 'Accessibility', status: 'PRACTISED' }],
      }],
      evidenceReferences: [{ label: 'Frontend Review result', sourceType: 'SIMULATION' }],
      simulationCards: [{ title: 'UI Debugging', careerTrack: 'FRONTEND_DEVELOPMENT' }],
    })

    expect(markdown).toContain('Frontend Review — 90/100')
    expect(markdown).toContain('Accessibility: Practised')
    expect(markdown).toContain('UI Debugging — FRONTEND_DEVELOPMENT')
    expect(markdown).toContain('Source: AI-assisted')
  })

  it('rejects a malformed server response without inventing live guidance', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true, data: { provenance: 'AI' } }),
    }))

    await expect(sendSynMessage({
      message: 'Review my latest result', actionType: 'REVIEW_LATEST',
      dashboard: { ...previewDashboard, source: 'LIVE' },
    })).rejects.toMatchObject({ code: 'INVALID_RESPONSE' })
  })

  it('exposes transport failure so retry can recover the same server turn', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('timeout')))

    await expect(sendSynMessage({
      message: 'Draft an exploration plan', actionType: 'DRAFT_PLAN',
      dashboard: { ...previewDashboard, source: 'LIVE' },
    })).rejects.toMatchObject({ code: 'NETWORK_ERROR' })
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
