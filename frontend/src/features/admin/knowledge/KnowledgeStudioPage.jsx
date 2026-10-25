import { ArrowLeft, ArrowRight, BookOpen, FileText, Plus, RefreshCw, Upload, X } from 'lucide-react'
import Button from '../../../shared/ui/Button.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import Tabs from '../../../shared/ui/Tabs.jsx'
import KnowledgeStatus from './KnowledgeStatus.jsx'
import './knowledgeStudio.css'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import KnowledgeBaseDialog from './KnowledgeBaseDialog.jsx'
import KnowledgeBaseTestPanel from './KnowledgeBaseTestPanel.jsx'
import KnowledgeDocumentInspector from './KnowledgeDocumentInspector.jsx'
import { kbPath, platformCommand, uploadSource } from '../agentPlatform/platformApi.js'
import { groupDocuments, selectSource, sourceFileError, SOURCE_ACCEPT, uploadInput, validUpload } from './knowledgeDocuments.js'

export default function KnowledgeStudioPage() {
  const { t } = useI18n(), [search, setSearch] = useSearchParams()
  const [me, setMe] = useState(null), [bases, setBases] = useState([]), [options, setOptions] = useState(null)
  const [error, setError] = useState(''), [loading, setLoading] = useState(true), [refreshing, setRefreshing] = useState(false), [creating, setCreating] = useState(false), [created, setCreated] = useState(null)
  const alive = useRef(true), request = useRef(0)
  const load = useCallback(async () => {
    const token = ++request.current
    setRefreshing(true)
    try {
      const [user, list, choices] = await Promise.all([apiRequest('/v2/me'), apiRequest('/v2/admin/knowledge-bases'), apiRequest('/v2/admin/agent-platform/options')])
      if (alive.current && token === request.current) { setMe(user); setBases(list); setOptions(choices); setError('') }
    } catch (e) { if (alive.current && token === request.current) setError(e.message) }
    finally { if (alive.current && token === request.current) { setLoading(false); setRefreshing(false) } }
  }, [])
  useEffect(() => { alive.current = true; Promise.resolve().then(load); return () => { alive.current = false } }, [load])
  const selected = search.get('kb')
  const [collectionEdit, setCollectionEdit] = useState(null)
  return <section className="cs-ui knowledge-studio">
    <div className="kb-main">
    {!selected ? <header className="cs-page-header"><div><p className="kb-eyebrow">{t('Knowledge Base Studio')}</p><h1>{t('Knowledge bases')}</h1><p>{t('Manage reviewed platform knowledge. Student private uploads and enterprise knowledge are unavailable here.')}</p></div><div className="cs-actions"><Button onClick={load} busy={refreshing} icon={<RefreshCw size={18} />}>{t('Refresh knowledge bases')}</Button><Button variant="primary" disabled={me?.role !== 'ADMIN'} onClick={() => setCreating(true)} icon={<Plus size={18} />}>{t('Create knowledge base')}</Button></div></header>
      : <Link className="cs-button cs-button--quiet kb-back" to="/admin/knowledge"><ArrowLeft size={18} aria-hidden="true" />{t('Back to knowledge bases')}</Link>}
    {error && <FeedbackPanel kind="error" action={<Button onClick={load}>{t('Refresh knowledge bases')}</Button>}>{t(error)}</FeedbackPanel>}
    {loading && <FeedbackPanel kind="loading">{t('Loading knowledge base...')}</FeedbackPanel>}
    {created && created === selected && <FeedbackPanel kind="success">{t('Knowledge base created.')}</FeedbackPanel>}
    {selected ? me && options && <KnowledgeWorkspace key={me.id + ':' + selected} owner={me.id} id={selected} options={options} onChanged={load} />
      : <div className="kb-collection">
        {!loading && !error && !bases.length && <FeedbackPanel kind="empty" title={t('A place for reviewed knowledge')} action={<Button variant="primary" disabled={me?.role !== 'ADMIN'} onClick={() => setCreating(true)}>{t('Create knowledge base')}</Button>}>{t('No knowledge bases yet. Create an empty knowledge base to begin.')}</FeedbackPanel>}
        {bases.map(base => <article className="cs-surface kb-collection-row" key={base.id}><div className="kb-collection-name"><div className="kb-document-icon" aria-hidden="true"><BookOpen size={22} /></div><div><Link className="kb-item-title" to={'?kb=' + encodeURIComponent(base.id)}>{base.name}</Link><p className="cs-caption">{t('Reviewed platform knowledge')}</p>{base.publicationRevision != null && <p className="cs-caption">{t('Publication revision')}: {base.publicationRevision}</p>}</div></div><KnowledgeStatus value={base.status} /><div className="cs-actions"><Button onClick={() => setCollectionEdit(base)} aria-label={t('Edit knowledge base') + ': ' + base.name}>{t('Edit knowledge base')}</Button><Link className="cs-button cs-button--primary" aria-label={t('Open knowledge base') + ': ' + base.name} to={'?kb=' + encodeURIComponent(base.id)}>{t('Open knowledge base')}<ArrowRight size={18} aria-hidden="true" /></Link></div></article>)}
      </div>}
    {creating && me && <KnowledgeBaseDialog owner={me.id} onClose={() => setCreating(false)} onSaved={saved => { setCreating(false); setCreated(saved.id); setBases(rows => [...rows.filter(base => base.id !== saved.id), saved]); setSearch({ kb: saved.id }); load() }} />}
    {collectionEdit && me && <KnowledgeBaseDialog owner={me.id} base={collectionEdit} onClose={() => setCollectionEdit(null)} onSaved={saved => { setCollectionEdit(null); setBases(rows => rows.map(base => base.id === saved.id ? saved : base)); load() }} />}
    </div>
  </section>
}


function KnowledgeWorkspace({ owner, id, options, onChanged }) {
  const { t } = useI18n()
  const alive = useRef(true), refreshRequest = useRef(0)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  const safeAction = useRef(null), workspaceHeading = useRef(null), sourceFile = useRef(null)
  const [inspectorOpen, setInspectorOpen] = useState(false)
  const [base, setBase] = useState(null), [editing, setEditing] = useState(false), [tab, setTab] = useState('documents'), [sourceEpoch, setSourceEpoch] = useState(0), [confirming, setConfirming] = useState(null)
  useEffect(() => { if (base?.id) workspaceHeading.current?.focus() }, [base?.id])
  const [versions, setVersions] = useState([]), [jobs, setJobs] = useState([]), [inspected, setInspected] = useState(null), [selection, setSelection] = useState(null)
  const key = draftKey(owner, 'kb-upload', id)
  const [metadata, setMetadata] = useState(() => readDraft(key) ?? {})
  const [file, setFile] = useState(null), [busy, setBusy] = useState(false), [ingesting, setIngesting] = useState(null), [error, setError] = useState(''), [notice, setNotice] = useState(''), [fileError, setFileError] = useState(''), [dragging, setDragging] = useState(false)
  const operation = useRef(false), receipt = useRef(metadata.stagedJob ?? null), uploadArea = useRef(null), documentTitle = useRef(null)
  useEffect(() => { if (file && !receipt.current) documentTitle.current?.focus() }, [file])
  const refresh = useCallback(async () => { const token = ++refreshRequest.current; const [b, assets, pending] = await Promise.all([apiRequest(kbPath(id)), apiRequest(`${kbPath(id)}/assets`), apiRequest(`${kbPath(id)}/ingestions`)]); if (alive.current && token === refreshRequest.current) { setBase(b); setVersions(assets); setJobs(pending) }; return b }, [id])
  useEffect(() => { refresh().catch(e => { if (alive.current) setError(e.message) }) }, [refresh])
  const runningJobs = jobs.some(job => ['PARSING', 'EMBEDDING'].includes(job.state))
  useEffect(() => {
    if (!runningJobs && !ingesting) return
    let active = true, timer
    async function observe() {
      try { await refresh() } catch (e) { if (active) setError(e.message) }
      if (active) timer = setTimeout(observe, 1500)
    }
    timer = setTimeout(observe, 1500)
    return () => { active = false; clearTimeout(timer) }
  }, [runningJobs, ingesting, refresh])
  useEffect(() => { const focus = () => { setSourceEpoch(n => n + 1); refresh().catch(e => { if (alive.current) setError(e.message) }) }; window.addEventListener('focus', focus); return () => window.removeEventListener('focus', focus) }, [refresh])
  function edit(field, value) {
    const next = { ...metadata, [field]: value, requestId: field === 'reviewed' || metadata[field] === value ? metadata.requestId : crypto.randomUUID() }
    setMetadata(next); writeDraft(key, next)
  }
  function chooseFiles(files) {
    if (operation.current || base.status !== 'ACTIVE') return
    const problem = sourceFileError(files); setFileError(problem)
    if (problem) return
    const next = selectSource(files[0], metadata)
    receipt.current = next.stagedJob ?? null
    setFile(files[0]); setMetadata(next); writeDraft(key, next)
  }
  function clearFile() { if (operation.current) return; setFile(null); setFileError(''); setMetadata({}); receipt.current = null; writeDraft(key, null); if (sourceFile.current) sourceFile.current.value = '' }
  function rememberJob(job) { setJobs(rows => [job, ...rows.filter(row => row.id !== job.id)]) }
  async function action(work) {
    if (operation.current) return
    operation.current = true
    setBusy(true); setError(''); setNotice('')
    try { await work(); if (!alive.current) return; setSourceEpoch(n => n + 1); await refresh(); if (alive.current) await onChanged() }
    catch (e) { if (alive.current) setError(e.message) }
    finally { operation.current = false; if (alive.current) setBusy(false) }
  }
  async function upload(event) {
    event.preventDefault()
    if (operation.current || receipt.current || !file) return
    if (!validUpload(metadata)) { setFileError('Complete the required document information and source authorization confirmation.'); event.currentTarget.querySelector(':invalid')?.focus(); return }
    operation.current = true; setBusy(true); setError(''); setFileError(''); setNotice('')
    try {
      const job = await uploadSource(id, file, uploadInput(metadata))
      if (!alive.current) return
      receipt.current = job; rememberJob(job)
      const saved = { ...metadata, stagedJob: job }; setMetadata(saved); writeDraft(key, saved)
      setFile(null); if (sourceFile.current) sourceFile.current.value = ''
      uploadArea.current?.focus()
      setNotice('Upload saved. Processing the document...')
      await processJob(job)
      if (alive.current) { setSourceEpoch(n => n + 1); await refresh(); if (alive.current) await onChanged() }
    } catch (e) { if (alive.current) setError(e.message) }
    finally { operation.current = false; if (alive.current) setBusy(false) }
  }
  async function processJob(job) {
    setIngesting(job.id); setError(''); setNotice('')
    try {
      const result = await platformCommand(`${kbPath(id)}/ingestions/${job.id}/retry`, {})
      if (!alive.current) return
      rememberJob(result)
      const last = result.attempts?.at(-1)
      if (result.state === 'FAILED') setError('Processing failed. Your uploaded file is saved. Retry processing from the document list.')
      else setNotice(last?.outcome === 'UNCHANGED' ? 'Index unchanged. The existing version was reused; no rebuild or new version was created.' : result.state === 'READY' ? 'Document ready. Review it before publishing.' : result.state === 'PUBLISHED' ? 'This document is already published.' : 'The document is waiting or processing. Its status will update here.')
      return result
    } catch (e) { if (alive.current) { setError('The processing request could not finish. Your uploaded file is saved. Refresh or retry from the document list.'); setNotice(e.message) } }
    finally { if (alive.current) setIngesting(null) }
  }
  async function ingest(job) {
    if (operation.current) return
    operation.current = true; setBusy(true)
    try { await processJob(job); if (alive.current) { setSourceEpoch(n => n + 1); await refresh(); if (alive.current) await onChanged() } }
    catch (e) { if (alive.current) setError(e.message) }
    finally { operation.current = false; if (alive.current) setBusy(false) }
  }
  function replace(version) {
    setTab('documents'); setFile(null); receipt.current = null
    const next = { replacementKey: version.assetKey, assetKey: version.assetKey, title: version.title, license: version.license ?? '', sourceUri: version.sourceUri ?? '', language: version.language ?? '', track: version.provenance?.track ?? '', reviewed: false, requestId: crypto.randomUUID() }
    setMetadata(next); writeDraft(key, next)
    setNotice('Choose the replacement file above. Published content stays in use until you review and publish a new version.')
    if (sourceFile.current) sourceFile.current.value = ''
    uploadArea.current?.scrollIntoView?.({ block: 'start' }); sourceFile.current?.focus()
  }
  async function lifecycle() {
    const selected = confirming
    await action(async () => {
      if (selected.kind === 'cancel') { await platformCommand(`${kbPath(id)}/ingestions/${selected.job.id}/cancel`, {}); return }
      await platformCommand(`${kbPath(id)}/assets/${encodeURIComponent(selected.version.assetKey)}/versions/${selected.version.id}/${selected.kind}`, selected.kind === 'publish' ? { confirmed: true } : {}, selected.kind === 'publish' ? selected.revision : undefined)
      if (alive.current) { setInspected(null); setSelection(null); setInspectorOpen(false) }
    })
    if (alive.current) setConfirming(null)
  }
  const reviewed = useCallback(value => setInspected(value), [])
  function inspect(version) { setInspectorOpen(true); setInspected(null); setSelection({ kind: 'version', id: version.id, assetKey: version.assetKey, revision: base.revision, inspectionId: crypto.randomUUID() }) }
  if (!base) return <FeedbackPanel kind={error ? 'error' : 'loading'}>{error ? t(error) : t('Loading knowledge base...')}</FeedbackPanel>
  const rows = groupDocuments(versions, jobs, base.id)
  const publishVersion = selection?.kind === 'version' ? versions.find(version => version.id === selection.id && version.status === 'READY') : null
  const statusLabels = { QUEUED: 'Waiting to process', PARSING: 'Processing', EMBEDDING: 'Processing', READY: 'Ready for review', PUBLISHED: 'Published', FAILED: 'Failed', CANCELLED: 'Cancelled', RETIRED: 'Retired' }
  const documents = <div className="cs-stack">
    <section ref={uploadArea} tabIndex={-1} aria-label={t('Upload document')} className="cs-stack">
      <div><h2>{t('Documents')}</h2><p className="cs-caption">{t('Upload a document, then review it before publishing. Existing published content stays available during processing.')}</p></div>
      <div className={'kb-dropzone' + (dragging ? ' kb-dropzone--active' : '')} aria-disabled={busy || base.status !== 'ACTIVE'}
        onDragOver={event => { event.preventDefault(); if (!busy && base.status === 'ACTIVE') setDragging(true) }}
        onDragLeave={event => { if (!event.currentTarget.contains(event.relatedTarget)) setDragging(false) }}
        onDrop={event => { event.preventDefault(); setDragging(false); chooseFiles([...event.dataTransfer.files]) }}>
        <div className="kb-upload-symbol" aria-hidden="true"><Upload size={32} /></div>
        <div><h3>{t('Drop a document here')}</h3><p className="cs-caption">{t('TXT, MD, PDF, DOCX or XLSX. One file per upload, up to 8 MiB (8,388,608 bytes).')}</p></div>
        <input ref={sourceFile} className="kb-native-file" type="file" aria-label={t('Source file')} accept={SOURCE_ACCEPT} disabled={busy || base.status !== 'ACTIVE'} onChange={event => chooseFiles([...event.target.files])} />
        <Button variant={file ? 'secondary' : 'primary'} disabled={busy || base.status !== 'ACTIVE'} onClick={() => sourceFile.current?.click()}>{t(file ? 'Choose another file' : 'Browse files')}</Button>
        {busy && <p role="status">{t(ingesting ? 'Processing document...' : 'Uploading document...')}</p>}
      </div>
      {fileError && <FeedbackPanel kind="error">{t(fileError)}</FeedbackPanel>}
      {metadata.replacementKey && <FeedbackPanel kind="info" live={false}>{t('Replacing an existing document. Its published version remains available until a new version is explicitly published.')}</FeedbackPanel>}
      {file && <div className="cs-surface cs-stack">
        <div className="cs-section-header"><div><h3>{file.name}</h3><p className="cs-caption">{file.size.toLocaleString()} {t('bytes')}</p></div><Button variant="quiet" disabled={busy} aria-label={t('Clear file selection')} icon={<X size={18} />} onClick={clearFile} /></div>
        {receipt.current ? <FeedbackPanel kind="info">{t('This file is already uploaded. Use the document list to retry processing.')}</FeedbackPanel> :
        <form onSubmit={upload} noValidate className="cs-stack">
          <fieldset className="cs-fieldset cs-stack" disabled={busy || base.status !== 'ACTIVE'}>
            <div className="cs-fields">
              <FormField label={t('Document title')} required><input ref={documentTitle} maxLength={240} value={metadata.title ?? ''} onChange={event => edit('title', event.target.value)} /></FormField>
              <FormField label={t('Career area')} required hint={t('For example: BACKEND or DATA. Enter the career area covered by this document.')}><input maxLength={60} value={metadata.track ?? ''} onChange={event => edit('track', event.target.value)} /></FormField>
              <FormField label={t('Usage rights or license')} required hint={t('State the license or permission that allows this content to be used. No permission is assumed.')}><input maxLength={240} value={metadata.license ?? ''} onChange={event => edit('license', event.target.value)} /></FormField>
              <FormField label={t('Document language')} required hint={t('Choose the language of the document. It is not detected automatically.')}><select value={metadata.language ?? ''} onChange={event => edit('language', event.target.value)}><option value="">{t('Choose a language')}</option><option value="EN">English (EN)</option><option value="VI">Tiếng Việt (VI)</option></select></FormField>
            </div>
            <details className="cs-disclosure"><summary>{t('Additional information (optional)')}</summary><FormField label={t('Source HTTPS reference (optional)')}><input type="url" maxLength={600} value={metadata.sourceUri ?? ''} onChange={event => edit('sourceUri', event.target.value)} /></FormField></details>
            <p className="cs-caption">{t('Only reviewed public or synthetic content is allowed. Ingestion sends source text to the configured embedding provider; answer tests send the query and retrieved passages.')}</p>
            <label className="cs-choice"><input type="checkbox" required checked={Boolean(metadata.reviewed)} onChange={event => edit('reviewed', event.target.checked)} /><span>{t('I reviewed this content and confirm the rights and approved external processing described above.')}</span></label>
            <div className="cs-actions"><Button variant="primary" type="submit" busy={busy}>{t('Upload and process')}</Button></div>
          </fieldset>
        </form>}
      </div>}
    </section>
    <section className="cs-surface" aria-label={t('Document list')}>
      <div className="cs-section-header"><h3>{t('Your documents')}</h3></div>
      {!rows.length && <FeedbackPanel kind="empty">{t('No documents yet. Upload a file to begin.')}</FeedbackPanel>}
      <ul className="kb-document-list">{rows.map(row => {
        const retryable = row.job && ['QUEUED', 'FAILED', 'CANCELLED'].includes(row.job.state)
        return <li className="kb-document-row" key={row.assetKey} aria-label={t('Document') + ': ' + row.name}>
          <div className="kb-document-name"><div className="kb-document-icon" aria-hidden="true"><FileText size={22} /></div><div><h4>{row.name}</h4>
            {row.published && <p className="cs-caption">{t('Published version')}: {row.published.versionNumber}</p>}
            {row.published && ['FAILED', 'CANCELLED'].includes(row.state) && <p className="kb-document-warning">{t('Replacement did not complete. The published version is still in use.')}</p>}
          </div></div>
          <KnowledgeStatus value={row.state} label={statusLabels[row.state]} />
          <div className="cs-actions kb-document-controls">
            {retryable ? <Button disabled={busy || base.status !== 'ACTIVE'} busy={ingesting === row.job.id} onClick={() => ingest(row.job)}>{t(row.job.state === 'QUEUED' ? 'Process document' : 'Retry processing')}</Button>
              : row.ready && !['PARSING', 'EMBEDDING'].includes(row.state) ? <Button variant="primary" disabled={busy || base.status !== 'ACTIVE'} onClick={() => inspect(row.ready)}>{t('Review and publish')}</Button>
              : row.latest && <Button disabled={busy} onClick={() => inspect(row.published ?? row.latest)}>{t('View document')}</Button>}
            <details className="kb-document-options"><summary>{t('Document options')}</summary><div className="kb-document-menu">
              {row.latest && <><Button variant="quiet" disabled={busy || base.status !== 'ACTIVE'} onClick={() => replace(row.published ?? row.latest)}>{t('Replace document')}</Button><FormField label={t('Inspect another version')}><select value="" disabled={busy} onChange={event => { const version = row.versions.find(item => item.id === event.target.value); if (version) inspect(version) }}><option value="">{t('Choose a version')}</option>{row.versions.map(version => <option key={version.id} value={version.id}>{t('Version')} {version.versionNumber ?? version.id} · {t(statusLabels[version.status] ?? version.status)}</option>)}</select></FormField></>}
              {row.job && <Button variant="quiet" onClick={() => { setInspected(null); setSelection({ kind: 'attempt', id: row.job.id, revision: row.job.revision, inspectionId: crypto.randomUUID() }); setInspectorOpen(true) }}>{t('Inspect processing details')}</Button>}
              {row.job && ['QUEUED', 'PARSING', 'EMBEDDING', 'FAILED'].includes(row.job.state) && <Button disabled={busy} onClick={() => setConfirming({ kind: 'cancel', job: row.job })}>{t('Cancel ingestion')}</Button>}
              {row.job && ['READY', 'PUBLISHED'].includes(row.job.state) && <Button disabled={busy || base.status !== 'ACTIVE'} onClick={() => ingest(row.job)}>{t('Check existing ingestion')}</Button>}
              <details className="cs-disclosure"><summary>{t('Technical details')}</summary><p>{t('Stable asset key')}: <code>{row.assetKey}</code></p>{row.job && <><p>{t('Attempt details')}: <code>{row.job.id}</code></p><p>{row.job.errorCode ?? ''} {row.job.attempts?.at(-1)?.stage ?? ''}</p></>}</details>
            </div></details>
          </div>
        </li>
      })}</ul>
    </section>
  </div>
  return <div className="cs-stack">
    <header className="cs-page-header"><div><p className="kb-eyebrow">{t('Knowledge Base Studio')}</p><h1 ref={workspaceHeading} tabIndex={-1}>{base.name}</h1><div className="cs-actions kb-workspace-meta"><KnowledgeStatus value={base.status} /></div></div><div className="cs-actions"><Button icon={<RefreshCw size={18} />} disabled={busy} onClick={() => action(async () => {})}>{t('Reload knowledge base')}</Button><Button disabled={busy} onClick={() => setEditing(true)}>{t('Edit knowledge base')}</Button></div></header>
    {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
    {notice && <FeedbackPanel kind="info" live={false}>{t(notice)}</FeedbackPanel>}
    {base.status !== 'ACTIVE' && <FeedbackPanel kind="warning">{t('Knowledge base unavailable.')}</FeedbackPanel>}
    {editing && <KnowledgeBaseDialog owner={owner} base={base} onClose={() => setEditing(false)} onSaved={saved => { setBase(saved); setEditing(false); setNotice('Knowledge base updated.'); setSourceEpoch(number => number + 1); setInspected(null); setSelection(null); setInspectorOpen(false); onChanged() }} />}
    <Tabs label={t('Knowledge workspace views')} value={tab} onChange={setTab} items={[
      { value: 'documents', label: t('Documents'), content: documents },
      { value: 'test', label: t('Test KB'), content: <KnowledgeBaseTestPanel key={id + ':' + sourceEpoch} owner={owner} id={id} options={options} active={base.status === 'ACTIVE'} /> },
    ]} />
    {selection && inspectorOpen && <KnowledgeDocumentInspector key={selection.inspectionId} kb={id} selection={selection} onReviewed={reviewed} onClose={() => setInspectorOpen(false)} publishDisabled={busy || base.status !== 'ACTIVE' || inspected?.id !== selection.id} onPublish={publishVersion ? () => { setInspectorOpen(false); setConfirming({ kind: 'publish', version: publishVersion, revision: inspected.revision }) } : undefined} onRetire={selection.kind === 'version' && versions.find(version => version.id === selection.id && version.status !== 'RETIRED') ? () => { setInspectorOpen(false); setConfirming({ kind: 'retire', version: versions.find(version => version.id === selection.id) }) } : undefined} />}
    {confirming && <Dialog title={t(confirming.kind === 'publish' ? 'Publish this reviewed version?' : confirming.kind === 'retire' ? 'Retire this source? Current citations will become unavailable.' : 'Cancel ingestion')} description={t(confirming.kind === 'publish' ? 'Publishing replaces the currently published content only after this reviewed version is accepted.' : confirming.kind === 'retire' ? 'Changing availability can revoke access to current sources and results.' : 'Cancellation preserves the current publication and stops this attempt.')} closeLabel={t('Close dialog')} busy={busy} initialFocusRef={safeAction} restoreFocusRef={workspaceHeading} onRequestClose={() => setConfirming(null)} footer={<>
      <Button ref={safeAction} disabled={busy} onClick={() => setConfirming(null)}>{t('Keep current publication')}</Button><Button variant={confirming.kind === 'publish' ? 'primary' : 'danger'} busy={busy} onClick={lifecycle}>{t('Confirm source action')}</Button>
    </>}><dl className="cs-metadata"><div><dt>{t('Stable asset key')}</dt><dd>{confirming.version?.assetKey ?? confirming.job.assetKey}</dd></div>{confirming.version && <div><dt>{t('Version')}</dt><dd>{confirming.version.versionNumber} · <code>{confirming.version.id}</code></dd></div>}</dl></Dialog>}
  </div>
}
