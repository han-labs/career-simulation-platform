import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { draftKey, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import KnowledgeBaseDialog from './KnowledgeBaseDialog.jsx'
import PlatformNav from '../agentPlatform/PlatformNav.jsx'
import KnowledgeBaseTestPanel from './KnowledgeBaseTestPanel.jsx'
import KnowledgeDocumentInspector from './KnowledgeDocumentInspector.jsx'
import { kbPath, platformCommand, uploadSource } from '../agentPlatform/platformApi.js'

export default function KnowledgeStudioPage() {
  const { t } = useI18n(), [search, setSearch] = useSearchParams()
  const [me, setMe] = useState(null), [bases, setBases] = useState([]), [options, setOptions] = useState(null)
  const [error, setError] = useState(''), [loading, setLoading] = useState(true), [creating, setCreating] = useState(false)
  const alive = useRef(true), request = useRef(0)
  const load = useCallback(async () => {
    const token = ++request.current
    try {
      const [user, list, choices] = await Promise.all([apiRequest('/v2/me'), apiRequest('/v2/admin/knowledge-bases'), apiRequest('/v2/admin/agent-platform/options')])
      if (alive.current && token === request.current) { setMe(user); setBases(list); setOptions(choices); setError('') }
    } catch (e) { if (alive.current && token === request.current) setError(e.message) }
    finally { if (alive.current && token === request.current) setLoading(false) }
  }, [])
  useEffect(() => { alive.current = true; Promise.resolve().then(load); return () => { alive.current = false } }, [load])
  const selected = search.get('kb')
  return <section className="feature-page section-shell core-flow"><PlatformNav /><h1>{t('Knowledge Base Studio')}</h1><p>{t('Manage reviewed platform knowledge. Student private uploads and enterprise knowledge are unavailable here.')}</p>
    {error && <p role="alert">{t(error)}</p>}{loading && <p role="status">{t('Loading knowledge base...')}</p>}
    <div className="platform-grid"><aside><h2>{t('Knowledge bases')}</h2><button disabled={me?.role !== 'ADMIN'} onClick={() => setCreating(true)}>{t('Create knowledge base')}</button><button onClick={load}>{t('Refresh knowledge bases')}</button>
      {!loading && !error && !bases.length && <p>{t('No knowledge bases yet. Create an empty knowledge base to begin.')}</p>}
      <ul className="platform-list">{bases.map(b => <li key={b.id}><button type="button" aria-pressed={selected === b.id} onClick={() => setSearch({ kb: b.id })}>{b.name} · {b.status}</button></li>)}</ul>
    </aside><div>{selected && me && options ? <KnowledgeWorkspace key={`${me.id}:${selected}`} owner={me.id} id={selected} options={options} onChanged={load} /> : <p>{t('Choose a knowledge base to inspect sources or run an independent test.')}</p>}</div></div>
    {creating && me && <KnowledgeBaseDialog owner={me.id} onClose={() => setCreating(false)} onSaved={saved => { setCreating(false); setBases(rows => [...rows.filter(b => b.id !== saved.id), saved]); setSearch({ kb: saved.id }); load() }} />}
  </section>
}

function KnowledgeWorkspace({ owner, id, options, onChanged }) {
  const { t, locale } = useI18n()
  const alive = useRef(true), refreshRequest = useRef(0)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  const [base, setBase] = useState(null), [editing, setEditing] = useState(false), [tab, setTab] = useState('documents'), [sourceEpoch, setSourceEpoch] = useState(0), [confirming, setConfirming] = useState(null)
  const [versions, setVersions] = useState([]), [jobs, setJobs] = useState([]), [inspected, setInspected] = useState(null), [selection, setSelection] = useState(null)
  const key = draftKey(owner, 'kb-upload', id)
  const [metadata, setMetadata] = useState(() => readDraft(key) ?? { assetKey: '', title: '', license: '', sourceUri: '', language: locale === 'vi' ? 'VI' : 'EN', track: '', reviewed: false, requestId: crypto.randomUUID() })
  const [file, setFile] = useState(null), [fileSelection, setFileSelection] = useState(0), [busy, setBusy] = useState(false), [ingesting, setIngesting] = useState(null), [error, setError] = useState(''), [notice, setNotice] = useState(''), [receipt, setReceipt] = useState(null), [uploadOpen, setUploadOpen] = useState(false)
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
  function edit(field, value) { setReceipt(null); const next = { ...metadata, [field]: value, requestId: crypto.randomUUID() }; setMetadata(next); writeDraft(key, next) }
  async function action(work) {
    setBusy(true); setError(''); setNotice('')
    try { await work(); if (!alive.current) return; setSourceEpoch(n => n + 1); await refresh(); if (alive.current) await onChanged() }
    catch (e) { if (alive.current) setError(e.message) }
    finally { if (alive.current) setBusy(false) }
  }
  async function upload(event) {
    event.preventDefault(); if (!file || receipt) return
    await action(async () => {
      const job = await uploadSource(id, file, { ...metadata, sourceUri: metadata.sourceUri || null })
      if (alive.current) { setReceipt(job); setNotice('Source staged privately. Run ingestion, inspect it, then publish explicitly.') }
    })
  }
  async function ingest(job) {
    setIngesting(job.id); setError(''); setNotice('')
    try {
      const result = await platformCommand(`${kbPath(id)}/ingestions/${job.id}/retry`, {})
      if (!alive.current) return
      const last = result.attempts?.at(-1)
      setNotice((last?.outcome === 'UNCHANGED' || ['READY', 'PUBLISHED'].includes(job.state)) ? 'Index unchanged. The existing version was reused; no rebuild or new version was created.' : result.state === 'READY' ? 'Ingestion ready. Inspect and explicitly publish the version.' : result.state === 'PUBLISHED' ? 'Index unchanged. The existing version was reused; no rebuild or new version was created.' : result.errorCode || result.state)
      await refresh()
    } catch (e) { if (alive.current) setError(e.message) }
    finally { if (alive.current) setIngesting(null) }
  }
  function replace(version) {
    setTab('documents'); setFile(null); setFileSelection(n => n + 1); setReceipt(null); setUploadOpen(true)
    const next = { assetKey: version.assetKey, title: version.title, license: version.license, sourceUri: version.sourceUri ?? '', language: version.language ?? 'EN', track: version.provenance?.track ?? '', reviewed: false, requestId: crypto.randomUUID() }
    setMetadata(next); writeDraft(key, next)
    setNotice('Select the source file again to replace or check its index. An identical valid fingerprint remains unchanged.')
  }
  async function lifecycle() {
    const selected = confirming; setConfirming(null)
    await action(async () => {
      await platformCommand(`${kbPath(id)}/assets/${encodeURIComponent(selected.version.assetKey)}/versions/${selected.version.id}/${selected.kind}`, selected.kind === 'publish' ? { confirmed: true } : {}, selected.kind === 'publish' ? selected.revision : undefined)
      if (alive.current) { setInspected(null); setSelection(null) }
    })
  }
  const reviewed = useCallback(value => setInspected(value), [])
  function inspect(version) { setInspected(null); setSelection({ kind: 'version', id: version.id, assetKey: version.assetKey, revision: base.revision, inspectionId: crypto.randomUUID() }) }
  if (!base) return <p role={error ? 'alert' : 'status'}>{error ? t(error) : t('Loading knowledge base...')}</p>
  return <><h2>{base.name}</h2><p>{base.status} · {t('Publication revision')}: {base.publicationRevision}</p>{error && <p role="alert">{t(error)}</p>}{notice && <p role="status">{t(notice)}</p>}
    <button onClick={() => setEditing(true)}>{t('Edit knowledge base')}</button>
    {editing && <KnowledgeBaseDialog owner={owner} base={base} onClose={() => setEditing(false)} onSaved={saved => { setBase(saved); setEditing(false); setSourceEpoch(n => n + 1); setInspected(null); setSelection(null); onChanged() }} />}
    <div className="platform-actions" role="group" aria-label={t('Knowledge workspace views')}><button aria-pressed={tab === 'documents'} onClick={() => setTab('documents')}>{t('Documents')}</button><button aria-pressed={tab === 'test'} onClick={() => setTab('test')}>{t('Test KB')}</button></div>
    <div hidden={tab !== 'documents'}>
    <details className="platform-card" open={uploadOpen || (!versions.length && !jobs.length)} onToggle={e => setUploadOpen(e.currentTarget.open)}><summary>{t('Upload a reviewed source')}</summary><p>{t('Only reviewed public or synthetic content is allowed. Ingestion sends source text to the configured embedding provider; answer tests send the query and retrieved passages.')}</p><form onSubmit={upload}><fieldset disabled={busy || base.status !== 'ACTIVE'}><label>{t('Source file')}<input key={fileSelection} type="file" accept=".txt,.md,.pdf,.docx,.xlsx" required onChange={e => { setFile(e.target.files?.[0] ?? null); setReceipt(null); const next = { ...metadata, reviewed: false, requestId: crypto.randomUUID() }; setMetadata(next); writeDraft(key, next) }} /></label><p>{t('File selection is not stored. Metadata drafts are saved for this account.')}</p><label>{t('Stable asset key')}<input value={metadata.assetKey} onChange={e => edit('assetKey', e.target.value)} pattern="[a-z0-9][a-z0-9-]{0,99}" required maxLength={100} /></label><label>{t('Source title')}<input required value={metadata.title} onChange={e => edit('title', e.target.value)} maxLength={240} /></label><label>{t('Source license or permission')}<input required value={metadata.license} onChange={e => edit('license', e.target.value)} maxLength={240} /></label><label>{t('Source HTTPS reference (optional)')}<input type="url" value={metadata.sourceUri} onChange={e => edit('sourceUri', e.target.value)} maxLength={600} /></label><label>{t('Content language')}<select value={metadata.language} onChange={e => edit('language', e.target.value)}><option>EN</option><option>VI</option></select></label><label>{t('Career track code')}<input required value={metadata.track} onChange={e => edit('track', e.target.value)} maxLength={60} /></label><label><input type="checkbox" required checked={metadata.reviewed} onChange={e => edit('reviewed', e.target.checked)} />{t('I reviewed this content and confirm the rights and approved external processing described above.')}</label><button className="button" disabled={Boolean(receipt)}>{t('Stage source upload')}</button></fieldset></form></details>
    <section className="platform-card"><h3>{t('Ingestion jobs')}</h3>{!jobs.length && <p>{t('No source has been uploaded to this knowledge base.')}</p>}<ul className="platform-list">{jobs.map(job => <li key={job.id}>{jobs.find(j => j.assetKey === job.assetKey)?.id === job.id && <strong>{t('Latest ingestion attempt')}</strong>}<p>{job.assetKey} · {job.state} · <code>{job.id}</code>{job.errorCode ? ` · ${job.errorCode}` : ''} · {t('Failed stage')}: {job.attempts?.at(-1)?.stage ?? t('Not reported')}</p><div className="platform-actions">{['QUEUED', 'FAILED', 'CANCELLED', 'READY', 'PUBLISHED'].includes(job.state) && <button type="button" disabled={Boolean(ingesting) || busy || base.status !== 'ACTIVE'} onClick={() => ingest(job)}>{t(['READY', 'PUBLISHED'].includes(job.state) ? 'Check existing ingestion' : 'Run or retry ingestion')}</button>}{['QUEUED', 'PARSING', 'EMBEDDING', 'FAILED'].includes(job.state) && <button type="button" onClick={() => action(() => platformCommand(`${kbPath(id)}/ingestions/${job.id}/cancel`, {}))}>{t('Cancel ingestion')}</button>}</div><button onClick={() => setSelection({ kind: 'attempt', id: job.id, revision: job.revision, inspectionId: crypto.randomUUID() })}>{t('Inspect ingestion attempt')}</button></li>)}</ul></section>
    <section className="platform-card"><h3>{t('Source versions')}</h3><button type="button" disabled={busy} onClick={() => action(async () => {})}>{t('Reload knowledge base')}</button><ul className="platform-list">{versions.map(v => <li key={v.id}><strong>{v.title}</strong>{v.status === 'PUBLISHED' && <p>{t('Currently published version')}: {v.versionNumber}</p>}<p>{t('Latest ingestion attempt')}: {jobs.find(j => j.assetKey === v.assetKey)?.state ?? t('Not reported')}</p><p>{v.assetKey} · {t('Version')} {v.versionNumber} · {v.status} · {v.mimeType}</p><div className="platform-actions"><button type="button" disabled={busy || base.status !== 'ACTIVE'} onClick={() => replace(v)}>{t('Replace or check unchanged index')}</button><button type="button" onClick={() => inspect(v)}>{t('Inspect extracted version')}</button><button type="button" disabled={busy || v.status !== 'READY' || base.status !== 'ACTIVE' || inspected?.id !== v.id || selection?.kind !== 'version' || selection.id !== v.id} onClick={() => setConfirming({ kind: 'publish', version: v, revision: inspected.revision })}>{t('Publish inspected source version')}</button><button type="button" disabled={busy || v.status === 'RETIRED'} onClick={() => setConfirming({ kind: 'retire', version: v })}>{t('Retire source version')}</button></div></li>)}</ul>{selection && <KnowledgeDocumentInspector key={selection.inspectionId} kb={id} selection={selection} onReviewed={reviewed} />}</section>
    </div>
    {confirming && <section className="platform-card" role="group" aria-label={t('Confirm source lifecycle action')}><p>{t(confirming.kind === 'publish' ? 'Publish this reviewed version?' : 'Retire this source? Current citations will become unavailable.')} · {confirming.version.assetKey} · {confirming.version.id}</p><button disabled={busy} onClick={lifecycle}>{t('Confirm source action')}</button><button onClick={() => setConfirming(null)}>{t('Keep current publication')}</button></section>}
    <div hidden={tab !== 'test'}><KnowledgeBaseTestPanel key={`${id}:${sourceEpoch}`} owner={owner} id={id} options={options} active={base.status === 'ACTIVE'} /></div>
  </>
}
