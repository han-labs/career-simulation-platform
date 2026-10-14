// Provides normalized API access for catalog, simulation tasks, and submissions.
import { apiRequest } from '../../../shared/api/httpClient.js'
import { coreEnabled } from '../../../shared/api/coreApi.js'

async function requestJson(path, options = {}) {
  return apiRequest(path, options)
}

function normalizeSummary(simulation) {
  return {
    id: simulation.slug || String(simulation.id),
    title: simulation.title,
    description: simulation.summary,
    duration: `${simulation.estimatedMinutes} minutes`,
    difficulty: simulation.difficulty,
    taskCount: 3,
  }
}

export async function fetchSimulations() {
  if (coreEnabled) {
    const scenarios = await requestJson('/v2/scenarios')
    return scenarios.map(s => ({ id: s.slug, title: s.metadata.title, description: s.metadata.summary, duration: `${s.metadata.estimatedMinutes} minutes`, difficulty: s.metadata.difficulty, translations: s.metadata.translations }))
  }
  const simulations = await requestJson('/v1/simulations')
  return simulations.map(normalizeSummary)
}

export async function fetchSimulationDetail(slug) {
  const simulation = await requestJson(`/v1/simulations/${encodeURIComponent(slug)}/tasks`)
  return {
    id: simulation.slug,
    title: simulation.title,
    description: simulation.summary,
    duration: `${simulation.estimatedMinutes} minutes`,
    difficulty: simulation.difficulty,
    taskCount: simulation.tasks.length,
    tasks: simulation.tasks.map((task) => ({
      id: task.id,
      title: task.title,
      prompt: task.instructions,
      options: task.options.map((option) => ({ id: option.id, label: option.label })),
    })),
  }
}

export function submitAttempt(slug, answers) {
  return requestJson(`/v1/simulations/${encodeURIComponent(slug)}/attempts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ answers }),
  })
}
