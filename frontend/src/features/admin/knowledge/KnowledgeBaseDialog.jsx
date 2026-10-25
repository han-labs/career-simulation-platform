import { useEffect, useId, useRef, useState } from 'react'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { kbPath, platformCommand } from '../agentPlatform/platformApi.js'
import Button from '../../../shared/ui/Button.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'

// Draft revision is captured once, never replaced by a background list refresh.
export default function KnowledgeBaseDialog({ owner, base, onSaved, onClose }) {
  const { t } = useI18n(), alive = useRef(true), nameField = useRef(null), formId = useId()
  const key = draftKey(owner, 'kb-settings', base?.id ?? 'new')
  const [draft, setDraft] = useState(() => readDraft(key) ?? { name: base?.name ?? '', status: base?.status ?? 'ACTIVE', revision: base?.revision })
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [nameError, setNameError] = useState(''), [confirmStatus, setConfirmStatus] = useState(false)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  function edit(field, value) { const next = { ...draft, [field]: value }; setDraft(next); writeDraft(key, next); setConfirmStatus(false); if (field === 'name') setNameError('') }
  async function discard() {
    setBusy(true); setError('')
    try {
      const latest = await apiRequest(kbPath(base.id))
      if (alive.current) { const next = { name: latest.name, status: latest.status, revision: latest.revision }; setDraft(next); writeDraft(key, next); setConfirmStatus(false); setNameError('') }
    } catch (failure) { if (alive.current) setError(failure.message) }
    finally { if (alive.current) setBusy(false) }
  }
  async function save(event, confirmed = false) {
    event.preventDefault()
    if (busy) return
    if (!draft.name.trim()) { setNameError('Enter a knowledge base name.'); nameField.current?.focus(); return }
    if (!confirmed && base && draft.status !== base.status && draft.status !== 'ACTIVE') { setConfirmStatus(true); return }
    setBusy(true); setError('')
    try {
      const saved = await platformCommand(base ? kbPath(base.id) : '/v2/admin/knowledge-bases', { name: draft.name, ...(base ? { status: draft.status } : {}) }, draft.revision, base ? 'PATCH' : 'POST')
      if (alive.current) { writeDraft(key, null); onSaved(saved) }
    } catch (failure) { if (alive.current) { setError(failure.message); setBusy(false); setConfirmStatus(false) } }
  }
  return <Dialog title={t(base ? 'Edit knowledge base' : 'Create knowledge base')} description={t('Drafts stay with this account and knowledge base. Closing keeps your draft; reloading a list does not change its reviewed revision.')} closeLabel={t('Close dialog')} initialFocusRef={nameField} busy={busy} onRequestClose={onClose} footer={<>
    <Button disabled={busy} onClick={onClose}>{t('Close and keep draft')}</Button>
    <Button variant="primary" type="submit" form={formId} busy={busy}>{t(base ? 'Save knowledge base settings' : 'Create knowledge base')}</Button>
  </>}>
    <form id={formId} onSubmit={save} noValidate className="cs-stack">
      {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
      {busy && <p role="status" className="cs-caption">{t('Saving...')}</p>}
      <fieldset className="cs-fieldset cs-stack" disabled={busy}>
        <FormField label={t(base ? 'Knowledge base name' : 'New knowledge base name')} required hint={t('Choose a clear name for this collection of reviewed sources.')} error={nameError && t(nameError)}>
          <input ref={nameField} maxLength={180} value={draft.name} onChange={event => edit('name', event.target.value)} />
        </FormField>
        {base && <FormField label={t('Knowledge visibility status')} hint={t('Hiding or archiving this knowledge base can revoke access to its sources.')}><select value={draft.status} onChange={event => edit('status', event.target.value)}>{['ACTIVE', 'HIDDEN', 'ARCHIVED'].map(status => <option key={status} value={status}>{t(status === 'ACTIVE' ? 'Active' : status === 'HIDDEN' ? 'Hidden' : 'Archived')}</option>)}</select></FormField>}
        {confirmStatus && <FeedbackPanel kind="warning" title={t('Confirm visibility change')} action={<Button variant="danger" onClick={event => save(event, true)}>{t('Confirm visibility change')}</Button>}>{t('Changing availability can revoke access to current sources and results.')}</FeedbackPanel>}
        {base && <div className="cs-disclosure"><p className="cs-caption">{t('Revision')}: {draft.revision}</p><Button variant="quiet" onClick={discard}>{t('Discard draft and use refreshed version')}</Button></div>}
      </fieldset>
    </form>
  </Dialog>
}
