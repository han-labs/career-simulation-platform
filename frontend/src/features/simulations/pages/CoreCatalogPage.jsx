import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import SimulationCatalog from '../components/SimulationCatalog.jsx'

export default function CoreCatalogPage() {
  const { t } = useI18n()
  const [query, setQuery] = useState({ sort: 'ID_ASC' }), [cursor, setCursor] = useState(null)
  const [page, setPage] = useState(null), [error, setError] = useState(''), [retry, setRetry] = useState(0)
  useEffect(() => {
    let active = true
    const params = new URLSearchParams({ paged: 'true', limit: '12', ...query, ...(cursor ? { cursor } : {}) })
    apiRequest(`/v2/scenarios?${params}`).then(data => { if (active) { setPage(data); setError('') } }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [query, cursor, retry])
  function search(e) { e.preventDefault(); setPage(null); setCursor(null); setQuery(Object.fromEntries(new FormData(e.currentTarget))) }
  const simulations = page?.items.map(s => ({ id: s.slug, title: s.metadata.title, description: s.metadata.summary, duration: `${s.metadata.estimatedMinutes} minutes`, difficulty: s.metadata.difficulty, translations: s.metadata.translations })) ?? []
  return <section className="catalog-page section-shell core-flow"><h1>{t('Career simulations')}</h1><Link to="/enterprise">{t('Enterprise workspace')}</Link>
    <form onSubmit={search}><label>{t('Search simulations')}<input name="q" maxLength={200} type="search" /></label><label>{t('Career track')}<input name="careerTrack" maxLength={60} /></label>
      <label>{t('Difficulty')}<select name="difficulty"><option value="">{t('All')}</option>{['INTRODUCTORY', 'INTERMEDIATE', 'ADVANCED'].map(v => <option key={v}>{v}</option>)}</select></label>
      <label>{t('Sort simulations')}<select name="sort"><option value="ID_ASC">{t('Publication order')}</option><option value="TITLE_ASC">{t('Title')}</option><option value="DURATION_ASC">{t('Duration')}</option><option value="NEWEST">{t('Newest first')}</option></select></label><button>{t('Search')}</button>
    </form>
    {error && <p role="alert">{t(error)}</p>}<SimulationCatalog status={error ? 'error' : page ? 'ready' : 'loading'} simulations={simulations} onRetry={() => setRetry(v => v + 1)} />
    {page?.nextCursor && <button onClick={() => { setCursor(page.nextCursor); setPage(null) }}>{t('Next page')}</button>}{cursor && <button onClick={() => { setCursor(null); setPage(null) }}>{t('First page')}</button>}
  </section>
}
