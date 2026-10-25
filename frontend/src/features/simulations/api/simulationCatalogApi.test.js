import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../../../shared/api/httpClient.js'
vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest: vi.fn() }))
afterEach(() => vi.unstubAllEnvs())
import { normalizeSimulation } from './simulationCatalogApi.js'

describe('normalizeSimulation', () => {
  it('uses the versioned metadata catalog consistently in core mode',async()=>{
    vi.resetModules();vi.stubEnv('VITE_CORE_V2_ENABLED','true');apiRequest.mockResolvedValue([{id:'8',slug:'fixture',scenarioVersionId:'9',enterpriseName:'Original provider',media:{companyLogoUrl:'/public-provider.png'},metadata:{title:'Reviewed',estimatedMinutes:10}}]);const {getPublishedSimulations}=await import('./simulationCatalogApi.js');expect(await getPublishedSimulations()).toMatchObject([{id:'8',title:'Reviewed',enterpriseName:'Original provider',media:{companyLogoUrl:'/public-provider.png'}}]);expect(apiRequest).toHaveBeenLastCalledWith('/v2/scenarios')
  })
  it('retains legacy read contract when core mode is off',async()=>{
    vi.resetModules();vi.stubEnv('VITE_CORE_V2_ENABLED','false');apiRequest.mockResolvedValue([{id:8,slug:'legacy',title:'Legacy',estimatedMinutes:10}]);const {getPublishedSimulations}=await import('./simulationCatalogApi.js');expect(await getPublishedSimulations()).toMatchObject([{id:8,title:'Legacy'}]);expect(apiRequest).toHaveBeenLastCalledWith('/v1/simulations')
  })
  it('normalizes the API duration as a number', () => {
    const simulation = normalizeSimulation({
      id: 1,
      slug: 'backend-api-triage',
      title: 'Backend API Triage',
      careerTrack: 'BACKEND_DEVELOPMENT',
      summary: 'Practice backend decisions.',
      difficulty: 'INTRODUCTORY',
      estimatedMinutes: '35',
    })

    expect(simulation.estimatedMinutes).toBe(35)
    expect(simulation.slug).toBe('backend-api-triage')
  })
})
