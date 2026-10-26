import { useEffect, useRef, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { kbPath } from './platformApi.js'
import Button from '../../../shared/ui/Button.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import StatusBadge from '../../../shared/ui/StatusBadge.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
export default function SourcePassages({ passages = [], citations = [], onUnavailable, presentation = 'legacy', showExcerpts = true }) {
  const { t } = useI18n(), [inspected, setInspected] = useState(null), [error, setError] = useState(''), [unavailable, setUnavailable] = useState([])
  const alive = useRef(true), sequence = useRef(0)
  const [opened, setOpened] = useState(null), selectedSource = useRef(null)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  async function inspect(p) {
    const token = ++sequence.current; setInspected(null); setError('')
    try {
      const source = await apiRequest(`${kbPath(p.knowledgeBaseId)}/sources/${p.chunkId}`)
      if (alive.current && token === sequence.current) {
        if (source.chunkId !== p.chunkId || source.knowledgeBaseId !== p.knowledgeBaseId || source.versionId !== p.versionId || source.text !== p.text) throw new Error('This knowledge source is no longer available.')
        setInspected(source)
      }
    } catch (failure) {
      if (alive.current && token === sequence.current) { setError(failure.message); setUnavailable(ids => [...ids, p.chunkId]); onUnavailable?.() }
    }
  }
  useEffect(() => {
    if (presentation !== 'citations') return undefined
    const revalidate = () => { if (selectedSource.current) inspect(selectedSource.current) }
    window.addEventListener('focus', revalidate)
    return () => window.removeEventListener('focus', revalidate)
  })
  const currentInspection = inspected && passages.some(p => p.chunkId === inspected.chunkId && p.knowledgeBaseId === inspected.knowledgeBaseId && p.versionId === inspected.versionId && p.text === inspected.text) ? inspected : null
  if (presentation === 'citations') return <div className="agent-citations" aria-label={t('Answer sources')}>
    {citations.map(citation => {
      const p = passages.find(p => p.referenceId === citation.referenceId)
      const available = p && !unavailable.includes(p.chunkId)
      return <div key={citation.referenceId} className="agent-citation"><Button variant="quiet" disabled={!available} onClick={() => { selectedSource.current = p; setOpened(p); inspect(p) }}>{available ? p.title + ' · ' + citation.referenceId : t('Source unavailable')}</Button>{available && showExcerpts && <details className="agent-citation-excerpt"><summary>{t('Supporting excerpt')}</summary><blockquote><q>{citation.quote}</q></blockquote></details>}</div>
    })}
    {opened && <Dialog title={t('Source evidence')} closeLabel={t('Close dialog')} onRequestClose={() => { setOpened(null); setInspected(null); selectedSource.current = null; sequence.current += 1 }}>
      {error ? <FeedbackPanel kind="error" title={t('Source unavailable')}>{t('This knowledge source is no longer available.')}</FeedbackPanel> : !currentInspection ? <FeedbackPanel kind="loading">{t('Resolving the current source...')}</FeedbackPanel> : <article className="cs-stack"><h3>{currentInspection.title}</h3><p className="cs-caption">{t('Source version')}: {currentInspection.versionId} · {currentInspection.section}</p><p className="cs-prose">{currentInspection.text}</p><dl className="cs-metadata">{Object.entries(currentInspection.sourceLocator ?? {}).filter(([,value]) => typeof value !== 'object').map(([key,value]) => <div key={key}><dt>{t(key)}</dt><dd>{String(value)}</dd></div>)}</dl><p className="cs-caption">{t('Embedding profile')}: {currentInspection.embeddingProfile ?? t('Not reported')}</p></article>}
    </Dialog>}
  </div>
  if (presentation === 'studio') return <section aria-label={t('Retrieved source passages')} className="cs-stack">
    <h3>{t('Retrieved source passages')}</h3>
    {!passages.length && <FeedbackPanel kind="empty">{t('No authorized passage was retrieved.')}</FeedbackPanel>}
    <ol className="kb-passage-list">{passages.filter(p => !unavailable.includes(p.chunkId)).map((p, index) => <li className="kb-passage" key={p.versionId + '-' + p.chunkId}>
      <header><StatusBadge tone="info">{p.referenceId ?? index + 1}</StatusBadge><strong>{p.title}</strong></header>
      <p className="cs-caption">{t('Source version')}: {p.versionId} · {p.section}</p>
      <p className="cs-prose">{p.text}</p>
      {p.retrievalMethod !== 'INSPECTION' && <div className="cs-actions"><Button onClick={() => inspect(p)}>{t('Inspect current source')}</Button></div>}
      <details className="cs-disclosure"><summary>{t('Source locator and profile')}</summary><dl className="cs-metadata">
        <div><dt>{t('Retrieval score')}</dt><dd>{p.score ?? t('Not reported')}</dd></div><div><dt>{t('Retrieval method')}</dt><dd>{p.retrievalMethod}</dd></div>
        {Object.entries(p.sourceLocator ?? {}).filter(([, value]) => typeof value !== 'object').map(([key, value]) => <div key={key}><dt>{t(key)}</dt><dd>{String(value)}</dd></div>)}
        <div><dt>{t('Embedding profile')}</dt><dd>{p.embeddingProfile ?? t('Not reported')}</dd></div>
      </dl><details className="cs-disclosure"><summary>{t('Technical details')}</summary><pre tabIndex={0} aria-label={t('Source locator and profile')}>{JSON.stringify(p.sourceLocator, null, 2)}</pre></details></details>
    </li>)}</ol>
    {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}
    {currentInspection && <article className="kb-source-inspection cs-stack" aria-label={t('Current source inspection')}><h4>{currentInspection.title}</h4><p className="cs-caption">{t('Source version')}: {currentInspection.versionId}</p><p className="cs-prose">{currentInspection.text}</p></article>}
  </section>
  return <section aria-label={t('Retrieved source passages')}><h3>{t('Retrieved source passages')}</h3>{!passages.length && <p>{t('No authorized passage was retrieved.')}</p>}<ol className="platform-passages">{passages.filter(p => !unavailable.includes(p.chunkId)).map(p => <li key={`${p.versionId}-${p.chunkId}`}><strong>[{p.referenceId}] {p.title}</strong><p>{t('Source version')}: {p.versionId} · {p.section} · {p.retrievalMethod} · {t('Retrieval score')}: {p.score}</p><p className="core-prewrap">{p.text}</p><details><summary>{t('Source locator and profile')}</summary><pre>{JSON.stringify(p.sourceLocator, null, 2)}</pre><code>{p.embeddingProfile}</code></details>{p.retrievalMethod !== 'INSPECTION' && <button type="button" onClick={() => inspect(p)}>{t('Inspect current source')}</button>}</li>)}</ol>{error && <p role="alert">{t(error)}</p>}{currentInspection && <article aria-label={t('Current source inspection')}><h4>{currentInspection.title}</h4><p>{t('Source version')}: {currentInspection.versionId}</p><p className="core-prewrap">{currentInspection.text}</p></article>}</section>
}
