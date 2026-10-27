export const policyLimits = {
  maxRounds: [1, 4], maxToolCalls: [0, 8], topK: [1, 10], timeoutSeconds: [5, 60], maxCharacters: [100, 4000],
}
export const toolDescriptions = {
  'kb.search': 'Search published passages from explicitly granted knowledge bases.',
  'assessment.lookup': 'Interpret the Student’s own authorized assessment results. Private evidence is unavailable in Admin Preview.',
  'simulation.explore': 'Explore the public simulation catalogue without changing enrollment or results.',
  'simulation.myEvidence': 'Read the Student’s own authorized simulation progress and results. Unavailable in Admin Preview.',
}
export function experienceDefaults(identity = {}) {
  return { schemaVersion:2, name:identity.name ?? '', description:identity.purpose ?? '', audience:'personal', language:'vi', greeting:'', timezone:'Asia/Ho_Chi_Minh', variables:[], chat:{includeHistory:true,maxTurns:6,showSourceExcerpts:true,responseStyle:'balanced'} }
}
export function configurationDraft(source, identity) {
  const experience=source.experienceConfig ?? experienceDefaults(identity)
  const variables=Array.isArray(experience.variables)?experience.variables:Object.entries(experience.variables ?? {}).map(([key,value])=>({key,value}))
  return structuredClone({ modelKey: source.modelKey, promptTemplate: source.promptTemplate, promptTemplateRef: source.promptTemplateRef,
    toolPolicy: source.toolPolicy, memoryPolicy: source.memoryPolicy, outputPolicy: source.outputPolicy, knowledgeBaseIds: source.knowledgeBaseIds,
    experienceConfig:{...experience,variables} })
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
  const experience = draft.experienceConfig
  if (experience) {
    const fallbacks = experience.fallbackModelKeys ?? []
    if (!Array.isArray(fallbacks) || fallbacks.length > 3 || new Set([draft.modelKey,...fallbacks]).size !== fallbacks.length+1 || fallbacks.some(key => !options.models.some(m => m.key === key && m.provider === draft.modelKey.split('/')[0]))) errors.fallbackModelKeys = 'Choose up to three unique fallback models from the same provider.'
    if (!['personal','business'].includes(experience.audience)) errors.audience = 'Choose an audience.'
    if (!['en','vi','bilingual'].includes(experience.language)) errors.language = 'Choose a language.'
    try { new Intl.DateTimeFormat('en',{timeZone:experience.timezone}).format() } catch { errors.timezone = 'Choose a valid IANA timezone.' }
    const variables=Array.isArray(experience.variables)?experience.variables:Object.entries(experience.variables ?? {}).map(([key,value])=>({key,value}))
    const keys=variables.map(v=>v.key)
    if(variables.length>20||new Set(keys).size!==keys.length||variables.some(v=>!/^[A-Za-z_][A-Za-z0-9_]{0,39}$/.test(v.key)||['__proto__','prototype','constructor'].includes(v.key)||/secret|password|token|api_?key|authorization|cookie/i.test(v.key)))errors.variables='Use up to 20 unique safe variable keys (1–40 characters).'
    if(variables.some(v=>typeof v.value!=='string'||v.value.length>500||[...v.value].some(c=>c.charCodeAt(0)<32)||/\{\{|\}\}/.test(v.value)||/bearer\s+|(?:api[_ -]?key|password|secret)\s*[:=]/i.test(v.value)))errors.variables='Variable values must be plain text without secrets or nested templates, up to 500 characters.'
    for(const text of [draft.promptTemplate,experience.greeting]){
      let unknown=false
      const rest=(text??'').replace(/\{\{([A-Za-z_][A-Za-z0-9_]{0,39})\}\}/g,(_,key)=>{if(!keys.includes(key))unknown=true;return ''})
      if(unknown||/\{\{|\}\}/.test(rest))errors.promptTemplate='Every {{key}} must reference a defined variable. Expressions are unsupported.'
    }
    if (experience.greeting?.length > 1000) errors.greeting = 'Use up to 1000 characters.'
    if (!Number.isInteger(experience.chat?.maxTurns) || experience.chat.maxTurns < 1 || experience.chat.maxTurns > 6) errors.maxTurns = 'Enter a whole number within the displayed bounds.'
    if (!['concise','balanced','detailed'].includes(experience.chat?.responseStyle)) errors.responseStyle = 'Choose a response style.'
  }
  return errors
}
