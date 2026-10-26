// Synthetic DTO for automated tests only; never imported by application pages.
export function usageFixture(overrides = {}) {
  return {
    from: '2026-10-20T00:00:00Z', to: '2026-10-27T00:00:00Z', bucketUnit: 'day',
    summary: { requests: 4, succeeded: 1, failed: 1, refused: 1, cancelled: 1, pending: 0, recordedEstimatedCostUsd: 1.27, estimatedCostExecutions: 1, partialCostExecutions: 1, unknownCostExecutions: 1, notApplicableCostExecutions: 1, averageLatencyMs: 240, latencySamples: 3, missingLatencyExecutions: 1 },
    options: { agents: [{ id: 'agent-a', name: 'Career guide' }, { id: 'agent-retired', name: 'Retired guide' }], models: [{ provider: 'gemini', model: 'gemini/synthetic' }, { provider: 'openai', model: 'openai/synthetic' }], truncated: false },
    trend: [{ startedAt: '2026-10-24T00:00:00Z', requests: 3, succeeded: 1, failed: 1, refused: 1, cancelled: 0, pending: 0 }, { startedAt: '2026-10-25T00:00:00Z', requests: 1, succeeded: 0, failed: 0, refused: 0, cancelled: 1, pending: 0 }],
    executions: [
      { id: 'execution-a', startedAt: '2026-10-24T12:00:00Z', finishedAt: '2026-10-24T12:00:01Z', mode: 'ADMIN_PREVIEW', agentId: 'agent-a', agentName: 'Career guide', configurationId: 'config-a', configurationVersion: 3, promptHash: 'safe-prompt-fingerprint', provider: 'gemini', model: 'gemini/synthetic', outcome: 'FAILED', failureCategory: 'PROVIDER_UNAVAILABLE', latencyMs: 240, inputTokens: null, outputTokens: null, estimatedCostUsd: null, costStatus: 'UNKNOWN', nativeToolCount: 2, tools: ['kb.search', 'simulation.explore'], serverRetrievalCount: 1, modelAttempts: 1 },
      { id: 'execution-b', startedAt: '2026-10-23T12:00:00Z', finishedAt: '2026-10-23T12:00:01Z', mode: 'KB_TEST', agentId: null, agentName: null, configurationId: null, configurationVersion: null, promptHash: null, provider: null, model: null, outcome: 'REFUSED', failureCategory: 'EMPTY_EVIDENCE', latencyMs: 20, inputTokens: null, outputTokens: null, estimatedCostUsd: null, costStatus: 'NOT_APPLICABLE', nativeToolCount: 0, tools: [], serverRetrievalCount: 1, modelAttempts: 0 },
    ], nextCursor: null, ...overrides,
  }
}
export function emptyUsageFixture() {
  return usageFixture({ summary: { requests: 0, succeeded: 0, failed: 0, refused: 0, cancelled: 0, pending: 0, recordedEstimatedCostUsd: null, estimatedCostExecutions: 0, partialCostExecutions: 0, unknownCostExecutions: 0, notApplicableCostExecutions: 0, averageLatencyMs: null, latencySamples: 0, missingLatencyExecutions: 0 }, trend: [], executions: [] })
}
