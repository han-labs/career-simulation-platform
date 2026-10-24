import { useEffect, useRef, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { kbPath } from './platformApi.js'
export default function SourcePassages({ passages = [], onUnavailable }) {
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
  return <section aria-label={t('Retrieved source passages')}><h3>{t('Retrieved source passages')}</h3>{!passages.length && <p>{t('No authorized passage was retrieved.')}</p>}<ol className="platform-passages">{passages.filter(p => !unavailable.includes(p.chunkId)).map(p => <li key={`${p.versionId}-${p.chunkId}`}><strong>[{p.referenceId}] {p.title}</strong><p>{t('Source version')}: {p.versionId} · {p.section} · {p.retrievalMethod} · {t('Retrieval score')}: {p.score}</p><p className="core-prewrap">{p.text}</p><details><summary>{t('Source locator and profile')}</summary><pre>{JSON.stringify(p.sourceLocator, null, 2)}</pre><code>{p.embeddingProfile}</code></details>{p.retrievalMethod !== 'INSPECTION' && <button type="button" onClick={() => inspect(p)}>{t('Inspect current source')}</button>}</li>)}</ol>{error && <p role="alert">{t(error)}</p>}{currentInspection && <article aria-label={t('Current source inspection')}><h4>{currentInspection.title}</h4><p>{t('Source version')}: {currentInspection.versionId}</p><p className="core-prewrap">{currentInspection.text}</p></article>}</section>
}
