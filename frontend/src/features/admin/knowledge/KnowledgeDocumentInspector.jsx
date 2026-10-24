import { useEffect, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import SourcePassages from '../agentPlatform/SourcePassages.jsx'
import { kbPath } from '../agentPlatform/platformApi.js'

// Parent keys this inspector by KB + exact version/job. Late reads cannot replace another selection.
export default function KnowledgeDocumentInspector({ kb, selection, onReviewed }) {
  const { t } = useI18n(), [detail, setDetail] = useState(null), [error, setError] = useState(''), [tab, setTab] = useState('text')
  const [generation, setGeneration] = useState(null)
  const { kind, id, assetKey, revision } = selection
  useEffect(() => {
    let active = true
    const path = kind === 'attempt' ? `${kbPath(kb)}/ingestions/${id}` : `${kbPath(kb)}/assets/${encodeURIComponent(assetKey)}/versions/${id}`
    apiRequest(path).then(value => { if (active) { setDetail(value); if (kind === 'version') onReviewed({ id, revision }) } }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [kb, kind, id, assetKey, revision, onReviewed])
  if (!detail) return <section className="platform-card" aria-label={t('Document inspector')}><p role={error ? 'alert' : 'status'}>{t(error || 'Loading document inspection...')}</p></section>
  const attempts = detail.attempts ?? []
  const attempt = attempts.find(a => String(a.generation) === String(generation)) ?? attempts.at(-1)
  const snapshot = kind === 'version' ? detail.provenance?.extractionSnapshot : attempt?.extractionSnapshot
  return <section className="platform-card" aria-label={t('Document inspector')}>
    <h3>{t(kind === 'version' ? 'Source version inspection' : 'Ingestion attempt inspection')}</h3>
    <p><code>{id}</code> · {detail.title ?? detail.assetKey} · {detail.status ?? detail.state}</p>
    {kind === 'attempt' && <><label>{t('Attempt generation')}<select value={attempt?.generation ?? ''} onChange={e => setGeneration(e.target.value)}>{attempts.map(a => <option key={a.generation} value={a.generation}>{a.generation} · {a.stage ?? t('Not reported')}</option>)}</select></label><p>{t('Failed stage')}: {attempt?.stage ?? t('Not reported')} · {attempt?.errorCode ?? detail.errorCode ?? ''}</p></>}
    <div className="platform-actions" role="group" aria-label={t('Inspection views')}><button aria-pressed={tab === 'text'} onClick={() => setTab('text')}>{t('Extracted text')}</button><button aria-pressed={tab === 'chunks'} onClick={() => setTab('chunks')}>{t('Chunks')}</button></div>
    {tab === 'text' ? snapshot ? <><p>{t('Normalized extraction snapshot')} · {snapshot.parserProfile} · {snapshot.textUnit}</p><div className="kb-extracted-text" aria-label={t('Normalized extracted text')}>{snapshot.blocks?.map((b, i) => <p key={i} className="core-prewrap">{b.text}</p>)}</div><details><summary>{t('Extraction locators and warnings')}</summary><ul>{snapshot.blocks?.map((b, i) => <li key={i}>{b.section}<pre>{JSON.stringify(b.locator, null, 2)}</pre></li>)}</ul><pre>{JSON.stringify(snapshot.warnings ?? [], null, 2)}</pre></details></> : <p>{t('No extraction snapshot was recorded for this version or attempt. Chunk text is not the original extraction snapshot.')}</p> : kind === 'version' ? <SourcePassages passages={detail.chunks} /> : <p>{t('Attempt extraction is not an indexed version. Inspect its produced version for stored chunks.')}</p>}
    <details><summary>{t('Version or attempt provenance')}</summary><pre>{JSON.stringify(kind === 'version' ? { ...detail, chunks: undefined, provenance: { ...detail.provenance, extractionSnapshot: undefined } } : { metadata: detail.metadata, generation: attempt?.generation, usage: { ...attempt, extractionSnapshot: undefined } }, null, 2)}</pre></details>
  </section>
}
