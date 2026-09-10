import { apiRequest } from '../../../shared/api/httpClient.js'

export function normalizeSimulation(item) {
  return {
    id: item.id,
    slug: item.slug,
    title: item.title,
    careerTrack: item.careerTrack,
    summary: item.summary,
    difficulty: item.difficulty,
    estimatedMinutes: Number(item.estimatedMinutes),
  }
}

export async function getPublishedSimulations() {
  const simulations = await apiRequest('/v1/simulations')
  return simulations.map(normalizeSimulation)
}
