import { apiRequest } from '../../../shared/api/httpClient.js'
import { coreEnabled } from '../../../shared/api/coreApi.js'

export function normalizeSimulation(item) {
  return {
    id: item.id,
    slug: item.slug,
    title: item.title,
    careerTrack: item.careerTrack,
    summary: item.summary,
    difficulty: item.difficulty,
    estimatedMinutes: Number(item.estimatedMinutes),
    translations: item.translations,
    enterpriseName: item.enterpriseName,
    media: item.media,
  }
}

export async function getPublishedSimulations() {
  const data = await apiRequest(coreEnabled ? '/v2/scenarios' : '/v1/simulations')
  const simulations = coreEnabled ? data.map(item => ({ ...item.metadata, id: item.id, slug: item.slug, enterpriseName: item.enterpriseName, media: item.media })) : data
  return simulations.map(normalizeSimulation)
}
