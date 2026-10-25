import { useEffect, useId, useRef, useState } from 'react'
import { Play } from 'lucide-react'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import ExecutionResult from '../agentPlatform/ExecutionResult.jsx'
import useExecution from '../agentPlatform/useExecution.js'
import { kbPath, platformCommand } from '../agentPlatform/platformApi.js'

export default function KnowledgeBaseTestPanel({ owner, id, options, active }) {
  const { t } = useI18n(), alive = useRef(true), modeId = useId()
  const key = draftKey(owner, 'kb-test-draft', id), savedKey = draftKey(owner, 'kb-test-execution', id)
  const [draft, setDraft] = useState(() => readDraft(key) ?? { query: '', retrievalOnly: true, topK: 5, modelKey: options.models.find(model => model.available)?.key ?? options.models[0]?.key ?? '', requestId: crypto.randomUUID() })
  const [submitted, setSubmitted] = useState(() => readDraft(savedKey)), [busy, setBusy] = useState(false), [error, setError] = useState('')
  const result = useExecution(active ? submitted?.id : null)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  function edit(field, value) { const next = { ...draft, [field]: value, requestId: crypto.randomUUID() }; setDraft(next); writeDraft(key, next) }
  async function test(event) {
    event.preventDefault(); setBusy(true); setError('')
    const input = { ...draft, topK: Number(draft.topK), modelKey: draft.retrievalOnly ? null : draft.modelKey }
    try {
      const execution = await platformCommand(kbPath(id) + '/tests', input)
      if (alive.current) { const snapshot = { ...input, id: execution.id }; setSubmitted(snapshot); writeDraft(savedKey, snapshot) }
    } catch (failure) { if (alive.current) setError(failure.message) }
    finally { if (alive.current) setBusy(false) }
  }
  const captured = result.execution?.inputSnapshot ?? submitted
  return <section aria-label={t('Independent KB Test')} className="kb-test-grid">
    <div className="cs-surface cs-stack"><div><h2>{t('Independent KB Test')}</h2><p className="cs-caption">{t('This test has no Student session. Evidence remains visible if answer generation fails.')}</p></div>
      {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
      <form onSubmit={test} className="cs-stack">
        <fieldset className="cs-fieldset kb-test-modes"><legend>{t('Test mode')}</legend>
          <label className="cs-choice"><input type="radio" aria-label={t('Retrieve passages')} aria-describedby={modeId + '-retrieve-hint'} name={modeId} checked={draft.retrievalOnly} onChange={() => edit('retrievalOnly', true)} /><span><strong>{t('Retrieve passages')}</strong><span id={modeId + '-retrieve-hint'} className="kb-mode-hint">{t('Retrieve ranked evidence without generating an answer.')}</span></span></label>
          <label className="cs-choice"><input type="radio" aria-label={t('Answer with sources')} aria-describedby={modeId + '-answer-hint'} name={modeId} checked={!draft.retrievalOnly} onChange={() => edit('retrievalOnly', false)} /><span><strong>{t('Answer with sources')}</strong><span id={modeId + '-answer-hint'} className="kb-mode-hint">{t('Generate an answer grounded in the retrieved passages.')}</span></span></label>
        </fieldset>
        <FormField label={t('Knowledge test question')} required hint={t('Ask a question about the published sources in this knowledge base.')}><textarea maxLength={2000} value={draft.query} onChange={event => edit('query', event.target.value)} /></FormField>
        <FormField label={t('Maximum retrieved passages')}><input type="number" min={1} max={10} value={draft.topK} onChange={event => edit('topK', event.target.value)} /></FormField>
        {!draft.retrievalOnly && <FormField label={t('Answer model')}><select value={draft.modelKey} onChange={event => edit('modelKey', event.target.value)}>{options.models.map(model => <option key={model.key} value={model.key}>{model.key}{model.available ? '' : ' · ' + t('Unavailable')}</option>)}</select></FormField>}
        <Button variant="primary" type="submit" icon={<Play size={18} />} busy={busy} disabled={!active || ['ACCEPTED', 'RUNNING'].includes(result.execution?.status)}>{t(draft.retrievalOnly ? 'Retrieve passages' : 'Answer with sources')}</Button>
        {busy && <p role="status" className="cs-caption">{t('Submitting test...')}</p>}
      </form>
    </div>
    <div className="kb-test-result">
      {submitted && <section className="kb-submitted" aria-label={t('Submitted test request')}><h3>{t('Submitted test request')}</h3>{captured.query ? <><p className="cs-prose">{captured.query}</p><p className="cs-caption">{t(captured.retrievalOnly ? 'Retrieve passages' : 'Answer with sources')} · {t('Maximum retrieved passages')}: {captured.topK}</p></> : <p>{t('Saved request details are unavailable for this historical execution.')}</p>}</section>}
      {!active ? <FeedbackPanel kind="warning">{t('Knowledge base unavailable.')}</FeedbackPanel> : submitted ? <ExecutionResult presentation="studio" key={submitted.id} id={submitted.id} {...result} /> : <FeedbackPanel kind="empty" title={t('Inspect evidence, then evaluate the answer.')}>{t('Submit a question to see ranked passages and a separate answer when requested.')}</FeedbackPanel>}
    </div>
  </section>
}
