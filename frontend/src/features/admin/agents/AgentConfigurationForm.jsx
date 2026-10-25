import { useLayoutEffect, useRef } from 'react'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import FormField from '../../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import { toolDescriptions } from './agentConfiguration.js'

const limits = [
  ['maxRounds', 'Model rounds', 1, 4], ['maxToolCalls', 'Maximum tool calls', 0, 8],
  ['topK', 'Maximum retrieved passages', 1, 10], ['timeoutSeconds', 'Execution timeout (seconds)', 5, 60],
]
export default function AgentConfigurationForm({ area, active, draft, edit, options, bases, busy, archived, errors }) {
  const { t } = useI18n(), form = useRef(null)
  const tools = draft.toolPolicy?.enabled ?? []
  const ids = draft.knowledgeBaseIds ?? []
  const model = options.models.find(m => m.key === draft.modelKey)
  useLayoutEffect(() => { if (active && Object.keys(errors).length) form.current?.querySelector('[aria-invalid="true"]')?.focus() }, [active, errors])
  function policy(key, value) { edit({ ...draft, toolPolicy: { ...draft.toolPolicy, [key]: value } }) }
  return <section ref={form} className="cs-stack agent-settings">
    <fieldset disabled={busy || archived} className="agent-builder-fields cs-stack">
      {area === 'brain' && <><section className="cs-surface cs-stack" aria-label={t('Model')}><h3>{t('Model')}</h3>
        <FormField label={t('Supported model')} required error={t(errors.modelKey)} hint={t('Models come from server configuration. Availability does not guarantee a successful provider call.')}>
          <select value={draft.modelKey} onChange={e => edit({ ...draft, modelKey: e.target.value })}>
            {!model && <option value={draft.modelKey}>{draft.modelKey || t('Choose a supported model.')}</option>}
            {options.models.map(m => <option key={m.key} value={m.key}>{m.key} · {t(m.available ? 'Available' : 'Unavailable')}</option>)}
          </select>
        </FormField>
        {!model?.available && <FeedbackPanel kind="warning">{t('This model can be saved, but Preview may fail while the provider is unavailable.')}</FeedbackPanel>}
      </section>
      <section className="cs-surface cs-stack" aria-label={t('Instructions')}><h3>{t('Instructions')}</h3>
        <FormField label={t('Saved Agent instructions')} required error={t(errors.promptTemplate)} hint={t('Describe career exploration, when to use tools, reply language, evidence handling and safe refusals. Your text is versioned and is never replaced automatically.')}>
          <textarea rows={8} maxLength={12000} value={draft.promptTemplate ?? ''} onChange={e => edit({ ...draft, promptTemplate: e.target.value })} />
        </FormField><p className="cs-caption">{t('Never put credentials in instructions. Platform security rules remain authoritative.')}</p>
      </section>
      <details className="cs-surface agent-safety"><summary>{t('Safety')}</summary><p>{t('The backend enforces current identity, permissions, read-only tools, execution limits and citation validation. Safety cannot be disabled here.')}</p><p className="cs-caption">{t('Authorized Students may interpret their own assessment and simulation evidence. Admin Preview never impersonates a Student or reads personal Student evidence.')}</p></details></>}
      {area === 'advanced' && <><section className="cs-surface cs-stack" aria-label={t('Policies')}><h3>{t('Policies')}</h3><div className="cs-fields">
        {limits.map(([key, label, min, max]) => <FormField key={key} label={t(label)} required error={t(errors[key])} hint={`${min}–${max}`}>
          <input type="number" min={min} max={max} step="1" value={draft.toolPolicy?.[key] ?? ''} onChange={e => policy(key, e.target.value === '' ? '' : Number(e.target.value))} />
        </FormField>)}
        <FormField label={t('Maximum answer characters')} required error={t(errors.maxCharacters)} hint="100–4000"><input type="number" min="100" max="4000" step="1" value={draft.outputPolicy?.maxCharacters ?? ''} onChange={e => edit({ ...draft, outputPolicy: { ...draft.outputPolicy, maxCharacters: e.target.value === '' ? '' : Number(e.target.value) } })} /></FormField>
        <FormField label={t('Grounding policy')} required error={t(errors.grounding)}><select value={draft.outputPolicy?.grounding ?? ''} onChange={e => edit({ ...draft, outputPolicy: { ...draft.outputPolicy, grounding: e.target.value } })}><option value="KB_ONLY">{t('Require knowledge base evidence')}</option><option value="GENERAL_ALLOWED">{t('Allow general exploration guidance')}</option></select></FormField>
      </div><p className="cs-caption">{t('General guidance does not turn missing evidence into a grounded answer. Citations must always match evidence from the current run.')}</p>
        {draft.toolPolicy?.maxRounds === 1 && tools.length > 0 && <FeedbackPanel kind="warning">{t('One model round is reserved for the final answer. With rounds set to 1, tools cannot run; increase rounds to allow tool use.')}</FeedbackPanel>}
      </section>
      <section className="cs-surface cs-stack" aria-label={t('Conversation and bounded context')}><h3>{t('Conversation and bounded context')}</h3>
        <label className="agent-builder-choice"><input type="checkbox" checked={draft.memoryPolicy?.enabled === true} onChange={e => edit({ ...draft, memoryPolicy: { enabled: e.target.checked } })} /><span>{t('Enable bounded Student context')}</span></label>
        <p className="cs-caption">{t('Uses supported, scoped Student history/context in Student runtime. This is not training, autonomous learning or unlimited permanent memory. Admin Preview has no Student history.')}</p>
      </section></>}
      {area === 'integrations' && <div className="agent-integrations-grid"><fieldset className="cs-surface cs-stack"><legend>{t('Knowledge Bases')}</legend><p className="cs-caption">{t('Selected knowledge bases define this Agent’s knowledge scope. Only authorized, published content can be retrieved; empty evidence is not successful grounding.')}</p>
        {bases.filter(b => b.status === 'ACTIVE' || ids.includes(b.id)).map(b => <label key={b.id} className="agent-builder-choice"><input type="checkbox" checked={ids.includes(b.id)} onChange={e => edit({ ...draft, knowledgeBaseIds: e.target.checked ? [...ids, b.id] : ids.filter(id => id !== b.id) })} disabled={b.status !== 'ACTIVE' && !ids.includes(b.id)} /><span><strong>{b.name}</strong><small>{b.status}</small></span></label>)}
        {ids.filter(id => !bases.some(b => b.id === id)).map(id => <label key={id} className="agent-builder-choice"><input type="checkbox" checked onChange={() => edit({ ...draft, knowledgeBaseIds: ids.filter(k => k !== id) })} /><span>{t('Unavailable knowledge base')}: {id}</span></label>)}
        {!bases.length && <p>{t('No authorized knowledge bases are available.')}</p>}{errors.knowledgeBaseIds && <FeedbackPanel kind="error">{t(errors.knowledgeBaseIds)}</FeedbackPanel>}
      </fieldset>
      <fieldset className="cs-surface cs-stack"><legend>{t('Domain tool grants')}</legend><p className="cs-caption">{t('Granting a knowledge base does not automatically enable its search tool.')}</p>
        {options.tools.filter(tool => Object.hasOwn(toolDescriptions, tool.code)).map(tool => <label key={tool.code} className="agent-builder-choice agent-tool-choice"><input type="checkbox" aria-label={tool.code} checked={tools.includes(tool.code)} onChange={e => policy('enabled', e.target.checked ? [...tools, tool.code] : tools.filter(k => k !== tool.code))} /><span><strong>{t(({ 'kb.search': 'Knowledge search', 'assessment.lookup': 'Assessment evidence', 'simulation.explore': 'Simulation catalogue', 'simulation.myEvidence': 'My simulation evidence' })[tool.code])}</strong><small>{tool.code}</small><span className="cs-caption">{t(toolDescriptions[tool.code])}</span></span></label>)}
        {errors.tools && <FeedbackPanel kind="error">{t(errors.tools)}</FeedbackPanel>}
      </fieldset></div>}

    </fieldset>
  </section>
}
