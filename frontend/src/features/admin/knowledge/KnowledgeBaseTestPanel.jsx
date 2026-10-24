import { useEffect, useRef, useState } from 'react'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import ExecutionResult from '../agentPlatform/ExecutionResult.jsx'
import useExecution from '../agentPlatform/useExecution.js'
import { kbPath, platformCommand } from '../agentPlatform/platformApi.js'

export default function KnowledgeBaseTestPanel({ owner, id, options, active }) {
  const { t } = useI18n(), alive = useRef(true)
  const key = draftKey(owner, 'kb-test-draft', id), savedKey = draftKey(owner, 'kb-test-execution', id)
  const [draft, setDraft] = useState(() => readDraft(key) ?? { query: '', retrievalOnly: true, topK: 5, modelKey: options.models.find(m => m.available)?.key ?? options.models[0]?.key ?? '', requestId: crypto.randomUUID() })
  const [submitted, setSubmitted] = useState(() => readDraft(savedKey)), [busy, setBusy] = useState(false), [error, setError] = useState('')
  const result = useExecution(active ? submitted?.id : null)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  function edit(field, value) { const next = { ...draft, [field]: value, requestId: crypto.randomUUID() }; setDraft(next); writeDraft(key, next) }
  async function test(event) {
    event.preventDefault(); setBusy(true); setError('')
    const input = { ...draft, topK: Number(draft.topK), modelKey: draft.retrievalOnly ? null : draft.modelKey }
    try {
      const execution = await platformCommand(`${kbPath(id)}/tests`, input)
      if (alive.current) { const snapshot = { ...input, id: execution.id }; setSubmitted(snapshot); writeDraft(savedKey, snapshot) }
    } catch (e) { if (alive.current) setError(e.message) }
    finally { if (alive.current) setBusy(false) }
  }
  const captured = result.execution?.inputSnapshot ?? submitted
  return <section className="platform-card"><h3>{t('Independent KB Test')}</h3><p>{t('This test has no Student session. Evidence remains visible if answer generation fails.')}</p>
    {error && <p role="alert">{t(error)}</p>}
    <form onSubmit={test}><label>{t('Knowledge test question')}<textarea required maxLength={2000} value={draft.query} onChange={e => edit('query', e.target.value)} /></label>
      <fieldset><legend>{t('Test mode')}</legend><label><input type="radio" name="kb-test-mode" checked={draft.retrievalOnly} onChange={() => edit('retrievalOnly', true)} />{t('Retrieve passages')}</label><label><input type="radio" name="kb-test-mode" checked={!draft.retrievalOnly} onChange={() => edit('retrievalOnly', false)} />{t('Answer with sources')}</label></fieldset>
      <label>{t('Maximum retrieved passages')}<input type="number" min={1} max={10} value={draft.topK} onChange={e => edit('topK', e.target.value)} /></label>
      <label>{t('Answer model')}<select disabled={draft.retrievalOnly} value={draft.modelKey} onChange={e => edit('modelKey', e.target.value)}>{options.models.map(m => <option key={m.key} value={m.key}>{m.key}{m.available ? '' : ` · ${t('Unavailable')}`}</option>)}</select></label>
      <button disabled={busy || !active || ['ACCEPTED', 'RUNNING'].includes(result.execution?.status)}>{t(draft.retrievalOnly ? 'Retrieve passages' : 'Answer with sources')}</button>
    </form>
    {submitted && <section aria-label={t('Submitted test request')}><h4>{t('Submitted test request')}</h4>{captured.query ? <><p className="core-prewrap">{captured.query}</p><p>{t(captured.retrievalOnly ? 'Retrieve passages' : 'Answer with sources')} · {t('Maximum retrieved passages')}: {captured.topK}</p></> : <p>{t('Saved request details are unavailable for this historical execution.')}</p>}</section>}
    {!active ? <p>{t('Knowledge base unavailable.')}</p> : <ExecutionResult key={submitted?.id} id={submitted?.id} {...result} />}
  </section>
}
