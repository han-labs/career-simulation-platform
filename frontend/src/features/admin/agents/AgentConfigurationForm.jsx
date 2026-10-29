import { useLayoutEffect, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import FormField from '../../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import Button from '../../../shared/ui/Button.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'

const toolRows = [
  ['kb.search', 'Knowledge search', 'Search published passages from granted knowledge bases.'],
  ['assessment.lookup', 'Assessment evidence', 'Authorized Student assessment. Public catalogue only in Admin Preview.'],
  ['simulation.explore', 'Simulation catalogue', 'Explore public simulations.'],
  ['simulation.myEvidence', 'My simulation evidence', 'Student-owned results. Unavailable in Admin Preview.'],
]
const limits = [['maxRounds','Model rounds',1,4],['maxToolCalls','Maximum tool calls',0,8],['timeoutSeconds','Execution timeout (seconds)',5,60]]
export default function AgentConfigurationForm({ area, active, draft, edit, options, bases, busy, archived, errors, identity }) {
  const { t } = useI18n(), form = useRef(null)
  const [picker, setPicker] = useState(null), [search, setSearch] = useState('')
  const tools = draft.toolPolicy?.enabled ?? [], ids = draft.knowledgeBaseIds ?? []
  const model = options.models.find(m => m.key === draft.modelKey)
  const experience = draft.experienceConfig, chat = experience.chat
  const provider = model?.provider ?? draft.modelKey?.split('/')[0] ?? ''
  const providers = [...new Set(options.models.map(m => m.provider))]
  const updateExperience = changes => edit({...draft,experienceConfig:{...experience,...changes}})
  const updateChat = changes => updateExperience({chat:{...chat,...changes}})
  const max = options.limits?.maxKnowledgeBases ?? 10
  const choices = [...bases.filter(b => b.status === 'ACTIVE' || ids.includes(b.id)), ...ids.filter(id => !bases.some(b => b.id === id)).map(id => ({ id, name: t('Unavailable knowledge base') + ': ' + id, status: 'UNAVAILABLE' }))]
  useLayoutEffect(() => { if (active && Object.keys(errors).length) { const invalid = form.current?.querySelector('[aria-invalid="true"]'); const disclosure = invalid?.closest('details'); if (disclosure) disclosure.open = true; invalid?.focus() } }, [active, errors])
  function policy(key, value) { edit({ ...draft, toolPolicy: { ...draft.toolPolicy, [key]: value } }) }
  const numeric = (key,label,min,max) => <FormField key={key} label={t(label)} required error={t(errors[key])} hint={`${min}–${max}`}><input type="number" min={min} max={max} step="1" value={draft.toolPolicy?.[key] ?? ''} onChange={e => policy(key,e.target.value === '' ? '' : Number(e.target.value))} /></FormField>
  return <section ref={form} className="cs-surface cs-stack agent-settings">
    <fieldset disabled={busy || archived} className="agent-builder-fields cs-stack">
      {area === 'brain' && <>{identity}
        <div className="agent-form-columns"><FormField label={t('Audience')} error={t(errors.audience)}><select value={experience.audience} onChange={e=>updateExperience({audience:e.target.value})}><option value="personal">{t('Personal')}</option><option value="business">{t('Business')}</option></select></FormField><FormField label={t('Language')} error={t(errors.language)}><select value={experience.language} onChange={e=>updateExperience({language:e.target.value})}><option value="en">{t('English')}</option><option value="vi">{t('Vietnamese')}</option><option value="bilingual">{t('Bilingual')}</option></select></FormField></div>
        <FormField label={t('System instructions / personality')} required error={t(errors.promptTemplate)} hint={t('Use {{key}} for a defined variable. Audience and language do not change data permissions.')}><textarea rows={6} maxLength={12000} value={draft.promptTemplate ?? ''} onChange={e => edit({ ...draft, promptTemplate:e.target.value })} /></FormField>
        <FormField label={t('Opening greeting')} error={t(errors.greeting)} hint={t('Configured text, not an AI response. Blank uses a greeting in the saved language.')}><textarea rows={2} maxLength={1000} value={experience.greeting} onChange={e=>updateExperience({greeting:e.target.value})}/></FormField>
        <div className="agent-form-columns"><FormField label={t('Provider')}><select value={provider} onChange={e=>edit({...draft,modelKey:options.models.find(m=>m.provider===e.target.value)?.key ?? ''})}>{!providers.includes(provider)&&<option value={provider}>{provider}</option>}{providers.map(p=><option key={p} value={p}>{p}</option>)}</select></FormField><FormField label={t('Model')} required error={t(errors.modelKey)}><select value={draft.modelKey} onChange={e=>edit({...draft,modelKey:e.target.value})}>{!model&&<option value={draft.modelKey}>{draft.modelKey || t('Choose a supported model.')}</option>}{options.models.filter(m=>m.provider===provider).map(m=><option key={m.key} value={m.key}>{m.model ?? m.key} · {t(m.available?'Available':'Unavailable')}</option>)}</select></FormField></div>
        <p className="cs-caption">{t('Availability reflects server configuration, not quota or live call success.')}</p>
        <fieldset className="agent-builder-fields cs-stack"><legend>{t('Ordered fallback models')}</legend>
          {(experience.fallbackModelKeys??[]).map((key,index,rows)=><div className="cs-actions" key={index}><FormField label={`${t('Fallback model')} ${index+1}`} error={t(errors.fallbackModelKeys)}><select value={key} onChange={e=>updateExperience({fallbackModelKeys:rows.map((k,i)=>i===index?e.target.value:k)})}>{!options.models.some(m=>m.key===key&&m.provider===provider)&&<option value={key}>{key} · {t('Unavailable')}</option>}{options.models.filter(m=>m.provider===provider).map(m=><option key={m.key} value={m.key}>{m.model} · {t(m.available?'Available':'Unavailable')}</option>)}</select></FormField><Button aria-label={`${t('Remove fallback')} ${index+1}`} onClick={()=>updateExperience({fallbackModelKeys:rows.filter((_,i)=>i!==index)})}>{t('Remove')}</Button></div>)}
          <Button disabled={(experience.fallbackModelKeys?.length??0)>=3||!options.models.some(m=>m.provider===provider&&m.key!==draft.modelKey&&!experience.fallbackModelKeys?.includes(m.key))} onClick={()=>updateExperience({fallbackModelKeys:[...(experience.fallbackModelKeys??[]),options.models.find(m=>m.provider===provider&&m.key!==draft.modelKey&&!experience.fallbackModelKeys?.includes(m.key)).key]})}>{t('+ Add fallback model')}</Button>
          {errors.fallbackModelKeys&&<FeedbackPanel kind="error">{t(errors.fallbackModelKeys)}</FeedbackPanel>}
          <p className="cs-caption">{t('Tried in order only for eligible failures, within the execution deadline. Refusals and authentication errors are not bypassed.')}</p>
        </fieldset>
        <FormField label={t('Timezone (IANA)')} required error={t(errors.timezone)}><input list="agent-timezones" maxLength={100} value={experience.timezone} onChange={e=>updateExperience({timezone:e.target.value})}/></FormField><datalist id="agent-timezones"><option value="Asia/Ho_Chi_Minh"/><option value="UTC"/></datalist>
        <details className="cs-disclosure"><summary>{t('Safety')}</summary><p className="cs-caption">{t('The backend enforces current identity, permissions, read-only tools, execution limits and citation validation. Safety cannot be disabled here.')}</p></details>
      </>}
      {area === 'chat' && <>
        <h3>{t('Conversation')}</h3><label className="agent-builder-choice"><input type="checkbox" checked={chat.includeHistory} onChange={e=>updateChat({includeHistory:e.target.checked})}/><span>{t('Include same-chat history')}</span></label>
        <FormField label={t('Previous turns')} error={t(errors.maxTurns)} hint="1–6"><input type="number" min="1" max="6" disabled={!chat.includeHistory} value={chat.maxTurns} onChange={e=>updateChat({maxTurns:e.target.value===''?'':Number(e.target.value)})}/></FormField>
        <p className="cs-caption">{t('History off keeps the transcript. No cross-chat memory.')}</p>
        <h3>{t('Retrieval')}</h3>
        {numeric('topK','Maximum retrieved passages',1,10)}
        <h3>{t('Sources')}</h3><label className="agent-builder-choice"><input type="checkbox" checked={chat.showSourceExcerpts} onChange={e=>updateChat({showSourceExcerpts:e.target.checked})}/><span>{t('Show source excerpts')}</span></label><p className="cs-caption">{t('Citation links remain available when excerpts are hidden.')}</p>
        <h3>{t('Response')}</h3><FormField label={t('Response style')} error={t(errors.responseStyle)}><select value={chat.responseStyle} onChange={e=>updateChat({responseStyle:e.target.value})}>{['concise','balanced','detailed'].map(style=><option key={style} value={style}>{t(style[0].toUpperCase()+style.slice(1))}</option>)}</select></FormField>
        <details className="cs-disclosure"><summary>{t('Advanced execution limits')}</summary><div className="cs-stack">
        <label className="agent-builder-choice"><input type="checkbox" checked={draft.memoryPolicy?.enabled === true} onChange={e => edit({ ...draft, memoryPolicy:{ enabled:e.target.checked } })} /><span>{t('Enable bounded Student context')}</span></label>
        <FormField label={t('Grounding policy')} required error={t(errors.grounding)}><select value={draft.outputPolicy?.grounding ?? ''} onChange={e => edit({ ...draft, outputPolicy:{ ...draft.outputPolicy, grounding:e.target.value } })}><option value="KB_ONLY">{t('Require knowledge base evidence')}</option><option value="GENERAL_ALLOWED">{t('Allow general exploration guidance')}</option></select></FormField>
        <FormField label={t('Maximum answer characters')} required error={t(errors.maxCharacters)} hint="100–4000"><input type="number" min="100" max="4000" step="1" value={draft.outputPolicy?.maxCharacters ?? ''} onChange={e => edit({ ...draft, outputPolicy:{ ...draft.outputPolicy, maxCharacters:e.target.value === '' ? '' : Number(e.target.value) } })} /></FormField>
        {limits.map(([key,label,min,max]) => numeric(key,label,min,max))}</div></details>
        <p className="cs-caption">{t('Model rounds limit one request, not the number of chat messages. One round is reserved for the final answer. Save changes and start a new preview to use the revised limits; existing chats keep their saved version.')}</p>
        {draft.toolPolicy?.maxRounds === 1 && tools.length > 0 && <FeedbackPanel kind="warning">{t('One model round is reserved for the final answer. With rounds set to 1, tools cannot run; increase rounds to allow tool use.')}</FeedbackPanel>}
      </>}
      {area === 'variables' && <><h3>{t('Variables')}</h3><p className="cs-caption">{t('Up to 20 plain-text variables. Use {{key}} in instructions and greeting only.')}</p>
        <div className="agent-variables">{(Array.isArray(experience.variables)?experience.variables:Object.entries(experience.variables??{}).map(([key,value])=>({key,value}))).map((variable,index,rows)=><div className="agent-variable-row" key={index}>
          <FormField label={t('Key')+' '+(index+1)} required><input aria-invalid={Boolean(errors.variables)} maxLength={40} value={variable.key} onChange={e=>updateExperience({variables:rows.map((row,i)=>i===index?{...row,key:e.target.value}:row)})}/></FormField>
          <FormField label={t('Value')+' '+(index+1)}><input aria-invalid={Boolean(errors.variables)} maxLength={500} value={variable.value} onChange={e=>updateExperience({variables:rows.map((row,i)=>i===index?{...row,value:e.target.value}:row)})}/></FormField>
          <Button aria-label={t('Remove variable')+' '+(index+1)} onClick={()=>updateExperience({variables:rows.filter((_,i)=>i!==index)})}>{t('Remove')}</Button>
        </div>)}</div>
        <Button disabled={(Array.isArray(experience.variables)?experience.variables.length:Object.keys(experience.variables??{}).length)>=20} onClick={()=>{const rows=Array.isArray(experience.variables)?experience.variables:Object.entries(experience.variables??{}).map(([key,value])=>({key,value}));updateExperience({variables:[...rows,{key:'',value:''}]})}}>{t('+ Add variable')}</Button>
        {errors.variables&&<FeedbackPanel kind="error">{t(errors.variables)}</FeedbackPanel>}
        <p className="cs-caption">{t('No expressions, recursive templates or secret variables.')}</p></>}
      {area === 'integrations' && <>
        <fieldset className="agent-builder-fields cs-stack"><legend>{t('Knowledge Bases')}</legend><div className="agent-kb-chips">{ids.slice(0,3).map(id => <span className="cs-badge" key={id}>{choices.find(b => b.id === id)?.name ?? t('Unavailable knowledge base')}</span>)}</div><p className="cs-caption">{ids.length} / {max} · {t('selected')}</p><Button onClick={() => { setPicker([...ids]); setSearch('') }}>{t('Manage knowledge bases')}</Button>{errors.knowledgeBaseIds && <FeedbackPanel kind="error">{t(errors.knowledgeBaseIds)}</FeedbackPanel>}</fieldset>
        <fieldset className="agent-builder-fields cs-stack"><legend>{t('Domain tool grants')}</legend>{toolRows.filter(([code]) => options.tools.some(tool => tool.code === code)).map(([code,label,description]) => <label key={code} className="agent-builder-choice agent-tool-choice"><input type="checkbox" aria-label={code} checked={tools.includes(code)} onChange={e => policy('enabled', e.target.checked ? [...tools,code] : tools.filter(k => k !== code))} /><span><strong>{t(label)}</strong><small>{code}</small><span className="cs-caption">{t(description)}</span></span></label>)}{errors.tools && <FeedbackPanel kind="error">{t(errors.tools)}</FeedbackPanel>}</fieldset>
      </>}
    </fieldset>
    {picker && <Dialog title={t('Manage knowledge bases')} closeLabel={t('Close dialog')} onRequestClose={() => setPicker(null)} footer={<><Button onClick={() => setPicker(null)}>{t('Cancel')}</Button><Button variant="primary" disabled={busy || archived || picker.length > max} onClick={() => { edit({ ...draft, knowledgeBaseIds:picker }); setPicker(null) }}>{t('Apply')}</Button></>}>
      <div className="cs-stack"><FormField label={t('Search knowledge bases')}><input value={search} onChange={e => setSearch(e.target.value)} /></FormField><p role="status">{picker.length} / {max} · {t('selected')}</p>
      {choices.filter(b => b.name.toLocaleLowerCase().includes(search.toLocaleLowerCase())).map(b => <label key={b.id} className="agent-builder-choice"><input type="checkbox" checked={picker.includes(b.id)} disabled={!picker.includes(b.id) && (b.status !== 'ACTIVE' || picker.length >= max)} onChange={e => setPicker(current => e.target.checked ? [...current,b.id] : current.filter(id => id !== b.id))} /><span>{b.name}{b.status !== 'ACTIVE' && <small>{t('Unavailable knowledge base')}</small>}</span></label>)}
      {!choices.length && <FeedbackPanel kind="empty">{t('No authorized knowledge bases are available.')}</FeedbackPanel>}</div>
    </Dialog>}
  </section>
}
