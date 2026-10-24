import { useEffect, useRef, useState } from 'react'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { kbPath, platformCommand } from '../agentPlatform/platformApi.js'

// Draft revision is captured once, never replaced by a background list refresh.
export default function KnowledgeBaseDialog({ owner, base, onSaved, onClose }) {
  const { t } = useI18n(), dialog = useRef(null), alive = useRef(true)
  const key = draftKey(owner, 'kb-settings', base?.id ?? 'new')
  const [draft, setDraft] = useState(() => readDraft(key) ?? { name: base?.name ?? '', status: base?.status ?? 'ACTIVE', revision: base?.revision })
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  useEffect(() => {
    alive.current = true
    const previous = document.activeElement
    dialog.current?.querySelector('input')?.focus()
    return () => { alive.current = false; previous?.focus() }
  }, [])
  function edit(field, value) { const next = { ...draft, [field]: value }; setDraft(next); writeDraft(key, next) }
  async function discard() {
    setBusy(true); setError('')
    try {
      const latest = await apiRequest(kbPath(base.id))
      if (alive.current) { const next = { name: latest.name, status: latest.status, revision: latest.revision }; setDraft(next); writeDraft(key, next) }
    } catch (failure) { if (alive.current) setError(failure.message) }
    finally { if (alive.current) setBusy(false) }
  }
  async function save(event) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const saved = await platformCommand(base ? kbPath(base.id) : '/v2/admin/knowledge-bases', { name: draft.name, ...(base ? { status: draft.status } : {}) }, draft.revision, base ? 'PATCH' : 'POST')
      // Treat the write as successful even if a later list refresh fails.
      if (alive.current) { writeDraft(key, null); onSaved(saved) }
    } catch (failure) { if (alive.current) { setError(failure.message); setBusy(false) } }
  }
  function keyboard(event) {
    if (event.key === 'Escape' && !busy) onClose()
    if (event.key !== 'Tab') return
    const fields = [...dialog.current.querySelectorAll('input, select, button')].filter(e => !e.disabled)
    const first = fields[0], last = fields.at(-1)
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
  }
  return <div className="kb-dialog-backdrop"><section ref={dialog} role="dialog" aria-modal="true" aria-labelledby="kb-dialog-title" className="platform-card kb-dialog" onKeyDown={keyboard}>
    <h2 id="kb-dialog-title">{t(base ? 'Edit knowledge base' : 'Create knowledge base')}</h2>
    <p>{t('Drafts stay with this account and knowledge base. Closing keeps your draft; reloading a list does not change its reviewed revision.')}</p>
    {error && <p role="alert">{t(error)}</p>}
    <form onSubmit={save}><fieldset disabled={busy}>
      <label>{t(base ? 'Knowledge base name' : 'New knowledge base name')}<input required maxLength={180} value={draft.name} onChange={e => edit('name', e.target.value)} /></label>
      {base && <label>{t('Knowledge visibility status')}<select value={draft.status} onChange={e => edit('status', e.target.value)}>{['ACTIVE', 'HIDDEN', 'ARCHIVED'].map(status => <option key={status} value={status}>{t(status === 'ACTIVE' ? 'Active' : status === 'HIDDEN' ? 'Hidden' : 'Archived')}</option>)}</select></label>}
      <div className="platform-actions"><button>{t(base ? 'Save knowledge base settings' : 'Create knowledge base')}</button><button type="button" onClick={onClose}>{t('Close and keep draft')}</button>{base && <button type="button" onClick={discard}>{t('Discard draft and use refreshed version')}</button>}</div>
    </fieldset></form>
  </section></div>
}
