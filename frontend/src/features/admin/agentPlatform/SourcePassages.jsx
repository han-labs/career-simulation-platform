import { useEffect, useRef, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { kbPath } from './platformApi.js'
import Button from '../../../shared/ui/Button.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import StatusBadge from '../../../shared/ui/StatusBadge.jsx'
export default function SourcePassages({ passages = [], onUnavailable, presentation = 'legacy' }) {
  const { t } = useI18n(), [inspected, setInspected] = useState(null), [error, setError] = useState(''), [unavailable, setUnavailable] = useState([])
  const alive = useRef(true), sequence = useRef(0)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  async function inspect(p) {
    const token = ++sequence.current; setInspected(null); setError('')
    try {
      const source = await apiRequest(`${kbPath(p.knowledgeBaseId)}/sources/${p.chunkId}`)
      if (alive.current && token === sequence.current) {
        if (source.versionId !== p.versionId || source.text !== p.text) throw new Error('This knowledge source is no longer available.')
        setInspected(source)
      }
    } catch (failure) {
      if (alive.current && token === sequence.current) { setError(failure.message); setUnavailable(ids => [...ids, p.chunkId]); onUnavailable?.() }
    }
  }
  const currentInspection = inspected && passages.some(p => p.chunkId === inspected.chunkId && p.versionId === inspected.versionId && p.text === inspected.text) ? inspected : null
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
