// Provides normalized API access for catalog, simulation tasks, and submissions.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

async function requestJson(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options)
  const json = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(json?.message || `Request failed with status ${response.status}`)
  }
  return unwrap(json)
}

function unwrap(json) {
  if (json?.success === true) return json.data
  throw new Error(json?.message || 'The API returned an unsuccessful response.')
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
      'X-Student-Id': '1',
    },
    body: JSON.stringify({ answers }),
  })
}
