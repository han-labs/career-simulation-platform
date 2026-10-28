import { expect, it } from 'vitest'
import { taskReferenceMaterials } from './referenceMaterials.js'

const hpe = { title: 'Software Engineering', targetRole: 'Software Engineer', careerTrack: 'Software Engineering' }
const proposal = { id: 'historic-task', title: 'Create a Proposal for a RESTful Web Service', resources: [] }
const spring = 'https://www.ibm.com/cloud/learn/java-spring-boot#toc-what-is-ja-WZk7U3ur'

it('matches references to the original program and task, including pinned older tasks', () => {
  const links = taskReferenceMaterials(hpe, proposal)
  expect(links).toHaveLength(4)
  expect(links.map(link => link.title)).toContain('Learn about Java Spring Boot')
  expect(links.map(link => link.title)).not.toContain('Gradle Install')
  expect(links.map(link => link.externalUrl).join(' ')).not.toMatch(/Model Answer|Example Answer|w3schools\.in/)
  expect(taskReferenceMaterials(hpe, { ...proposal, id: 'different-version-task' })).toEqual(links)
})

it('does not guess links from a matching title in another program or unrelated source', () => {
  expect(taskReferenceMaterials({ ...hpe, title: 'Different program' }, proposal)).toEqual([])
  expect(taskReferenceMaterials({ ...hpe, targetRole: 'Different role' }, proposal)).toEqual([])
  expect(taskReferenceMaterials({ ...hpe, careerTrack: 'Different track' }, proposal)).toEqual([])
  expect(taskReferenceMaterials(hpe, { ...proposal, title: 'Different task' })).toEqual([])
  expect(taskReferenceMaterials(undefined, undefined)).toEqual([])
})

it('keeps existing server resources and does not duplicate their URLs', () => {
  const existing = { id: 'server-resource', title: 'Current title', externalUrl: spring, releasePolicy: 'TASK_ACCESS' }
  const task = { ...proposal, resources: [existing] }
  const links = taskReferenceMaterials(hpe, task)
  expect(links[0]).toEqual(existing)
  expect(links.filter(link => link.externalUrl === spring)).toEqual([existing])
  expect(task.resources).toEqual([existing])
})

it('never replaces a hidden server resource with an ungated supplemental link', () => {
  const gated = { id: 'gated', title: 'Reviewed material', externalUrl: spring, releasePolicy: 'AFTER_SUBMISSION' }
  const file = { id: 'file', title: 'Authorized file', fileAssetId: 'asset', releasePolicy: 'TASK_ACCESS' }
  const task = { ...proposal, resources: [gated, file] }
  const draft = taskReferenceMaterials(hpe, task)
  expect(draft).toContainEqual(file)
  expect(draft.some(link => link.externalUrl === spring)).toBe(false)
  const final = taskReferenceMaterials(hpe, task, true)
  expect(final.filter(link => link.externalUrl === spring)).toEqual([gated])
})

it('keeps task inputs while excluding model answers and unavailable references', () => {
  const automation = taskReferenceMaterials({ title: 'Automation AI Accelerator: From Co-pilot to Autonomous Agent', targetRole: 'Automation Engineer', careerTrack: 'Automation and AI' }, { title: 'AI-Powered Debugging and Refactoring', resources: [] })
  expect(automation.map(link => link.title)).toEqual(['Customers', 'Error Log', 'Process Data', 'Transactions'])
  expect(automation.map(link => link.externalUrl).join(' ')).not.toMatch(/process_data_final|TEST_CASES|DEBUG_LOG/)
  const upload = taskReferenceMaterials(hpe, { title: 'Implement the Ability to Upload Data', resources: [] })
  expect(upload.map(link => link.externalUrl).join(' ')).not.toMatch(/devwithus|Model Answer|Task3/)
  expect(upload).toHaveLength(2)
})

it('leaves tasks without a matching workbook resource unchanged', () => {
  expect(taskReferenceMaterials({ title: 'Partnering with AI in the workplace', targetRole: 'AI-enabled Professional', careerTrack: 'Artificial Intelligence' }, { title: 'Writing effective prompts', resources: [] })).toEqual([])
  expect(taskReferenceMaterials({ title: 'Digital Sales', targetRole: 'Digital Sales Representative', careerTrack: 'Digital Technology Sales' }, { title: 'Managing Your Time', resources: [] })).toEqual([])
})
