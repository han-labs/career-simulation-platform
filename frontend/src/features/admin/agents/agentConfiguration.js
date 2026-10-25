export const policyLimits = {
  maxRounds: [1, 4], maxToolCalls: [0, 8], topK: [1, 10], timeoutSeconds: [5, 60], maxCharacters: [100, 4000],
}
export const toolDescriptions = {
  'kb.search': 'Search published passages from explicitly granted knowledge bases.',
  'assessment.lookup': 'Interpret the Student’s own authorized assessment results. Private evidence is unavailable in Admin Preview.',
  'simulation.explore': 'Explore the public simulation catalogue without changing enrollment or results.',
  'simulation.myEvidence': 'Read the Student’s own authorized simulation progress and results. Unavailable in Admin Preview.',
}
export function configurationDraft(source) {
  return structuredClone({ modelKey: source.modelKey, promptTemplate: source.promptTemplate, promptTemplateRef: source.promptTemplateRef,
    toolPolicy: source.toolPolicy, memoryPolicy: source.memoryPolicy, outputPolicy: source.outputPolicy, knowledgeBaseIds: source.knowledgeBaseIds })
}
export function emptyConfiguration(options) {
  return configurationDraft({ modelKey: options.models.find(m => m.available)?.key ?? options.models[0]?.key ?? '', promptTemplate: '',
    promptTemplateRef: options.promptTemplateRef, toolPolicy: options.defaultToolPolicy, memoryPolicy: options.defaultMemoryPolicy,
    outputPolicy: options.defaultOutputPolicy, knowledgeBaseIds: [] })
}
export function validateConfiguration(draft, options, bases) {
  const errors = {}
  if (!options.models.some(m => m.key === draft.modelKey)) errors.modelKey = 'Choose a supported model.'
  if (!draft.promptTemplate?.trim() || draft.promptTemplate.length > 12000) errors.promptTemplate = 'Enter instructions between 1 and 12000 characters.'
  for (const [key, [min, max]] of Object.entries(policyLimits)) {
    const value = key === 'maxCharacters' ? draft.outputPolicy?.[key] : draft.toolPolicy?.[key]
    if (!Number.isInteger(value) || value < min || value > max) errors[key] = 'Enter a whole number within the displayed bounds.'
  }
  const tools = draft.toolPolicy?.enabled
  if (!Array.isArray(tools) || tools.some(code => !options.tools.some(t => t.code === code)) || tools.length > 4 || new Set(tools).size !== tools.length) errors.tools = 'Select only supported domain tools.'
  if (tools?.length && draft.toolPolicy?.maxToolCalls === 0) errors.maxToolCalls = 'Enabled tools require a positive tool-call budget.'
  if (!['KB_ONLY', 'GENERAL_ALLOWED'].includes(draft.outputPolicy?.grounding) || draft.outputPolicy?.schemaVersion !== 1) errors.grounding = 'Choose a supported grounding policy.'
  if (typeof draft.memoryPolicy?.enabled !== 'boolean') errors.memory = 'Choose whether bounded Student context is enabled.'
  if (!Array.isArray(draft.knowledgeBaseIds) || draft.knowledgeBaseIds.length > (options.limits?.maxKnowledgeBases ?? 10) || new Set(draft.knowledgeBaseIds).size !== draft.knowledgeBaseIds.length || draft.knowledgeBaseIds.some(id => !bases.some(b => b.id === id && b.status === 'ACTIVE'))) errors.knowledgeBaseIds = 'Choose at most ten distinct authorized active knowledge bases.'
  return errors
}
