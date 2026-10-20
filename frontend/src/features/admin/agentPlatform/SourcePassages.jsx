import { useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import { kbPath } from './platformApi.js'
export default function SourcePassages({ passages = [] }) {
  const { t } = useI18n(), [inspected, setInspected] = useState(null), [error, setError] = useState('')
  async function inspect(p) { setInspected(null); setError(''); try { setInspected(await apiRequest(`${kbPath(p.knowledgeBaseId)}/sources/${p.chunkId}`)) } catch (failure) { setError(failure.message) } }
  return <section aria-label={t('Retrieved source passages')}><h3>{t('Retrieved source passages')}</h3>{!passages.length && <p>{t('No authorized passage was retrieved.')}</p>}<ol className="platform-passages">{passages.map(p => <li key={`${p.versionId}-${p.chunkId}`}><strong>[{p.referenceId}] {p.title}</strong><p>{t('Source version')}: {p.versionId} · {p.section} · {p.retrievalMethod} · {t('Retrieval score')}: {p.score}</p><p className="core-prewrap">{p.text}</p><details><summary>{t('Source locator and profile')}</summary><pre>{JSON.stringify(p.sourceLocator, null, 2)}</pre><code>{p.embeddingProfile}</code></details>{p.retrievalMethod !== 'INSPECTION' && <button type="button" onClick={() => inspect(p)}>{t('Inspect current source')}</button>}</li>)}</ol>{error && <p role="alert">{t(error)}</p>}{inspected && <article aria-label={t('Current source inspection')}><h4>{inspected.title}</h4><p>{t('Source version')}: {inspected.versionId}</p><p className="core-prewrap">{inspected.text}</p></article>}</section>
}
